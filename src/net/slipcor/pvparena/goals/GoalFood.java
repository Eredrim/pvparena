package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * <pre>
 * Arena Goal class "Food"
 * </pre>
 * <p/>
 * Players are equipped with raw food, the goal is to bring back cooked food
 * to their base. The first team having gathered enough wins!
 *
 * @author slipcor
 */

public class GoalFood extends ArenaGoal implements Listener {
    public GoalFood() {
        super("Food");
        debug = new Debug(105);
    }

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    private static final int PRIORITY = 12;

    private Map<ArenaTeam, Material> foodtypes;
    private Map<Block, ArenaTeam> chestMap;
    private static final Map<Material, Material> cookmap = new HashMap<>();

    static {
        cookmap.put(Material.BEEF, Material.COOKED_BEEF);
        cookmap.put(Material.CHICKEN, Material.COOKED_CHICKEN);
        cookmap.put(Material.COD, Material.COOKED_COD);
        cookmap.put(Material.MUTTON, Material.COOKED_MUTTON);
        cookmap.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        cookmap.put(Material.POTATO, Material.BAKED_POTATO);
        cookmap.put(Material.SALMON, Material.COOKED_SALMON);
    }

    @Override
    public boolean allowsJoinInBattle() {
        return arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public PACheck checkCommand(final PACheck res, final String string) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        for (final ArenaTeam team : arena.getTeams()) {
            final String sTeam = team.getName();
            if (string.contains(sTeam + "foodchest") || string.contains(sTeam + "foodfurnace")) {
                res.setPriority(this, PRIORITY);
            }
        }

        return res;
    }

    @Override
    public List<String> getMain() {
        final List<String> result = new ArrayList<>();
        if (arena != null) {
            for (final ArenaTeam team : arena.getTeams()) {
                final String sTeam = team.getName();
                result.add(sTeam + "foodchest");
                result.add(sTeam + "foodfurnace");
            }
        }
        return result;
    }

    @Override
    public PACheck checkEnd(final PACheck res) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        final int count = TeamManager.countActiveTeams(arena);

        if (count == 1) {
            res.setPriority(this, PRIORITY); // yep. only one team left. go!
        } else if (count == 0) {
            res.setError(this, MSG.ERROR_NOTEAMFOUND.toString());
        }

        return res;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        final String error = checkForMissingTeamSpawn(list);
        if (error != null) {
            return error;
        }
        return checkForMissingTeamCustom(list, "foodchest");
    }

    @Override
    public PACheck checkJoin(final CommandSender sender, final PACheck res, final String[] args) {
        if (res.getPriority() >= PRIORITY) {
            return res;
        }

        final int maxPlayers = arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS);
        final int maxTeamPlayers = arena.getArenaConfig().getInt(
                CFG.READY_MAXTEAMPLAYERS);

        if (maxPlayers > 0 && arena.getFighters().size() >= maxPlayers) {
            res.setError(this, Language.parse(arena, MSG.ERROR_JOIN_ARENA_FULL));
            return res;
        }

        if (args == null || args.length < 1) {
            return res;
        }

        if (!arena.isFreeForAll()) {
            final ArenaTeam team = arena.getTeam(args[0]);

            if (team != null && maxTeamPlayers > 0
                    && team.getTeamMembers().size() >= maxTeamPlayers) {
                res.setError(this, Language.parse(arena, MSG.ERROR_JOIN_TEAM_FULL, team.getName()));
                return res;
            }
        }

        res.setPriority(this, PRIORITY);
        return res;
    }

    @Override
    public PACheck checkPlayerDeath(final PACheck res, final Player player) {
        if (res.getPriority() <= PRIORITY) {
            res.setPriority(this, PRIORITY);
        }
        return res;
    }

    @Override
    public PACheck checkSetBlock(final PACheck res, final Player player, final Block block) {

        if (res.getPriority() > PRIORITY
                || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return res;
        }
        if (flagName == null || block == null
                || block.getType() != Material.CHEST && block.getType() != Material.FURNACE) {
            return res;
        }

        if (!PermissionManager.hasAdminPerm(player)
                && !PermissionManager.hasBuilderPerm(player, arena)) {
            return res;
        }
        res.setPriority(this, PRIORITY); // success :)

        return res;
    }

    private String flagName;

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].contains("foodchest")) {
            for (final ArenaTeam team : arena.getTeams()) {
                final String sTeam = team.getName();
                if (args[0].contains(sTeam + "foodchest")) {
                    flagName = args[0];
                    PAA_Region.activeSelections.put(sender.getName(), arena);

                    arena.msg(sender,
                            Language.parse(arena, MSG.GOAL_FOOD_TOSET, flagName));
                }
            }
        } else if (args[0].contains("foodfurnace")) {
            for (final ArenaTeam team : arena.getTeams()) {
                final String sTeam = team.getName();
                if (args[0].contains(sTeam + "foodfurnace")) {
                    flagName = args[0];
                    PAA_Region.activeSelections.put(sender.getName(), arena);

                    arena.msg(sender,
                            Language.parse(arena, MSG.GOAL_FOODFURNACE_TOSET, flagName));
                }
            }
        }
    }

    @Override
    public void commitEnd(final boolean force) {
        if (arena.realEndRunner != null) {
            arena.getDebugger().i("[FOOD] already ending");
            return;
        }
        arena.getDebugger().i("[FOOD]");

        final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        ArenaTeam aTeam = null;

        for (final ArenaTeam team : arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == Status.FIGHT) {
                    aTeam = team;
                    break;
                }
            }
        }

        if (aTeam != null && !force) {
            ArenaModuleManager.announce(
                    arena,
                    Language.parse(arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "END");

            ArenaModuleManager.announce(
                    arena,
                    Language.parse(arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "WINNER");
            arena.broadcast(Language.parse(arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                    + aTeam.getName() + ChatColor.YELLOW));
        }

        if (ArenaModuleManager.commitEnd(arena, aTeam)) {
            return;
        }
        new EndRunnable(arena, arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player respawnPlayer, final boolean doesRespawn,
                                  final String error, final PlayerDeathEvent event) {

        if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            this.broadcastSimpleDeathMessage(respawnPlayer, event);
        }

        final List<ItemStack> returned;

        if (arena.getArenaConfig().getBoolean(
                CFG.PLAYER_DROPSINVENTORY)) {
            returned = InventoryManager.drop(respawnPlayer);
            event.getDrops().clear();
        } else {
            returned = new ArrayList<>(event.getDrops());
        }

        PACheck.handleRespawn(this.arena, ArenaPlayer.parsePlayer(respawnPlayer.getName()), returned);

    }

    @Override
    public boolean commitSetFlag(final Player player, final Block block) {

        arena.getDebugger().i("trying to set a foodchest/furnace", player);

        // command : /pa redflag1
        // location: red1flag:

        SpawnManager.setBlock(arena, new PABlockLocation(block.getLocation()),
                flagName);


        if (flagName.contains("furnace")) {
            if (block.getType() != Material.FURNACE) {
                return false;
            }
            arena.msg(player, Language.parse(arena, MSG.GOAL_FOODFURNACE_SET, flagName));

        } else {
            if (block.getType() != Material.CHEST) {
                return false;
            }
            arena.msg(player, Language.parse(arena, MSG.GOAL_FOOD_SET, flagName));

        }

        PAA_Region.activeSelections.remove(player.getName());
        flagName = "";

        return true;
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        Bukkit.getPluginManager().registerEvents(this, PVPArena.instance);
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("items needed: "
                + arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS));
        sender.sendMessage("items per player: "
                + arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS));
        sender.sendMessage("items per team: "
                + arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FTEAMITEMS));
    }

    private Map<ArenaTeam, Material> getFoodMap() {
        if (foodtypes == null) {
            foodtypes = new HashMap<>();
        }
        return foodtypes;
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(arena.getArenaConfig()
                            .getInt(CFG.GOAL_FOOD_FMAXITEMS) - (getLifeMap()
                            .containsKey(aPlayer.getArenaTeam().getName()) ? getLifeMap()
                            .get(aPlayer.getArenaTeam().getName()) : 0)));
        }
        return res;
    }

    @Override
    public boolean hasSpawn(final String string) {
        for (final String teamName : arena.getTeamNames()) {
            if (string.toLowerCase().startsWith(
                    teamName.toLowerCase() + "spawn")) {
                return true;
            }
            if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                for (final ArenaClass aClass : arena.getClasses()) {
                    if (string.toLowerCase().startsWith(teamName.toLowerCase() +
                            aClass.getName().toLowerCase() + "spawn")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void initate(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        if (getLifeMap().get(aPlayer.getArenaTeam().getName()) == null) {
            getLifeMap().put(aPlayer.getArenaTeam().getName(), arena.getArenaConfig()
                    .getInt(CFG.GOAL_FOOD_FMAXITEMS));
        }
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceClick(final PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock().getType() != Material.FURNACE) {
            return;
        }

        final ArenaPlayer player = ArenaPlayer.parsePlayer(event.getPlayer().getName());

        if (player.getArena() == null || !player.getArena().isFightInProgress()) {
            return;
        }

        final Set<PABlock> spawns = SpawnManager.getPABlocksContaining(arena, "foodfurnace");

        if (spawns.size() < 1) {
            return;
        }

        final String teamName = player.getArenaTeam().getName();

        final Set<PABlockLocation> validSpawns = new HashSet<>();

        for (final PABlock block : spawns) {
            final String spawnName = block.getName();
            if (spawnName.startsWith(teamName + "foodfurnace")) {
                validSpawns.add(block.getLocation());
            }
        }

        if (validSpawns.size() < 1) {
            return;
        }

        if (!validSpawns.contains(new PABlockLocation(event.getClickedBlock().getLocation()))) {
            arena.msg(player.get(), Language.parse(arena, MSG.GOAL_FOOD_NOTYOURFOOD));
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemTransfer(final InventoryMoveItemEvent event) {

        if (arena == null || !arena.isFightInProgress()) {
            return;
        }

        final InventoryType type = event.getDestination().getType();

        if (type != InventoryType.CHEST) {
            return;
        }

        if (chestMap == null || !chestMap.containsKey(((Chest) event.getDestination()
                .getHolder()).getBlock())) {
            return;
        }

        final ItemStack stack = event.getItem();

        final ArenaTeam team = chestMap.get(((Chest) event.getDestination()
                .getHolder()).getBlock());

        if (team == null || stack == null || stack.getType() != cookmap.get(getFoodMap().get(team))) {
            return;
        }

        ArenaPlayer noone = null;

        for (final ArenaPlayer player : team.getTeamMembers()) {
            noone = player;
            break;
        }

        if (noone == null) {
            return;
        }

        // INTO container
        final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "score:" +
                noone.getName() + ':' + team.getName() + ':' + stack.getAmount());
        Bukkit.getPluginManager().callEvent(gEvent);
        reduceLives(arena, team, stack.getAmount());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {

        if (arena == null || !arena.isFightInProgress()) {
            return;
        }

        final InventoryType type = event.getInventory().getType();

        if (type != InventoryType.CHEST) {
            return;
        }

        if (chestMap == null || !chestMap.containsKey(((Chest) event.getInventory()
                .getHolder()).getBlock())) {
            return;
        }

        if (!event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        final ItemStack stack = event.getCurrentItem();

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(event.getWhoClicked().getName());

        final ArenaTeam team = aPlayer.getArenaTeam();

        if (team == null || stack == null || stack.getType() != cookmap.get(getFoodMap().get(team))) {
            return;
        }

        final SlotType sType = event.getSlotType();

        if (sType == SlotType.CONTAINER) {
            // OUT of container
            final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "score:" +
                    aPlayer.getName() + ':' + team.getName() + ":-" + stack.getAmount());
            Bukkit.getPluginManager().callEvent(gEvent);
            reduceLives(arena, team, -stack.getAmount());
        } else {
            // INTO container
            final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "score:" +
                    aPlayer.getName() + ':' + team.getName() + ':' + stack.getAmount());
            Bukkit.getPluginManager().callEvent(gEvent);
            reduceLives(arena, team, stack.getAmount());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void parseStart() {

        final int pAmount = arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS);
        final int tAmount = arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FTEAMITEMS);

        chestMap = new HashMap<>();

        for (final ArenaTeam team : arena.getTeams()) {
            int pos = new Random().nextInt(cookmap.size());
            for (final Material mat : cookmap.keySet()) {
                if (pos <= 0) {
                    getFoodMap().put(team, mat);
                    break;
                }
                pos--;
            }
            int totalAmount = pAmount;
            totalAmount += tAmount / team.getTeamMembers().size();

            if (totalAmount < 1) {
                totalAmount = 1;
            }
            for (final ArenaPlayer player : team.getTeamMembers()) {

                player.get().getInventory().addItem(new ItemStack(getFoodMap().get(team), totalAmount));
                player.get().updateInventory();
            }
            chestMap.put(SpawnManager.getBlockByExactName(arena, team.getName() + "foodchest").toLocation().getBlock(), team);
            getLifeMap().put(team.getName(),
                    arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS));
        }
    }

    private void reduceLives(final Arena arena, final ArenaTeam team, final int amount) {
        final int iLives = getLifeMap().get(team.getName());

        if (iLives <= amount && amount > 0) {
            for (final ArenaTeam otherTeam : arena.getTeams()) {
                if (otherTeam.equals(team)) {
                    continue;
                }
                getLifeMap().remove(otherTeam.getName());
                for (final ArenaPlayer ap : otherTeam.getTeamMembers()) {
                    if (ap.getStatus() == Status.FIGHT) {
                        ap.setStatus(Status.LOST);/*
                        arena.removePlayer(ap.get(), CFG.TP_LOSE.toString(),
								true, false);*/
                    }
                }
            }
            PACheck.handleEnd(arena, false);
            return;
        }

        getLifeMap().put(team.getName(), iLives - amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void refillInventory(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (team == null) {
            return;
        }

        player.getInventory().addItem(new ItemStack(getFoodMap().get(team), arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS)));
        player.updateInventory();
    }


    @Override
    public void reset(final boolean force) {
        getLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (arena.isFreeForAll()) {
            return;
        }

        if (config.get("teams.free") != null) {
            config.set("teams", null);
        }
        if (config.get("teams") == null) {
            arena.getDebugger().i("no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaTeam team : arena.getTeams()) {
            double score = arena.getArenaConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS)
                    - (getLifeMap().containsKey(team.getName()) ? getLifeMap().get(team
                    .getName()) : 0);
            if (scores.containsKey(team.getName())) {
                scores.put(team.getName(), scores.get(team.getName()) + score);
            } else {
                scores.put(team.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final Player player) {
        if (allowsJoinInBattle()) {
            arena.hasNotPlayed(ArenaPlayer.parsePlayer(player.getName()));
        }
    }
}
