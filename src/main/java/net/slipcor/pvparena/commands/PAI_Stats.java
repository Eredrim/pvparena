package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.StatisticsManager;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.bukkit.command.CommandSender;

import java.util.Collections;
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
        super(new String[]{"pvparena.cmds.stats"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1, 2})) {
            return;
        }

        final StatEntry statType = StatEntry.parse(args[0]);

        if (statType == null) {
            Arena.pmsg(sender, MSG.STATS_TYPENOTFOUND, StringParser.joinArray(StatEntry.getStatTypes(), ", "));
            return;
        }

        long max = 10;

        if (args.length > 1) {
            try {
                max = Long.parseLong(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        Map<String, Long> playersStats = StatisticsManager.getStats(arena, statType, max);

        final String s2 = Language.parse(MSG.getByName("STATTYPE_" + statType.name()));

        final String s1 = Language.parse(MSG.STATS_HEAD, String.valueOf(max), s2);


        Arena.pmsg(sender, s1);

        playersStats.forEach((key, value) -> Arena.pmsg(sender, key + " : " + value));
    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
        for (StatEntry val : StatEntry.getStatTypes()) {
            result.define(new String[]{val.name()});
        }
        return result;
    }
}
