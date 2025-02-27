package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import net.slipcor.pvparena.loader.Loadable;
import net.slipcor.pvparena.managers.RegionManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regionshapes.CuboidRegion;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.slipcor.pvparena.core.CollectionUtils.containsIgnoreCase;
import static org.bukkit.block.BlockFace.*;

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

    private static final List<BlockFace> DIRECTIONS = List.of(UP, DOWN, NORTH, SOUTH, EAST, WEST);

    private static String selector;

    public PAA_Region() {
        super(new String[]{"pvparena.cmds.region"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1, 2, 3, 4})) {
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

        } else if (args.length == 2 && args[1].equalsIgnoreCase("border")) {
            // usage: /pa {arenaname} region [regionname] border | check a region border
            final ArenaRegion region = arena.getRegion(args[0]);

            if (region == null) {
                arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[0]);
                return;
            }
            region.getShape().showBorder((Player) sender);

        } else if (args.length == 2 && args[1].equalsIgnoreCase("remove")) {
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

        } else if (args.length < 3) {
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
                arena.msg(sender, MSG.REGION_SHAPE_UNKNOWN, args[1]);
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

        } else if (containsIgnoreCase(List.of("shift", "expand", "contract"), args[1])) {
            final ArenaRegion region = arena.getRegion(args[0]);

            if (region == null) {
                arena.msg(sender, MSG.ERROR_REGION_NOTFOUND, args[0]);
                return;
            }

            final int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (final Exception e) {
                arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[2]);
                return;
            }

            BlockFace direction;
            Player player = (Player) sender;

            if (args.length == 4 && DIRECTIONS.stream().anyMatch(dir -> dir.name().equalsIgnoreCase(args[3]))) {
                direction = BlockFace.valueOf(args[3].toUpperCase());
            } else {
                BlockFace playerFacing = player.getFacing();
                if (player.getFacing().isCartesian()) {
                    direction = playerFacing;

                    float pitch = player.getLocation().getPitch();
                    if (pitch > 70) {
                        direction = DOWN;
                    } else if (pitch < -70) {
                        direction = UP;
                    }
                } else {
                    arena.msg(sender, MSG.ERROR_INVALID_VALUE, playerFacing.name());
                    return;
                }
            }

            if("shift".equalsIgnoreCase(args[1])) {
                region.getShape().move(direction, amount);
                arena.msg(sender, MSG.REGION_SHIFTED, amount, direction);
            } else if ("expand".equalsIgnoreCase(args[1])) {
                region.getShape().extend(direction, amount);
                arena.msg(sender, MSG.REGION_EXPANDED, amount, direction);
            } else if ("contract".equalsIgnoreCase(args[1])) {
                region.getShape().extend(direction, amount * -1);
                arena.msg(sender, MSG.REGION_CONTACTED, amount, direction);
            }

            region.getShape().showBorder(player);
            region.saveToConfig();

            // Save modified region if worldedit autosave is set
            if (arena.getConfig().getBoolean(CFG.MODULES_WORLDEDIT_AUTOSAVE)) {
                arena.getMods().stream()
                                .filter(mod -> mod.getName().equals("WorldEdit"))
                                .findAny()
                                .ifPresent(mod -> mod.commitCommand(sender, new String[]{"!we", "save", region.getRegionName()}));
            }

        } else {
            arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "border, remove, shift, expand, contract");
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
            }
            result.define(new String[]{region.getRegionName(), "border"});
            result.define(new String[]{region.getRegionName(), "remove"});

            DIRECTIONS.forEach(direction -> {
                result.define(new String[]{region.getRegionName(), "shift", "{int}", direction.name()});
                result.define(new String[]{region.getRegionName(), "expand", "{int}", direction.name()});
                result.define(new String[]{region.getRegionName(), "contract", "{int}", direction.name()});
            });
        }
        return result;
    }
}
