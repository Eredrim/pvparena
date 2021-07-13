package net.slipcor.pvparena.events.goal;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.loadables.ArenaGoal;


public class PAGoalPlayerDeathEvent extends PAGoalEvent {

    private final ArenaPlayer arenaPlayer;
    private final ArenaPlayer killer;
    private final boolean respawning;

    /**
     * create an arena goal event
     *
     * @param arena  the arena where the event is happening in
     * @param arenaPlayer arenaPlayer
     * @param respawning does player respawn
     * @param goal the goal triggering the event
     */
    public PAGoalPlayerDeathEvent(final Arena arena, final ArenaGoal goal, final ArenaPlayer arenaPlayer, final ArenaPlayer killer, final boolean respawning) {
        super(arena, goal);
        this.arenaPlayer = arenaPlayer;
        this.killer = killer;
        this.respawning = respawning;
    }

    @Override
    public boolean refreshScoreboard() {
        return true;
    }

    public ArenaPlayer getArenaPlayer() {
        return this.arenaPlayer;
    }

    public ArenaPlayer getKiller() {
        return this.killer;
    }

    public boolean isRespawning() {
        return this.respawning;
    }
}
