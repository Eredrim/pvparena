package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Language.MSG;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Runnable class "Start"</pre>
 * <p/>
 * An arena timer to start the arena
 *
 * @author slipcor
 * @version v0.9.8
 */

public class StartRunnable extends ArenaRunnable {
//	private final static Debugger debug = Debugger.getInstance();

    /**
     * create a timed arena start runnable
     *
     * @param arena the arena we are running in
     */
    public StartRunnable(final Arena arena, final int seconds) {
        super(MSG.ARENA_STARTING_IN.getNode(), seconds, null, arena, false);
        debug(arena, "StartRunnable constructor");
        arena.startRunner = this;
        for (ArenaPlayer player : arena.getFighters()) {
            if (player.getStatus() != PlayerStatus.READY) {
                player.setStatus(PlayerStatus.READY);
            }
        }
    }

    @Override
    protected void commit() {
        this.arena.startRunner = null;
        debug(this.arena, "StartRunnable commiting");
        this.arena.start();
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("StartRunnable not scheduled yet!");
    }
}
