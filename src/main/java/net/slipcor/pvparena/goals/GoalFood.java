package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.*;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.slipcor.pvparena.classes.PABlock.PA_BLOCK_TEAM_FORMAT;
import static net.slipcor.pvparena.config.Debugger.debug;

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

public class GoalFood extends ArenaGoal {

    private static final String FOODCHEST = "foodchest";
    private static final String FOODFURNACE = "foodfurnace";

    private String blockName;
    private String blockTeamName;

    public GoalFood() {
        super("Food");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    private final Map<ArenaTeam, Material> foodtypes = new HashMap<>();
    private final Map<Block, ArenaTeam> chestMap = new HashMap<>();
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
        return this.arena.getConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public boolean checkCommand(final String string) {
        return this.arena.getTeams().stream()
                .map(ArenaTeam::getName)
                .anyMatch(sTeam -> string.contains(sTeam + FOODCHEST) || string.contains(sTeam + FOODFURNACE));
    }

    @Override
    public List<String> getGoalCommands() {
        List<String> result = new ArrayList<>();
        if (this.arena != null) {
            this.arena.getTeams()
                    .forEach(team -> {
                        result.add(String.format(PA_BLOCK_TEAM_FORMAT, team.getName(), FOODCHEST));
                        result.add(String.format(PA_BLOCK_TEAM_FORMAT, team.getName(), FOODFURNACE));
                    });
        }
        return result;
    }

    @Override
    public boolean checkEnd() throws GameplayException {
        final int count = TeamManager.countActiveTeams(this.arena);

        if (count == 1) {
            return true; // yep. only one team left. go!
        } else if (count == 0) {
            throw new GameplayException(MSG.ERROR_TEAM_NOT_FOUND);
        }

        return false;
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawnsNames) {
        return SpawnManager.getMissingTeamSpawn(this.arena, spawnsNames);
    }

    @Override
    public Set<PABlock> checkForMissingBlocks(Set<PABlock> blocks) {
        // at least one flag must be set
        return SpawnManager.getMissingBlocksTeamCustom(this.arena, blocks, FOODCHEST);
    }

    @Override
    public Boolean shouldRespawnPlayer(Player player, PADeathInfo deathInfo) {
        return true;
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (!PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }

        if (StringUtils.isBlank(this.blockName) || block == null ||
                (block.getType() != Material.CHEST && block.getType() != Material.FURNACE)) {
            return false;
        }

        return PVPArena.hasAdminPerms(player) || PVPArena.hasCreatePerms(player, this.arena);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].contains(FOODCHEST)) {
            commitSetBlockCommand(sender, args);
        } else if (args[0].contains(FOODFURNACE)) {
            commitSetBlockCommand(sender, args);
        }
    }

    private void commitSetBlockCommand(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String teamName = args[1];
            if (this.arena.getTeam(teamName) == null) {
                this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, this.blockName);
                return;
            }
            this.blockTeamName = teamName;
        } else {
            this.blockTeamName = null;
        }
        this.blockName = args[0];
        PAA_Region.activeSelections.put(sender.getName(), this.arena);

        this.arena.msg(sender, MSG.GOAL_SABOTAGE_TOSETTNT, this.blockName);
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[FOOD] already ending");
            return;
        }
        debug(this.arena, "[FOOD]");

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        ArenaTeam aTeam = null;

        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == PlayerStatus.FIGHT) {
                    aTeam = team;
                    break;
                }
            }
        }

        if (aTeam != null && !force) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "END");

            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "WINNER");
            this.arena.broadcast(Language.parse(MSG.TEAM_HAS_WON, aTeam.getColor()
                    + aTeam.getName() + ChatColor.YELLOW));
        }

        if (ArenaModuleManager.commitEnd(this.arena, aTeam)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player respawnPlayer, final boolean doesRespawn, PADeathInfo deathInfo) {

        if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            this.broadcastSimpleDeathMessage(respawnPlayer, deathInfo);
        }

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(respawnPlayer);
        arenaPlayer.setMayDropInventory(true);
        arenaPlayer.setMayRespawn(true);
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        debug(this.arena, player, "trying to set a foodchest/furnace");

        // command : /pa redflag1
        // location: red1flag:

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), this.blockName, this.blockTeamName);

        if (this.blockName.contains("furnace")) {
            if (block.getType() != Material.FURNACE) {
                return false;
            }
            this.arena.msg(player, MSG.GOAL_FOODFURNACE_SET, this.blockName);

        } else {
            if (block.getType() != Material.CHEST) {
                return false;
            }
            this.arena.msg(player, MSG.GOAL_FOOD_SET, this.blockName);
        }

        PAA_Region.activeSelections.remove(player.getName());
        this.blockName = null;
        this.blockTeamName = null;

        return true;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("items needed: "
                + this.arena.getConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS));
        sender.sendMessage("items per player: "
                + this.arena.getConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS));
        sender.sendMessage("items per team: "
                + this.arena.getConfig().getInt(CFG.GOAL_FOOD_FTEAMITEMS));
    }

    @NotNull
    private Map<ArenaTeam, Material> getFoodMap() {
        return this.foodtypes;
    }

    @Override
    public void initiate(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        if (this.getTeamLifeMap().get(aPlayer.getArenaTeam()) == null) {
            this.getTeamLifeMap().put(aPlayer.getArenaTeam(), this.arena.getConfig()
                    .getInt(CFG.GOAL_FOOD_FMAXITEMS));
        }
    }

    @Override
    public boolean checkInteract(Player player, PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null || clickedBlock.getType() != Material.FURNACE) {
            return false;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (aPlayer.getArena() == null || !aPlayer.getArena().isFightInProgress()) {
            return false;
        }

        final Set<PABlock> spawns = SpawnManager.getPABlocksContaining(this.arena, FOODFURNACE);

        if (spawns.isEmpty()) {
            return false;
        }

        final String teamName = aPlayer.getArenaTeam().getName();

        final Set<PABlockLocation> validSpawns = new HashSet<>();

        for (PABlock block : spawns) {
            final String spawnName = block.getName();
            if (spawnName.startsWith(teamName + FOODFURNACE)) {
                validSpawns.add(block.getLocation());
            }
        }

        if (validSpawns.isEmpty()) {
            return false;
        }

        if (!validSpawns.contains(new PABlockLocation(clickedBlock.getLocation()))) {
            this.arena.msg(player.getPlayer(), MSG.GOAL_FOOD_NOTYOURFOOD);
            return true;
        }

        return false;
    }

    @Override
    public void checkItemTransfer(InventoryMoveItemEvent event) {
        if (this.chestMap.isEmpty() || !this.chestMap.containsKey(((Chest) event.getDestination().getHolder()).getBlock())) {
            return;
        }

        final ItemStack stack = event.getItem();

        final ArenaTeam arenaTeam = this.chestMap.get(((Chest) event.getDestination().getHolder()).getBlock());

        if (arenaTeam == null || stack.getType() != cookmap.get(this.getFoodMap().get(arenaTeam))) {
            return;
        }

        Optional<ArenaPlayer> arenaPlayerOptional = arenaTeam.getTeamMembers().stream().findFirst();

        if (!arenaPlayerOptional.isPresent()) {
            return;
        }

        // INTO container
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this,
                String.format("score:%s:%s:%d", arenaPlayerOptional.get().getName(), arenaTeam.getName(), stack.getAmount()));
        Bukkit.getPluginManager().callEvent(gEvent);
        this.reduceLives(this.arena, arenaTeam, stack.getAmount());
    }

    @Override
    public void checkInventory(InventoryClickEvent event) throws GameplayException {
        if (this.arena == null || !this.arena.isFightInProgress()) {
            return;
        }

        final InventoryType type = event.getInventory().getType();

        if (type != InventoryType.CHEST) {
            return;
        }

        if (!this.chestMap.containsKey(((Chest) event.getInventory()
                .getHolder()).getBlock())) {
            return;
        }

        if (!event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        final ItemStack stack = event.getCurrentItem();

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(event.getWhoClicked().getName());

        final ArenaTeam team = aPlayer.getArenaTeam();

        if (team == null || stack == null || stack.getType() != cookmap.get(this.getFoodMap().get(team))) {
            return;
        }

        final SlotType sType = event.getSlotType();

        if (sType == SlotType.CONTAINER) {
            // OUT of container
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "score:" +
                    aPlayer.getName() + ':' + team.getName() + ":-" + stack.getAmount());
            Bukkit.getPluginManager().callEvent(gEvent);
            this.reduceLives(this.arena, team, -stack.getAmount());
        } else {
            // INTO container
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "score:" +
                    aPlayer.getName() + ':' + team.getName() + ':' + stack.getAmount());
            Bukkit.getPluginManager().callEvent(gEvent);
            this.reduceLives(this.arena, team, stack.getAmount());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void parseStart() {

        final int pAmount = this.arena.getConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS);
        final int tAmount = this.arena.getConfig().getInt(CFG.GOAL_FOOD_FTEAMITEMS);

        for (ArenaTeam team : this.arena.getTeams()) {
            int pos = new Random().nextInt(cookmap.size());
            for (Material mat : cookmap.keySet()) {
                if (pos <= 0) {
                    this.getFoodMap().put(team, mat);
                    break;
                }
                pos--;
            }
            int totalAmount = pAmount;
            totalAmount += tAmount / team.getTeamMembers().size();

            if (totalAmount < 1) {
                totalAmount = 1;
            }
            for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {

                arenaPlayer.getPlayer().getInventory().addItem(new ItemStack(this.getFoodMap().get(team), totalAmount));
                arenaPlayer.getPlayer().updateInventory();
            }
            this.chestMap.put(SpawnManager.getBlockByExactName(this.arena, team.getName() + FOODCHEST).toLocation().getBlock(), team);
            this.getTeamLifeMap().put(team, this.arena.getConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS));
        }
    }

    private void reduceLives(final Arena arena, final ArenaTeam team, final int amount) {
        final int iLives = this.getTeamLifeMap().get(team);

        if (iLives <= amount && amount > 0) {
            for (ArenaTeam otherTeam : arena.getTeams()) {
                if (otherTeam.equals(team)) {
                    continue;
                }
                this.getTeamLifeMap().remove(otherTeam);
                for (ArenaPlayer ap : otherTeam.getTeamMembers()) {
                    if (ap.getStatus() == PlayerStatus.FIGHT) {
                        ap.setStatus(PlayerStatus.LOST);
                    }
                }
            }
            WorkflowManager.handleEnd(arena, false);
            return;
        }

        this.getTeamLifeMap().put(team, iLives - amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void refillInventory(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (team == null) {
            return;
        }

        player.getInventory().addItem(new ItemStack(this.getFoodMap().get(team), this.arena.getConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS)));
        player.updateInventory();
    }


    @Override
    public void reset(final boolean force) {
        this.getTeamLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (config.get("teams") == null) {
            debug(this.arena, "no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            double score = this.arena.getConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS)
                    - this.getTeamLifeMap().getOrDefault(arenaTeam, 0);
            if (scores.containsKey(arenaTeam.getName())) {
                scores.put(arenaTeam.getName(), scores.get(arenaTeam.getName()) + score);
            } else {
                scores.put(arenaTeam.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final Player player) {
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.fromPlayer(player));
        }
    }
}
