package net.slipcor.pvparena.listeners;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.runnables.DamageResetRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Fire;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Block Listener class
 * </pre>
 *
 * @author slipcor
 * @version v0.10.2
 */

public class BlockListener implements Listener {

    private boolean willBeSkipped(final Event event, final Location loc, final RegionProtection rp) {
        Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(loc));

        if (arena == null) {
            // no arena at all
            return true;
        }

        if (arena.isLocked() || !arena.isFightInProgress()) {
            if (event instanceof Cancellable) {
                final Cancellable cEvent = (Cancellable) event;
                cEvent.setCancelled(!PAA_Edit.activeEdits.containsValue(arena));
            }
            return PAA_Edit.activeEdits.containsValue(arena);
        }

        if (!arena.hasRegionsProtectingLocation(loc, rp)) {
            return false;
        }

        return PAA_Edit.activeEdits.containsValue(arena);
    }

    static boolean isProtected(Arena arena, Location loc, Cancellable event, RegionProtection node) {
        if (arena != null && arena.hasRegionsProtectingLocation(loc, node)) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        debug(event.getPlayer(), "onBlockBreak");
        if (this.willBeSkipped(event, event.getBlock().getLocation(), RegionProtection.BREAK)) {
            debug(event.getPlayer(), "willbeskipped. GFYS!!!!");
            return;
        }

        if (ArenaPlayer.fromPlayer(event.getPlayer().getName()).getStatus() == PlayerStatus.LOST
                || ArenaPlayer.fromPlayer(event.getPlayer().getName()).getStatus() == PlayerStatus.WATCH
                || ArenaPlayer.fromPlayer(event.getPlayer().getName()).getStatus() == PlayerStatus.LOUNGE
                || ArenaPlayer.fromPlayer(event.getPlayer().getName()).getStatus() == PlayerStatus.READY) {
            event.setCancelled(true);
            return;
        }

        Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(event.getBlock().getLocation()));
        try {
            arena.getGoal().checkBreak(event);
        } catch (GameplayException e) {
            debug(event.getPlayer(), "onBlockBreak cancelled by goal: {}", arena.getGoal().getName());
            return;
        }

        List<String> list = arena.getConfig().getStringList(CFG.LISTS_WHITELIST.getNode() + ".break", new ArrayList<>());

        if (!list.isEmpty() && !list.contains(event.getBlock().getType().name())) {
            arena.msg(event.getPlayer(), MSG.ERROR_WHITELIST_DISALLOWED, Language.parse(MSG.PERMISSION_BREAK));
            // not on whitelist. DENY!
            event.setCancelled(true);
            debug(event.getPlayer(), "whitelist out");
            return;
        }

        if (isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.BREAK)) {
            debug(event.getPlayer(), "isprotected!");
            return;
        }

        list.clear();
        list.addAll(arena.getConfig().getStringList(CFG.LISTS_BLACKLIST.getNode() + ".break", new ArrayList<>()));

        if (list.contains(event.getBlock().getType().name())) {
            arena.msg(event.getPlayer(), MSG.ERROR_BLACKLIST_DISALLOWED, Language.parse(MSG.PERMISSION_BREAK));
            // on blacklist. DENY!
            event.setCancelled(true);
            debug(event.getPlayer(), "blacklist out");
            return;
        }

        debug(event.getPlayer(), "onBlockBreak !!!");

        ArenaModuleManager.onBlockBreak(arena, event.getBlock());

        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(),
                new DamageResetRunnable(arena, event.getPlayer(), null), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        final Arena arena = ArenaManager.getArenaByRegionLocation(
                new PABlockLocation(event.getBlock().getLocation()));
        if (arena == null) {
            return; // no arena => out
        }
        if (isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.FIRE)) {
            return;
        }

        ArenaModuleManager.onBlockBreak(arena, event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDecay(final LeavesDecayEvent event) {
        final Block block = event.getBlock();
        final Arena arena = ArenaManager.getArenaByRegionLocation(
                new PABlockLocation(block.getLocation()));

        if (arena == null) {
            return;
        }

        debug(arena, "block block decaying inside the arena");

        if (isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.NATURE)) {
            return;
        }

        ArenaModuleManager.onBlockBreak(arena, event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(final BlockFadeEvent event) {
        if (this.willBeSkipped(event, event.getBlock().getLocation(),
                RegionProtection.NATURE)) {
            return;
        }

        final Block block = event.getBlock();
        final Arena arena = ArenaManager.getArenaByRegionLocation(
                new PABlockLocation(block.getLocation()));

        if (arena == null) {
            return;
        }

        debug(arena, "block block fading inside the arena");
        if (isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.NATURE)) {
            return;
        }

        ArenaModuleManager.onBlockChange(arena, event.getBlock(),
                event.getNewState());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(final BlockFromToEvent event) {
        if (this.willBeSkipped(event, event.getToBlock().getLocation(),
                RegionProtection.NATURE)) {
            return;
        }

        final Block block = event.getToBlock();
        final Arena arena = ArenaManager.getArenaByRegionLocation(
                new PABlockLocation(block.getLocation()));

        if (arena == null) {
            return;
        }

        // arena.debugnfo("block fluids inside the arena");

        if (isProtected(arena, block.getLocation(), event, RegionProtection.NATURE)) {
            return;
        }

        ArenaModuleManager.onBlockBreak(arena, event.getBlock());

        ArenaModuleManager.onBlockPlace(arena, block, Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(final BlockFormEvent event) {
        if (this.willBeSkipped(event, event.getBlock().getLocation(),
                RegionProtection.NATURE)) {
            return;
        }

        final Block block = event.getBlock();
        final Arena arena = ArenaManager.getArenaByRegionLocation(
                new PABlockLocation(block.getLocation()));
        if (arena == null) {
            return;
        }

        if (isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.NATURE)) {
            return;
        }

        ArenaModuleManager.onBlockChange(arena, event.getBlock(),
                event.getNewState());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public static void onBlockGrow(final BlockGrowEvent event) {
        Arena arena = ArenaManager.getArenaByProtectedRegionLocation(
                new PABlockLocation(event.getBlock().getLocation()),
                RegionProtection.NATURE);
        Block block = event.getBlock();
        if (arena == null) {
            debug("BlockGrowEvent -> no arena");
            return; // no arena => out
        }
        if (isProtected(arena, block.getLocation(), event, RegionProtection.NATURE)) {
            return;
        }

        ArenaModuleManager.onBlockChange(arena, block, block.getState());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public static void onBlockGrow(final StructureGrowEvent event) {
        Arena arena = null;

        for (BlockState block : event.getBlocks()) {
            arena = ArenaManager.getArenaByRegionLocation(
                    new PABlockLocation(block.getLocation()));
            if (arena != null) {
                break;
            }
        }

        if (arena == null) {
            debug("StructureGrowEvent -> no arena");
            return; // no arena => out
        }
        for (BlockState block : event.getBlocks()) {
            arena = ArenaManager.getArenaByProtectedRegionLocation(
                    new PABlockLocation(block.getLocation()),
                    RegionProtection.NATURE);
            if (arena == null) {
                continue;
            }
            if (isProtected(arena, block.getLocation(), event, RegionProtection.NATURE)) {
                return;
            }

            ArenaModuleManager.onBlockChange(arena, block.getBlock(), block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(final BlockIgniteEvent event) {
        if (this.willBeSkipped(event, event.getBlock().getLocation(), RegionProtection.FIRE)) {
            return;
        }
        final Arena arena = ArenaManager.getArenaByRegionLocation(
                new PABlockLocation(event.getBlock().getLocation()));
        if (arena == null) {
            return;
        }

        if (isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.FIRE)) {
            return;
        }
        ArenaModuleManager.onBlockBreak(arena, event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionPrime(final ExplosionPrimeEvent event) {

        if (this.willBeSkipped(event, event.getEntity().getLocation(), RegionProtection.TNT)) {
            return;
        }
        final Arena arena = ArenaManager.getArenaByRegionLocation(
                new PABlockLocation(event.getEntity().getLocation()));
        // all checks done in willBeSkipped
        ArenaModuleManager.onBlockBreak(arena, event.getEntity().getLocation().getBlock());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        Arena arena = null;

        for (Block block : event.getBlocks()) {
            arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(block.getLocation()));
            if (arena != null) {
                if (isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.PISTON)) {
                    return;
                }
                break;
            }
        }

        if (arena == null) {
            return; // no arena => out
        }
        debug(arena, "block piston extend inside the arena");

        ArenaModuleManager.onBlockPiston(arena, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        debug(player, "BlockPlace");

        if (this.willBeSkipped(event, block.getLocation(), RegionProtection.PLACE)) {
            return;
        }

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        if (asList(PlayerStatus.LOST, PlayerStatus.WATCH, PlayerStatus.LOUNGE, PlayerStatus.READY).contains(arenaPlayer.getStatus())) {
            event.setCancelled(true);
            return;
        }

        final Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(block.getLocation()));
        final Block placedBlock = event.getBlockPlaced();

        if (block.getType() == Material.TNT && arena.getConfig().getBoolean(CFG.PLAYER_AUTOIGNITE)) {
            debug(arena, "autoignite tnt");
            placedBlock.setType(Material.AIR);
            block.getWorld().spawnEntity(Utils.getCenteredLocation(block.getLocation()), EntityType.PRIMED_TNT);
            return;
        }


        List<String> list = arena.getConfig().getStringList(
                CFG.LISTS_WHITELIST.getNode() + ".place",
                new ArrayList<>());

        if (!list.isEmpty() && !list.contains(placedBlock.getType().name())) {
            arena.msg(player, MSG.ERROR_WHITELIST_DISALLOWED, Language.parse(MSG.PERMISSION_PLACE));
            event.setCancelled(true);
            debug(arena, "not on whitelist. DENY!");
            return;
        }

        if (isProtected(arena, block.getLocation(), event, RegionProtection.PLACE)) {
            if (arena.isFightInProgress() && !isProtected(arena, block.getLocation(), event, RegionProtection.TNT)
                    && block.getType() == Material.TNT) {

                ArenaModuleManager.onBlockPlace(arena, block, event.getBlockReplacedState().getType());
                event.setCancelled(false);
                debug(arena, "we do not block TNT, so just return if it is TNT");
            } else if (arena.isFightInProgress() && !isProtected(arena, block.getLocation(), event, RegionProtection.FIRE)
                    && block.getBlockData() instanceof Fire) {

                ArenaModuleManager.onBlockPlace(arena, block, event.getBlockReplacedState().getType());
                event.setCancelled(false);
                debug(arena, "we do not block FIRE, so just return if it is FIRE");
            }
            return;
        }

        list = arena.getConfig().getStringList(
                CFG.LISTS_BLACKLIST.getNode() + ".place",
                new ArrayList<>());

        if (list.contains(placedBlock.getType().name())) {
            arena.msg(player, MSG.ERROR_BLACKLIST_DISALLOWED, Language.parse(MSG.PERMISSION_PLACE));
            event.setCancelled(true);
            debug(arena, "on blacklist. DENY!");
            return;
        }

        try {
            arena.getGoal().checkPlace(event);
        } catch (GameplayException e) {
            debug(player, "onBlockPlace cancelled by goal: " + arena.getGoal().getName());
            return;
        }

        debug(arena, "BlockPlace not cancelled!");

        ArenaModuleManager.onBlockPlace(arena, block, event.getBlockReplacedState().getType());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final HangingPlaceEvent event) {
        if (this.willBeSkipped(event, event.getBlock().getLocation(),
                RegionProtection.PAINTING)) {
            return;
        }

        final Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(event.getBlock().getLocation()));

        debug(event.getPlayer(), "painting place");

        if (arena == null || isProtected(arena, event.getBlock().getLocation(), event, RegionProtection.PAINTING)) {
            return;
        }

        ArenaModuleManager.onBlockPlace(arena, event.getBlock(), event.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final HangingBreakEvent event) {
        if (this.willBeSkipped(event, event.getEntity().getLocation(), RegionProtection.PAINTING)) {
            return;
        }

        final Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(event.getEntity().getLocation()));

        if (arena == null) {
            debug("painting break outside arena");
            return;
        }

        if (isProtected(arena, event.getEntity().getLocation(), event, RegionProtection.PAINTING)) {
            return;
        }
        debug(arena, "painting break inside an arena");
        ArenaModuleManager.onPaintingBreak(arena, event.getEntity(), event.getEntity().getType());
    }
}
