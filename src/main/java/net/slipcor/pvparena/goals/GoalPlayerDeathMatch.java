package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.goal.PAGoalEndEvent;
import net.slipcor.pvparena.events.goal.PAGoalPlayerDeathEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "PlayerDeathMatch"
 * </pre>
 * <p/>
 * The first Arena Goal. Players have lives. When every life is lost, the player
 * is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalPlayerDeathMatch extends ArenaGoal {
    public GoalPlayerDeathMatch() {
        super("PlayerDeathMatch");
    }

    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean isFreeForAll() {
        return true;
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH);
    }

    @Override
    public boolean checkEnd() {
        final int count = this.getActivePlayerLifeMap().size();

        return count <= 1; // yep. only one player left. go!
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingFFASpawn(this.arena, spawns);
    }

    @Override
    public Boolean shouldRespawnPlayer(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        return true;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[PDM] already ending");
            return;
        }
        final PAGoalEndEvent gEvent = new PAGoalEndEvent(this.arena, this);
        Bukkit.getPluginManager().callEvent(gEvent);
        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.PLAYER_HAS_WON, ap.getName()),
                        "END");
                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.PLAYER_HAS_WON, ap.getName()),
                        "WINNER");

                this.arena.addWinner(ap.getName());
                this.arena.broadcast(Language.parse(MSG.PLAYER_HAS_WON, ap.getName()));
                if (ArenaModuleManager.commitEnd(this.arena, team, ap)) {
                    return;
                }
            }
        }
        this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final ArenaPlayer arenaPlayer, final boolean doesRespawn, PADeathInfo deathInfo) {

        ArenaPlayer killer = ofNullable(deathInfo.getKiller()).map(ArenaPlayer::fromPlayer).orElse(null);

        if (killer == null || !this.getPlayerLifeMap().containsKey(killer) || arenaPlayer.equals(killer)) { // suicide
            deathInfo.clearKiller();
            final PAGoalPlayerDeathEvent gEvent = new PAGoalPlayerDeathEvent(this.arena, this, arenaPlayer, deathInfo, false);
            Bukkit.getPluginManager().callEvent(gEvent);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastSimpleDeathMessage(arenaPlayer, deathInfo);
            }


            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);

            if (this.arena.getConfig().getBoolean(CFG.USES_SUICIDEPUNISH)) {
                for (ArenaPlayer ap : this.arena.getFighters()) {
                    if (arenaPlayer.equals(ap)) {
                        continue;
                    }
                    if (this.increaseScore(ap, arenaPlayer, deathInfo)) {
                        return;
                    }
                }
            }

        } else {
            int iLives = this.getPlayerLifeMap().get(killer);
            final PAGoalPlayerDeathEvent gEvent = new PAGoalPlayerDeathEvent(this.arena, this, arenaPlayer, deathInfo, false);
            Bukkit.getPluginManager().callEvent(gEvent);

            if (this.increaseScore(killer, arenaPlayer, deathInfo)) {
                return;
            }

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (this.arena.getConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_FRAGS, arenaPlayer, deathInfo, iLives - 1);
                } else {
                    this.broadcastSimpleDeathMessage(arenaPlayer, deathInfo);
                }
            }

            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);
        }
    }

    private boolean increaseScore(ArenaPlayer killer, ArenaPlayer killedPlayer, PADeathInfo deathInfo) {
        int iLives = this.getPlayerLifeMap().get(killer);
        debug(killer, "kills to go: " + iLives);
        if (iLives <= 1) {
            // player has won!
            final Set<ArenaPlayer> arenaPlayers = new HashSet<>();
            for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
                if (arenaPlayer.getName().equals(killer.getName())) {
                    continue;
                }
                arenaPlayers.add(arenaPlayer);
            }
            for (ArenaPlayer arenaPlayer : arenaPlayers) {
                this.getPlayerLifeMap().remove(arenaPlayer);

                arenaPlayer.getStats().incLosses();

                debug(arenaPlayer, "no remaining lives -> LOST");
                arenaPlayer.handleDeathAndLose(deathInfo);

                if (ArenaManager.checkAndCommit(this.arena, false)) {
                    killedPlayer.revive(deathInfo);
                    return true;
                }
            }

            debug(killedPlayer, "no remaining lives -> LOST");
            killedPlayer.handleDeathAndLose(deathInfo);

            WorkflowManager.handleEnd(this.arena, false);
            return true;
        }
        iLives--;
        this.getPlayerLifeMap().put(killer, iLives);
        return false;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + this.arena.getConfig().getInt(CFG.GOAL_PDM_LIVES));
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getPlayerLifeMap().getOrDefault(arenaPlayer, 0);
    }

    @Override
    public void initiate(final ArenaPlayer arenaPlayer) {
        this.updateLives(arenaPlayer, this.arena.getConfig().getInt(CFG.GOAL_PDM_LIVES));
    }


    @Override
    public void lateJoin(final ArenaPlayer arenaPlayer) {
        this.initiate(arenaPlayer);
    }

    @Override
    public void parseLeave(final ArenaPlayer arenaPlayer) {
        if (arenaPlayer == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        this.getPlayerLifeMap().remove(arenaPlayer);
    }

    @Override
    public void parseStart() {
        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                this.updateLives(ap, this.arena.getConfig().getInt(CFG.GOAL_PDM_LIVES));
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.arena.getConfig().getInt(CFG.GOAL_PDM_LIVES)
                    - (this.getPlayerLifeMap()
                    .getOrDefault(arenaPlayer, 0));
            if (scores.containsKey(arenaPlayer.getName())) {
                scores.put(arenaPlayer.getName(), scores.get(arenaPlayer.getName()) + score);
            } else {
                scores.put(arenaPlayer.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final ArenaPlayer arenaPlayer) {
        this.getPlayerLifeMap().remove(arenaPlayer);
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(arenaPlayer);
        }
    }
}
