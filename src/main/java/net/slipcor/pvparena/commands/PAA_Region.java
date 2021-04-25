package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.RegionManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import net.slipcor.pvparena.loader.Loadable;
import net.slipcor.pvparena.regionshapes.CuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>PVP Arena REGION Command class</pre>
 * <p/>
 * A command to manage arena regions
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Region extends AbstractArenaCommand {

    public static final Map<String, Arena> activeSelections = new HashMap<>();

    private static String selector;

    public PAA_Region() {
        super(new String[]{"pvparena.cmds.region"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1, 2, 3})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        if (args.length < 1) {
            // usage: /pa {arenaname} region | activate region selection

            if (activeSelections.get(sender.getName()) != null) {
                // already selecting!
                if (sender.getName().equals(selector)) {
                    arena.msg(sender, MSG.ERROR_REGION_YOUSELECTEXIT);
                    selector = null;
                    activeSelections.remove(sender.getName());
                } else {
                    arena.msg(sender, MSG.ERROR_REGION_YOUSELECT, arena.getName());
                    arena.msg(sender, MSG.ERROR_REGION_YOUSELECT2);
                    selector = sender.getName();
                }
                return;
            }
            // selecting now!
            activeSelections.put(sender.getName(), arena);
            arena.msg(sender, MSG.REGION_YOUSELECT, arena.getName());
            arena.msg(sender, MSG.REGION_SELECT, arena.getName());
            return;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("border")) {
            // usage: /pa {arenaname} region [regionname] border | check a region border
            final ArenaRegion region = arena.getRegion(args[0]);

            if (region == null) {
                arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[0]);
                return;
            }
            region.getShape().showBorder((Player) sender);
            return;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("remove")) {
            // usage: /pa {arenaname} region [regionname] remove | remove a region
            final ArenaRegion region = arena.getRegion(args[0]);

            if (region == null) {
                arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[0]);
                return;
            }
            arena.getConfig().setManually("arenaregion." + region.getRegionName(), null);
            arena.msg(sender, MSG.REGION_REMOVED, region.getRegionName());

            arena.getRegions().remove(region);
            arena.getConfig().save();
            RegionManager.getInstance().reloadCache();
            return;
        }
        if (args.length < 3) {
            // usage: /pa {arenaname} region [regionname] {regionshape} | save selected region

            final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer((Player) sender);

            if (!aPlayer.didValidSelection()) {
                arena.msg(sender, MSG.REGION_SELECT, arena.getName());
                return;
            }

            final PABlockLocation[] locs = aPlayer.getSelection();
            ArenaRegionShape shape;

            if (args.length == 2) {
                shape = PVPArena.getInstance().getArsm().getNewInstance(args[1]);
            } else {
                shape = new CuboidRegion();
            }

            if (shape == null) {
                arena.msg(sender, MSG.ARENA_REGION_SHAPE_UNKNOWN, args[1]);
                return;
            }

            final ArenaRegion region = new ArenaRegion(arena, args[0], shape, locs);

            if (!region.getShape().hasVolume()) {
                arena.msg(sender, MSG.ERROR_REGION_INVALID);
                return;
            }

            region.saveToConfig();

            activeSelections.remove(sender.getName());

            aPlayer.unsetSelection();

            arena.msg(sender, MSG.REGION_SAVED, args[0]);

            arena.msg(sender, MSG.REGION_SAVED_NOTICE, arena.getName(), args[0]);

            return;
        }

        final ArenaRegion region = arena.getRegion(args[0]);

        if (region == null) {
            arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[0]);
            return;
        }

        final String message = region.update(args[1], args[2]);

        if (message != null) {
            if (arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE)) {
                Bukkit.getServer().dispatchCommand(sender, "pvparena " + arena.getName() + " regsave " + region.getRegionName());
            }
            arena.msg(sender, message);
        }

        // usage: /pa {arenaname} region [regionname] radius [number]
        // usage: /pa {arenaname} region [regionname] height [number]
        // usage: /pa {arenaname} region [regionname] position [position]


        // #region name can be anything you want
        // #radius should be clear
        // #height is not needed / parsed for spheric regions
        // #position is the alignment to the battlefield
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
        return Collections.singletonList("region");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!r");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }

        for (ArenaRegion region : arena.getRegions()) {
            for (Loadable<?> shapeLoadable : PVPArena.getInstance().getArsm().getAllLoadables()) {
                result.define(new String[]{region.getRegionName(), shapeLoadable.getName()});
                result.define(new String[]{region.getRegionName(), "border"});
                result.define(new String[]{region.getRegionName(), "remove"});
            }
        }
        return result;
    }
}
