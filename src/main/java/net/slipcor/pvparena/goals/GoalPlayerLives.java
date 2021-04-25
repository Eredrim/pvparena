package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
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
        debug(this.arena, "lives: " + StringParser.joinSet(this.getPlayerLifeMap().keySet(), "|"));
        final int count = this.getPlayerLifeMap().size();
        return (count <= 1); // yep. only one player left. go!
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingFFASpawn(this.arena, spawns);
    }

    @Override
    protected void broadcastEndMessagesIfNeeded(ArenaTeam teamToCheck) {
        for (ArenaPlayer arenaPlayer : teamToCheck.getTeamMembers()) {
            if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                        "END");
                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                        "WINNER");

                this.arena.broadcast(Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()));
            }
        }
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaPlayer ap : this.arena.getFighters()) {
            double score = this.getPlayerLifeMap().getOrDefault(ap.getPlayer(), 0);
            if (scores.containsKey(ap.getName())) {
                scores.put(ap.getName(), scores.get(ap.getName()) + score);
            } else {
                scores.put(ap.getName(), score);
            }
        }

        return scores;
    }
}
