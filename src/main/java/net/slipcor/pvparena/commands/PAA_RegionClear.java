package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * <pre>PVP Arena REGIONCLEAR Command class</pre>
 * <p/>
 * A command to manage arena regions
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_RegionClear extends AbstractArenaCommand {

    public static final Map<String, Arena> activeSelections = new HashMap<>();

    private static String selector;

    public PAA_RegionClear() {
        super(new String[]{"pvparena.cmds.regionclear"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1, 2})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        if (args.length < 1) {
            // usage: /pa {arenaname} regionclear | show region clearance exceptions

            final List<String> list = arena.getConfig().getStringList(CFG.GENERAL_REGIONCLEAREXCEPTIONS.getNode(), new ArrayList<String>());
            arena.msg(sender, MSG.REGION_CLEAR_LIST, StringParser.joinList(list, ", "));
            return;
        }
        if (args.length >= 2) {
            // usage: /pa {arenaname} regionclear [entitytype] {value} | toggle / set an exception

            final List<String> list = arena.getConfig().getStringList(CFG.GENERAL_REGIONCLEAREXCEPTIONS.getNode(), new ArrayList<String>());

            final List<String> valids = new ArrayList<>();

            for (EntityType type : EntityType.values()) {
                if (type.name().equals(args[1].toUpperCase())) {
                    if (!list.contains(type.name()) || args.length>2 && StringParser.positive.contains(args[2])) {
                        list.add(type.name());
                        arena.getConfig().setManually(CFG.GENERAL_REGIONCLEAREXCEPTIONS.getNode(), list);
                        arena.getConfig().save();
                        arena.msg(sender, MSG.REGION_CLEAR_ADDED, type.name());
                        return;
                    }
                    list.remove(type.name());
                    arena.getConfig().setManually(CFG.GENERAL_REGIONCLEAREXCEPTIONS.getNode(), list);
                    arena.getConfig().save();
                    arena.msg(sender, MSG.REGION_CLEAR_REMOVED, type.name());
                    return;
                }
                valids.add(type.name());
            }

            arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], StringParser.joinList(valids, ", "));
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.REGION);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("regionclear");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!rc");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }

        for (EntityType et : EntityType.values()) {
            for (String val : StringParser.positive) {
                result.define(new String[]{et.name(), val});
            }
            for (String val : StringParser.negative) {
                result.define(new String[]{et.name(), val});
            }
        }
        return result;
    }
}
