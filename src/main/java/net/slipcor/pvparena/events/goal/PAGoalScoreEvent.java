package net.slipcor.pvparena.events.goal;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.loadables.ArenaGoal;


public class PAGoalScoreEvent extends PAGoalEvent {

    private final ArenaPlayer arenaPlayer;
    private final ArenaTeam arenaTeam;
    private final Long points;

    /**
     * create an arena goal event
     *
     * @param arena the arena where the event is happening in
     * @param goal  the goal triggering the event
     * @param arenaPlayer player adding score point
     * @param arenaTeam player's team
     * @param points score points added
     */
    public PAGoalScoreEvent(final Arena arena, final ArenaGoal goal, final ArenaPlayer arenaPlayer, final ArenaTeam arenaTeam, final Long points) {
        super(arena, goal);
        this.arenaPlayer = arenaPlayer;
        this.arenaTeam = arenaTeam;
        this.points = points;
    }

    @Override
    public boolean refreshScoreboard() {
        return true;
    }

    public ArenaPlayer getArenaPlayer() {
        return this.arenaPlayer;
    }

    public ArenaTeam getArenaTeam() {
        return this.arenaTeam;
    }

    public Long getPoints() {
        return this.points;
    }
}
