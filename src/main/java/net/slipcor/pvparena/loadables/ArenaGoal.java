package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.exceptions.GameplayException;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 * Arena Goal class
 * </pre>
 * <p/>
 * The framework for adding goals to an arena
 *
 * @author slipcor
 */

public class ArenaGoal implements IArenaCommandHandler {
    public static final String SPAWN = "spawn";
    protected String name;
    protected Arena arena;
    protected Map<ArenaTeam, Integer> teamLifeMap = new HashMap<>();
    protected Map<Player, Integer> playerLifeMap = new HashMap<>();


    /**
     * create an arena type instance
     *
     * @param goalName the arena type name
     */
    public ArenaGoal(final String goalName) {
        this.name = goalName;
    }

    public String getName() {
        return this.name;
    }

    public boolean isFreeForAll() { return false; }

    /**
     * does the arena type allow joining in battle?
     */
    public boolean allowsJoinInBattle() {
        return false;
    }

    /**
     * check if the goal should commit a command
     *
     * @param string the command argument
     * @return true if the goal commits the command
     */
    public boolean checkCommand(final String string) {
        return false;
    }

    @Override
    public final List<String> getMain() {
        return Collections.emptyList();
    }

    /**
     * Get Main commands for the goal
     *
     * @return list of commands for the goal
     */
    public List<String> getGoalCommands() {
        return getMain();
    }

    @Override
    public final List<String> getShort() {
        return Collections.emptyList();
    }

    /**
     * Get Main shortcuts commands for the goal
     *
     * @return list of shortcuts commands for the goal
     */
    public List<String> getGoalShortCommands() {
        return getShort();
    }

    @Override
    public final CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }

    /**
     * Get sub-commands for the goal
     *
     * @return list of sub-commands for the goal
     */
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        return getSubs(arena);
    }

    @Override
    public boolean hasPerms(final CommandSender sender, final Arena arena) {
        if (arena == null) {
            return PVPArena.hasAdminPerms(sender);
        }
        return PVPArena.hasAdminPerms(sender) || PVPArena.hasCreatePerms(sender, arena);
    }

    /**
     * the goal version (should be overridden!)
     *
     * @return the version String
     */
    public String version() {
        return "outdated";
    }

    public void checkBreak(BlockBreakEvent event) throws GameplayException {
    }

    public void checkExplode(EntityExplodeEvent event) throws GameplayException {
    }

    public void checkCraft(CraftItemEvent result) throws GameplayException {
    }

    public void checkDrop(PlayerDropItemEvent event) throws GameplayException {
    }

    public void checkInventory(InventoryClickEvent event) throws GameplayException {
    }

    public void checkPickup(EntityPickupItemEvent event) throws GameplayException {
    }

    public void checkPlace(BlockPlaceEvent event) throws GameplayException {
    }

    public void checkItemTransfer(InventoryMoveItemEvent event) throws GameplayException {
    }

    /**
     * check if the goal should commit the end
     *
     * @return true if the goal handles the end
     */
    public boolean checkEnd() throws GameplayException {
        return false;
    }

    /**
     * check if all necessary spawns are set
     *
     * @param spawnsNames the list of all set spawns
     * @return empty if ready, error message otherwise
     */
    public Set<String> checkForMissingSpawns(final Set<String> spawnsNames) {
        return new HashSet<>();
    }

    /**
     * check if necessary FFA spawns are set
     *
     * @return null if ready, error message otherwise
     */
    protected Set<String> checkForMissingFFASpawn(final Set<String> spawnNames) {
        final Set<String> errors = new HashSet<>();
        int minPlayers = this.arena.getConfig().getInt(CFG.READY_MINPLAYERS);
        for (int i = 1; i <= minPlayers; i++) {
            if(!spawnNames.contains(SPAWN + i)){
                errors.add(String.format("spawn%s", i));
            }
        }

        return errors;
    }

    /**
     * check if necessary custom FFA spawns areList.of set
     *
     * + check if a spawn start with custom
     *
     * @return null if ready, error message otherwise
     */
    protected Set<String> checkForMissingFFACustom(final Set<String> spawnsNames, final String custom) {
        final Set<String> errors = new HashSet<>();
        if (!spawnsNames.contains(custom)
                && spawnsNames.stream().noneMatch(spawnName -> spawnName.startsWith(custom))){
            errors.add(custom);
        }
        return errors;
    }

    /**
     * check if necessary team spawns are set
     *
     * @return empty if ready, team(s) missing otherwise
     */
    protected Set<String> checkForMissingTeamSpawn(final Set<String> spawnsNames) {
        return this.arena.getTeams().stream()
                .map(team -> team.getName() + SPAWN)
                .filter(teamSpawnName -> !spawnsNames.contains(teamSpawnName))
                .collect(Collectors.toSet());
    }

    /**
     * check if necessary custom team spawns are set
     *
     * + check if a spawn start with team's name + custom
     *
     * @return null if ready, error message otherwise
     */
    protected Set<String> checkForMissingTeamCustom(final Set<String> spawnsNames, final String custom) {
        return this.arena.getTeams().stream()
                .filter(arenaTeam -> !spawnsNames.contains(arenaTeam.getName() + custom))
                .filter(arenaTeam -> spawnsNames.stream().noneMatch(spawnName -> spawnName.startsWith(arenaTeam + custom)))
                .map(arenaTeam -> arenaTeam.getName() + custom)
                .collect(Collectors.toSet());
    }

    /**
     * hook into an interacting player
     *
     * @param player       the interacting player
     * @param event        the interact event
     * @return true if the goals handle the event
     */
    public boolean checkInteract(final Player player, final PlayerInteractEvent event) {
        return false;
    }

    /**
     * check if the goal should commit a player join
     *  @param player the joining player
     * @param args   command arguments
     */
    public void checkJoin(final Player player, final String[] args) throws GameplayException {
        final int maxPlayers = this.arena.getConfig().getInt(CFG.READY_MAXPLAYERS);
        final int maxTeamPlayers = this.arena.getConfig().getInt(
                CFG.READY_MAXTEAMPLAYERS);

        if (maxPlayers > 0 && this.arena.getFighters().size() >= maxPlayers) {
            throw new GameplayException(Language.parse(Language.MSG.ERROR_JOIN_ARENA_FULL));
        }

        if (!this.arena.isFreeForAll() && args != null && args.length > 0) {
            final ArenaTeam team = this.arena.getTeam(args[0]);

            if (team != null && maxTeamPlayers > 0 && team.getTeamMembers().size() >= maxTeamPlayers) {
                throw new GameplayException(Language.parse(Language.MSG.ERROR_JOIN_TEAM_FULL));
            }
        }
    }

    /**
     * check if the goal should commit a player death
     *
     * @param player the dying player
     * @param deathInfo death info
     * @return true if player should respawn, false otherwise, null if goal doesn't handle respawn
     */
    public Boolean shouldRespawnPlayer(Player player, PADeathInfo deathInfo) {
        return null;
    }

    /**
     * check if the goal should set a block
     *
     * @param player the setting player
     * @param block  the block being set
     * @return true if the handling is successful
     */
    public boolean checkSetBlock(final Player player, final Block block) {
        return false;
    }

    /**
     * check if the goal should start the game
     *
     * @return true if the goal overrides default starting
     */
    public boolean overridesStart() {
        return false;
    }

    /**
     * commit a command
     *
     * @param sender the committing player
     * @param args   the command arguments
     */
    public void commitCommand(final CommandSender sender, final String[] args) {
        throw new IllegalStateException(this.getName());
    }

    /**
     * commit the arene end
     *
     * @param force true, if we need to force
     */
    public void commitEnd(final boolean force) {
        throw new IllegalStateException(this.getName());
    }

    /**
     * commit a player death
     * @param player      the dying player
     * @param doesRespawn true if the player will respawn
     * @param deathInfo death information object containing cause and damager
     */
    public void commitPlayerDeath(Player player, boolean doesRespawn, PADeathInfo deathInfo) {
        throw new IllegalStateException(this.getName());
    }

    /**
     * commit setting a flag
     *
     * @param player the setting player
     * @param block  the flag block
     * @return true if the interact event should be cancelled
     */
    public boolean commitSetFlag(final Player player, final Block block) {
        throw new IllegalStateException(this.getName());
    }

    /**
     * commit an arena start
     */
    public void commitStart() {
        throw new IllegalStateException(this.getName());
    }

    /**
     * hook into the config parsing
     *
     * @param config the arena config
     */
    public void configParse(final YamlConfiguration config) {
    }

    /**
     * hook into disconnecting a player
     *
     * @param player the player being disconnected
     */
    public void disconnect(final ArenaPlayer player) {
    }

    /**
     * display information about the goal
     *
     * @param sender the sender to receive more information
     */
    public void displayInfo(final CommandSender sender) {
    }

    /**
     * Getter for the goal team life map
     *
     * @return the goal team life map
     */
    @NotNull
    public Map<ArenaTeam, Integer> getTeamLifeMap() {
        return this.teamLifeMap;
    }

    /**
     * Getter for the goal life map
     *
     * @return the goal life map
     */
    @NotNull
    public Map<Player, Integer> getPlayerLifeMap() {
        return this.playerLifeMap;
    }

    /**
     * Get a player's remaining lives
     *
     * @param arenaPlayer the player to check
     * @return the PACheck instance for more information, eventually an ERROR
     * containing the lives
     */
    public int getLives(ArenaPlayer arenaPlayer) {
        if(this.arena.isFreeForAll()){
            return this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
        } else {
            return this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
        }
    }

    /**
     * does a goal know this spawn?
     *
     * @param string the spawn name to check
     * @return if the goal knows this spawn
     */
    public boolean hasSpawn(final String string) {
        return false;
    }

    /**
     * hook into initializing a player being put directly to the battlefield
     * (contrary to lounge/spectate)
     *
     * @param player the player being put
     */
    public void initiate(final Player player) {
    }

    /**
     * hook into an arena joining the game after it has begin
     *
     * @param player the joining player
     */
    public void lateJoin(final Player player) {
    }

    /**
     * hook into the initial goal loading
     */
    public void onThisLoad() {
    }

    public void onPlayerPickUp(final EntityPickupItemEvent event) {
    }

    /**
     * hook into a player leaving the arena
     *
     * @param player the leaving player
     */
    public void parseLeave(final Player player) {
    }

    /**
     * hook into a player dying
     *  @param player          the dying player
     * @param deathInfo the last damage cause
     */
    public void parsePlayerDeath(Player player, PADeathInfo deathInfo) {
    }

    /**
     * hook into an arena start
     */
    public void parseStart() {
    }

    /**
     * hook into a player being refilled
     *
     * @param player the player being refilled
     */
    public void refillInventory(final Player player) {
    }

    /**
     * hook into an arena reset
     *
     * @param force is the resetting forced?
     */
    public void reset(final boolean force) {
    }

    /**
     * update the arena instance (should only be used on instanciation)
     *
     * @param arena the new instance
     */
    public void setArena(final Arena arena) {
        this.arena = arena;
    }

    /**
     * hook into setting config defaults
     *
     * @param config the arena config
     */
    public void setDefaults(final YamlConfiguration config) {
    }

    /**
     * set all player lives
     *
     * @param lives the value being set
     */
    public void setPlayersLives(final int lives) {
        this.playerLifeMap.entrySet()
                .forEach(playerIntegerEntry -> playerIntegerEntry.setValue(lives));
    }
    /**
     * set a specific player's lives
     *
     * @param player the player to update
     * @param lives  the value being set
     */
    public void setPlayerLives(final Player player, final int lives) {
        this.playerLifeMap.put(player, lives);
    }

    /**
     * hook into the score calculation
     *
     * @param scores the scores so far: team name or player name is key
     * @return the updated map
     */
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {
        return scores;
    }

    /**
     * hook into arena player unloading
     *
     * @param player the player to unload
     */
    public void unload(final Player player) {
        if(player != null) {
            this.getPlayerLifeMap().remove(player);
        }
    }

    protected void updateLives(final ArenaTeam team, final int value) {
        if (this.arena.getConfig().getBoolean(CFG.GENERAL_ADDLIVESPERPLAYER)) {
            this.getTeamLifeMap().put(team, team.getTeamMembers().size() * value);
        } else {
            this.getTeamLifeMap().put(team, value);
        }
    }

    protected void updateLives(final Player player, final int value) {
        if (this.arena.getConfig().getBoolean(CFG.GENERAL_ADDLIVESPERPLAYER)) {
            this.getPlayerLifeMap().put(player, this.arena.getFighters().size() * value);
        } else {
            this.getPlayerLifeMap().put(player, value);
        }
    }

    protected void broadcastSimpleDeathMessage(Player player, PADeathInfo deathInfo) {
        this.broadcastDeathMessage(Language.MSG.FIGHT_KILLED_BY, player, deathInfo, null);
    }

    protected void broadcastDeathMessage(Language.MSG deathMessage, Player player, PADeathInfo deathInfo, Integer remainingLives) {
        final ArenaTeam respawnTeam = ArenaPlayer.fromPlayer(player).getArenaTeam();

        String deathCause = this.arena.parseDeathCause(player, deathInfo.getCause(), deathInfo.getDamager());
        String coloredPlayerName = respawnTeam.colorizePlayer(player) + ChatColor.YELLOW;

        if(deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING || deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_FRAGS) {
            this.arena.broadcast(Language.parse(deathMessage,
                    coloredPlayerName, deathCause, String.valueOf(remainingLives)));
        } else if(deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_TEAM || deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_TEAM_FRAGS) {
            this.arena.broadcast(Language.parse(deathMessage,
                    coloredPlayerName,deathCause, String.valueOf(remainingLives),
                    respawnTeam.getColoredName()));
        } else {
            this.arena.broadcast(Language.parse(Language.MSG.FIGHT_KILLED_BY,
                    coloredPlayerName, deathCause));
        }
    }
}
