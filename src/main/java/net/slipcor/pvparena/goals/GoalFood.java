package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.core.StringUtils;

import net.slipcor.pvparena.events.goal.PAGoalEndEvent;
import net.slipcor.pvparena.events.goal.PAGoalScoreEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.StringUtils.startsWithIgnoreCase;

/**
 * <pre>
 * Arena Goal class "Food"
 * </pre>
 * <p/>
 * Players are equipped with raw food, the goal is to bring back cooked food
 * to their base. The first team having gathered enough wins!
 *
 * Business rules:
 * - Each team can own several chests and several furnaces
 * - A furnace can belong to several teams in the same time, but not a chest
 * - Teams not having a furnace will be able to access all of them
 *
 */
public class GoalFood extends ArenaGoal {

    private static final String FOODCHEST = "foodchest";
    private static final String FOODFURNACE = "foodfurnace";

    private String blockTypeName;
    private String blockTeamName;

    public GoalFood() {
        super("Food");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    private final Map<ArenaTeam, Material> teamFoodMap = new HashMap<>();
    private static final Map<Material, Material> COOK_MAP = new HashMap<>();

    static {
        COOK_MAP.put(Material.BEEF, Material.COOKED_BEEF);
        COOK_MAP.put(Material.CHICKEN, Material.COOKED_CHICKEN);
        COOK_MAP.put(Material.COD, Material.COOKED_COD);
        COOK_MAP.put(Material.MUTTON, Material.COOKED_MUTTON);
        COOK_MAP.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        COOK_MAP.put(Material.POTATO, Material.BAKED_POTATO);
        COOK_MAP.put(Material.SALMON, Material.COOKED_SALMON);
        COOK_MAP.put(Material.KELP, Material.DRIED_KELP);
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH);
    }

    @Override
    public boolean checkCommand(final String string) {
        return FOODCHEST.equalsIgnoreCase(string) || FOODFURNACE.equalsIgnoreCase(string);
    }

    @Override
    public List<String> getGoalCommands() {
        return asList(FOODCHEST, FOODFURNACE);
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        final Pattern blockRegexPattern = Pattern.compile("[a-z]+(\\d+)");

        arena.getTeamNames().forEach(teamName -> result.define(new String[]{"add", teamName}));
        arena.getBlocks().forEach(paBlock -> {
            Matcher matcher = blockRegexPattern.matcher(paBlock.getName());
            if(matcher.find()) {
                result.define(new String[]{"remove", paBlock.getTeamName(), matcher.group(1)});
            }
        });
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
    public Boolean shouldRespawnPlayer(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        return true;
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (!PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }

        if (StringUtils.isBlank(this.blockTypeName) || block == null ||
                (block.getType() != Material.CHEST && !(block.getState() instanceof Furnace))) {
            return false;
        }

        return PermissionManager.hasAdminPerm(player) || PermissionManager.hasBuilderPerm(player, this.arena);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].equalsIgnoreCase(FOODCHEST) || args[0].equalsIgnoreCase(FOODFURNACE)) {
            if(args.length != 3 && args.length != 4) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3, 4");
            }  else {
                if (this.arena.getTeam(args[2]) == null) {
                    this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, this.blockTypeName);
                    return;
                }

                if("add".equalsIgnoreCase(args[1])) {
                    this.commitAddBlockCommand(sender, args[0], args[2]);
                } else if ("remove".equalsIgnoreCase(args[1])) {
                    if(args.length != 4) {
                        this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "4");
                        return;
                    }

                    this.commitRemoveBlockCommand(sender, args[0], args[2], args[3]);
                } else {
                    this.arena.msg(sender, MSG.ERROR_COMMAND_INVALID, String.join(" ", args[0], args[1]));
                }
            }
        }
    }

    private void commitAddBlockCommand(CommandSender sender, String blockType, String teamName) {
        if (PAA_Region.activeSelections.containsKey(sender.getName())) {
            PAA_Region.activeSelections.remove(sender.getName());
            this.arena.msg(sender, MSG.GOAL_CLOSED_SELECTION);
        } else {
            this.blockTypeName = blockType;
            this.blockTeamName = teamName;
            PAA_Region.activeSelections.put(sender.getName(), this.arena);
            this.arena.msg(sender, MSG.GOAL_FOOD_TOSET, this.blockTypeName.substring(4));
        }
    }

    private void commitRemoveBlockCommand(CommandSender sender, String blockType, String teamName, String index) {
        String flagName = blockType + index;
        Optional<PABlock> paBlock = this.arena.getBlocks().stream()
                .filter(block -> block.getTeamName().equalsIgnoreCase(teamName) && block.getName().equalsIgnoreCase(flagName))
                .findAny();

        if (!paBlock.isPresent()) {
            this.arena.msg(sender, MSG.GOAL_FOOD_NOTFOUND, teamName, blockType, index);
            return;
        }
        SpawnManager.removeBlock(this.arena, paBlock.get());
        this.arena.msg(sender, MSG.GOAL_FOOD_REMOVED, teamName, blockType, index);
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[FOOD] already ending");
            return;
        }
        debug(this.arena, "[FOOD]");

        final PAGoalEndEvent gEvent = new PAGoalEndEvent(this.arena, this);
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
            this.arena.addWinner(aTeam.getName());
        }

        if (ArenaModuleManager.commitEnd(this.arena, aTeam, null)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final ArenaPlayer respawnPlayer, final boolean doesRespawn, PADeathInfo deathInfo) {

        if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            this.broadcastSimpleDeathMessage(respawnPlayer, deathInfo);
        }

        respawnPlayer.setMayDropInventory(true);
        respawnPlayer.setMayRespawn(true);
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        debug(this.arena, player, "trying to add a new foodchest/furnace");

        final Set<PABlockLocation> flags = SpawnManager.getBlocksStartingWith(this.arena, this.blockTypeName, this.blockTeamName);

        if (flags.contains(new PABlockLocation(block.getLocation()))) {
            this.arena.msg(player, MSG.GOAL_FOOD_EXISTING_BLOCK, this.blockTeamName);
            return true;
        }

        int newIndex = this.arena.getBlocks().stream()
                .filter(paBlock -> paBlock.getTeamName().equalsIgnoreCase(this.blockTeamName) && startsWithIgnoreCase(paBlock.getName(), this.blockTypeName))
                .map(paBlock -> Integer.valueOf(paBlock.getName().replace(this.blockTypeName.toLowerCase(), "")))
                .max(Integer::compareTo)
                .map(index -> index + 1)
                .orElse(0);

        String blockName = this.blockTypeName.toLowerCase() + newIndex;

        if (FOODFURNACE.equalsIgnoreCase(this.blockTypeName)) {
            if (!(block.getState() instanceof Furnace)) {
                return false;
            }
            SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), blockName, this.blockTeamName);
            this.arena.msg(player, MSG.GOAL_FOOD_FURNACE_SET, String.valueOf(newIndex), this.blockTeamName);

        } else {
            if (block.getType() != Material.CHEST) {
                return false;
            }

            Set<PABlock> allFoodChests = SpawnManager.getPABlocksContaining(this.arena, FOODCHEST);
            Optional<PABlock> existingChest = allFoodChests.stream()
                    .filter(paBlock -> paBlock.getLocation().equals(new PABlockLocation(block.getLocation())))
                    .findAny();

            if(existingChest.isPresent()) {
                this.arena.msg(player, MSG.GOAL_FOOD_EXISTING_BLOCK, existingChest.get().getTeamName());
                return false;
            }

            SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), blockName, this.blockTeamName);
            this.arena.msg(player, MSG.GOAL_FOOD_CHEST_SET, String.valueOf(newIndex), this.blockTeamName);
        }

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

    @Override
    public void initiate(final ArenaPlayer arenaPlayer) {
        if (this.getTeamLifeMap().get(arenaPlayer.getArenaTeam()) == null) {
            this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(), this.arena.getConfig()
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

        String teamName = aPlayer.getArenaTeam().getName();
        Set<PABlockLocation> furnacesLocations = SpawnManager.getBlocksStartingWith(this.arena, FOODFURNACE, teamName);

        if (furnacesLocations.isEmpty()) {
            return false;
        }

        if (!furnacesLocations.contains(new PABlockLocation(clickedBlock.getLocation()))) {
            this.arena.msg(player.getPlayer(), MSG.GOAL_FOOD_NOTYOURFOOD);
            return true;
        }

        return false;
    }

    @Override
    public void checkItemTransfer(InventoryMoveItemEvent event) {
        Location chestLoc = ((Chest) event.getDestination().getHolder()).getLocation();

        Optional<PABlock> teamFoodChest = SpawnManager.getPABlocksContaining(this.arena, FOODCHEST).stream()
                .filter(paBlock -> paBlock.getLocation().equals(new PABlockLocation(chestLoc)))
                .findAny();

        if (!teamFoodChest.isPresent()) {
            return;
        }

        final ItemStack stack = event.getItem();

        final ArenaTeam arenaTeam = this.arena.getTeam(teamFoodChest.get().getTeamName());

        if (arenaTeam == null || stack.getType() != COOK_MAP.get(this.teamFoodMap.get(arenaTeam))) {
            return;
        }

        Optional<ArenaPlayer> arenaPlayerOptional = arenaTeam.getTeamMembers().stream().findFirst();

        if (!arenaPlayerOptional.isPresent()) {
            return;
        }

        // INTO container
        final PAGoalScoreEvent gEvent = new PAGoalScoreEvent(this.arena, this,
                arenaPlayerOptional.get(), arenaTeam, (long) stack.getAmount());
        Bukkit.getPluginManager().callEvent(gEvent);
        this.reduceLives(arenaTeam, stack.getAmount());
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

        Location chestLoc = ((Chest) event.getInventory().getHolder()).getLocation();

        if (!SpawnManager.getBlocksContaining(this.arena, FOODCHEST).contains(new PABlockLocation(chestLoc))) {
            return;
        }

        if (!event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        final ItemStack stack = event.getCurrentItem();

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(event.getWhoClicked().getName());

        final ArenaTeam team = aPlayer.getArenaTeam();

        if (team == null || stack == null || stack.getType() != COOK_MAP.get(this.teamFoodMap.get(team))) {
            return;
        }

        final SlotType sType = event.getSlotType();

        if (sType == SlotType.CONTAINER) {
            // OUT of container
            final PAGoalScoreEvent gEvent = new PAGoalScoreEvent(this.arena, this, aPlayer, team, (long) stack.getAmount() * -1);
            Bukkit.getPluginManager().callEvent(gEvent);
            this.reduceLives(team, -stack.getAmount());
        } else {
            // INTO container
            final PAGoalScoreEvent gEvent = new PAGoalScoreEvent(this.arena, this, aPlayer, team, (long) stack.getAmount());
            Bukkit.getPluginManager().callEvent(gEvent);
            this.reduceLives(team, stack.getAmount());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void parseStart() {
        this.clearBlockInventories();

        final int pAmount = this.arena.getConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS);
        final int tAmount = this.arena.getConfig().getInt(CFG.GOAL_FOOD_FTEAMITEMS);

        for (ArenaTeam team : this.arena.getTeams()) {
            Random random = new Random();

            Material teamMaterial;

            // Setting random ingredient per team. No duplicates except if there's more teams than ingredients.
            do {
                teamMaterial = RandomUtils.getRandom(COOK_MAP.keySet(), random);

            } while (this.arena.getTeams().size() < COOK_MAP.size() && this.teamFoodMap.containsValue(teamMaterial));

            this.teamFoodMap.put(team, teamMaterial);

            int teamPartAmount = tAmount / Math.max(team.getTeamMembers().size(), 1);
            int totalAmount = Math.max(pAmount + teamPartAmount, 1);
            
            for (ArenaPlayer teamPlayer : team.getTeamMembers()) {
                teamPlayer.getPlayer().getInventory().addItem(new ItemStack(teamMaterial, totalAmount));
                teamPlayer.getPlayer().updateInventory();
            }
            this.getTeamLifeMap().put(team, this.arena.getConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS));
        }
    }

    private void reduceLives(final ArenaTeam team, final int amount) {
        final int iLives = this.getTeamLifeMap().get(team);

        int maxILives = this.arena.getConfig().getInt(CFG.GOAL_FOOD_FMAXITEMS);
        int newILives = Math.max(iLives - amount, 0);
        int score = maxILives - newILives;
        MSG selectedMsg = (amount < 0) ? MSG.GOAL_FOOD_ITEMS_REMOVED : MSG.GOAL_FOOD_ITEMS_PUT;
        this.arena.broadcast(Language.parse(selectedMsg, team.getColoredName(), Math.abs(amount), score, maxILives));

        if (iLives <= amount && amount > 0) {
            for (ArenaTeam otherTeam : this.arena.getTeams()) {
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
            WorkflowManager.handleEnd(this.arena, false);
            return;
        }

        this.getTeamLifeMap().put(team, iLives - amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void editInventoryOnRefill(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (team == null) {
            return;
        }

        player.getInventory().addItem(new ItemStack(this.teamFoodMap.get(team), this.arena.getConfig().getInt(CFG.GOAL_FOOD_FPLAYERITEMS)));
        player.updateInventory();
    }


    @Override
    public void reset(final boolean force) {
        this.getTeamLifeMap().clear();
        this.clearBlockInventories();
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
    public void unload(final ArenaPlayer arenaPlayer) {
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(arenaPlayer);
        }
    }

    private void clearBlockInventories() {
        this.arena.getBlocks().stream()
                .map(paBlock -> paBlock.getLocation().toLocation().getBlock().getState())
                .filter(blockState -> blockState instanceof Container)
                .map(blockState -> (Container) blockState)
                .forEach(container -> container.getInventory().clear());
    }
}
