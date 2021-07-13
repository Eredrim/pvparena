package net.slipcor.pvparena.events.goal;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.loadables.ArenaGoal;

public class PAGoalEndEvent extends PAGoalEvent {

    /**
     * create an arena goal event
     *
     * @param arena the arena where the event is happening in
     * @param goal  the goal triggering the event
     */
    public PAGoalEndEvent(final Arena arena, final ArenaGoal goal) {
        super(arena, goal);
    }

    @Override
    public boolean refreshScoreboard() {
        return false;
    }
}
