package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.*;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAJoinEvent;
import net.slipcor.pvparena.events.PAStartEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.exceptions.GameplayExceptionNotice;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.runnables.InventoryRefillRunnable;
import net.slipcor.pvparena.runnables.PVPActivateRunnable;
import net.slipcor.pvparena.runnables.SpawnCampRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * Class with static methods to abstractly invoke arena goal and modules respecting their priorities
 */
public class WorkflowManager {

    public static boolean handleCommand(final Arena arena, final CommandSender sender, final String[] args) {
        ArenaGoal goal = arena.getGoal();
        if(goal != null && goal.checkCommand(args[0])) {
            goal.commitCommand(sender, args);
            return true;
        }

        for (final ArenaModule am : arena.getMods()) {
            if (am.checkCommand(args[0].toLowerCase())) {
                am.commitCommand(sender, args);
                return true;
            }
        }
        return false;
    }

    public static boolean handleEnd(final Arena arena, final boolean force) {
        debug(arena, "handleEnd: " + arena.getName() + "; force: " + force);

        try {
            ArenaGoal goal = arena.getGoal();
            if(goal.checkEnd()) {
                debug(arena, "committing end: " + goal.getName());
                goal.commitEnd(force);
                return true;
            }
            debug(arena, "FALSE");
        } catch (GameplayException e) {
            arena.msg(Bukkit.getConsoleSender(), MSG.ERROR_ERROR, e.getMessage());
        }

        return false;
    }

    public static int handleGetLives(final Arena arena, final ArenaPlayer aPlayer) {

        if (aPlayer.getStatus() == PlayerStatus.LOUNGE || aPlayer.getStatus() == PlayerStatus.WATCH) {
            return 0;
        }

        return arena.getGoal().getLives(aPlayer);
    }

    public static void handleInteract(final Arena arena, final Player player, final PlayerInteractEvent event) {
        ArenaGoal goal = arena.getGoal();
        if (goal != null && goal.checkInteract(player, event)) {
            event.setCancelled(true);
        }
    }

    public static boolean handleJoin(final Arena arena, final Player player, final String[] args) {
        debug(arena, "handleJoin!");

        ArenaModule joinModule = null;

        // Mods are sorted to get most prioritized first
        List<ArenaModule> sortedModules = arena.getMods().stream()
                .sorted(Comparator.comparingInt(ArenaModule::getPriority).reversed())
                .collect(Collectors.toList());

        try {
            for(ArenaModule mod : sortedModules) {
                mod.checkJoin(player);
            }

            for(ArenaModule mod : sortedModules) {
                if(mod.handleJoin(player)) {
                    joinModule = mod;
                    break;
                }
            }

            if(joinModule == null && !ArenaManager.checkJoin(player, arena)) {
                throw new GameplayException(Language.parse(MSG.ERROR_JOIN_REGION));
            }
        } catch (GameplayExceptionNotice e) {
            arena.msg(player, MSG.NOTICE_NOTICE, e.getMessage());
        } catch (GameplayException e) {
            arena.msg(player, MSG.ERROR_ERROR, e.getMessage());
            return false;
        }

        ArenaGoal joinGoal = arena.getGoal();

        try {
            joinGoal.checkJoin(player, args);
        } catch (GameplayException e) {
            arena.msg(player, MSG.ERROR_ERROR, e.getMessage());
            return false;
        } catch (NullPointerException e) {
            arena.msg(player, MSG.ERROR_NO_GOAL);
            return false;
        }

        if(TeamManager.isArenaFull(arena)){
            arena.msg(player, MSG.ERROR_JOIN_ARENA_FULL);
            return false;
        }

        final ArenaTeam arenaTeam;
        if (args.length < 1) {
            // usage: /pa {arenaname} join | join an arena
            arenaTeam = TeamManager.getRandomTeam(arena);
            if(arenaTeam == null){
                arena.msg(player, MSG.ERROR_NO_TEAM_AVAILABLE);
                return false;
            }
        } else {
            arenaTeam = arena.getTeam(args[0]);
            if(arenaTeam == null) {
                arena.msg(player, MSG.ERROR_TEAMNOTFOUND, args[0]);
                return false;
            }
        }

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        ArenaModuleManager.choosePlayerTeam(arena, player, arenaTeam.getColoredName());

        arena.markPlayedPlayer(player.getName());

        arenaPlayer.setPublicChatting(!arena.getConfig().getBoolean(CFG.CHAT_DEFAULTTEAM));

        debug(arena, "calling join event");
        final PAJoinEvent event = new PAJoinEvent(arena, player, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            debug(arena, "! Join event cancelled by plugin !");
            return false;
        }

        if (joinModule == null) {
            debug(arena, "join module null");
            // join module null, just put the joiner to some spawn
            if (!arena.tryJoin(player, arenaTeam)) {
                return false;
            }

            if (arena.isFreeForAll()) {
                arena.msg(player, arena.getConfig().getString(CFG.MSG_YOUJOINED));
                arena.broadcastExcept(player, Language.parse(arena, CFG.MSG_PLAYERJOINED, player.getName()));
            } else {
                arena.msg(player, arena.getConfig().getString(CFG.MSG_YOUJOINEDTEAM).replace("%1%", arenaTeam.getColoredName() + ChatColor.RESET));
                arena.broadcastExcept(player, Language.parse(arena, CFG.MSG_PLAYERJOINEDTEAM, arenaPlayer.getName(), arenaTeam.getColoredName() + ChatColor.RESET));
            }

            ArenaModuleManager.parseJoin(arena, player, arenaTeam);

            joinGoal.initiate((player));
            ArenaModuleManager.initiate(arena, player);

            if (arena.getFighters().size() >= Math.max(1, arena.getConfig().getInt(CFG.READY_MINPLAYERS))) {
                arena.setFightInProgress(true);

                arena.getTeams().forEach(aTeam -> SpawnManager.distribute(arena, aTeam));
                joinGoal.parseStart();

                arena.getMods().forEach(ArenaModule::parseStart);
            }

            if (arenaPlayer.getArenaClass() != null && arena.startRunner != null) {
                arenaPlayer.setStatus(PlayerStatus.READY);
            }

            return true;
        }

        joinModule.commitJoin(player, arenaTeam);

        ArenaModuleManager.parseJoin(arena, player, arenaTeam);

        if (arenaPlayer.getArenaClass() != null && arena.startRunner != null) {
            arenaPlayer.setStatus(PlayerStatus.READY);
        }

        return true;
    }

    public static void handlePlayerDeath(Arena arena, Player player, EntityDamageEvent event) {

        ArenaGoal goal = arena.getGoal();
        if(goal == null) {
            return;
        }

        PADeathInfo deathInfo = new PADeathInfo(event);
        Player killer = deathInfo.getKiller();

        if(killer != null) {
            applyKillerModifiers(arena, killer);
        }

        Boolean shouldGoalRespawnPlayer = goal.shouldRespawnPlayer(player, deathInfo);
        boolean goalHandlesDeath = (shouldGoalRespawnPlayer != null);
        boolean doesRespawn = goalHandlesDeath && shouldGoalRespawnPlayer;

        StatisticsManager.kill(arena, killer, player, doesRespawn);

        // Calculating dropped exp. Source: https://minecraft.fandom.com/wiki/Experience
        int droppedExp = Math.max(player.getLevel() * 7, 100);

        // Player inventory before death. Will be refilled or not.
        List<ItemStack> droppedInv = new ArrayList<>();
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if (goalHandlesDeath) {
            debug(arena, player, "handled by: " + goal.getName());
            goal.commitPlayerDeath(player, doesRespawn, deathInfo);

            if (arena.getConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY) && arenaPlayer.mayDropInventory()) {
                droppedInv = InventoryManager.drop(player);
            }

            if (doesRespawn && arena.getConfig().getBoolean(CFG.PLAYER_DROPSEXP)) {
                debug(arena, player, "exp: " + droppedExp);
                InventoryManager.dropExp(player, droppedExp);
            }

            if (arenaPlayer.mayRespawn()) {
                handleRespawn(arenaPlayer, deathInfo, droppedInv);
            }

        } else {
            debug(arena, player, "goal doesn't handles player deaths");

            if (arena.getConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
                droppedInv = InventoryManager.drop(player);
            }

            if (arena.getConfig().getBoolean(CFG.PLAYER_DROPSEXP)) {
                debug(arena, player, "exp: " + droppedExp);
                InventoryManager.dropExp(player, droppedExp);
            }

            final ArenaTeam respawnTeam = ArenaPlayer.fromPlayer(player.getName()).getArenaTeam();

            if (arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                String deathCause = arena.parseDeathCause(player, event.getCause(), killer);
                arena.broadcast(Language.parse(MSG.FIGHT_KILLED_BY,
                        respawnTeam.colorizePlayer(player) + ChatColor.YELLOW,
                        deathCause));
            }

            handleRespawn(arenaPlayer, deathInfo, droppedInv);
        }

        debug(arena, player, "parsing death: " + goal.getName());
        goal.parsePlayerDeath(player, deathInfo);
        ArenaModuleManager.parsePlayerDeath(arena, player, event);
    }

    private static void applyKillerModifiers(Arena arena, Player killer) {
        killer.setFoodLevel(killer.getFoodLevel() + arena.getConfig().getInt(CFG.PLAYER_FEEDFORKILL));
        if (arena.getConfig().getBoolean(CFG.PLAYER_HEALFORKILL)) {
            PlayerState.playersetHealth(killer, (int) killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        }
        if (arena.getConfig().getBoolean(CFG.PLAYER_REFILLFORKILL)) {
            InventoryManager.clearInventory(killer);
            ArenaPlayer.fromPlayer(killer).getArenaClass().equip(killer);
        }
        if (arena.getConfig().getItems(CFG.PLAYER_ITEMSONKILL) != null) {
            ItemStack[] items = arena.getConfig().getItems(CFG.PLAYER_ITEMSONKILL);
            for (ItemStack item : items) {
                if (item != null) {
                    killer.getInventory().addItem(item.clone());
                }
            }
        }
        if (arena.getConfig().getBoolean(CFG.USES_TELEPORTONKILL)) {
            SpawnManager.respawn(ArenaPlayer.fromPlayer(killer.getName()), null);
        }
    }

    /**
     * If player should respawn after death, teleport them to a new spawn, refill their inventory and revive them
     * @param aPlayer the player to respawn
     * @param deathInfo death info object (cause & killer)
     * @param keptItems items kept from inventory dropped by player
     */
    public static void handleRespawn(ArenaPlayer aPlayer, PADeathInfo deathInfo, List<ItemStack> keptItems) {

        for (final ArenaModule mod : aPlayer.getArena().getMods()) {
            if (mod.tryDeathOverride(aPlayer, deathInfo, keptItems)) {
                return;
            }
        }
        debug(aPlayer, "handleRespawn!");
        new InventoryRefillRunnable(aPlayer.getArena(), aPlayer.getPlayer(), keptItems);
        aPlayer.revive(deathInfo);
        SpawnManager.respawn(aPlayer, null);

        // Resetting mayRespawn property
        aPlayer.setMayRespawn(false);
    }

    /**
     * try to set a flag
     *
     * @param player the player trying to set
     * @param block  the block being set
     * @return true if the handling is successful and if the event should be
     * cancelled
     */
    public static boolean handleSetFlag(final Player player, final Block block) {
        final Arena arena = PAA_Region.activeSelections.get(player.getName());

        if (arena == null) {
            return false;
        }

        ArenaGoal goal = arena.getGoal();
        if (goal.checkSetBlock(player, block)) {
            return goal.commitSetFlag(player, block);
        }

        return false;
    }

    public static boolean handleSpectate(final Arena arena, final Player player) {
        debug(arena, player, "handling spectator");

        ArenaModule spectateModule = null;

        // Mods are sorted to get most prioritized first
        List<ArenaModule> sortedModules = arena.getMods().stream()
                .sorted(Comparator.comparingInt(ArenaModule::getPriority).reversed())
                .collect(Collectors.toList());

        try {
            for(ArenaModule mod : sortedModules) {
                mod.checkSpectate(player);
            }

            for(ArenaModule mod : sortedModules) {
                if(mod.handleSpectate(player)) {
                    spectateModule = mod;
                    break;
                }
            }
        } catch (GameplayException e) {
            arena.msg(player, MSG.ERROR_ERROR, e.getMessage());
            return false;
        }

        if(spectateModule == null) {
            debug(arena, player, "commit null");
            return false;
        }

        final PAJoinEvent event = new PAJoinEvent(arena, player, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            debug(arena, "! Spectate event cancelled by plugin !");
            return false;
        }

        spectateModule.commitSpectate(player);
        return true;
    }

    public static Boolean handleStart(final Arena arena, final CommandSender sender, final boolean force) {
        debug(arena, "handling start!");

        if (!force && arena.getFighters().size() < Math.min(2, arena.getConfig().getInt(CFG.READY_MINPLAYERS))) {
            debug(arena, "not forcing and we have less than minplayers");
            return null;
        }

        final PAStartEvent event = new PAStartEvent(arena);
        Bukkit.getPluginManager().callEvent(event);
        if (!force && event.isCancelled()) {
            debug(arena, "not forcing and cancelled by other plugin");
            return false;
        }

        debug(arena, sender, "teleporting all players to their spawns");

        ArenaGoal goal = arena.getGoal();
        if (goal.overridesStart()) {
            goal.commitStart(); // override spawning
        } else {
            for (final ArenaTeam team : arena.getTeams()) {
                SpawnManager.distribute(arena, team);
            }
        }

        debug(arena, sender, "teleported everyone!");

        arena.broadcast(Language.parse(MSG.FIGHT_BEGINS));
        arena.setFightInProgress(true);

        goal.parseStart();

        for (final ArenaModule x : arena.getMods()) {
            x.parseStart();
        }

        final SpawnCampRunnable scr = new SpawnCampRunnable(arena);
        scr.runTaskTimer(PVPArena.getInstance(), 100L, arena.getConfig().getInt(CFG.TIME_REGIONTIMER));

        if (arena.getConfig().getInt(CFG.TIME_PVP) > 0) {
            arena.pvpRunner = new PVPActivateRunnable(arena, arena.getConfig().getInt(CFG.TIME_PVP));
        }

        arena.setStartingTime();
        arena.getScoreboard().refresh();
        return true;
    }
}
