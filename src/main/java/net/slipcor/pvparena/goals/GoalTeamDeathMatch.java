package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;

import net.slipcor.pvparena.events.goal.PAGoalPlayerDeathEvent;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import static java.util.Optional.ofNullable;

/**
 * <pre>
 * Arena Goal class "TeamDeathMatch"
 * </pre>
 * <p/>
 * The second Arena Goal. Arena Teams have lives. When every life is lost, the
 * team is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalTeamDeathMatch extends AbstractTeamKillGoal {
    public GoalTeamDeathMatch() {
        super("TeamDeathMatch");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH);
    }

    @Override
    public int getScore(ArenaTeam team) {
        return this.getTeamLivesCfg() - (this.getTeamLifeMap().getOrDefault(team, 0));
    }

    @Override
    protected int getTeamLivesCfg() {
        return this.arena.getConfig().getInt(CFG.GOAL_TDM_LIVES);
    }

    @Override
    public Boolean shouldRespawnPlayer(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        return true;
    }

    @Override
    public void commitPlayerDeath(final ArenaPlayer respawnPlayer, final boolean doesRespawn, PADeathInfo deathInfo) {

        ArenaPlayer killer = ofNullable(deathInfo.getKiller()).map(ArenaPlayer::fromPlayer).orElse(null);
        if (killer == null || respawnPlayer.equals(killer)) {
            if (!this.arena.getConfig().getBoolean(CFG.GOAL_TDM_SUICIDESCORE)) {
                this.broadcastSimpleDeathMessage(respawnPlayer, deathInfo);
                this.respawnPlayer(respawnPlayer);
                final PAGoalPlayerDeathEvent gEvent;
                gEvent = new PAGoalPlayerDeathEvent(this.arena, this, respawnPlayer, deathInfo, doesRespawn);
                Bukkit.getPluginManager().callEvent(gEvent);
                return;
            }
            killer = respawnPlayer;
        }

        final ArenaTeam respawnTeam = respawnPlayer.getArenaTeam();
        final ArenaTeam killerTeam = killer.getArenaTeam();

        if (killerTeam.equals(respawnTeam)) { // suicide
            for (ArenaTeam newKillerTeam : this.arena.getTeams()) {
                if (!newKillerTeam.equals(respawnTeam) && this.reduceLives(newKillerTeam, respawnPlayer, deathInfo, killer)) {
                    this.makePlayerLose(respawnPlayer);
                    return;
                }
            }
        } else if (this.reduceLives(killerTeam, respawnPlayer, deathInfo, killer)) {
            this.makePlayerLose(respawnPlayer);
            return;
        }

        final PAGoalPlayerDeathEvent gEvent = new PAGoalPlayerDeathEvent(this.arena, this, respawnPlayer, deathInfo, false);
        Bukkit.getPluginManager().callEvent(gEvent);

        if (this.getTeamLifeMap().get(killerTeam) != null) {
            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (killerTeam.equals(respawnTeam) || !this.arena.getConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastSimpleDeathMessage(respawnPlayer, deathInfo);
                } else {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_TEAM_FRAGS, respawnPlayer, deathInfo, this.getTeamLifeMap().get(killerTeam));
                }
            }
            this.respawnPlayer(respawnPlayer);
        }

    }

    private void respawnPlayer(ArenaPlayer arenaPlayer) {
        arenaPlayer.setMayDropInventory(true);
        arenaPlayer.setMayRespawn(true);
    }

    private void makePlayerLose(ArenaPlayer respawnPlayer) {
        this.respawnPlayer(respawnPlayer);
        respawnPlayer.setStatus(PlayerStatus.LOST);
    }

    /**
     * @param arenaTeam the killing team
     * @return true if the player should not respawn but be removed
     */
    private boolean reduceLives(ArenaTeam arenaTeam, ArenaPlayer respawnPlayer, PADeathInfo deathInfo, ArenaPlayer killer) {
        final int iLives = this.getTeamLifeMap().get(arenaTeam);

        if (iLives <= 1) {
            for (ArenaTeam otherTeam : this.arena.getNotEmptyTeams()) {
                if (otherTeam.equals(arenaTeam)) {
                    continue;
                }
                this.getTeamLifeMap().remove(otherTeam);
                for (ArenaPlayer arenaPlayer : otherTeam.getTeamMembers()) {
                    if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                        arenaPlayer.setStatus(PlayerStatus.LOST);
                    }
                }
            }
            String deathCause = this.arena.parseDeathCause(respawnPlayer.getPlayer(), deathInfo.getCause(), killer.getPlayer());
            this.arena.broadcast(Language.parse(MSG.FIGHT_KILLED_BY, arenaTeam.colorizePlayer(respawnPlayer) + ChatColor.YELLOW, deathCause));
            WorkflowManager.handleEnd(this.arena, false);
            return true;
        }

        this.getTeamLifeMap().put(arenaTeam, iLives - 1);
        return false;
    }

    @Override
    public void unload(final ArenaPlayer arenaPlayer) {
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(arenaPlayer);
        }
    }
}
