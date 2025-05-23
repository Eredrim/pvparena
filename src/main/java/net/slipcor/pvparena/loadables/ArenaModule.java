package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * Arena Module class
 * </pre>
 * <p/>
 * The framework for adding modules to an arena
 *
 * @author slipcor
 */

public abstract class ArenaModule implements IArenaCommandHandler {
    protected Arena arena;
    protected String name;

    protected ArenaModule(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getPriority() {
        return 0;
    }

    public ModuleType getType() {
        return ModuleType.OTHER;
    }

    /**
     * hook into an announcement
     *
     * @param message the message being announced
     * @param type    the announcement type
     */
    public void announce(final String message, final String type) {
    }

    /**
     * check if a player can select a class
     *
     * @param player    the player to check
     * @param className the classname being selected
     * @return true if the player is not allowed to select
     */
    public boolean cannotSelectClass(final Player player, final String className) {
        return false;
    }

    /**
     * check if a module should commit a command
     *
     * @param arg the first command argument
     * @return if the module will commit
     */
    public boolean checkCommand(final String arg) {
        return false;
    }

    public boolean checkCountOverride(Player player, String message) {
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
    public boolean hasPerms(final CommandSender sender, final Arena arena, final boolean silent) {
        if (arena == null) {
            return PermissionManager.hasAdminPerm(sender);
        }
        return PermissionManager.hasAdminPerm(sender) || PermissionManager.hasBuilderPerm(sender, arena);
    }

    /**
     * check if the module should commit a player join
     *
     * @param player the player trying to join
     */
    public void checkJoin(Player player) throws GameplayException {
    }

    /**
     * check if the module should commit a player spectate
     *
     * @param player the player trying to spectate
     */
    public void checkSpectate(Player player) throws GameplayException {
    }

    /**
     * Handle player join
     *
     * @param player the player trying to join
     * @return true if modules add player to arena (no need to handle join in other modules)
     * @throws GameplayException Exception if arena can't start for gameplay reasons
     */
    public boolean handleJoin(Player player) throws GameplayException {
        return false;
    }

    /**
     * Handle player spectate
     *
     * @param player the player trying to spectate
     * @return true if modules add player to arena (no need to handle spectate in other modules)
     * @throws GameplayException Exception if arena can't start for gameplay reasons
     */
    public boolean handleSpectate(Player player) throws GameplayException {
        return false;
    }

    /**
     * Move a fighting player to spectate area using all features of spectate module
     * @param player the player
     */
    public void switchToSpectate(Player player) {
    }

    /**
     * check for unset spawns
     *
     * @param spawns the set spawns
     * @return missing spawns
     */
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return new HashSet<>();
    }

    /**
     * check for unset blocks
     *
     * @param blocks the set blocks
     * @return missing block
     */
    public Set<PABlock> checkForMissingBlocks(Set<PABlock> blocks) {
        return new HashSet<>();
    }

    /**
     * hook into a player choosing a team
     *
     * @param player    the choosing player
     * @param team      the team
     * @param canSwitch if true, the player didn't specify a particular team
     * @return an ArenaTeam if the team has to be changed, null otherwise
     */
    public ArenaTeam choosePlayerTeam(Player player, ArenaTeam team, boolean canSwitch) throws GameplayException {
        return null;
    }

    /**
     * commit a command
     *
     * @param sender the player committing the command
     * @param args   the command arguments
     */
    public void commitCommand(final CommandSender sender, final String[] args) {
        throw new IllegalStateException(this.name);
    }

    /**
     * commit the arena end
     *
     * @param aTeam       the arena team triggering the end
     * @param arenaPlayer the arena player triggering the end (FFA only)
     * @return true if the arena has ended
     */
    public boolean commitEnd(ArenaTeam aTeam, ArenaPlayer arenaPlayer) {
        return false;
    }

    /**
     * commit an arena join
     *
     * @param player the joining player
     * @param team   the chosen team
     */
    public void commitJoin(final Player player, final ArenaTeam team) {
        throw new IllegalStateException(this.name);
    }

    /**
     * commit an arena join after the beginning of the match
     * allowed only if join.allowDuringMatch is true
     *
     * @param player the joining player
     * @param team   the chosen team
     */
    public void commitJoinDuringMatch(Player player, ArenaTeam team) {
        throw new IllegalStateException(this.name);
    }

    /**
     * commit a spectator join
     *
     * @param player the spectating player
     */
    public void commitSpectate(final Player player) {
        throw new IllegalStateException(this.name);
    }

    /**
     * Hook into arena module enabling to create config
     */
    public void initConfig() {

    }

    /**
     * hook into the arena config parsing
     *
     * @param config the arena config
     */
    public void configParse(final YamlConfiguration config) {
    }

    /**
     * show information about the module
     *
     * @param sender the sender to be messaged
     */
    public void displayInfo(final CommandSender sender) {
    }


    public Arena getArena() {
        return this.arena;
    }

    /**
     * hook into giving rewards
     *
     * @param player the player being given rewards
     */
    public void giveRewards(final ArenaPlayer player) {
    }

    /**
     * check if a module knows a spawn name
     *
     * @param spawnName the spawn to check
     * @param teamName  the team to check
     * @return true if the module knows the spawn name
     */
    public boolean hasSpawn(final String spawnName, final String teamName) {
        return false;
    }

    /**
     * check if a module has a player waiting for join (like LateLounge)
     *
     * @param player the player to check
     * @return true if the player has pre-joined the arena
     */
    public boolean hasPlayerWaitingForJoin(Player player) {
        return false;
    }

    /**
     * hook into initiating a player when he joins directly into the battlefield
     * (contrary to standardlounge and spectating)
     *
     * @param player the joining player
     */
    public void initiate(final Player player) {
    }

    public boolean isMissingBattleRegion(final Arena arena) {
        if (this.needsBattleRegion()) {
            return arena.getRegionsByType(RegionType.BATTLE).isEmpty();
        }
        return false;
    }

    /**
     * hook into an arena joining the game after it has begin
     *
     * @param player the joining player
     */
    public void lateJoin(final Player player) {
    }

    public boolean needsBattleRegion() {
        return false;
    }

    /**
     * hook into a block being broken
     *
     * @param block the block being broken
     */
    public void onBlockBreak(final Block block) {
    }

    /**
     * hook into a block being changed
     *
     * @param block the block being changed
     * @param state the block state
     */
    public void onBlockChange(final Block block, final BlockState state) {
    }

    /**
     * hook into a block being pushed/pulled
     *
     * @param event the piston extend being pushed/pulled
     */
    public void onBlockPiston(final BlockPistonExtendEvent event) {
    }

    /**
     * hook into a block being placed
     *
     * @param block the block being placed
     */
    public void onBlockPlace(final Block block, final Material mat) {
    }

    /**
     * hook into a player receiving damage
     *
     * @param attacker the attacking player
     * @param defender the attacked player
     * @param event    the damage event
     */
    public void onEntityDamageByEntity(final Player attacker,
                                       final Player defender, final EntityDamageByEntityEvent event) {
    }

    /**
     * hook into a player throwing projectile
     *
     * @param attacker the attacking player
     * @param defender the attacked player
     * @param event    the projectileHit event
     */
    public void onProjectileHit(final Player attacker, final Player defender, final ProjectileHitEvent event) {

    }

    /**
     * hook into an exploding entity
     *
     * @param event the explode event
     */
    public void onEntityExplode(final EntityExplodeEvent event) {
    }

    /**
     * hook into a player recovering health
     *
     * @param event the regain health event
     */
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
    }

    /**
     * hook into a hanging being broken
     *
     * @param painting the hanging entity
     * @param type     the entity type
     */
    public void onPaintingBreak(final Hanging painting, final EntityType type) {
    }

    /**
     * hook into an interacting player
     *
     * @param event the interact event
     * @return true if the event should be cancelled
     */
    public boolean onPlayerInteract(final PlayerInteractEvent event) {
        return false;
    }

    /**
     * hook into a player picking up something
     *
     * @param event the pickup event
     */
    public void onPlayerPickupItem(final EntityPickupItemEvent event) {
    }

    /**
     * hook into the velocity event
     *
     * @param event the velocity event
     */
    public void onPlayerVelocity(final PlayerVelocityEvent event) {
    }

    /**
     * hook into a player toggling sprint state
     *
     * @param event the toggle sprint event
     */
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
    }

    /**
     * hook into the initial module loading
     */
    public void onThisLoad() {
    }

    public void parseClassChange(Player player, ArenaClass aClass) {
    }

    /**
     * hook into a player joining the arena
     *
     * @param player the joining player
     * @param team   the chosen team
     */
    public void parseJoin(final Player player, final ArenaTeam team) {
    }

    /**
     * hook into a player joining the arena during a running match
     * Calls parseJoin by default
     *
     * @param player the joining player
     * @param team   the chosen team
     */
    public void parseJoinDuringMatch(final Player player, final ArenaTeam team) {
        this.parseJoin(player, team);
    }
    /**
     * hook into a player dying
     *
     * @param player          the dying player
     * @param lastDamageCause the last damage cause
     */
    public void parsePlayerDeath(final Player player,
                                 final EntityDamageEvent lastDamageCause) {
    }

    /**
     * hook into a player being respawned
     *
     * @param player  the respawning player
     * @param team    the team he is part of
     * @param cause   the last damage cause
     * @param damager the last damaging entity
     */
    public void parseRespawn(final Player player, final ArenaTeam team,
                             final DamageCause cause, final Entity damager) {
    }

    /**
     * hook into an arena player leaving
     *
     * @param player the leaving player
     * @param team   the arena team being left
     */
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
    }

    /**
     * hook into an arena start
     */
    public void parseStart() {
    }

    /**
     * hook into starting an arena countdown
     *
     * @param seconds the initial countdown seconds to go
     * @param message the message being displayed (to check which one it is)
     * @param global  whether the whole arena will be messaged
     * @return possibly different remaining seconds to go
     */
    public Integer parseStartCountDown(Integer seconds, String message, Boolean global) {
        return seconds;
    }

    /**
     * hook into an arena being reset
     *
     * @param force if the arena is forcefully reset
     */
    public void reset(final boolean force) {
    }

    /**
     * hook into an arena player being reset
     *
     * @param player the player being reset
     * @param soft   if the reset should be soft (another teleport incoming)
     * @param force  if the arena is forcefully reset
     */
    public void resetPlayer(final Player player, final boolean soft, final boolean force) {
    }

    /**
     * update the arena instance only do that if you know what you're doing
     *
     * @param arena the new arena instance
     */
    public void setArena(final Arena arena) {
        this.arena = arena;
    }

    /**
     * hook into the arena ending due to time goal
     *
     * @param result the winner names
     */
    public void timedEnd(final Set<String> result) {
    }

    /**
     * hook into an player being teleported
     *
     * @param player the teleported player
     * @param place  the destination spawn name
     */
    public void teleportPlayer(final Player player, final PASpawn place) {
    }

    /**
     * check if a module is trying to override player deaths
     *
     * @param aPlayer the player dying
     * @param deathInfo death information object
     * @param list    the player's death drops
     * @return true if a module cares
     */
    public boolean tryDeathOverride(ArenaPlayer aPlayer, PADeathInfo deathInfo, List<ItemStack> list) {
        return false;
    }

    /**
     * Call a special leave directly from the module
     *
     * @param aPlayer the player who leaves
     * @return true if a module cares
     */
    public boolean handleQueuedLeave(final ArenaPlayer aPlayer) {
        return false;
    }

    /**
     * hook into a player removal
     *
     * @param player the player being removed
     */
    public void unload(final Player player) {
    }

    /**
     * the version string, should be overridden!
     *
     * @return the version string
     */
    public String version() {
        return "outdated";
    }
}
