package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.loader.Loadable;
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
        super(new String[]{"pvparena.user", "pvparena.cmds.version"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0})) {
            return;
        }

        Arena.pmsg(sender, String.format("%s%s-- PVP Arena version information --", ChatColor.YELLOW, ChatColor.UNDERLINE));
        Arena.pmsg(sender, String.format("%sPVP Arena version: %s%s", ChatColor.YELLOW, ChatColor.BOLD, PVPArena.getInstance().getDescription().getVersion()));
        if (args.length < 2 || args[1].toLowerCase().startsWith("goal")) {
            Arena.pmsg(sender, String.format("%s-----------------------------------", ChatColor.GRAY));
            Arena.pmsg(sender, String.format("%sArena Goals:", ChatColor.RED));
            for (Loadable<?> ag : PVPArena.getInstance().getAgm().getAllLoadables()) {
                Arena.pmsg(sender, String.format("%s%s - %s", ChatColor.RED, ag.getName(), ag.getVersion()));
            }
        }
        if (args.length < 2 || args[1].toLowerCase().startsWith("mod")) {
            Arena.pmsg(sender, String.format("%s-----------------------------------", ChatColor.GRAY));
            Arena.pmsg(sender, String.format("%sMods:", ChatColor.GREEN));
            for (Loadable<?> am : PVPArena.getInstance().getAmm().getAllLoadables()) {
                Arena.pmsg(sender, String.format("%s%s - %s", ChatColor.GREEN, am.getName(), am.getVersion()));
            }
        }
        if (args.length < 2 || args[1].toLowerCase().startsWith("reg")) {
            Arena.pmsg(sender, String.format("%s-----------------------------------", ChatColor.GRAY));
            Arena.pmsg(sender, String.format("%sRegionshapes:", ChatColor.GREEN));
            for (Loadable<?> arsLoadable : PVPArena.getInstance().getArsm().getAllLoadables()) {
                Arena.pmsg(sender, String.format("%s%s - %s", ChatColor.GREEN, arsLoadable.getName(), arsLoadable.getVersion()));
            }
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.VERSION);
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
