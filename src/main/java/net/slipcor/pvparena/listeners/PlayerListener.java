package net.slipcor.pvparena.listeners;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.goal.PAGoalEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InteractionManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.RegionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.IllegalPluginAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.arena.PlayerStatus.*;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Player Listener class
 * </pre>
 *
 * @author slipcor
 * @version v0.10.2
 */

public class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        String message = event.getMessage();

        ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        Arena arena = aPlayer.getArena();

        if (arena == null) {
            return; // no fighting player => OUT
        }

        ArenaTeam team = aPlayer.getArenaTeam();
        if (team == null || asList(DEAD, LOST, WATCH).contains(aPlayer.getStatus())) {
            if (!arena.getConfig().getBoolean(CFG.PERMS_SPECTALK)) {
                event.setCancelled(true);
            }
            return; // no fighting player => OUT
        }

        debug(arena, player, "fighting player chatting!");

        if (arena.getConfig().getBoolean(CFG.CHAT_ENABLED)) {
            if(aPlayer.isPublicChatting()) {
                if(arena.getConfig().getBoolean(CFG.CHAT_ONLYPRIVATE)) {
                    arena.broadcastColored(message, team.getColor(), event.getPlayer());
                    event.setCancelled(true);
                }
                // else regular chatting => just let push message to chat
            } else {
                team.sendMessage(aPlayer, message);
                event.setCancelled(true);

                String toGlobal = arena.getConfig().getString(CFG.CHAT_TOGLOBAL);

                if (!toGlobal.equalsIgnoreCase("none") && StringUtils.startsWithIgnoreCase(message, toGlobal)) {
                    event.setMessage(message.substring(toGlobal.length()));
                    // global chatting => just let push message to chat
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        final Arena arena = ArenaPlayer.fromPlayer(player).getArena();
        if (arena == null || player.isOp() || PermissionManager.hasAdminPerm(player)
                || PermissionManager.hasBuilderPerm(player, arena)) {
            return; // no fighting player => OUT
        }

        final List<String> list = PVPArena.getInstance().getConfig().getStringList(
                "whitelist");
        list.add("pa");
        list.add("pvparena");
        debug(arena, player, "checking command whitelist");

        boolean wildcard = PVPArena.getInstance().getConfig().getBoolean("whitelist_wildcard", false);

        for (String s : list) {
            if ("*".equals(s) ||
                    (wildcard && event.getMessage().toLowerCase().startsWith('/' + s)) ||
                    (!wildcard && event.getMessage().trim().toLowerCase().equalsIgnoreCase('/' + s))) {
                debug(arena, player, "command allowed: " + s);
                return;
            }
        }

        list.clear();
        list.addAll(arena.getConfig().getStringList(
                CFG.LISTS_CMDWHITELIST.getNode(), new ArrayList<String>()));

        if (list.size() < 1) {
            list.clear();
            list.add("ungod");
            arena.getConfig().set(CFG.LISTS_CMDWHITELIST, list);
            arena.getConfig().save();
        }

        list.add("pa");
        list.add("pvparena");
        debug(arena, player, "checking command whitelist");

        for (String s : list) {
            if (event.getMessage().toLowerCase().startsWith('/' + s)) {
                debug(arena, player, "command allowed: " + s);
                return;
            }
        }

        debug(arena, player, "command blocked: " + event.getMessage());
        arena.msg(player, MSG.ERROR_COMMAND_BLOCKED, event.getMessage());
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public static void onPlayerCraft(final CraftItemEvent event) {

        final Player player = (Player) event.getWhoClicked();

        final Arena arena = ArenaPlayer.fromPlayer(player).getArena();
        if (arena == null || player.isOp() || PermissionManager.hasAdminPerm(player)
                || PermissionManager.hasBuilderPerm(player, arena)) {
            return; // no fighting player => OUT
        }

        try {
            arena.getGoal().checkCraft(event);
        } catch (GameplayException e) {
            debug(player, "onPlayerCraft cancelled by goal: " + arena.getGoal().getName());
            return;
        }

        if (!BlockListener.isProtected(arena, player.getLocation(), event, RegionProtection.CRAFT)) {
            return; // no craft protection
        }

        debug(arena, player, "onCraftItemEvent: fighting player");
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final Arena arena = aPlayer.getArena();
        if (arena == null) {
            return; // no fighting player => OUT
        }
        if (aPlayer.getStatus() == READY || aPlayer.getStatus() == LOUNGE) {
            event.setCancelled(true);
            arena.msg(player, MSG.NOTICE_NO_DROP_ITEM);
            return;
        }

        try {
            arena.getGoal().checkDrop(event);
        } catch (GameplayException e) {
            debug(player, "onPlayerDropItem cancelled by goal: " + arena.getGoal().getName());
            return;
        }

        if (!BlockListener.isProtected(arena, player.getLocation(), event, RegionProtection.DROP)) {
            return; // no drop protection
        }

        if (Bukkit.getPlayer(player.getName()) == null || aPlayer.getStatus() == DEAD || aPlayer.getStatus() == LOST) {
            debug(arena, "Player is dead. allowing drops!");
            return;
        }

        debug(arena, player, "onPlayerDropItem: fighting player");
        arena.msg(player, MSG.NOTICE_NO_DROP_ITEM);
        event.setCancelled(true);
        // cancel the drop event for fighting players, with message
    }

    @EventHandler
    public void onPlayerGoal(final PAGoalEvent event) {
        if (event.refreshScoreboard() && event.getArena() != null) {
            event.getArena().getScoreboard().refresh();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final Arena arena = ArenaPlayer.fromPlayer(player).getArena();
        if (arena != null) {
            debug(player, "playDeathEvent thrown. That should not happen.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerHunger(final FoodLevelChangeEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        final Player player = (Player) event.getEntity();

        final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);

        if (ap.getStatus() == READY || ap.getStatus() == LOUNGE || ap.getArena() != null && !ap.getArena().getConfig().getBoolean(CFG.PLAYER_HUNGER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        debug(player, "PlayerInteractEvent");

        if (event.getAction() == Action.PHYSICAL) {
            debug(player, "exiting : physical actions are ignored/allowed");
            return;
        }

        if (Objects.equals(event.getHand(), EquipmentSlot.OFF_HAND)) {
            debug(player, "exiting: offhand clicks are ignored/allowed");
            return;
        }


        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        Arena arena = arenaPlayer.getArena();

        if (arena == null && event.getClickedBlock() != null) {
            Arena arenaByLocation = ArenaManager.getArenaByRegionLocation(new PABlockLocation(event.getClickedBlock().getLocation()));
            if (arenaByLocation != null) {
                // Player doesn't belong to an arena but interacts with an arena region

                if (!arenaByLocation.equals(PAA_Edit.activeEdits.get(player.getName()))) {
                    debug(player, "[Cancel #1] Player in area of arena '{}' but not in edit mode");
                    event.setCancelled(true);
                    return;
                }
                // else: player has edit mode. Interaction allowed as if they are out of an arena.
            }

            // player is out of any arena
            if (WorkflowManager.handleSetBlock(player, event.getClickedBlock()) || ArenaRegion.handleSetRegionPosition(event, player)) {
                debug(player, "[Cancel #2] Admin is setting a block or a region");
                event.setCancelled(true);
            } else {
                debug(player, "Try sign join");
                InteractionManager.handleJoinSignInteract(event, player);
            }
        } else {
            ArenaTeam team = arenaPlayer.getArenaTeam();

            if(team != null) {
                // Player is in an Arena and is not a spectator

                if (ArenaModuleManager.onPlayerInteract(arena, event)) {
                    debug(arenaPlayer, "[Cancel #3] Module caught and cancelled the event");
                    event.setCancelled(true);
                    return;
                }

                WorkflowManager.handleInteract(arena, player, event);
                debug(player, "Event cancelled by a goal? useInteractedBlock: {}, useItemInHand: {}", event.useInteractedBlock(), event.useItemInHand());

                if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Block block = event.getClickedBlock();

                    debug(arenaPlayer, "Block click! Block name : {}", block.getType().name());
                    debug(arenaPlayer, "player team: {}", team.getName());

                    if (block.getState() instanceof Sign) {
                        InteractionManager.handleClassSignInteract(event, arena, arenaPlayer);

                    } else if (event.useInteractedBlock() != Event.Result.DENY && block.getType() == arena.getReadyBlock()) {
                        InteractionManager.handleReadyBlockInteract(event, arena, arenaPlayer);

                    } else if (event.useInteractedBlock() != Event.Result.DENY) {
                        if (arenaPlayer.getStatus() != FIGHT) {
                            InteractionManager.handleNotFightingPlayersWithTeam(event, arena, arenaPlayer);

                        } else if (block.getState() instanceof Container) {
                            InteractionManager.handleContainerInteract(event, arena, arenaPlayer);
                        }
                        // else: regular fighting player => interaction allowed
                    }
                }
            } else {
                if (arenaPlayer.getStatus() != WATCH || !arena.getConfig().getBoolean(CFG.PERMS_SPECINTERACT)) {
                    // disable all event in arena for player without team or special spectate setting
                    debug(arenaPlayer, "[Cancel #9] player without team who is not a spectator with allowed interactions");
                    event.setCancelled(true);
                } else {
                    debug(arenaPlayer, "allowing spectator interaction due to config setting!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerItemConsume(final PlayerItemConsumeEvent event) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(event.getPlayer().getName());
        if (arenaPlayer.getArena() != null && arenaPlayer.getStatus() != FIGHT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public static void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (player.isDead()) {
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        final Arena arena = aPlayer.getArena();
        if(arena != null) {
            if(arena.isFightInProgress() && arena.getConfig().getBoolean(CFG.JOIN_ALLOW_REJOIN) &&
                    arena.hasPlayerInTeams(aPlayer) && aPlayer.getStatus() == OFFLINE) {

                    aPlayer.reloadBukkitPlayer();
                    aPlayer.setStatus(FIGHT);
                    arena.getScoreboard().setupPlayer(aPlayer);
                    SpawnManager.respawn(aPlayer, null);
            } else {
                arena.playerLeave(player, CFG.TP_EXIT, true, true, false);
            }
        } else {
            // instantiate and/or reset a player. This fixes issues with leaving
            // players
            // and makes sure every player is an arenaplayer ^^

            aPlayer.readDump();
            Arena loadedArena = aPlayer.getArena();

            if (loadedArena != null) {
                loadedArena.playerLeave(player, CFG.TP_EXIT, true, true, false);
            }
        }

        debug(player, "OP joins the game");
        if (player.isOp() && PVPArena.getInstance().getUpdateChecker() != null) {
            PVPArena.getInstance().getUpdateChecker().displayMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        Arena arena = arenaPlayer.getArena();
        if (arena != null) {
            if(arena.getConfig().getBoolean(CFG.JOIN_ALLOW_REJOIN) && arenaPlayer.canLeaveWithoutEndingArena()) {
                arenaPlayer.setStatus(OFFLINE);
                try {
                    if(arena.getGoal().checkEnd()) {
                        // If after passing player to "offline" end should be triggered, then force leave the player.
                        // Arena will automatically end with player removal
                        arena.playerLeave(player, CFG.TP_EXIT, false, true, false);
                    }
                } catch (GameplayException e) {
                    arena.msg(Bukkit.getConsoleSender(), MSG.ERROR_ERROR, e.getMessage());
                    arenaPlayer.unload();
                }
            } else {
                arena.playerLeave(player, CFG.TP_EXIT, false, true, false);
                arenaPlayer.unload();
            }
        } else if(arenaPlayer.getQueuedArena() != null) {
            for (ArenaModule mod : arenaPlayer.getQueuedArena().getMods()) {
                if(mod.handleQueuedLeave(arenaPlayer)) {
                    arenaPlayer.unload();
                    return;
                }
            }
        } else {
            arenaPlayer.unload();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKicked(final PlayerKickEvent event) {
        final Player player = event.getPlayer();
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        final Arena arena = arenaPlayer.getArena();
        if (arena == null) {
            if (arenaPlayer.getQueuedArena() != null) {
                for (ArenaModule mod : arenaPlayer.getQueuedArena().getMods()) {
                    if (mod.handleQueuedLeave(arenaPlayer)) {
                        arenaPlayer.unload();
                        return;
                    }
                }
            }
            return; // no fighting player => OUT
        }
        arena.playerLeave(player, CFG.TP_EXIT, false, true, false);
        arenaPlayer.unload();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        // aPlayer.setArena(null);
        // instantiate and/or reset a player. This fixes issues with leaving
        // players and makes sure every player is an arenaplayer ^^


        if (aPlayer.getArena() != null && aPlayer.getStatus() == FIGHT) {
            Arena arena = aPlayer.getArena();
            debug(arena, "Trying to override a rogue RespawnEvent!");
        }

        aPlayer.debugPrint();

        // aPlayer.readDump();
        final Arena arena = aPlayer.getArena();
        if (arena != null) {
            arena.playerLeave(player, CFG.TP_EXIT, true, false, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getEntity();

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        final Arena arena = arenaPlayer.getArena();

        if (arenaPlayer.getStatus() == LOST) {
            debug(arenaPlayer, "cancelling because LOST");
            event.setCancelled(true);
            return;
        }

        if (arena != null) {

            try {
                arena.getGoal().checkPickup(event);
            } catch (GameplayException e) {
                debug(player, "onPlayerPickupItem cancelled by goal: " + arena.getGoal().getName());
                return;
            }

            // Call goal and mod hooks. They should cancel the event if they catch it.
            arena.getGoal().onPlayerPickUp(event);
            ArenaModuleManager.onPlayerPickupItem(arena, event);

            if(!event.isCancelled() && BlockListener.isProtected(arena, player.getLocation(), event, RegionProtection.PICKUP)) {
                // If event has not been caught by goals and mods and if a region prevents pickup => cancel
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
            PABlockLocation locTo = new PABlockLocation(to);
            RegionManager.getInstance().checkPlayerLocation(event.getPlayer(), locTo, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        Arena arena = arenaPlayer.getArena();

        if (event.getTo() == null) {

            PVPArena.getInstance().getLogger().warning("Player teleported to NULL: " + event.getPlayer());
            return;
        }

        if (arena == null) {

            arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(
                    event.getTo()));

            if (arena == null) {
                return; // no fighting player and no arena location => OUT
            }

            final Set<ArenaRegion> regs = arena.getRegionsByType(RegionType.BATTLE);
            boolean contained = false;
            for (ArenaRegion reg : regs) {
                if (reg.getShape().contains(new PABlockLocation(event.getTo()))) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                return;
            }
        }

        debug(arena, player, "onPlayerTeleport: fighting player '"
                + event.getPlayer().getName() + "' (uncancel)");
        event.setCancelled(false); // fighting player - first recon NOT to
        // cancel!

        if (player.getGameMode() == GameMode.SPECTATOR && event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return; // ignore spectators
        }

        debug(arena, player, "aimed location: " + event.getTo());

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && arenaPlayer.getStatus() != FIGHT) {
            debug(arena, player, "onPlayerTeleport: ender pearl when not fighting, cancelling!");
            event.setCancelled(true); // cancel and out
            return;
        }

        final Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.BATTLE);

        if (regions == null || regions.isEmpty()) {
            this.maybeFixInvisibility(arena, player);
            return;
        }

        PABlockLocation toLoc = new PABlockLocation(event.getTo());
        PABlockLocation fromLoc = new PABlockLocation(event.getFrom());

        if (!arenaPlayer.isTelePass() && !player.hasPermission("pvparena.telepass")) {
            for (ArenaRegion r : regions) {
                if (r.getShape().contains(toLoc) || r.getShape().contains(fromLoc)) {
                    // teleport inside the arena, allow, unless:
                    if (r.getProtections().contains(RegionProtection.TELEPORT)) {
                        debug(arena, player, "onPlayerTeleport: protected region, cancelling!");
                        event.setCancelled(true); // cancel and tell
                        arena.msg(player, MSG.NOTICE_NO_TELEPORT);
                        return;
                    }
                }
            }
        } else {
            debug(arena, player, "onPlayerTeleport: using telepass");
        }

        if (arena.isFightInProgress() && !arenaPlayer.isTeleporting() && arenaPlayer.getStatus() == FIGHT) {
            RegionManager.getInstance().handleFightingPlayerMove(arenaPlayer, toLoc, event);
        }

        this.maybeFixInvisibility(arena, player);
    }

    private void maybeFixInvisibility(final Arena arena, final Player player) {
        if (arena.getConfig().getBoolean(CFG.USES_EVILINVISIBILITYFIX)) {
            class RunLater implements Runnable {

                @Override
                public void run() {
                    for (ArenaPlayer otherPlayer : arena.getFighters()) {
                        if (otherPlayer.getPlayer() != null) {
                            otherPlayer.getPlayer().showPlayer(PVPArena.getInstance(), player);
                        }
                    }
                }

            }
            try {
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 5L);
            } catch (final IllegalPluginAccessException e) {

            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerVelocity(final PlayerVelocityEvent event) {
        final Player player = event.getPlayer();

        final Arena arena = ArenaPlayer.fromPlayer(player).getArena();
        if (arena == null) {
            return; // no fighting player or no powerups => OUT
        }
        ArenaModuleManager.onPlayerVelocity(arena, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerToggleSprint(final PlayerToggleSprintEvent event) {
        final Player player = event.getPlayer();

        final Arena arena = ArenaPlayer.fromPlayer(player).getArena();
        if (arena == null) {
            return; // no fighting player or no powerups => OUT
        }
        ArenaModuleManager.onPlayerToggleSprint(arena, event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerProjectileLaunch(final ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            final Player player = (Player) event.getEntity().getShooter();
            final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
            final Arena arena = aPlayer.getArena();
            if (arena == null) {
                return; // no fighting player => OUT
            }
            if (aPlayer.getStatus() == FIGHT || aPlayer.getStatus() == NULL) {
                return;
            }
            event.setCancelled(true);
        }
    }

}
