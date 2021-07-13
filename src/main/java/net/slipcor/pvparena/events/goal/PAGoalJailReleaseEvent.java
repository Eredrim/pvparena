package net.slipcor.pvparena.events.goal;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.loadables.ArenaGoal;


public class PAGoalJailReleaseEvent extends PAGoalEvent {

    private final ArenaPlayer arenaPlayer;
    private final ArenaTeam prisonerTeam;

    /**
     * create an arena goal event
     *
     * @param arena the arena where the event is happening in
     * @param goal  the goal triggering the event
     * @param arenaPlayer player adding score point
     * @param prisonerTeam flag's team
     */
    public PAGoalJailReleaseEvent(final Arena arena, final ArenaGoal goal, final ArenaPlayer arenaPlayer, final ArenaTeam prisonerTeam) {
        super(arena, goal);
        this.arenaPlayer = arenaPlayer;
        this.prisonerTeam = prisonerTeam;
    }

    @Override
    public boolean refreshScoreboard() {
        return true;
    }

    public ArenaPlayer getArenaPlayer() {
        return this.arenaPlayer;
    }

    public ArenaTeam getPrisonerTeam() {
        return this.prisonerTeam;
    }
}
