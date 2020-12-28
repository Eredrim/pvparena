package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language.MSG;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Runnable class "TimedEnd"</pre>
 * <p/>
 * An arena timer to end the arena match after a certain amount of time
 */

public class TimedEndRunnable extends ArenaRunnable {

    /**
     * create a timed arena runnable
     *
     * @param arena    the arena we are running in
     */
    public TimedEndRunnable(final Arena arena, final int seconds) {
        super(MSG.TIMER_ENDING_IN.getNode(), seconds, null, arena, false);
        debug(arena, "TimedEndRunnable constructor arena");
        arena.endRunner = this;
    }

    @Override
    public void commit() {
        debug(this.arena, "TimedEndRunnable commiting");
        if (this.arena.isFightInProgress()) {
            PVPArena.getInstance().getAgm().timedEnd(this.arena);
        }
        this.arena.endRunner = null;
        if (this.arena.realEndRunner != null) {
            this.arena.realEndRunner.cancel();
            this.arena.realEndRunner = null;
        }
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("TimedEndRunnable not scheduled yet!");
    }
}
