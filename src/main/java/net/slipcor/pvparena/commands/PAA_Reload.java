package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.RegionManager;
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

    public PAA_Reload() {
        super(new String[]{"pvparena.cmds.reload"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        final String name = arena.getName();

        ArenaManager.removeArena(arena, false);
        final Arena newArena = new Arena(name);

        if (ArenaManager.loadArena(newArena)) {
            RegionManager.getInstance().reloadCache();
            newArena.msg(sender, MSG.RELOAD_DONE, arena.getName());
        } else {
            newArena.msg(sender, MSG.RELOAD_FAILED, arena.getName());
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.RELOAD);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("reload");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!rl");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}