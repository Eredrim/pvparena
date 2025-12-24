package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.managers.PermissionManager;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.classes.PASpawn.FIGHT;
import static net.slipcor.pvparena.config.Debugger.trace;

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
    protected String name;
    protected Arena arena;
    protected Map<ArenaTeam, Integer> teamLifeMap = new HashMap<>();
    protected Map<ArenaPlayer, Integer> playerLifeMap = new HashMap<>();


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

    public boolean isFreeForAll() {
        return false;
    }

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
        return getGoalCommands();
    }

    /**
     * Get Main commands for the goal
     *
     * @return list of commands for the goal
     */
    public List<String> getGoalCommands() {
        return Collections.emptyList();
    }

    @Override
    public final List<String> getShort() {
        return this.getGoalShortCommands();
    }

    /**
     * Get Main shortcuts commands for the goal
     *
     * @return list of shortcuts commands for the goal
     */
    public List<String> getGoalShortCommands() {
        return Collections.emptyList();
    }

    @Override
    public final CommandTree<String> getSubs(final Arena arena) {
        return this.getGoalSubCommands(arena);
    }

    /**
     * Get sub-commands for the goal
     *
     * @return list of sub-commands for the goal
     */
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        return new CommandTree<>(null);
    }

    @Override
    public boolean hasPerms(final CommandSender sender, final Arena arena, final boolean silent) {
        if (arena == null) {
            return PermissionManager.hasAdminPerm(sender);
        }
        return PermissionManager.hasAdminPerm(sender) || PermissionManager.hasBuilderPerm(sender, arena);
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
     * @param spawns the list of all set spawns
     * @return empty if ready, missing spawn otherwise
     */
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return new HashSet<>();
    }

    /**
     * check if all necessary blocks are set
     *
     * @param blocks the list of all set blocks
     * @return empty if ready, missing blocks otherwise
     */
    public Set<PABlock> checkForMissingBlocks(Set<PABlock> blocks) {
        return new HashSet<>();
    }

    /**
     * hook into an interacting player
     *
     * @param player the interacting player
     * @param event  the interact event
     * @return true if the goals handle the event
     */
    public boolean checkInteract(final Player player, final PlayerInteractEvent event) {
        return false;
    }

    /**
     * check if the goal should commit a player join
     *
     * @param player the joining player
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
     * @param arenaPlayer the dying player
     * @param deathInfo   death info
     * @return true if player should respawn, false otherwise, null if goal doesn't handle respawn
     */
    public Boolean shouldRespawnPlayer(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
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
     *
     * @param arenaPlayer the dying player
     * @param doesRespawn true if the player will respawn
     * @param deathInfo   death information object containing cause and damager
     */
    public void commitPlayerDeath(ArenaPlayer arenaPlayer, boolean doesRespawn, PADeathInfo deathInfo) {
        throw new IllegalStateException(this.getName());
    }

    /**
     * commit setting a flag
     *
     * @param player the setting player
     * @param block  the flag block
     * @return true if the interact event should be cancelled
     */
    public boolean commitSetBlock(final Player player, final Block block) {
        throw new IllegalStateException(this.getName());
    }

    /**
     * commit an arena start
     */
    public void commitStart() {
        throw new IllegalStateException(this.getName());
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
    public Map<ArenaPlayer, Integer> getPlayerLifeMap() {
        return this.playerLifeMap;
    }

    /**
     * Get the goal life map having only active players (so without offline ones)
     * @return the filtered goal life map
     */
    @NotNull
    public Map<ArenaPlayer, Integer> getActivePlayerLifeMap() {
        return this.playerLifeMap.entrySet().stream()
                .filter(entry -> asList(PlayerStatus.FIGHT, PlayerStatus.DEAD).contains(entry.getKey().getStatus()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get a player's remaining lives
     *
     * @param arenaPlayer the player to check
     * @return the PACheck instance for more information, eventually an ERROR
     * containing the lives
     */
    public int getLives(ArenaPlayer arenaPlayer) {
        if (this.arena.isFreeForAll()) {
            return this.getPlayerLifeMap().getOrDefault(arenaPlayer, 0);
        } else {
            return this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
        }
    }

    public int getScore(ArenaTeam arenaTeam) {
        return this.getTeamLifeMap().getOrDefault(arenaTeam, 0);
    }

    public int getScore(ArenaPlayer arenaPlayer) {
        if (this.arena.isFreeForAll()) {
            return this.getPlayerLifeMap().getOrDefault(arenaPlayer, 0);
        }
        return this.getScore(arenaPlayer.getArenaTeam());
    }

    /**
     * does a goal know this spawn?
     *
     * @param spawnName     the spawn name to check
     * @param spawnTeamName the team name owner of the spawn
     * @return if the goal knows this spawn
     */
    public boolean hasSpawn(final String spawnName, final String spawnTeamName) {
        if (this.arena.isFreeForAll()) {
            return hasFfaSpawn(spawnName);
        } else {
            return hasTeamSpawn(spawnName, spawnTeamName);
        }
    }

    public boolean hasFfaSpawn(String spawnName) {
        boolean hasByClass = false;
        if (this.arena.getConfig().getBoolean(CFG.GENERAL_SPAWN_PER_CLASS)) {
            hasByClass = this.arena.getClasses().stream().
                    anyMatch(aClass -> spawnName.toLowerCase().startsWith(aClass.getName().toLowerCase() + FIGHT));
        }
        return hasByClass || spawnName.toLowerCase().startsWith(FIGHT);
    }

    public boolean hasTeamSpawn(String spawnName, String spawnTeamName) {
        for (String teamName : this.arena.getTeamNames()) {

            boolean hasByClass = false;
            if (this.arena.getConfig().getBoolean(CFG.GENERAL_SPAWN_PER_CLASS)) {
                hasByClass = this.arena.getClasses().stream().
                        anyMatch(aClass -> spawnName.toLowerCase().startsWith(aClass.getName().toLowerCase() + FIGHT)
                                && spawnTeamName.equalsIgnoreCase(teamName));
            }
            trace("Has team {} spawn: class spawn: {}, spawn name: {}", teamName, hasByClass, spawnName);
            final boolean hasSpawn = hasByClass || (spawnName.toLowerCase().startsWith(FIGHT)
                    && teamName.equalsIgnoreCase(spawnTeamName));
            if (hasSpawn) {
                return true;
            }
        }
        return false;
    }

    /**
     * hook into initializing a player being put directly to the battlefield
     * (contrary to lounge/spectate)
     *
     * @param arenaPlayer the player being put
     */
    public void initiate(final ArenaPlayer arenaPlayer) {
    }

    /**
     * hook into an arena joining the game after it has begin
     *
     * @param arenaPlayer the joining player
     */
    public void lateJoin(final ArenaPlayer arenaPlayer) {
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
     * @param arenaPlayer the leaving player
     */
    public void parseLeave(final ArenaPlayer arenaPlayer) {
    }

    /**
     * hook into a player dying
     *
     * @param arenaPlayer the dying player
     * @param deathInfo   the last damage cause
     */
    public void parsePlayerDeath(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
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
    public void editInventoryOnRefill(final Player player) {
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
        if (config.get("teams") == null) {
            if (this.arena.isFreeForAll()) {
                config.set("teams.free", "WHITE");
            } else {
                config.set("teams.red", "RED");
                config.set("teams.blue", "BLUE");
            }
        }
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
    public void setPlayerLives(final ArenaPlayer player, final int lives) {
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
     * @param arenaPlayer the player to unload
     */
    public void unload(final ArenaPlayer arenaPlayer) {
        if (arenaPlayer != null) {
            this.getPlayerLifeMap().remove(arenaPlayer);
        }
    }

    protected void updateLives(final ArenaTeam team, final int value) {
        if (this.arena.getConfig().getBoolean(CFG.GENERAL_ADDLIVESPERPLAYER)) {
            this.getTeamLifeMap().putIfAbsent(team, team.getTeamMembers().size() * value);
        } else {
            this.getTeamLifeMap().putIfAbsent(team, value);
        }
    }

    protected void updateLives(final ArenaPlayer arenaPlayer, final int value) {
        if (this.arena.getConfig().getBoolean(CFG.GENERAL_ADDLIVESPERPLAYER)) {
            this.getPlayerLifeMap().put(arenaPlayer, this.arena.getFighters().size() * value);
        } else {
            this.getPlayerLifeMap().put(arenaPlayer, value);
        }
    }

    protected void broadcastSimpleDeathMessage(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        this.broadcastDeathMessage(Language.MSG.FIGHT_KILLED_BY, arenaPlayer, deathInfo, null);
    }

    protected void broadcastDeathMessage(Language.MSG deathMessage, ArenaPlayer arenaPlayer, PADeathInfo deathInfo, Integer remainingLives) {
        final ArenaTeam respawnTeam = arenaPlayer.getArenaTeam();

        String deathCause = this.arena.parseDeathCause(arenaPlayer.getPlayer(), deathInfo.getCause(), deathInfo.getDamager());
        String coloredPlayerName = respawnTeam.colorizePlayer(arenaPlayer) + ChatColor.YELLOW;

        if (deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING || deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_FRAGS) {
            this.arena.broadcast(Language.parse(deathMessage,
                    coloredPlayerName, deathCause, String.valueOf(remainingLives)));
        } else if (deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_TEAM || deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_TEAM_FRAGS) {
            this.arena.broadcast(Language.parse(deathMessage,
                    coloredPlayerName, deathCause, String.valueOf(remainingLives),
                    respawnTeam.getColoredName()));
        } else {
            this.arena.broadcast(Language.parse(Language.MSG.FIGHT_KILLED_BY,
                    coloredPlayerName, deathCause));
        }
    }
}
