package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.SpawnManager;

import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "PlayerLives"
 * </pre>
 * <p/>
 * The first Arena Goal. Players have lives. When every life is lost, the player
 * is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalPlayerLives extends AbstractPlayerLivesGoal {

    public GoalPlayerLives() {
        super("PlayerLives");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean isFreeForAll() {
        return true;
    }

    @Override
    public boolean checkEnd() {
        debug(this.arena, "checkEnd - " + this.arena.getName());
        debug(this.arena, () -> "lives: " + StringParser.joinSet(this.getActivePlayerLifeMap().keySet(), "|"));
        final int count = this.getActivePlayerLifeMap().size();
        return (count <= 1); // yep. only one player left. go!
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingFFASpawn(this.arena, spawns);
    }

    @Override
    protected void setWinnerAndBroadcastEndMessages(ArenaTeam teamToCheck) {
        for (ArenaPlayer arenaPlayer : teamToCheck.getTeamMembers()) {
            if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.ANNOUNCE_PLAYER_HAS_WON, arenaPlayer.getName()),
                        "END");
                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.ANNOUNCE_PLAYER_HAS_WON, arenaPlayer.getName()),
                        "WINNER");

                this.arena.broadcast(Language.parse(MSG.ANNOUNCE_PLAYER_HAS_WON, arenaPlayer.getName()));
                this.arena.addWinner(arenaPlayer.getName());
                break;
            }
        }
    }

    @Override
    protected ArenaPlayer getWinningPlayerIfNeeded(ArenaTeam teamToCheck) {
        for (ArenaPlayer arenaPlayer : teamToCheck.getTeamMembers()) {
            if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                return arenaPlayer;
            }
        }
        return null;
    }

    @Override
    protected int getLivesConfigValue() {
        return this.arena.getConfig().getInt(Config.CFG.GOAL_PLIVES_LIVES);
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getPlayerLifeMap().getOrDefault(arenaPlayer, 0);
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaPlayer ap : this.arena.getFighters()) {
            double score = this.getPlayerLifeMap().getOrDefault(ap, 0);
            if (scores.containsKey(ap.getName())) {
                scores.put(ap.getName(), scores.get(ap.getName()) + score);
            } else {
                scores.put(ap.getName(), score);
            }
        }

        return scores;
    }
}
