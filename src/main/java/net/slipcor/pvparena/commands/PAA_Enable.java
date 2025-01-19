package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena ENABLE Command class</pre>
 * <p/>
 * A command to enable an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Enable extends AbstractArenaCommand {

    public PAA_Enable() {
        super(new String[]{"pvparena.cmds.enable"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        if (!ArenaManager.loadArena(arena)) {
            arena.msg(sender, MSG.ARENA_ENABLE_FAIL);
            return;
        }

        arena.getConfig().set(CFG.GENERAL_ENABLED, true);
        arena.getConfig().save();
        arena.setLocked(false);

        arena.msg(sender, MSG.ARENA_ENABLE_DONE);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("enable");
    }

    @Override
    public List<String> getShort() {
        return Arrays.asList("!en", "!on");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
