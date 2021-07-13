package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena RELOAD Command class</pre>
 * <p/>
 * A command to reload an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Reload extends AbstractArenaCommand {

    public static final String CMD_RELOAD_PERM = "pvparena.cmds.reload";
    public static final String RELOAD = "reload";
    public static final String RELOAD_SHORT = "!rl";

    public PAA_Reload() {
        super(new String[]{CMD_RELOAD_PERM});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        final String name = arena.getName();

        ArenaManager.removeArena(arena, false);
        final Arena newArena = new Arena(name);

        if (ArenaManager.loadArena(newArena)) {
            newArena.msg(sender, Language.parse(arena, MSG.RELOAD_DONE));
        }
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.RELOAD));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList(RELOAD);
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList(RELOAD_SHORT);
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
