package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>PVP Arena STOP Command class</pre>
 * <p/>
 * A command to stop an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Stop extends AbstractArenaCommand {

    public PAA_Stop() {
        super(new String[]{"pvparena.cmds.stop"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        final boolean force = args.length < 1 || !"soft".equalsIgnoreCase(args[1]);

        arena.stop(force);
        arena.msg(sender, MSG.ARENA_STOP_DONE);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("stop", "forcestop");
    }

    @Override
    public List<String> getShort() {
        return Arrays.asList("!st", "!fs");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"soft"});
        return result;
    }
}
