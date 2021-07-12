package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena ARENALIST Command class</pre>
 * <p/>
 * A command to display the available arenas
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_ArenaList extends AbstractGlobalCommand {

    public PAI_ArenaList() {
        super(new String[]{"pvparena.cmds.arenalist"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0})) {
            return;
        }
        final List<String> names;

        if (!PermissionManager.hasOverridePerm(sender) && ArenaManager.isUsingShortcuts()) {
            names = ArenaManager.getColoredShortcuts();
        } else {
            names = new ArrayList<>();
            for (final Arena a : ArenaManager.getArenasSorted()) {
                names.add((a.isLocked() ? "&c" : PAA_Edit.activeEdits.containsValue(a) || PAA_Setup.activeSetups.containsValue(a) ? "&e" : a.isFightInProgress() ? "&a" : "&f") + a.getName() + "&r");
            }
        }

        Arena.pmsg(sender, Language.parse(MSG.ARENA_LIST, StringParser.joinList(names, ", ")));
    }

    @Override
    public boolean hasVersionForArena() {
        return true;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.ARENALIST));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("list");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-l");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        return new CommandTree<>(null);
    }
}
