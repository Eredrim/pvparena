package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        return this.arena.getConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public boolean checkEnd() {
        final int count = this.getPlayerLifeMap().size();

        return count <= 1; // yep. only one player left. go!
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingTeamSpawn(this.arena, spawns);
    }

    @Override
    public Boolean shouldRespawnPlayer(Player player, PADeathInfo deathInfo) {
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
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
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

                this.arena.broadcast(Language.parse(MSG.PLAYER_HAS_WON, ap.getName()));
            }
            if (ArenaModuleManager.commitEnd(this.arena, team)) {
                return;
            }
        }
        this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn, PADeathInfo deathInfo) {

        Player killer = deathInfo.getKiller();

        if (killer == null || !this.getPlayerLifeMap().containsKey(killer) || player.equals(killer)) {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerKill:" + player.getName() + ':' + player.getName(), "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastSimpleDeathMessage(player, deathInfo);
            }

            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);

            if (this.arena.getConfig().getBoolean(CFG.USES_SUICIDEPUNISH)) {
                for (ArenaPlayer ap : this.arena.getFighters()) {
                    if (player.equals(ap.getPlayer())) {
                        continue;
                    }
                    if (this.increaseScore(ap.getPlayer(), player, deathInfo)) {
                        return;
                    }
                }
            }

            return;
        }

        int iLives = this.getPlayerLifeMap().get(killer);
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerKill:" + killer.getName() + ':' + player.getName(), "playerDeath:" + player.getName());
        Bukkit.getPluginManager().callEvent(gEvent);

        if (this.increaseScore(killer, player, deathInfo)) {
            return;
        }

        if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            if (this.arena.getConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_FRAGS, player, deathInfo, iLives-1);
            } else {
                this.broadcastSimpleDeathMessage(player, deathInfo);
            }
        }

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setMayDropInventory(true);
        arenaPlayer.setMayRespawn(true);
    }

    private boolean increaseScore(Player killer, Player killed, PADeathInfo deathInfo) {
        int iLives = this.getPlayerLifeMap().get(killer);
        debug(this.arena, killer, "kills to go: " + iLives);
        if (iLives <= 1) {
            ArenaPlayer killedArenaPlayer = ArenaPlayer.fromPlayer(killed);

            // player has won!
            final Set<ArenaPlayer> arenaPlayers = new HashSet<>();
            for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
                if (arenaPlayer.getName().equals(killer.getName())) {
                    continue;
                }
                arenaPlayers.add(arenaPlayer);
            }
            for (ArenaPlayer arenaPlayer : arenaPlayers) {
                this.getPlayerLifeMap().remove(arenaPlayer.getPlayer());

                arenaPlayer.addLosses();

                debug(arenaPlayer, "no remaining lives -> LOST");
                arenaPlayer.handleDeathAndLose(deathInfo);

                if (ArenaManager.checkAndCommit(this.arena, false)) {
                    killedArenaPlayer.revive(deathInfo);
                    return true;
                }
            }

            debug(killedArenaPlayer, "no remaining lives -> LOST");
            killedArenaPlayer.handleDeathAndLose(deathInfo);

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
        return this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
    }

    @Override
    public void initiate(final Player player) {
        this.updateLives(player, this.arena.getConfig().getInt(CFG.GOAL_PDM_LIVES));
    }



    @Override
    public void lateJoin(final Player player) {
        this.initiate(player);
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        this.getPlayerLifeMap().remove(player);
    }

    @Override
    public void parseStart() {
        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                this.updateLives(ap.getPlayer(), this.arena.getConfig().getInt(CFG.GOAL_PDM_LIVES));
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
                    .getOrDefault(arenaPlayer.getPlayer(), 0));
            if (scores.containsKey(arenaPlayer.getName())) {
                scores.put(arenaPlayer.getName(), scores.get(arenaPlayer.getName()) + score);
            } else {
                scores.put(arenaPlayer.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final Player player) {
        this.getPlayerLifeMap().remove(player);
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.fromPlayer(player));
        }
    }
}
