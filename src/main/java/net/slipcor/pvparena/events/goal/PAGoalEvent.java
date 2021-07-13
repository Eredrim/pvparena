package net.slipcor.pvparena.events.goal;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.loadables.ArenaGoal;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class PAGoalEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final ArenaGoal goal;

    /**
     * create an arena goal event
     *
     * @param arena  the arena where the event is happening in
     * @param goal the goal triggering the event
     */
    PAGoalEvent(final Arena arena, final ArenaGoal goal) {
        this.arena = arena;
        this.goal = goal;
    }

    /**
     * Should this event refresh scoreboard when caught ?
     *
     * @return true if scoreboard must be refreshed
     */
    public abstract boolean refreshScoreboard();

    /**
     * hand over the arena instance
     *
     * @return the arena the event is happening in
     */
    public Arena getArena() {
        return this.arena;
    }

    public ArenaGoal getGoal() {
        return this.goal;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Mandatory for spigot
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
