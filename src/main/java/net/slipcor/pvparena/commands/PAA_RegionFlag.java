package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionFlag;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena REGIONFLAG Command class</pre>
 * <p/>
 * A command to set an arena region flag
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_RegionFlag extends AbstractArenaCommand {

    public PAA_RegionFlag() {
        super(new String[]{"pvparena.cmds.regionflag"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{2, 3})) {
            return;
        }

        final ArenaRegion region = arena.getRegion(args[0]);

        if (region == null) {
            arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[0]);
            return;
        }

        final RegionFlag regionFlag;

        try {
            regionFlag = RegionFlag.valueOf(args[1].toUpperCase());
        } catch (final Exception e) {
            arena.msg(sender, MSG.ERROR_REGION_FLAG_NOTFOUND, args[1], StringParser.joinArray(RegionFlag.values(), " "));
            return;
        }

        if (args.length < 3) {
            // toggle
            if (region.flagToggle(regionFlag)) {
                arena.msg(sender, MSG.REGION_FLAG_ADDED, args[1]);
            } else {
                arena.msg(sender, MSG.REGION_FLAG_REMOVED, args[1]);
            }
            region.saveToConfig();
            return;
        }

        if (StringParser.isPositiveValue(args[2])) {
            region.flagAdd(regionFlag);
            region.saveToConfig();
            arena.msg(sender, MSG.REGION_FLAG_ADDED, args[1]);
            return;
        }

        if (StringParser.isNegativeValue(args[2])) {
            region.flagRemove(regionFlag);
            region.saveToConfig();
            arena.msg(sender, MSG.REGION_FLAG_REMOVED, args[1]);
            return;
        }

        // usage: /pa {arenaname} regionflag [regionname] [regionflag] {value}

        arena.msg(sender, MSG.ERROR_INVALID_VALUE, args[2]);
        arena.msg(sender, MSG.ERROR_POSITIVES, StringParser.joinSet(StringParser.positive, " | "));
        arena.msg(sender, MSG.ERROR_NEGATIVES, StringParser.joinSet(StringParser.negative, " | "));
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.REGIONFLAG);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("regionflag");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!rf");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }
        for (ArenaRegion region : arena.getRegions()) {
            result.define(new String[]{region.getRegionName(), "{RegionFlag}", "{Boolean}"});
        }
        return result;
    }
}
