package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena DISABLE Command class</pre>
 * <p/>
 * A command to disable an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Disable extends AbstractArenaCommand {

    public PAA_Disable() {
        super(new String[]{"pvparena.cmds.disable"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        final PAA_Stop cmd = new PAA_Stop();
        cmd.commit(arena, sender, new String[0]);

        arena.getConfig().set(CFG.GENERAL_ENABLED, false);
        arena.getConfig().save();
        arena.setLocked(true);

        arena.msg(sender, MSG.ARENA_DISABLE_DONE);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("disable");
    }

    @Override
    public List<String> getShort() {
        return Arrays.asList("!dis", "!off");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
