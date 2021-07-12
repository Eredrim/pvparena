package net.slipcor.pvparena.listeners;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerState;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.PAA_Setup;
import net.slipcor.pvparena.commands.PAG_Arenaclass;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoalManager;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.loadables.ArenaRegion.RegionProtection;
import net.slipcor.pvparena.loadables.ArenaRegion.RegionType;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.IllegalPluginAccessException;

import java.util.*;

import static java.util.Arrays.asList;

/**
 * <pre>
 * Player Listener class
 * </pre>
 *
 * @author slipcor
 * @version v0.10.2
 */

public class PlayerListener implements Listener {
    private static final Debug DEBUG = new Debug(23);

    private boolean checkAndCommitCancel(final Arena arena, final Player player,
                                         final Cancellable event) {

        if (willBeCancelled(player, event)) {
            return true;
        }

        if (!(event instanceof PlayerInteractEvent)) {
            return false;
        }
        final PlayerInteractEvent pie = (PlayerInteractEvent) event;
        final Block block = pie.getClickedBlock();
        final Material check = arena == null ? Material.IRON_BLOCK : arena.getReadyBlock();

        if (block != null && (block.getState() instanceof Sign || block.getType() == check)) {
            DEBUG.i("signs and ready blocks allowed!", player);
            DEBUG.i("> false", player);
            return false;
        }

        DEBUG.i("checkAndCommitCancel", player);
        if (arena == null || PermissionManager.hasAdminPerm(player)) {
            DEBUG.i("no arena or admin", player);
            DEBUG.i("> false", player);
            return false;
        }

        if (arena.getArenaConfig().getBoolean(CFG.PERMS_LOUNGEINTERACT)) {
            return false;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        if ((aPlayer.getStatus() == Status.WATCH || aPlayer.getStatus() == Status.LOST) &&
                arena.getArenaConfig().getBoolean(CFG.PERMS_SPECINTERACT)) {
            return false;
        }

        if (!arena.isFightInProgress()) {
            arena.getDebugger().i("arena != null and fight not in progress => cancel", player);
            arena.getDebugger().i("> true", player);

            PACheck.handleInteract(arena, player, pie, pie.getClickedBlock());
            event.setCancelled(true);
            return true;
        }

        if (aPlayer.getStatus() != Status.FIGHT) {
            DEBUG.i("not fighting => cancel", player);
            DEBUG.i("> true", player);
            event.setCancelled(true);
            return true;
        }

        DEBUG.i("> false", player);
        return false;
    }

    private boolean willBeCancelled(final Player player, final Cancellable event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) event;
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                DEBUG.i("Allowing right click interact", player);
                return false;
            }
        }
        if (ArenaPlayer.parsePlayer(player.getName()).getStatus() == Status.LOST) {
            DEBUG.i("cancelling because LOST", player);
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {

        final Player player = event.getPlayer();

        if (PAA_Setup.activeSetups.containsKey(player.getName())) {
            PAA_Setup.chat(player, event.getMessage());
            return;
        }

        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        if (arena == null) {
            return; // no fighting player => OUT
        }
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (team == null ||
                aPlayer.getStatus() == Status.DEAD && aPlayer.get() == null ||
                aPlayer.getStatus() == Status.LOST ||
                aPlayer.getStatus() == Status.WATCH) {
            if (!arena.getArenaConfig().getBoolean(CFG.PERMS_SPECTALK)) {
                event.setCancelled(true);
            }
            return; // no fighting player => OUT
        }
        arena.getDebugger().i("fighting player chatting!", player);
        final String sTeam = team.getName();

        if (!arena.getArenaConfig().getBoolean(CFG.CHAT_ONLYPRIVATE)) {
            if (!arena.getArenaConfig().getBoolean(CFG.CHAT_ENABLED)) {
                return; // no chat editing
            }

            if (aPlayer.isPublicChatting()) {
                return; // player not privately chatting
            }

            String toGlobal = arena.getArenaConfig().getString(CFG.CHAT_TOGLOBAL);

            if (!toGlobal.equalsIgnoreCase("none")) {
                if (event.getMessage().toLowerCase().startsWith(
                        toGlobal.toLowerCase())) {
                    event.setMessage(event.getMessage().substring(toGlobal.length()));
                    return;
                }
            }

            arena.tellTeam(sTeam, event.getMessage(), team.getColor(),
                    event.getPlayer());
            event.setCancelled(true);
            return;
        }

        if (arena.getArenaConfig().getBoolean(CFG.CHAT_ENABLED)
                && !aPlayer.isPublicChatting()) {
            arena.tellTeam(sTeam, event.getMessage(), team.getColor(),
                    event.getPlayer());
            event.setCancelled(true);
            return;
        }

        arena.broadcastColored(event.getMessage(), team.getColor(),
                event.getPlayer()); //
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        if (PAA_Setup.activeSetups.containsKey(player.getName())) {
            PAA_Setup.chat(player, event.getMessage().substring(1));
            return;
        }

        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        if (arena == null || player.isOp() || PermissionManager.hasAdminPerm(player)
                || PermissionManager.hasBuilderPerm(player, arena)) {
            return; // no fighting player => OUT
        }

        final List<String> list = PVPArena.instance.getConfig().getStringList(
                "whitelist");
        list.add("pa");
        list.add("pvparena");
        arena.getDebugger().i("checking command whitelist", player);

        boolean wildcard = PVPArena.instance.getConfig().getBoolean("whitelist_wildcard", false);

        for (final String s : list) {
            if ("*".equals(s) ||
                    ((wildcard || s.endsWith(" ")) && event.getMessage().toLowerCase().startsWith('/' + s)) ||
                    (!wildcard && event.getMessage().toLowerCase().startsWith('/' + s +' '))) {
                arena.getDebugger().i("command allowed: " + s, player);
                return;
            }
        }

        list.clear();
        list.addAll(arena.getArenaConfig().getStringList(
                CFG.LISTS_CMDWHITELIST.getNode(), new ArrayList<String>()));

        if (list.size() < 1) {
            list.clear();
            list.add("ungod");
            arena.getArenaConfig().set(CFG.LISTS_CMDWHITELIST, list);
            arena.getArenaConfig().save();
        }

        list.add("pa");
        list.add("pvparena");
        arena.getDebugger().i("checking command whitelist", player);

        for (final String s : list) {
            if (event.getMessage().toLowerCase().startsWith('/' + s)) {
                arena.getDebugger().i("command allowed: " + s, player);
                return;
            }
        }

        arena.getDebugger().i("command blocked: " + event.getMessage(), player);
        arena.msg(player,
                Language.parse(arena, MSG.ERROR_COMMAND_BLOCKED, event.getMessage()));
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCraft(final CraftItemEvent event) {

        final Player player = (Player) event.getWhoClicked();

        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        if (arena == null || player.isOp() || PermissionManager.hasAdminPerm(player)
                || PermissionManager.hasBuilderPerm(player, arena)) {
            return; // no fighting player => OUT
        }

        PACheck res = ArenaGoalManager.checkCraft(arena, event);

        if (res.hasError()) {
            DEBUG.i("onPlayerCraft cancelled by goal: " + res.getModName(), player);
            return;
        }

        if (!BlockListener.isProtected(player.getLocation(), event,
                RegionProtection.CRAFT)) {
            return; // no craft protection
        }

        arena.getDebugger().i("onCraftItemEvent: fighting player", player);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        if (willBeCancelled(player, event)) {
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        final Arena arena = aPlayer.getArena();
        if (arena == null) {
            return; // no fighting player => OUT
        }
        if (aPlayer.getStatus() == Status.READY
                || aPlayer.getStatus() == Status.LOUNGE) {
            event.setCancelled(true);
            arena.msg(player, Language.parse(arena, MSG.NOTICE_NO_DROP_ITEM));
            return;
        }

        PACheck res = ArenaGoalManager.checkDrop(arena, event);

        if (res.hasError()) {
            DEBUG.i("onPlayerDropItem cancelled by goal: " + res.getModName(), player);
            return;
        }

        if (!BlockListener.isProtected(player.getLocation(), event,
                RegionProtection.DROP)) {
            return; // no drop protection
        }

        if (Bukkit.getPlayer(player.getName()) == null || aPlayer.getStatus() == Status.DEAD || aPlayer.getStatus() == Status.LOST) {
            arena.getDebugger().i("Player is dead. allowing drops!");
            return;
        }

        arena.getDebugger().i("onPlayerDropItem: fighting player", player);
        arena.msg(player, Language.parse(arena, MSG.NOTICE_NO_DROP_ITEM));
        event.setCancelled(true);
        // cancel the drop event for fighting players, with message
    }

    @EventHandler
    public void onPlayerGoal(final PAGoalEvent event) {
        /*
         * content[X].contains(playerDeath) => "playerDeath:playerName"
         * content[X].contains(playerKill) => "playerKill:playerKiller:playerKilled"
         * content[X].contains(trigger) => "trigger:playerName" triggered a score
         * content[X].equals(tank) => player is tank
         * content[X].equals(infected) => player is infected
         * content[X].equals(doesRespawn) => player will respawn
         * content[X].contains(score) => "score:player:team:value"
         */
        String[] args = event.getContents();
        for (String content : args) {
            if (content != null) {
                if (content.startsWith("playerDeath")||content.startsWith("trigger")||content.startsWith("playerKill")||content.startsWith("score")) {
                    event.getArena().updateScoreboards();
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        if (arena == null) {
            return;
        }
        PACheck.handlePlayerDeath(arena, player, event);
    }

    /**
     * pretend a player death
     *
     * @param arena  the arena the player is playing in
     * @param player the player to kill
     * @param eEvent the event triggering the death
     */
    public static void finallyKillPlayer(final Arena arena, final Player player,
                                         final Event eEvent) {
        EntityDamageEvent cause = null;

        if (eEvent instanceof EntityDeathEvent) {
            cause = player.getLastDamageCause();
        } else if (eEvent instanceof EntityDamageEvent) {
            cause = (EntityDamageEvent) eEvent;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        final ArenaTeam team = aPlayer.getArenaTeam();

        final String playerName = (team == null) ? player.getName() : team.colorizePlayer(player);
        if (arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            arena.broadcast(Language.parse(arena,
                    MSG.FIGHT_KILLED_BY,
                    playerName + ChatColor.YELLOW,
                    arena.parseDeathCause(player, cause == null ? EntityDamageEvent.DamageCause.VOID : cause.getCause(),
                            ArenaPlayer.getLastDamagingPlayer(cause, player))));
        }

        if (arena.getArenaConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
            InventoryManager.drop(player);
            if (eEvent instanceof EntityDeathEvent) {
                ((EntityDeathEvent) eEvent).getDrops().clear();
            }
        }

        // Trick to avoid death screen
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.instance, player::closeInventory, 1);

        if (!aPlayer.hasCustomClass()) {
            InventoryManager.clearInventory(player);
        }

        arena.removePlayer(player, arena.getArenaConfig().getString(CFG.TP_DEATH), true, false);

        aPlayer.setStatus(Status.LOST);
        aPlayer.addDeath();

        PlayerState.fullReset(arena, player);

        class RunLater implements Runnable {

            @Override
            public void run() {

                boolean found = false;
                for (final ArenaModule mod : arena.getMods()) {
                    if (mod.getName().contains("Spectate")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    new PAG_Leave().commit(arena, player, new String[0]);
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(PVPArena.instance, new RunLater(), 5L);

        ArenaManager.checkAndCommit(arena, false);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerHunger(final FoodLevelChangeEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        final Player player = (Player) event.getEntity();

        final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());

        if (ap.getStatus() == Status.READY || ap.getStatus() == Status.LOUNGE || ap.getArena() != null && !ap.getArena().getArenaConfig().getBoolean(CFG.PLAYER_HUNGER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        DEBUG.i("onPlayerInteract", player);

        if (event.getAction() == Action.PHYSICAL) {
            DEBUG.i("returning: physical", player);
            return;
        }

        if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            DEBUG.i("exiting: offhand", player);
            return;
        }

        DEBUG.i("event pre cancelled: " + event.isCancelled(),
                player);

        Arena arena = null;

        if (event.hasBlock()) {
            DEBUG.i("block: " + event.getClickedBlock().getType().name(), player);

            arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(
                    event.getClickedBlock().getLocation()));
            if (checkAndCommitCancel(arena, event.getPlayer(), event)) {
                if (arena != null) {
                    PACheck.handleInteract(arena, player, event, event.getClickedBlock());
                }
                return;
            }
        }

        if (arena != null && ArenaModuleManager.onPlayerInteract(arena, event)) {
            DEBUG.i("returning: #1", player);
            return;
        }

        if (PACheck.handleSetFlag(player, event.getClickedBlock())) {
            DEBUG.i("returning: #2", player);
            event.setCancelled(true);
            return;
        }

        if (ArenaRegion.checkRegionSetPosition(event, player)) {
            DEBUG.i("returning: #3", player);
            return;
        }

        arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        if (arena == null) {
            DEBUG.i("returning: #4", player);
            ArenaManager.trySignJoin(event, player);
            return;
        }

        PACheck.handleInteract(arena, player, event, event.getClickedBlock());

        arena.getDebugger().i("event post cancelled: " + event.isCancelled(),
                player);

        //TODO: seriously, why?
        final boolean whyMe = arena.isFightInProgress()
                && !PVPArena.instance.getAgm().allowsJoinInBattle(arena);

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        final ArenaTeam team = aPlayer.getArenaTeam();

        if (aPlayer.getStatus() == Status.WATCH &&
                arena.getArenaConfig().getBoolean(CFG.PERMS_SPECINTERACT)) {
            arena.getDebugger().i("allowing spectator interaction due to config setting!");
            return;
        }

        if (aPlayer.getStatus() != Status.FIGHT) {
            if (whyMe) {
                arena.getDebugger().i("exiting! fight in progress AND no INBATTLEJOIN arena!", player); return;
            }
            if (asList(Status.LOUNGE, Status.READY).contains(aPlayer.getStatus()) &&
                    arena.getArenaConfig().getBoolean(CFG.PERMS_LOUNGEINTERACT)) {
                arena.getDebugger().i("allowing lounge interaction due to config setting!");
                event.setCancelled(false);
            } else if (aPlayer.getStatus() != Status.LOUNGE && aPlayer.getStatus() != Status.READY) {
                arena.getDebugger().i("cancelling: not fighting nor in the lounge", player);
                event.setCancelled(true);
            } else if (aPlayer.getArena() != null && team != null) {
                // fighting player inside the lobby!
                event.setCancelled(true);
            }
        }

        if (team == null) {
            arena.getDebugger().i("returning: no team", player);
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block block = event.getClickedBlock();
            arena.getDebugger().i("player team: " + team.getName(), player);
            if (block.getState() instanceof Sign) {
                arena.getDebugger().i("sign click!", player);
                final Sign sign = (Sign) block.getState();

                if ("custom".equalsIgnoreCase(sign.getLine(0)) || arena
                        .getClass(sign.getLine(0)) != null) {
                    if (arena.isFightInProgress()) {
                        PAG_Arenaclass ac = new PAG_Arenaclass();
                        ac.commit(arena, player, new String[]{sign.getLine(0)});
                    } else {
                        arena.chooseClass(player, sign, sign.getLine(0));
                    }
                } else {
                    arena.getDebugger().i('|' + sign.getLine(0) + '|', player);
                    arena.getDebugger().i(String.valueOf(arena.getClass(sign.getLine(0))),
                            player);
                    arena.getDebugger().i(String.valueOf(team), player);

                    if (whyMe) {
                        arena.getDebugger().i("exiting! fight in progress AND no INBATTLEJOIN arena!", player);
                    }
                }
                return;
            }

            if (whyMe) {
                arena.getDebugger().i("exiting! fight in progress AND no INBATTLEJOIN arena!", player);
                return;
            }
            arena.getDebugger().i("block click!", player);

            final Material mMat = arena.getReadyBlock();
            arena.getDebugger().i("clicked " + block.getType().name() + ", is it " + mMat.name()
                    + '?', player);
            if (block.getType() == mMat) {
                arena.getDebugger().i("clicked ready block!", player);
                if (event.getHand() == EquipmentSlot.OFF_HAND) {
                    arena.getDebugger().i("out: offhand!", player);
                    return; // double event
                }
                if (aPlayer.getArenaClass() == null || aPlayer.getArenaClass().getName() != null && aPlayer.getArenaClass().getName().isEmpty()) {
                    arena.msg(player, Language.parse(arena, MSG.ERROR_READY_NOCLASS));
                    return; // not chosen class => OUT
                }
                if (arena.startRunner != null) {
                    return; // counting down => OUT
                }
                if (aPlayer.getStatus() != Status.LOUNGE && aPlayer.getStatus() != Status.READY) {
                    return;
                }
                event.setCancelled(true);
                arena.getDebugger().i("Cancelled ready block click event to prevent itemstack consumation");
                Bukkit.getScheduler().runTaskLater(PVPArena.instance, new Runnable() {
                    @Override
                    public void run() {
                        aPlayer.get().updateInventory();
                    }
                }, 1L);
                final boolean alreadyReady = aPlayer.getStatus() == Status.READY;

                arena.getDebugger().i("===============", player);
                arena.getDebugger().i("===== class: " + (aPlayer.getArenaClass() == null ? "null" : aPlayer.getArenaClass().getName()) + " =====", player);
                arena.getDebugger().i("===============", player);

                if (!arena.isFightInProgress()) {
                    if (aPlayer.getStatus() != Status.READY) {
                        arena.msg(player, Language.parse(arena, MSG.READY_DONE));
                        if (!alreadyReady) {
                            arena.broadcast(Language.parse(arena, MSG.PLAYER_READY, aPlayer
                                    .getArenaTeam().colorizePlayer(aPlayer.get())));
                        }
                    }
                    aPlayer.setStatus(Status.READY);
                    if (!alreadyReady && aPlayer.getArenaTeam().isEveryoneReady()) {
                        arena.broadcast(Language.parse(arena, MSG.TEAM_READY, aPlayer
                                .getArenaTeam().getColoredName()));
                    }

                    if (arena.getArenaConfig().getBoolean(CFG.USES_EVENTEAMS)
                            && !TeamManager.checkEven(arena)) {
                        arena.msg(player,
                                Language.parse(arena, MSG.NOTICE_WAITING_EQUAL));
                        return; // even teams desired, not done => announce
                    }

                    if (!ArenaRegion.checkRegions(arena)) {
                        arena.msg(player,
                                Language.parse(arena, MSG.NOTICE_WAITING_FOR_ARENA));
                        return;
                    }

                    final String error = arena.ready();

                    if (error == null) {
                        arena.start();
                    } else if (error.isEmpty()) {
                        arena.countDown();
                    } else {
                        arena.msg(player, error);
                    }
                    return;
                }

                final Set<PASpawn> spawns = new HashSet<>();
                if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                    final String arenaClass = aPlayer.getArenaClass().getName();
                    spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName() + arenaClass + "spawn"));
                } else if (arena.isFreeForAll()) {
                    if ("free".equals(team.getName())) {
                        spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, "spawn"));
                    } else {
                        spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName()));
                    }
                } else {
                    spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName() + "spawn"));
                }

                int pos = new Random().nextInt(spawns.size());

                for (final PASpawn spawn : spawns) {

                    if (--pos < 0) {
                        arena.tpPlayerToCoordName(aPlayer, spawn.getName());
                        break;
                    }
                }

                ArenaPlayer.parsePlayer(player.getName()).setStatus(
                        Status.FIGHT);

                ArenaModuleManager.lateJoin(arena, player);
                ArenaGoalManager.lateJoin(arena, player);
            } else if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(block.getLocation()));
                if (arena != null) {

                    final Set<ArenaRegion> bl_regions = arena.getRegionsByType(RegionType.BL_INV);
                    out:
                    if (!event.isCancelled() && bl_regions != null && !bl_regions.isEmpty()) {
                        for (final ArenaRegion region : bl_regions) {
                            if (region.getShape().contains(new PABlockLocation(block.getLocation()))) {
                                if (region.getRegionName().toLowerCase().contains(team.getName().toLowerCase())
                                        || region.getRegionName().toLowerCase().contains(
                                        aPlayer.getArenaClass().getName().toLowerCase())) {
                                    event.setCancelled(true);
                                    break out;
                                }
                            }
                        }
                    }
                    final Set<ArenaRegion> wl_regions = arena.getRegionsByType(RegionType.WL_INV);
                    out:
                    if (!event.isCancelled() && wl_regions != null && !wl_regions.isEmpty()) {
                        event.setCancelled(true);
                        for (final ArenaRegion region : wl_regions) {
                            if (region.getShape().contains(new PABlockLocation(block.getLocation()))) {
                                if (region.getRegionName().toLowerCase().contains(team.getName().toLowerCase())
                                        || region.getRegionName().toLowerCase().contains(
                                        aPlayer.getArenaClass().getName().toLowerCase())) {
                                    event.setCancelled(false);
                                    break out;
                                }
                            }
                        }
                    }


                    if (!event.isCancelled() && arena.getArenaConfig().getBoolean(CFG.PLAYER_QUICKLOOT)) {
                        final Chest c = (Chest) block.getState();
                        InventoryManager.transferItems(player, c.getBlockInventory());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerItemConsume(final PlayerItemConsumeEvent event) {
        ArenaPlayer arenaPlayer = ArenaPlayer.parsePlayer(event.getPlayer().getName());
        if (arenaPlayer.getArena() != null && arenaPlayer.getStatus() != Status.FIGHT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (player.isDead()) {
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        aPlayer.setArena(null);
        // instantiate and/or reset a player. This fixes issues with leaving
        // players
        // and makes sure every player is an arenaplayer ^^

        aPlayer.readDump();
        final Arena arena = aPlayer.getArena();

        if (arena != null) {
            arena.playerLeave(player, CFG.TP_EXIT, true, true, false);
        }

        DEBUG.i("OP joins the game", player);
        if (player.isOp() && PVPArena.instance.getUpdateChecker() != null) {
            PVPArena.instance.getUpdateChecker().displayMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKicked(final PlayerKickEvent event) {
        final Player player = event.getPlayer();
        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        if (arena == null) {
            return; // no fighting player => OUT
        }
        arena.playerLeave(player, CFG.TP_EXIT, false, true, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        // aPlayer.setArena(null);
        // instantiate and/or reset a player. This fixes issues with leaving
        // players and makes sure every player is an arenaplayer ^^


        if (aPlayer.getArena() != null && aPlayer.getStatus() == Status.FIGHT) {
            Arena arena = aPlayer.getArena();
            arena.getDebugger().i("Trying to override a rogue RespawnEvent!");
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

        if (willBeCancelled(player, event)) {
            return;
        }

        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();

        if (arena != null) {

            PACheck res = ArenaGoalManager.checkPickup(arena, event);

            if (res.hasError()) {
                DEBUG.i("onPlayerPickupItem cancelled by goal: " + res.getModName(), player);
                return;
            }
        }
        if (arena == null
                || !BlockListener.isProtected(player.getLocation(), event,
                RegionProtection.PICKUP)) {
            return; // no fighting player or no powerups => OUT
        }
        ArenaGoalManager.onPlayerPickUp(arena, event);
        ArenaModuleManager.onPlayerPickupItem(arena, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        if (arena == null) {
            return; // no fighting player => OUT
        }
        arena.playerLeave(player, CFG.TP_EXIT, false, true, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();

        if (arena == null) {
            if (event.getTo() == null) {

                PVPArena.instance.getLogger().warning("Player teleported to NULL: " + event.getPlayer());

                return;
            }
            arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(
                    event.getTo()));

            if (arena == null) {
                return; // no fighting player and no arena location => OUT
            }

            final Set<ArenaRegion> regs = arena.getRegionsByType(RegionType.BATTLE);
            boolean contained = false;
            for (final ArenaRegion reg : regs) {
                if (reg.getShape().contains(new PABlockLocation(event.getTo()))) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                return;
            }
        }

        arena.getDebugger().i("onPlayerTeleport: fighting player '"
                + event.getPlayer().getName() + "' (uncancel)", player);
        event.setCancelled(false); // fighting player - first recon NOT to
        // cancel!

        if (player.getGameMode() == GameMode.SPECTATOR && event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return; // ignore spectators
        }

        arena.getDebugger().i("aimed location: " + event.getTo(), player);


        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && ArenaPlayer.parsePlayer(player.getName()).getStatus() != Status.FIGHT) {
            arena.getDebugger().i("onPlayerTeleport: ender pearl when not fighting, cancelling!", player);
            event.setCancelled(true); // cancel and out
            return;
        }

        if (ArenaPlayer.parsePlayer(player.getName()).isTelePass()
                || player.hasPermission("pvparena.telepass")) {

            maybeFixInvisibility(arena, player);

            return; // if allowed => OUT
        }
        arena.getDebugger().i("telepass: no!!", player);

        final Set<ArenaRegion> regions = arena
                .getRegionsByType(RegionType.BATTLE);

        if (regions == null || regions.isEmpty()) {
            maybeFixInvisibility(arena, player);

            return;
        }

        for (final ArenaRegion r : regions) {
            if (r.getShape().contains(new PABlockLocation(event.getTo()))
                    || r.getShape().contains(new PABlockLocation(event.getFrom()))) {
                // teleport inside the arena, allow, unless:
                if (r.getProtections().contains(RegionProtection.TELEPORT)) {
                    continue;
                }
                maybeFixInvisibility(arena, player);

                return;
            }
        }

        arena.getDebugger().i("onPlayerTeleport: no tele pass, cancelling!", player);
        event.setCancelled(true); // cancel and tell
        arena.msg(player, Language.parse(arena, MSG.NOTICE_NO_TELEPORT));
    }

    private void maybeFixInvisibility(final Arena arena, final Player player) {
        if (arena.getArenaConfig().getBoolean(CFG.USES_EVILINVISIBILITYFIX)) {
            class RunLater implements Runnable {

                @Override
                public void run() {
                    for (final ArenaPlayer otherPlayer : arena.getFighters()) {
                        if (otherPlayer.get() != null) {
                            otherPlayer.get().showPlayer(PVPArena.instance, player);
                        }
                    }
                }

            }
            try {
                Bukkit.getScheduler().runTaskLater(PVPArena.instance, new RunLater(), 5L);
            } catch (final IllegalPluginAccessException e) {

            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerVelocity(final PlayerVelocityEvent event) {
        final Player player = event.getPlayer();

        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();
        if (arena == null) {
            return; // no fighting player or no powerups => OUT
        }
        ArenaModuleManager.onPlayerVelocity(arena, event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerVelocity(final ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            final Player player = (Player) event.getEntity().getShooter();
            final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
            final Arena arena = aPlayer.getArena();
            if (arena == null) {
                return; // no fighting player => OUT
            }
            if (aPlayer.getStatus() == Status.FIGHT || aPlayer.getStatus() == Status.NULL) {
                return;
            }
            event.setCancelled(true);
        }
    }

}
