package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena VERSION Command class</pre>
 * <p/>
 * A command to display the plugin and module versions
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_Version extends AbstractGlobalCommand {

    public PAI_Version() {
        super(new String[]{"pvparena.cmds.version"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0})) {
            return;
        }

        Arena.pmsg(sender, ChatColor.COLOR_CHAR + "e" + ChatColor.COLOR_CHAR + "n-- PVP Arena version information --");
        Arena.pmsg(sender, ChatColor.COLOR_CHAR + "ePVP Arena version: " + ChatColor.COLOR_CHAR + 'l' + PVPArena.instance.getDescription().getVersion());
        if (args.length < 2 || args[1].toLowerCase().startsWith("goal")) {
            Arena.pmsg(sender, ChatColor.COLOR_CHAR + "7-----------------------------------");
            Arena.pmsg(sender, ChatColor.COLOR_CHAR + "cArena Goals:");
            for (final ArenaGoal ag : PVPArena.instance.getAgm().getAllGoals()) {
                Arena.pmsg(sender, ChatColor.COLOR_CHAR + "c" + ag.getName() + " - " + ag.version());
            }
        }
        if (args.length < 2 || args[1].toLowerCase().startsWith("mod")) {
            Arena.pmsg(sender, ChatColor.COLOR_CHAR + "7-----------------------------------");
            Arena.pmsg(sender, ChatColor.COLOR_CHAR + "aMods:");
            for (final ArenaModule am : PVPArena.instance.getAmm().getAllMods()) {
                Arena.pmsg(sender, ChatColor.COLOR_CHAR + "a" + am.getName() + " - " + am.version());
            }
        }
        if (args.length < 2 || args[1].toLowerCase().startsWith("reg")) {
            Arena.pmsg(sender, ChatColor.COLOR_CHAR + "7-----------------------------------");
            Arena.pmsg(sender, ChatColor.COLOR_CHAR + "aRegionshapes:");
            for (final ArenaRegionShape ars : PVPArena.instance.getArsm().getRegions()) {
                Arena.pmsg(sender, ChatColor.COLOR_CHAR + "a" + ars.getName() + " - " + ars.version());
            }
        }
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.VERSION));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("version");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-v");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        return new CommandTree<>(null);
    }
}
