package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

import static java.util.Optional.ofNullable;

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
    protected Map<String, Integer> lifeMap;


    /**
     * create an arena type instance
     *
     * @param sName the arena type name
     */
    public ArenaGoal(final String sName) {
        this.name = sName;
    }

    public String getName() {
        return this.name;
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
    public List<String> getMain() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getShort() {
        return Collections.emptyList();
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
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
     * @param list the list of all set spawns
     * @return null if ready, error message otherwise
     */
    public String checkForMissingSpawns(final Set<String> list) {
        return null;
    }

    /**
     * check if necessary FFA spawns are set
     *
     * @return null if ready, error message otherwise
     */
    protected String checkForMissingSpawn(final Set<String> list) {
        int count = 0;
        for (final String s : list) {
            if (s.startsWith("spawn")) {
                count++;
            }
        }
        int minPlayers = this.arena.getArenaConfig().getInt(CFG.READY_MINPLAYERS);
        return count >= minPlayers ? null : "need more spawns! (" + count + "/" + minPlayers + ")";
    }

    /**
     * check if necessary team spawns are set
     *
     * @return null if ready, error message otherwise
     */
    protected String checkForMissingTeamSpawn(final Set<String> list) {
        for (final ArenaTeam team : this.arena.getTeams()) {
            final String sTeam = team.getName();
            if (!list.contains(team + "spawn")) {
                boolean found = false;
                for (final String s : list) {
                    if (s.startsWith(sTeam + "spawn")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return team.getName() + "spawn not set";
                }
            }
        }
        return null;
    }

    /**
     * check if necessary custom team spawns are set
     *
     * @return null if ready, error message otherwise
     */
    protected String checkForMissingTeamCustom(final Set<String> list, final String custom) {
        for (final ArenaTeam team : this.arena.getTeams()) {
            final String sTeam = team.getName();
            if (!list.contains(sTeam + custom)) {
                boolean found = false;
                for (final String s : list) {
                    if (s.startsWith(sTeam + custom)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return sTeam + custom + "not set";
                }
            }
        }
        return null;
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
        final int maxPlayers = this.arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS);
        final int maxTeamPlayers = this.arena.getArenaConfig().getInt(
                CFG.READY_MAXTEAMPLAYERS);

        if (maxPlayers > 0 && this.arena.getFighters().size() >= maxPlayers) {
            throw new GameplayException(Language.parse(this.arena, Language.MSG.ERROR_JOIN_ARENA_FULL));
        }

        if (!this.arena.isFreeForAll() && args != null && args.length > 0) {
            final ArenaTeam team = this.arena.getTeam(args[0]);

            if (team != null && maxTeamPlayers > 0 && team.getTeamMembers().size() >= maxTeamPlayers) {
                throw new GameplayException(Language.parse(this.arena, Language.MSG.ERROR_JOIN_TEAM_FULL));
            }
        }
    }

    /**
     * check if the goal should commit a player death
     *
     * @param player the dying player
     * @return true if player should respawn, false otherwise, null if goal doesn't handle respawn
     */
    public Boolean checkPlayerDeath(final Player player) {
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
     *  @param player      the dying player
     * @param doesRespawn true if the player will respawn
     * @param event       the causing death event
     */
    public void commitPlayerDeath(final Player player,
                                  final boolean doesRespawn,
                                  final PlayerDeathEvent event) {
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
     * Getter for the goal life map
     *
     * @return the goal life map
     */
    public Map<String, Integer> getLifeMap() {
        if (this.lifeMap == null) {
            this.lifeMap = new HashMap<>();
        }
        return this.lifeMap;
    }

    /**
     * Get a player's remaining lives
     *
     * @param player the player to check
     * @return the PACheck instance for more information, eventually an ERROR
     * containing the lives
     */
    public int getLives(ArenaPlayer player) {
        return 0;
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
     *
     * @param player          the dying player
     * @param lastDamageCause the last damage cause
     */
    public void parsePlayerDeath(final Player player, final EntityDamageEvent lastDamageCause) {
    }

    /**
     * hook into an arena start
     */
    public void parseStart() {
    }

    /**
     * check if the arena is ready
     *
     * @return null if ready, error message otherwise
     */
    public String ready() {
        return null;
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
     * @param value the value being set
     */
    public void setPlayerLives(final int value) {
    }

    /**
     * set a specific player's lives
     *
     * @param player the player to update
     * @param value  the value being set
     */
    public void setPlayerLives(final ArenaPlayer player, final int value) {
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
    }

    protected void updateLives(final ArenaTeam team, final int value) {
        if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_ADDLIVESPERPLAYER)) {
            this.getLifeMap().put(team.getName(), team.getTeamMembers().size() * value);
        } else {
            this.getLifeMap().put(team.getName(), value);
        }
    }

    protected void updateLives(final Player player, final int value) {
        if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_ADDLIVESPERPLAYER)) {
            this.getLifeMap().put(player.getName(), this.arena.getFighters().size() * value);
        } else {
            this.getLifeMap().put(player.getName(), value);
        }
    }

    protected void broadcastSimpleDeathMessage(Player player, PlayerDeathEvent event) {
        this.broadcastDeathMessage(Language.MSG.FIGHT_KILLED_BY, player, event, null);
    }

    protected void broadcastDeathMessage(Language.MSG deathMessage, Player player, PlayerDeathEvent event, Integer remainingLives) {
        final ArenaTeam respawnTeam = ArenaPlayer.parsePlayer(player.getName()).getArenaTeam();

        EntityDamageEvent.DamageCause damageCause = ofNullable(event.getEntity().getLastDamageCause())
                .map(EntityDamageEvent::getCause)
                .orElse(null);
        String deathCause = this.arena.parseDeathCause(player, damageCause, event.getEntity().getKiller());
        String coloredPlayerName = respawnTeam.colorizePlayer(player) + ChatColor.YELLOW;

        if(deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING || deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_FRAGS) {
            this.arena.broadcast(Language.parse(this.arena, deathMessage,
                    coloredPlayerName, deathCause, String.valueOf(remainingLives)));
        } else if(deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_TEAM || deathMessage == Language.MSG.FIGHT_KILLED_BY_REMAINING_TEAM_FRAGS) {
            this.arena.broadcast(Language.parse(this.arena, deathMessage,
                    coloredPlayerName,deathCause, String.valueOf(remainingLives),
                    respawnTeam.getColoredName()));
        } else {
            this.arena.broadcast(Language.parse(this.arena, Language.MSG.FIGHT_KILLED_BY,
                    coloredPlayerName, deathCause));
        }
    }
}
