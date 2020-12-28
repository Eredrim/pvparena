package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.runnables.TimedEndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;

/**
 * <pre>Arena Timer</pre>
 * <p/>
 * Time is ticking ^^
 */
public class ArenaTimer {

    protected Arena arena;
    private TimedEndRunnable ter;

    public ArenaTimer(Arena arena) {
        this.arena = arena;
    }

    public void start() {
        final int timed = arena.getArenaConfig().getInt(CFG.GENERAL_TIME);
        if (timed > 0) {
            PVPArena.getInstance().getLogger().info("arena timing!");
            // initiate arena timer
            ter = new TimedEndRunnable(arena, timed);
        }
    }

    public void stop() {
        if (ter == null) {
            PVPArena.getInstance().getLogger().info("timer already stopped");
            return;
        }

        class EndTimerRunner implements Runnable {
            @Override
            public void run() {
                ter.commit();
            }
        }

        PVPArena.getInstance().getLogger().info("Stopping arena end timer");
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new EndTimerRunner(), 1L);
        } catch (IllegalPluginAccessException ex) {
            PVPArena.getInstance().getLogger().warning(ex.getMessage());
        }
    }
}
