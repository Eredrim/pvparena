package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * <pre>PVP Arena BLACKLIST Command class</pre>
 * <p/>
 * A command to toggle block blacklist entries
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_BlackList extends AbstractArenaCommand {
    private static final Set<String> SUBCOMMANDS = new HashSet<>();
    private static final Set<String> SUBTYPES = new HashSet<>();

    static {
        SUBCOMMANDS.add("add");
        SUBCOMMANDS.add("remove");
        SUBCOMMANDS.add("show");
        SUBTYPES.add("break");
        SUBTYPES.add("place");
        SUBTYPES.add("use");
    }

    public PAA_BlackList() {
        super(new String[]{"pvparena.cmds.blacklist"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1, 2, 3})) {
            return;
        }

        //                                  args[0]
        // usage: /pa {arenaname} blacklist clear

        if (args.length < 2) {
            if ("clear".equalsIgnoreCase(args[0])) {
                arena.getConfig().set(CFG.LISTS_BLACKLIST, null);
                arena.getConfig().save();
                arena.msg(sender, MSG.BLACKLIST_ALLCLEARED);
                return;
            }
            arena.msg(sender, MSG.BLACKLIST_HELP);
            return;
        }
        if (args.length == 2) {
            // usage: /pa {arenaname} blacklist [type] clear
            if (!SUBTYPES.contains(args[0].toLowerCase())) {
                arena.msg(sender, MSG.ERROR_BLACKLIST_UNKNOWN_TYPE, StringParser.joinSet(SUBTYPES, "|"));
                return;
            }
            if (args[1].equalsIgnoreCase("clear")) {
                arena.getConfig().set(CFG.LISTS_BLACKLIST, null);
                arena.getConfig().save();
                arena.msg(sender, MSG.BLACKLIST_ALLCLEARED);
                return;
            }
            arena.msg(sender, MSG.BLACKLIST_HELP);
            return;
        }

        if (!SUBTYPES.contains(args[0].toLowerCase())) {
            arena.msg(sender, MSG.ERROR_BLACKLIST_UNKNOWN_TYPE, StringParser.joinSet(SUBTYPES, "|"));
            return;
        }

        if (!SUBCOMMANDS.contains(args[1].toLowerCase())) {
            arena.msg(sender, MSG.ERROR_BLACKLIST_UNKNOWN_SUBCOMMAND, StringParser.joinSet(SUBCOMMANDS, "|"));
            return;
        }


        final List<String> list = arena.getConfig().getStringList(
                CFG.LISTS_BLACKLIST.getNode() + '.' + args[0].toLowerCase(), new ArrayList<String>());

        if ("add".equalsIgnoreCase(args[1])) {
            list.add(args[2]);
            arena.msg(sender, MSG.BLACKLIST_ADDED, args[2], args[0].toLowerCase());
        } else if ("show".equalsIgnoreCase(args[1])) {
            final StringBuilder output = new StringBuilder(Language.parse(MSG.BLACKLIST_SHOW, args[0].toLowerCase()));
            for (String s : list) {
                output.append(": ");
                output.append(Material.getMaterial(s).name());
            }
            if (list.size() < 1) {
                output.append(": ---------");
            }
            arena.msg(sender, output.toString());
        } else {
            list.remove(args[2]);
            arena.msg(sender, MSG.BLACKLIST_REMOVED, args[2], args[1]);
        }

        arena.getConfig().setManually(CFG.LISTS_BLACKLIST.getNode() + '.' + args[0].toLowerCase(), list);
        arena.getConfig().save();

    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("blacklist");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!bl");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"clear"});
        for (String main : SUBTYPES) {
            result.define(new String[]{main, "clear"});
            for (String sub : SUBCOMMANDS) {
                result.define(new String[]{main, sub, "{Material}"});
            }
        }

        return result;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.BLACKLIST);
    }
}
