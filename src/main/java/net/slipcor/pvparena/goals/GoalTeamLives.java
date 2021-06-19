package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "TeamLives"
 * </pre>
 * <p/>
 * The second Arena Goal. Arena Teams have lives. When every life is lost, the
 * team is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalTeamLives extends AbstractTeamKillGoal {
    public GoalTeamLives() {
        super("TeamLives");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    protected int getScore(ArenaTeam team) {
        return this.getTeamLifeMap().getOrDefault(team, 0);
    }

    @Override
    protected int getTeamLivesCfg() {
        return this.arena.getConfig().getInt(CFG.GOAL_TLIVES_LIVES);
    }

    @Override
    public Boolean shouldRespawnPlayer(Player player, PADeathInfo deathInfo) {
        final ArenaTeam respawnTeam = ArenaPlayer.fromPlayer(player).getArenaTeam();

        if (this.getTeamLives(respawnTeam) != null) {
            return true;
        }
        return this.getTeamLives(respawnTeam) > 1;
    }

    @Override
    public void commitPlayerDeath(final Player respawnPlayer, final boolean doesRespawn, PADeathInfo deathInfo) {
        final PAGoalEvent gEvent;
        if (doesRespawn) {
            gEvent = new PAGoalEvent(this.arena, this, "doesRespawn", "playerDeath:" + respawnPlayer.getName());
        } else {
            gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + respawnPlayer.getName());
        }
        Bukkit.getPluginManager().callEvent(gEvent);

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(respawnPlayer);
        ArenaTeam respawnTeam = arenaPlayer.getArenaTeam();
        this.reduceLives(this.arena, respawnTeam);

        if (this.getTeamLives(respawnTeam) != null) {
            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (this.arena.getConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_TEAM, respawnPlayer, deathInfo,
                            this.getTeamLives(respawnTeam));
                } else {
                    this.broadcastSimpleDeathMessage(respawnPlayer, deathInfo);
                }
            }

            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);

        } else {
            debug(arenaPlayer, "no remaining lives -> LOST");
            arenaPlayer.handleDeathAndLose(deathInfo);
        }
    }

    private void reduceLives(final Arena arena, final ArenaTeam arenaTeam) {
        final int iLives = this.getTeamLives(arenaTeam);

        if (iLives <= 1) {
            this.getTeamLifeMap().remove(arenaTeam);
            for (ArenaPlayer ap : arenaTeam.getTeamMembers()) {
                if (ap.getStatus() == PlayerStatus.FIGHT) {
                    ap.setStatus(PlayerStatus.LOST);
                }
            }
            WorkflowManager.handleEnd(arena, false);
            return;
        }

        this.getTeamLifeMap().put(arenaTeam, iLives - 1);
    }

    private Integer getTeamLives(ArenaTeam respawnTeam) {
        return this.getTeamLifeMap().get(respawnTeam);
    }
}
