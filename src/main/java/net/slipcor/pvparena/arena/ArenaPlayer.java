package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PAStatMap;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.events.PAPlayerClassChangeEvent;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.StatisticsManager.Type;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Player class
 * </pre>
 * <p/>
 * contains Arena Player methods and variables for quicker access
 *
 * @author slipcor
 * @version v0.10.2
 */

public class ArenaPlayer {
    private static final Map<UUID, ArenaPlayer> totalPlayers = new HashMap<>();

    private final Player player;

    private boolean telePass;
    private boolean ignoreAnnouncements;
    private boolean teleporting;
    private boolean mayDropInventory;
    private boolean mayRespawn;

    private Boolean flying;

    private Arena arena;
    private ArenaClass arenaClass;
    private ArenaClass nextArenaClass;
    private PlayerState state;
    private PALocation location;
    private PlayerStatus status = PlayerStatus.NULL;

    private ItemStack[] savedInventory;
    private final Set<PermissionAttachment> tempPermissions = new HashSet<>();
    private final Map<String, PAStatMap> statistics = new HashMap<>();

    private Scoreboard backupBoard;
    private String backupBoardTeam;


    private boolean publicChatting = true;
    private final PABlockLocation[] selection = new PABlockLocation[2];

    /**
     * Create new ArenaPlayer
     * 
     * @param player bukkit player
     */
    private ArenaPlayer(@NotNull final Player player) {
        Objects.requireNonNull(player);
        this.player = player;
    }

    @NotNull
    public Player getPlayer() {
        return this.player;
    }

    public static Set<ArenaPlayer> getAllArenaPlayers() {
        return new HashSet<>(totalPlayers.values());
    }

    public boolean getFlyState() {
        return this.flying != null && this.flying;
    }

    public boolean mayRespawn() {
        return this.mayRespawn;
    }

    public void setMayRespawn(boolean mayRespawn) {
        this.mayRespawn = mayRespawn;
    }

    /**
     * try to find the last damaging player
     *
     * @param damageEvent the Event
     * @return the player instance if found, null otherwise
     */
    public static Player getLastDamagingPlayer(EntityDamageEvent damageEvent) {
        Entity damagee = damageEvent.getEntity();
        debug(damagee, "trying to get the last damaging player");
        if (damageEvent instanceof EntityDamageByEntityEvent) {
            debug(damagee, "there was an EDBEE");
            final EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) damageEvent;

            Entity eDamager = event.getDamager();

            if (event.getCause() == DamageCause.PROJECTILE && eDamager instanceof Projectile) {

                final ProjectileSource p = ((Projectile) eDamager).getShooter();

                if (p instanceof LivingEntity) {
                    eDamager = (LivingEntity) p;
                    debug(damagee, "killed by projectile, shooter is found");
                }
            }

            if (event.getEntity() instanceof Wolf) {
                final Wolf wolf = (Wolf) event.getEntity();
                if (wolf.getOwner() != null) {
                    eDamager = (Entity) wolf.getOwner();
                    debug(damagee, "tamed wolf is found");
                }
            }

            if (eDamager instanceof Player) {
                debug(damagee, "it was a player!");
                return (Player) eDamager;
            }
        }
        debug(damagee, "last damaging player is null");
        debug(damagee, "last damaging event: {}", damageEvent.getEventName());
        return null;
    }

    /**
     * supply current player with class items and eventually wool head
     */
    public void equipPlayerFightItems() {
        final ArenaClass playerClass = this.arenaClass;
        if (playerClass == null) {
            return;
        }
        debug(this, "giving items to player '{}', class '{}'", this, playerClass);

        playerClass.equip(this.player);

        if (this.arena.getConfig().getBoolean(CFG.USES_WOOLHEAD)) {
            final ArenaTeam aTeam = this.getArenaTeam();
            final ChatColor color = aTeam.getColor();
            debug(this, "forcing woolhead: {}/{}", aTeam.getName(), color);
            this.player.getInventory().setHelmet(new ItemStack(ColorUtils.getWoolMaterialFromChatColor(color)));
        }
    }

    /**
     * get an ArenaPlayer from a player name
     *
     * @param name the playername to use
     * @return an ArenaPlayer instance belonging to that player
     */
    public static ArenaPlayer fromPlayer(final String name) {
        synchronized (ArenaPlayer.class) {
            Player player = Bukkit.getPlayerExact(name);

            // Offline player or NPC
            if (player == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
                if(offlinePlayer.getPlayer() == null) {
                    throw new RuntimeException(String.format("Player %s not found", name));
                }
                player = offlinePlayer.getPlayer();
            }
            return fromPlayer(player);
        }
    }

    /**
     * get an ArenaPlayer from a Player
     *
     * @param player the player to use
     * @return an ArenaPlayer instance belonging to that player
     */
    public static ArenaPlayer fromPlayer(final Player player) {
        synchronized (ArenaPlayer.class) {
            ArenaPlayer aPlayer = new ArenaPlayer(player);
            totalPlayers.putIfAbsent(player.getUniqueId(), aPlayer);
            return totalPlayers.get(player.getUniqueId());
        }
    }

    /**
     * prepare a player's inventory, back it up and clear it
     *
     * @param player the player to save
     */
    public static void backupAndClearInventory(final Arena arena, final Player player) {
        debug(player, "saving player inventory: {}", player);

        final ArenaPlayer aPlayer = fromPlayer(player);
        aPlayer.savedInventory = player.getInventory().getContents().clone();
        InventoryManager.clearInventory(player);
    }

    /**
     * reset player variables
     * @param deathInfo death information object
     */
    public void revive(PADeathInfo deathInfo) {
        debug(this, "respawning player");
        
        final Config config = this.arena.getConfig();
        double iHealth = config.getInt(CFG.PLAYER_HEALTH, -1);

        if (iHealth < 1) {
            iHealth = this.player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        }

        PlayerState.playersetHealth(this.player, iHealth);
        this.player.setFoodLevel(config.getInt(CFG.PLAYER_FOODLEVEL, 20));
        this.player.setSaturation(config.getInt(CFG.PLAYER_SATURATION, 20));
        this.player.setExhaustion((float) config.getDouble(CFG.PLAYER_EXHAUSTION, 0.0));
        this.player.setVelocity(new Vector());
        this.player.setFallDistance(0);

        if (config.getBoolean(CFG.PLAYER_DROPSEXP)) {
            this.player.setTotalExperience(0);
            this.player.setLevel(0);
            this.player.setExp(0);
        }

        ArenaTeam team = this.getArenaTeam();

        if (team == null) {
            return;
        }

        PlayerState.removeEffects(this.player);

        if (this.nextArenaClass != null) {
            InventoryManager.clearInventory(this.player);
            this.setArenaClass(this.nextArenaClass);
            if (this.arenaClass != null) {
                this.equipPlayerFightItems();
                this.mayDropInventory = true;
            }
            this.nextArenaClass = null;
        }

        ArenaModuleManager.parseRespawn(this.arena, this.player, team, deathInfo);
        this.player.setFireTicks(0);
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if (this.player.getFireTicks() > 0) {
                    this.player.setFireTicks(0);
                }
            }, 5L);
        } catch (Exception ignored) {
        }
        this.player.setNoDamageTicks(config.getInt(CFG.TIME_TELEPORTPROTECT) * 20);
    }

    /**
     * handles the final death of the player, marks them as lost and teleports them to DEATH spawnpoint
     * @param deathInfo information about fake death event
     */
    public void handleDeathAndLose(PADeathInfo deathInfo) {
        ArenaTeam team = this.getArenaTeam();

        final String playerName = (team == null) ? this.getName() : team.colorizePlayer(this.player);
        if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            this.arena.broadcast(Language.parse(
                    Language.MSG.FIGHT_KILLED_BY,
                    playerName + ChatColor.YELLOW,
                    this.arena.parseDeathCause(
                            this.player,
                            deathInfo.getCause(),
                            deathInfo.getKiller()
                    )
            ));
        }

        if (!this.hasCustomClass()) {
            InventoryManager.clearInventory(this.player);
        }

        this.setStatus(PlayerStatus.LOST);
        this.arena.removePlayer(this, this.arena.getConfig().getString(CFG.TP_DEATH), true, false);

        this.addDeath();

        PlayerState.fullReset(this.arena, this.player);

        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
            boolean found = this.arena.getMods().stream().anyMatch(mod -> mod.getName().contains("Spectate"));
            if (!found) {
                new PAG_Leave().commit(this.arena, this.player, new String[0]);
            }
        }, 5L);

        ArenaManager.checkAndCommit(this.arena, false);
    }

    public void reloadInventory(boolean instant) {
        debug(this.player, "resetting inventory");

        if (this.arena.getConfig().getYamlConfiguration().contains(CFG.ITEMS_TAKEOUTOFGAME.getNode())) {
            ItemStack[] items = this.arena.getConfig().getItems(CFG.ITEMS_TAKEOUTOFGAME);
            List<Material> allowedMats = Arrays.stream(items).map(ItemStack::getType).collect(Collectors.toList());

            List<ItemStack> keepItems = Arrays.stream(this.player.getInventory().getContents())
                    .filter(item -> item != null && allowedMats.contains(item.getType()))
                    .map(ItemStack::clone)
                    .collect(Collectors.toList());

            try {
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                    for (final ItemStack item : keepItems) {
                        this.player.getInventory().addItem(item.clone());
                    }
                    keepItems.clear();
                }, 60L);
            } catch (Exception ignored) {

            }
        }

        InventoryManager.clearInventory(player);

        if (this.savedInventory == null) {
            debug(this.player, "saved inventory null!");
            return;
        }
        // AIR AIR AIR AIR instead of contents !!!!

        if (instant) {
            debug(this.player, "adding saved inventory");
            this.player.getInventory().setContents(this.savedInventory);
        } else {
            class GiveLater extends BukkitRunnable {
                final ItemStack[] inv;

                GiveLater(final ItemStack[] inv) {
                    this.inv = inv.clone();
                }

                @Override
                public void run() {
                    debug(ArenaPlayer.this.player, "adding saved inventory");
                    ArenaPlayer.this.player.getInventory().setContents(this.inv);
                }
            }

            final GiveLater giveLater = new GiveLater(this.savedInventory);
            try {
                giveLater.runTaskLater(PVPArena.getInstance(), 60L);
            } catch (final Exception e) {
                giveLater.run();
            }
        }
    }

    public void addDeath() {
        this.getStatistics(this.arena).incStat(Type.DEATHS);
    }

    public void addKill() {
        this.getStatistics(this.arena).incStat(Type.KILLS);
    }

    public void addLosses() {
        this.getStatistics(this.arena).incStat(Type.LOSSES);
    }

    public void addStatistic(final String arenaName, final Type type,
                             final int value) {
        if (!this.statistics.containsKey(arenaName)) {
            this.statistics.put(arenaName, new PAStatMap());
        }

        this.statistics.get(arenaName).incStat(type, value);
    }

    public void addWins() {
        this.getStatistics(this.arena).incStat(Type.WINS);
    }

    private void clearDump() {
        debug(this, "clearing dump of {}", this.player.getName());
        this.debugPrint();
        final File file = new File(PVPArena.getInstance().getDataFolder().getPath()
                + "/dumps/" + this.player.getName() + ".yml");
        if (!file.exists()) {
            return;
        }
        file.delete();
    }

    public void clearFlyState() {
        this.flying = null;
    }

    /**
     * save the player state
     *
     * @param player the player to save
     */
    public void createState(final Player player) {
        this.state = new PlayerState(player);
        this.mayDropInventory = true;
    }

    public boolean didValidSelection() {
        return this.selection[0] != null && this.selection[1] != null;
    }

    public void debugPrint() {
        if (this.status == null || this.location == null) {
            debug(this, "DEBUG PRINT OUT:");
            debug(this, this.player.getName());
            debug(this, String.valueOf(this.status));
            debug(this, String.valueOf(this.location));
            debug(this, String.valueOf(this.selection[0]));
            debug(this, String.valueOf(this.selection[1]));
            return;
        }
        debug(this, "------------------");
        debug(this, "Player: {}", this.player.getName());
        debug(this, "telepass: {} | mayDropInv: {} | chatting: {}", this.telePass, this.mayDropInventory, this.publicChatting);
        debug(this, "arena: {}", (this.arena == null ? "null" : this.arena.getName()));
        debug(this, "aClass: {}", (this.arenaClass == null ? "null" : this.arenaClass.getName()));
        debug(this, "location: {}", this.location);
        debug(this, "status: {}", this.status.name());
        debug(this, "tempPermissions:");
        for (final PermissionAttachment pa : this.tempPermissions) {
            debug(this, "> {}", pa);
        }
        debug(this, "------------------");
    }

    public void dump() {
        debug(this, "dumping...");
        this.debugPrint();
        final File file = new File(PVPArena.getInstance().getDataFolder().getPath()
                + "/dumps/" + this.player.getName() + ".yml");
        try {
            file.createNewFile();
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("arena", this.arena.getName());
        if (this.state != null) {
            this.state.dump(cfg);
        }

        try {
            cfg.set("inventory", this.savedInventory);
            cfg.set("loc", Config.parseToString(this.location));

            cfg.save(file);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * return the arena
     *
     * @return the arena
     */
    public Arena getArena() {
        return this.arena;
    }

    /**
     * return the arena class
     *
     * @return the arena class
     */
    public ArenaClass getArenaClass() {
        return this.arenaClass;
    }

    public ArenaClass getNextArenaClass() {
        return this.nextArenaClass;
    }

    public ArenaTeam getArenaTeam() {
        if (this.arena == null) {
            return null;
        }
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (team.getTeamMembers().contains(this)) {
                return team;
            }
        }
        return null;
    }

    public Scoreboard getBackupScoreboard() {
        return this.backupBoard;
    }

    public String getBackupScoreboardTeam() {
        return this.backupBoardTeam;
    }

    public PALocation getSavedLocation() {
        debug(this, "reading loc!");
        if (this.location != null) {
            debug(this, ": {}", this.location);
        }
        return this.location;
    }

    public PALocation getLocation() {
        return new PALocation(this.player.getLocation());
    }

    /**
     * return the player name
     *
     * @return the player name
     */
    public String getName() {
        return this.player.getName();
    }

    public PABlockLocation[] getSelection() {
        return this.selection.clone();
    }

    /**
     * return the player state
     *
     * @return the player state
     */
    public PlayerState getState() {
        return this.state;
    }

    public PAStatMap getStatistics() {
        return this.getStatistics(this.arena);
    }

    public PAStatMap getStatistics(final Arena arena) {
        if (arena == null) {
            return new PAStatMap();
        }
        if (this.statistics.get(arena.getName()) == null) {
            this.statistics.put(arena.getName(), new PAStatMap());
        }
        return this.statistics.get(arena.getName());
    }

    public PlayerStatus getStatus() {
        return this.status;
    }

    /**
     * hand over a player's tele pass
     *
     * @return true if may pass, false otherwise
     */
    public boolean isTelePass() {
        return this.hasTelePass();
    }

    public boolean isTeleporting() {
        return this.teleporting;
    }

    public boolean mayDropInventory() {
        return this.mayDropInventory;
    }

    public Set<PermissionAttachment> getTempPermissions() {
        return this.tempPermissions;
    }

    public int getTotalStatistics(final Type statType) {
        int sum = 0;

        for (final PAStatMap stat : this.statistics.values()) {
            sum += stat.getStat(statType);
        }

        return sum;
    }

    public boolean hasBackupScoreboard() {
        return this.backupBoard != null;
    }

    public boolean hasTelePass() {
        return this.telePass;
    }

    public boolean isIgnoringAnnouncements() {
        return this.ignoreAnnouncements;
    }

    public boolean isPublicChatting() {
        return this.publicChatting;
    }

    public boolean hasCustomClass() {
        return this.getArenaClass() != null && "custom".equalsIgnoreCase(this.getArenaClass().getName());
    }

    public void readDump() {
        debug(this, "reading dump: {}", this.player.getName());
        this.debugPrint();
        final File file = new File(PVPArena.getInstance().getDataFolder().getPath()
                + "/dumps/" + this.player.getName() + ".yml");
        if (!file.exists()) {
            debug(this, "no dump!");
            return;
        }

        final YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(file);
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        this.arena = ArenaManager.getArenaByName(cfg.getString("arena"));
        this.savedInventory = cfg.getList("inventory").toArray(new ItemStack[0]);
                /*StringParser.getItemStacksFromString(cfg.getString(
                "inventory", "AIR"));*/
        this.location = Config.parseLocation(cfg.getString("loc"));

        if (this.arena != null) {
            final String goTo = this.arena.getConfig().getString(CFG.TP_EXIT);
            if (!"old".equals(goTo)) {
                this.location = SpawnManager.getSpawnByExactName(this.arena, "exit");
            }

            if (Bukkit.getPlayer(this.player.getName()) == null) {
                debug(this, "player offline, OUT!");
                return;
            }
            this.state = PlayerState.undump(cfg, this.player.getName());
        }

        file.delete();
        this.debugPrint();
    }

    /**
     * save and reset a player instance
     */
    public void reset() {
        debug(this, "destroying arena player {}", this.player.getName());
        this.debugPrint();
        final YamlConfiguration cfg = new YamlConfiguration();
        try {
            if (PVPArena.getInstance().getConfig().getBoolean("stats")) {

                final String file = PVPArena.getInstance().getDataFolder()
                        + "/players.yml";
                cfg.load(file);

                if (this.arena != null) {
                    final String arenaName = this.arena.getName();
                    cfg.set(arenaName + '.' + this.player.getName() + ".losses", this.getStatistics()
                            .getStat(Type.LOSSES)
                            + this.getTotalStatistics(Type.LOSSES));
                    cfg.set(arenaName + '.' + this.player.getName() + ".wins",
                            this.getStatistics()
                                    .getStat(Type.WINS)
                                    + this.getTotalStatistics(Type.WINS));
                    cfg.set(arenaName + '.' + this.player.getName() + ".kills",
                            this.getStatistics().getStat(
                                    Type.KILLS)
                                    + this.getTotalStatistics(Type.KILLS));
                    cfg.set(arenaName + '.' + this.player.getName() + ".deaths", this.getStatistics()
                            .getStat(Type.DEATHS)
                            + this.getTotalStatistics(Type.DEATHS));
                    cfg.set(arenaName + '.' + this.player.getName() + ".damage", this.getStatistics()
                            .getStat(Type.DAMAGE)
                            + this.getTotalStatistics(Type.DAMAGE));
                    cfg.set(arenaName + '.' + this.player.getName() + ".maxdamage",
                            this.getStatistics().getStat(
                                    Type.MAXDAMAGE)
                                    + this.getTotalStatistics(Type.MAXDAMAGE));
                    cfg.set(arenaName + '.' + this.player.getName() + ".damagetake",
                            this.getStatistics().getStat(
                                    Type.DAMAGETAKE)
                                    + this.getTotalStatistics(Type.DAMAGETAKE));
                    cfg.set(arenaName + '.' + this.player.getName() + ".maxdamagetake",
                            this.getStatistics().getStat(
                                    Type.MAXDAMAGETAKE)
                                    + this.getTotalStatistics(Type.MAXDAMAGETAKE));
                }

                cfg.save(file);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (this.getPlayer() == null) {
            debug(this, "reset() ; out! null");
            return;
        }

        this.telePass = false;

        if (this.state != null) {
            this.state.reset();
            this.state = null;
        }

        this.setStatus(PlayerStatus.NULL);
        this.nextArenaClass = null;

        if (this.arena != null) {
            final ArenaTeam team = this.getArenaTeam();
            if (team != null) {
                team.remove(this);
            }
        }
        this.arena = null;
        this.arenaClass = null;
        this.getPlayer().setFireTicks(0);
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if (ArenaPlayer.this.getPlayer() != null && ArenaPlayer.this.getPlayer().getFireTicks() > 0) {
                    ArenaPlayer.this.getPlayer().setFireTicks(0);
                }
            }, 5L);
        } catch (Exception e) {
        }

        this.clearDump();
    }

    /**
     * set the player's arena
     *
     * @param arena the arena to set
     */
    public final void setArena(final Arena arena) {
        this.arena = arena;
    }

    /**
     * set the player's arena class
     *
     * @param aClass the arena class to set
     */
    public void setArenaClass(final ArenaClass aClass) {
        final PAPlayerClassChangeEvent event = new PAPlayerClassChangeEvent(this.arena, this.player, aClass);
        Bukkit.getServer().getPluginManager().callEvent(event);
        this.arenaClass = event.getArenaClass();
        if (this.arena != null && this.status != PlayerStatus.NULL) {
            ArenaModuleManager.parseClassChange(this.arena, this.player, this.arenaClass);
        }
    }

    /**
     * set a player's arena class by name
     *
     * @param className an arena class name
     */
    public void setArenaClass(final String className) {

        for (final ArenaClass ac : this.arena.getClasses()) {
            if (ac.getName().equalsIgnoreCase(className)) {
                this.setArenaClass(ac);
                return;
            }
        }
        PVPArena.getInstance().getLogger().warning(
                String.format("[PA-debug] failed to set unknown class %s to player %s", className, this.player.getName()));
    }

    public void setBackupScoreboard(Scoreboard board) {
        this.backupBoard = board;
    }

    public void setBackupScoreboardTeam(String sbTeamName) {
        this.backupBoardTeam = sbTeamName;
    }

    public void setMayDropInventory(boolean value) {
        this.mayDropInventory = value;
    }

    public void setNextArenaClass(ArenaClass aClass) {
        this.nextArenaClass = aClass;
    }

    public void setFlyState(boolean flyState) {
        this.flying = flyState;
    }

    public void setIgnoreAnnouncements(final boolean value) {
        this.ignoreAnnouncements = value;
    }

    public void setLocation(final PALocation location) {
        this.location = location;
    }

    public void setPublicChatting(final boolean chatPublic) {
        this.publicChatting = chatPublic;
    }

    public void setSelection(final Location loc, final boolean second) {
        if (second) {
            this.selection[1] = new PABlockLocation(loc);
        } else {
            this.selection[0] = new PABlockLocation(loc);
        }
    }

    public void setStatistic(final String arenaName, final Type type,
                             final int value) {
        if (!this.statistics.containsKey(arenaName)) {
            this.statistics.put(arenaName, new PAStatMap());
        }

        final PAStatMap map = this.statistics.get(arenaName);
        map.setStat(type, value);
    }

    public void setStatus(final PlayerStatus status) {
        debug(this,"{}>{}", this.player.getName(), status.name());
        this.status = status;
    }

    /**
     * hand over a player's tele pass
     *
     * @param canTeleport true if may pass, false otherwise
     */
    public void setTelePass(final boolean canTeleport) {
        if (this.arena != null) {
            debug(this.arena, "TelePass := {}", canTeleport);
        }
        this.telePass = canTeleport;
    }

    public void setTeleporting(final boolean isTeleporting) {
        this.teleporting = isTeleporting;
    }

    public void showBloodParticles() {
        this.player.getLocation()
                .getWorld()
                .playEffect(this.player.getEyeLocation(), Effect.STEP_SOUND, Material.NETHER_WART_BLOCK);

    }

    @Override
    public String toString() {
        final ArenaTeam team = this.getArenaTeam();

        return team == null ? this.player.getName() : team.getColorCodeString() + this.player.getName() + ChatColor.RESET;
    }

    public void unsetSelection() {
        this.selection[0] = null;
        this.selection[1] = null;
    }
}
