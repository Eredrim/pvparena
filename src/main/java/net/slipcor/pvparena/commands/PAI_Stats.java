package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.StatisticsManager;
import net.slipcor.pvparena.managers.StatisticsManager.Type;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * <pre>PVP Arena STATS Command class</pre>
 * <p/>
 * A command to display the player statistics
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_Stats extends AbstractArenaCommand {

    public PAI_Stats() {
        super(new String[]{"pvparena.user", "pvparena.cmds.stats"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1, 2})) {
            return;
        }

        final Type statType = Type.getByString(args[0]);

        if (statType == null) {
            Arena.pmsg(sender, MSG.STATS_TYPENOTFOUND, StringParser.joinArray(Type.values(), ", ").replace("NULL, ", ""));
            return;
        }

        Map<String, Integer> playersStats = StatisticsManager.getStats(arena, statType);

        int max = 10;

        if (args.length > 1) {
            try {
                max = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        final String s2 = Language.parse(MSG.getByName("STATTYPE_" + statType.name()));

        final String s1 = Language.parse(MSG.STATS_HEAD, String.valueOf(max), s2);


        Arena.pmsg(sender, s1);

        playersStats.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(max)
                .forEach(stat -> Arena.pmsg(sender, stat.getKey() + " : " + stat.getValue()));
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.STATS);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("stats");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-s");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        for (Type val : Type.values()) {
            result.define(new String[]{val.name()});
        }
        return result;
    }
}
