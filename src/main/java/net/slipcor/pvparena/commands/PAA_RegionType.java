package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import net.slipcor.pvparena.managers.RegionManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena REGIONTYPE Command class</pre>
 * <p/>
 * A command to set an arena region type
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_RegionType extends AbstractArenaCommand {

    public PAA_RegionType() {
        super(new String[]{"pvparena.cmds.regiontype"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{2})) {
            return;
        }

        final ArenaRegion region = arena.getRegion(args[0]);

        if (region == null) {
            arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[0]);
            return;
        }

        final RegionType regionType;

        try {
            regionType = RegionType.valueOf(args[1].toUpperCase());
        } catch (final Exception e) {
            arena.msg(sender, MSG.ERROR_REGION_TYPE_NOTFOUND, args[1], StringParser.joinArray(RegionType.values(), " "));
            return;
        }

        region.setType(regionType);
        if (regionType == RegionType.BATTLE) {
            region.protectionSetAll(true);
        } else if (regionType == RegionType.JOIN) {
            RegionManager.getInstance().reloadCache();
        }
        region.saveToConfig();
        arena.msg(sender, MSG.REGION_TYPE_SET, regionType.name());

    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.REGIONTYPE);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("regiontype");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!rt");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }
        for (ArenaRegion region : arena.getRegions()) {
            result.define(new String[]{region.getRegionName(), "{RegionType}"});
        }
        return result;
    }
}
