package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PAClassSign;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.ArrowHack;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.PAEndEvent;
import net.slipcor.pvparena.events.PAExitEvent;
import net.slipcor.pvparena.events.PALeaveEvent;
import net.slipcor.pvparena.events.PALoseEvent;
import net.slipcor.pvparena.events.PAWinEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.managers.TeleportManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.regions.RegionType;
import net.slipcor.pvparena.runnables.StartRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.classes.PASpawn.FIGHT;
import static net.slipcor.pvparena.classes.PASpawn.OLD;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena class
 * </pre>
 * <p/>
 * contains >general< arena methods and variables
 *
 * @author slipcor
 * @version v0.10.2
 */

public class Arena {

    private final Set<ArenaClass> classes = new HashSet<>();
    private final Set<ArenaModule> mods = new HashSet<>();
    private final Set<ArenaRegion> regions = new HashSet<>();
    private final Set<PAClassSign> signs = new HashSet<>();
    private final Set<ArenaTeam> teams = new HashSet<>();
    private final Set<String> playedPlayers = new HashSet<>();
    private final Set<String> winners = new HashSet<>();

    private Set<PABlock> blocks = new HashSet<>();
    private Set<PASpawn> spawns = new HashSet<>();

    private final Map<Player, UUID> entities = new HashMap<>();

    private final String name;
    private String prefix = "PVP Arena";
    private String owner = "%server%";

    // arena status
    private boolean fightInProgress;
    private boolean locked;
    private boolean valid;
    private int startCount;

    private ArenaGoal goal;

    // Runnable IDs
    public BukkitRunnable endRunner;
    public BukkitRunnable pvpRunner;
    public BukkitRunnable realEndRunner;
    public BukkitRunnable startRunner;

    private Config config;
    private long startTime;
    private ArenaScoreboard scoreboard = null;

    private ArenaTimer timer;

    public Arena(final String name) {
        this.name = name;
    }

    public void setConfig(Config cfg) {
        this.config = cfg;
    }

    public Config getConfig() {
        return this.config;
    }

    public Set<PABlock> getBlocks() {
        return this.blocks;
    }

    public ArenaClass getArenaClass(String className) {
        return this.classes.stream()
                .filter(ac -> ac.getName().equalsIgnoreCase(className))
                .findAny()
                .orElse(null);
    }

    /**
     * Convert the team class from autoClass configuration
     * NB: AutoClass config should have one of the following formats:
     * - className
     * - teamName1:classNameA;teamName2:classNameB;defaultClassName
     * @param autoClassStr raw string configuration of autoClass
     * @param team The team object of the current player
     * @return An arena class name or an empty optional
     */
    public Optional<String> getAutoClass(String autoClassStr, ArenaTeam team) {
        // Handle complex autoClass config
        if (autoClassStr != null && autoClassStr.contains(":") && autoClassStr.contains(";")) {
            final String[] definitions = autoClassStr.split(";");
            String defaultClass = definitions[definitions.length - 1]; // last element of array is the name of the default class

            String selectedClass = Arrays.stream(definitions)
                    .map(def -> def.split(":"))
                    .filter(defArray -> defArray.length > 1 && team.getName().equalsIgnoreCase(defArray[0]))
                    .findAny()
                    .map(defArray -> defArray[1])
                    .orElse(defaultClass);

            return ofNullable(this.getArenaClass(selectedClass)).map(ArenaClass::getName);
        }

        // Handle simple autoClass config (only class name)
        return ofNullable(this.getArenaClass(autoClassStr)).map(ArenaClass::getName);
    }

    public Set<ArenaClass> getClasses() {
        return this.classes;
    }

    public Player getEntityOwner(final Entity entity) {
        return this.entities.entrySet().stream()
                .filter(entry -> entry.getValue().equals(entity.getUniqueId()))
                .findAny()
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * hand over everyone being part of the arena
     */
    public Set<ArenaPlayer> getEveryone() {
        return ArenaPlayer.getAllArenaPlayers().stream()
                .filter(ap -> this.equals(ap.getArena()))
                .collect(Collectors.toSet());
    }

    public boolean isFightInProgress() {
        return this.fightInProgress;
    }

    public Set<ArenaPlayer> getFighters() {
        return this.teams.stream().flatMap(team -> team.getTeamMembers().stream()).collect(Collectors.toSet());
    }

    public boolean isFreeForAll() {
        return this.goal.isFreeForAll();
    }

    public ArenaScoreboard getScoreboard() {
        if (this.scoreboard == null) {
            this.scoreboard = new ArenaScoreboard(this);
        }
        return this.scoreboard;
    }

    public ArenaGoal getGoal() {
        return this.goal;
    }

    public void setGoal(ArenaGoal goal, boolean updateConfig) {
        goal.setArena(this);
        this.goal = goal;

        if (goal.isFreeForAll()) {
            this.teams.clear();
            this.teams.add(new ArenaTeam("free", "WHITE"));
        }

        if (updateConfig) {
            debug(this, "updating goal config");
            this.config.updateGoal(goal);
        }
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Set<ArenaModule> getMods() {
        return this.mods;
    }

    public boolean hasMod(String modName) {
        return this.mods.stream().anyMatch(m -> m.getName().equalsIgnoreCase(modName));
    }

    public void addModule(ArenaModule module, boolean updateConfig) {
        module.setArena(this);
        module.initConfig();
        this.mods.add(module);

        if (updateConfig) {
            this.updateModsInCfg();
        }
    }

    public void removeModule(String moduleName) {
        this.mods.removeIf(mod -> mod.getName().equalsIgnoreCase(moduleName));
        this.updateModsInCfg();
    }

    private void updateModsInCfg() {
        final List<String> list = this.mods.stream().map(ArenaModule::getName).collect(Collectors.toList());
        this.config.set(CFG.LISTS_MODS, list);

        // createDefaults() saves the config
        this.config.createDefaults(null, list);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public String getName() {
        return this.name;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public Set<String> getPlayedPlayers() {
        return this.playedPlayers;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public Material getReadyBlock() {
        try {
            return this.config.getMaterial(CFG.READY_BLOCK, Material.STICK);
        } catch (final Exception e) {
            Language.logWarn(MSG.ERROR_MAT_NOT_FOUND, "ready block");
        }
        return Material.IRON_BLOCK;
    }

    public ArenaRegion getRegion(final String name) {
        return this.regions.stream()
                .filter(region -> region.getRegionName().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    public Set<ArenaRegion> getRegions() {
        return this.regions;
    }

    public Set<ArenaRegion> getRegionsByType(final RegionType regionType) {
        return this.regions.stream()
                .filter(rg -> rg.getType() == regionType)
                .collect(Collectors.toSet());
    }

    public boolean hasRegionsProtectingLocation(Location loc, RegionProtection protection) {
        return this.getConfig().getBoolean(CFG.PROTECT_ENABLED) && this.regions.stream()
                .anyMatch(region -> region.getShape().contains(new PABlockLocation(loc))
                        && region.getProtections().contains(protection)
                );
    }

    public Set<PAClassSign> getSigns() {
        return this.signs;
    }

    public Set<PASpawn> getSpawns() {
        return this.spawns;
    }

    public ArenaTeam getTeam(final String name) {
        return this.teams.stream()
                .filter(team -> team.getName().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    public Set<ArenaTeam> getTeams() {
        return this.teams;
    }

    public Set<ArenaTeam> getNotEmptyTeams() {
        return this.teams.stream()
                .filter(ArenaTeam::isNotEmpty)
                .collect(Collectors.toSet());
    }

    public Set<String> getTeamNames() {
        return this.teams.stream().map(ArenaTeam::getName).collect(Collectors.toSet());
    }

    public Set<String> getTeamNamesColored() {
        return this.teams.stream().map(ArenaTeam::getColoredName).collect(Collectors.toSet());
    }

    public int getPlayedSeconds() {
        final int seconds = (int) (System.currentTimeMillis() - this.startTime);
        return seconds / 1000;
    }

    public void setStartingTime() {
        this.startTime = System.currentTimeMillis();
    }

    public void addWinner(String winner) {
        this.winners.add(winner);
    }

    public void setWinners(Collection<String> winners) {
        this.winners.clear();
        this.winners.addAll(winners);
    }

    public Set<String> getWinners() {
        return this.winners;
    }

    private boolean isWinner(ArenaPlayer arenaPlayer) {
        if (this.goal.isFreeForAll()) {
            return this.winners.contains(arenaPlayer.getName());
        }
        return this.winners.contains(arenaPlayer.getArenaTeam().getName());
    }

    public boolean isValid() {
        return this.valid;
    }

    public World getWorld() {
        return this.getRegionsByType(RegionType.BATTLE).stream()
                .findAny()
                .map(rg -> Bukkit.getWorld(rg.getWorldName()))
                .orElse(this.spawns.stream()
                        .filter(spawn -> spawn.getName().contains(FIGHT))
                        .findAny()
                        .map(spawn -> spawn.getPALocation().getWorld())
                        .orElse(Bukkit.getWorlds().get(0))
                );
    }

    public void addClass(String className, ItemStack[] items, ItemStack offHand, ItemStack[] armors) {
        if (this.getArenaClass(className) != null) {
            this.removeClass(className);
        }

        this.classes.add(new ArenaClass(className, items, offHand, armors));
    }

    public void addEntity(final Player player, final Entity entity) {
        this.entities.put(player, entity.getUniqueId());
    }

    public void addRegion(final ArenaRegion region) {
        this.regions.add(region);
        debug(this, "adding region: " + region.getRegionName());
    }

    public void broadcast(final String msg) {
        debug(this, "@all: " + msg);
        final Set<ArenaPlayer> players = this.getEveryone();
        for (ArenaPlayer arenaPlayer : players) {
            if (arenaPlayer.getArena() == null || !arenaPlayer.getArena().equals(this)) {
                continue;
            }
            this.msg(arenaPlayer.getPlayer(), msg);
        }
    }

    /**
     * send a message to every player, prefix player name and ChatColor
     *
     * @param msg    the message to send
     * @param color  the color to use
     * @param player the player to prefix
     */
    public void broadcastColored(final String msg, final ChatColor color, final Player player) {
        final String sColor = this.config.getBoolean(CFG.CHAT_COLORNICK) ? color.toString() : "";
        synchronized (this) {
            this.broadcast(sColor + player.getName() + ChatColor.WHITE + ": " + msg.replace("&", "%%&%%"));
        }
    }

    /**
     * send a message to every player except the given one
     *
     * @param sender the player to exclude
     * @param msg    the message to send
     */
    public void broadcastExcept(final CommandSender sender, final String msg) {
        debug(this, sender, "@all/" + sender.getName() + ": " + msg);
        final Set<ArenaPlayer> players = this.getEveryone();
        for (ArenaPlayer arenaPlayer : players) {
            if (this.equals(arenaPlayer.getArena()) && !arenaPlayer.getName().equals(sender.getName())) {
                this.msg(arenaPlayer.getPlayer(), msg);
            }
        }
    }

    public void chooseClass(final Player player, final Sign sign, final String className) {

        debug(this, player, "choosing player class");

        debug(this, player, "checking class perms");
        if (this.config.getBoolean(CFG.PERMS_EXPLICITCLASS) && !player.hasPermission("pvparena.class." + className)) {
            this.msg(player, MSG.ERROR_NOPERM_CLASS, className);
            return; // class permission desired and failed =>
            // announce and OUT
        }

        if (sign != null) {
            if (this.config.getBoolean(CFG.USES_CLASSSIGNSDISPLAY)) {
                PAClassSign.remove(this.signs, player);
                final Block block = sign.getBlock();
                PAClassSign classSign = PAClassSign.used(block.getLocation(), this.signs);
                if (classSign == null) {
                    classSign = new PAClassSign(block.getLocation());
                    this.signs.add(classSign);
                }
                if (!classSign.add(player)) {
                    this.msg(player, MSG.ERROR_CLASS_FULL, className);
                    return;
                }
            }

            if (ArenaModuleManager.cannotSelectClass(this, player, className)) {
                return;
            }
            if (this.startRunner != null) {
                ArenaPlayer.fromPlayer(player).setStatus(PlayerStatus.READY);
            }
        }
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        if (aPlayer.getArena() == null) {

            PVPArena.getInstance().getLogger().warning(String.format("failed to set class %s to player %s", className, player.getName()));
        } else if (!ArenaModuleManager.cannotSelectClass(this, player, className)) {

            aPlayer.setArenaClass(className);
            if (aPlayer.getArenaClass() != null) {
                if ("custom".equalsIgnoreCase(className)) {
                    // if custom, give stuff back
                    aPlayer.reloadInventory(true);
                } else {
                    InventoryManager.clearInventory(player);
                    aPlayer.equipPlayerFightItems();
                }
            }
            return;
        }
        InventoryManager.clearInventory(player);
    }

    public void clearRegions() {
        this.regions.forEach(ArenaRegion::reset);
    }

    /**
     * initiate the arena start countdown
     */
    public void countDown() {
        if (this.startRunner != null || this.fightInProgress) {

            if (!this.config.getBoolean(CFG.READY_ENFORCECOUNTDOWN) && this.getArenaClass(this.config.getString(CFG.READY_AUTOCLASS)) == null && !this.fightInProgress) {
                this.startRunner.cancel();
                this.startRunner = null;
                this.broadcast(Language.parse(MSG.TIMER_COUNTDOWN_INTERRUPTED));
            }
            return;
        }

        new StartRunnable(this, this.config.getInt(CFG.TIME_STARTCOUNTDOWN));
    }

    /**
     * count all players being ready
     *
     * @return the number of ready players
     */
    public int countReadyPlayers() {
        long sum = this.teams.stream()
                .flatMap(team -> team.getTeamMembers().stream())
                .filter(p -> p.getStatus() == PlayerStatus.READY)
                .count();
        debug(this, "counting ready players: " + sum);
        return (int) sum;
    }


    /**
     * give customized rewards to players
     *
     * @param arenaPlayer the arenaPlayer to give the reward
     */
    public void giveRewards(final ArenaPlayer arenaPlayer) {
        debug(arenaPlayer, "giving rewards to " + arenaPlayer.getName());

        ArenaModuleManager.giveRewards(this, arenaPlayer);
        final ItemStack[] items = this.config.getItems(CFG.ITEMS_REWARDS);

        boolean isRandom = this.config.getBoolean(CFG.ITEMS_RANDOMREWARD);
        Random rRandom = new Random();

        PAWinEvent dEvent = new PAWinEvent(this, arenaPlayer.getPlayer(), items);
        Bukkit.getPluginManager().callEvent(dEvent);

        debug(arenaPlayer, "start " + this.startCount + " - minplayers: " + this.config.getInt(CFG.ITEMS_MINPLAYERSFORREWARD));

        if (items == null || items.length < 1 || this.config.getInt(CFG.ITEMS_MINPLAYERSFORREWARD) > this.startCount) {
            return;
        }

        int randomItemIdx = rRandom.nextInt(items.length);
        List<ItemStack> rewardItems = isRandom ? List.of(items[randomItemIdx]) : List.of(items);
        Player player = arenaPlayer.getPlayer();

        Bukkit.getScheduler().runTask(PVPArena.getInstance(), () -> {
            try {
                rewardItems.stream()
                        .filter(Objects::nonNull)
                        .forEach(item -> {
                            Inventory playerInv = player.getInventory();
                            playerInv.setItem(playerInv.firstEmpty(), item);
                        });
            } catch (final Exception e) {
                this.msg(player, MSG.ERROR_INVENTORY_FULL);
            }
        });

    }

    public boolean hasEntity(final Entity entity) {
        return this.entities.containsValue(entity.getUniqueId());
    }

    public boolean hasAlreadyPlayed(final String playerName) {
        return this.playedPlayers.contains(playerName);
    }

    public void hasNotPlayed(final ArenaPlayer player) {
        if (this.config.getBoolean(CFG.JOIN_ALLOW_REJOIN)) {
            return;
        }
        this.playedPlayers.remove(player.getName());
    }

    public boolean hasPlayer(final Player player) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        for (ArenaTeam team : this.teams) {
            if (team.hasPlayer(arenaPlayer)) {
                return true;
            }
        }
        return this.equals(arenaPlayer.getArena());
    }

    public boolean hasPlayerInTeams(ArenaPlayer aPlayer) {
        return this.teams.stream().anyMatch(t -> t.hasPlayer(aPlayer));
    }

    public void increasePlayerCount() {
        this.startCount++;
    }

    public void markPlayedPlayer(final String playerName) {
        this.playedPlayers.add(playerName);
    }

    public void msg(final CommandSender sender, Help.HELP helpMsg) {
        helpMsg.get().forEach(helpLine -> pmsg(sender, StringParser.colorize(helpLine)));
    }

    public void msg(final CommandSender sender, final MSG msg, Object... args) {
        this.msg(sender, Language.parse(msg, args));
    }

    public void msg(final CommandSender sender, String msg) {
        if (sender != null && !StringUtils.isBlank(msg)) {
            debug(this, '@' + sender.getName() + ": " + msg);
            sender.sendMessage(Language.parse(MSG.MESSAGES_GENERAL, this.prefix, msg));
        }
    }

    /**
     * return an understandable representation of a player's death cause
     *
     * @param player  the dying player
     * @param cause   the cause
     * @param damager an eventual damager entity
     * @return a colored string
     */
    public String parseDeathCause(final Player player, final DamageCause cause,
                                  final Entity damager) {

        if (cause == null) {
            return Language.parse(MSG.DEATHCAUSE_CUSTOM);
        }

        debug(this, player, "return a damage name for : " + cause);

        debug(this, player, "damager: " + damager);

        ArenaPlayer aPlayer = null;
        ArenaTeam team = null;
        if (damager instanceof Player) {
            aPlayer = ArenaPlayer.fromPlayer((Player) damager);
            team = aPlayer.getArenaTeam();
        }

        final EntityDamageEvent lastDamageCause = player.getLastDamageCause();

        switch (cause) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                if (damager instanceof Player && team != null) {
                    return team.colorizePlayer(aPlayer) + ChatColor.YELLOW;
                }

                try {
                    Entity eventDamager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
                    debug(this, player, "last damager: " + eventDamager.getType());
                    return Language.parse(MSG.getByName("DEATHCAUSE_" + eventDamager.getType().name()));
                } catch (final Exception e) {
                    return Language.parse(MSG.DEATHCAUSE_CUSTOM);
                }
            case ENTITY_EXPLOSION:
                try {
                    Entity eventDamager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
                    debug(this, player, "last damager: " + eventDamager.getType());
                    return Language.parse(MSG.getByName("DEATHCAUSE_" + eventDamager.getType().name()));
                } catch (final Exception e) {
                    return Language.parse(MSG.DEATHCAUSE_ENTITY_EXPLOSION);
                }
            case PROJECTILE:
                if (damager instanceof Player && team != null) {
                    return team.colorizePlayer(aPlayer) + ChatColor.YELLOW;
                }
                try {
                    Entity eventDamager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
                    ProjectileSource source = ((Projectile) eventDamager).getShooter();
                    LivingEntity lEntity = (LivingEntity) source;

                    debug(this, player, "last damager: " + lEntity.getType());

                    return Language.parse(MSG.getByName("DEATHCAUSE_" + lEntity.getType().name()));
                } catch (final Exception e) {

                    return Language.parse(MSG.DEATHCAUSE_PROJECTILE);
                }
            default:
                break;
        }
        MSG string = MSG.getByName("DEATHCAUSE_" + cause);
        if (string == null) {
            PVPArena.getInstance().getLogger().warning("Unknown cause: " + cause);
            string = MSG.DEATHCAUSE_VOID;
        }
        return Language.parse(string);
    }

    /**
     * a player leaves from the arena
     *
     * @param player the leaving player
     */
    public void playerLeave(final Player player, final CFG location, final boolean silent,
                            final boolean force, final boolean soft) {
        if (player == null) {
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        this.goal.parseLeave(aPlayer);

        if (!this.fightInProgress) {
            this.startCount--;
            this.playedPlayers.remove(player.getName());
        }
        debug(this, player, "fully removing player from arena");
        if (!silent) {

            final ArenaTeam team = aPlayer.getArenaTeam();
            if (team == null) {

                this.broadcastExcept(
                        player,
                        Language.parse(MSG.FIGHT_PLAYER_LEFT, player.getName()
                                + ChatColor.YELLOW));
            } else {
                ArenaModuleManager.parsePlayerLeave(this, player, team);

                this.broadcastExcept(
                        player,
                        Language.parse(MSG.FIGHT_PLAYER_LEFT,
                                team.colorizePlayer(aPlayer) + ChatColor.YELLOW));
            }
            this.msg(player, MSG.NOTICE_YOU_LEFT);
        }

        this.removePlayer(aPlayer, this.config.getString(location), soft, force);

        if (!this.config.getBoolean(CFG.READY_ENFORCECOUNTDOWN) && this.startRunner != null && this.config.getInt(CFG.READY_MINPLAYERS) > 0 &&
                this.getFighters().size() <= this.config.getInt(CFG.READY_MINPLAYERS)) {
            this.startRunner.cancel();
            this.broadcast(Language.parse(MSG.TIMER_COUNTDOWN_INTERRUPTED));
            this.startRunner = null;
        }

        if (this.fightInProgress) {
            ArenaManager.checkAndCommit(this, force);
        }

        aPlayer.reset();
    }

    /**
     * check if an arena is ready
     *
     * @return null if ok, error message otherwise
     */
    public String ready() {
        debug(this, "ready check !!");

        final int players = TeamManager.countPlayersInTeams(this);
        if (players < 2) {
            return Language.parse(MSG.ERROR_READY_1_ALONE);
        }
        if (players < this.config.getInt(CFG.READY_MINPLAYERS)) {
            return Language.parse(MSG.ERROR_READY_4_MISSING_PLAYERS);
        }

        if (!this.isFightInProgress() && this.config.getBoolean(CFG.READY_CHECKEACHPLAYER)) {
            for (ArenaTeam team : this.teams) {
                for (ArenaPlayer ap : team.getTeamMembers()) {
                    if (ap.getStatus() != PlayerStatus.READY) {
                        return Language.parse(MSG.ERROR_READY_0_ONE_PLAYER_NOT_READY);
                    }
                }
            }
        }

        if (!this.isFreeForAll()) {
            final Set<String> activeTeams = new HashSet<>();

            if (!this.isFightInProgress()) {
                for (ArenaTeam team : this.teams) {
                    for (ArenaPlayer ap : team.getTeamMembers()) {
                        if (!this.config.getBoolean(CFG.READY_CHECKEACHTEAM) || ap.getStatus() == PlayerStatus.READY) {
                            activeTeams.add(team.getName());
                            break;
                        }
                    }
                }

                if (activeTeams.size() < 2) {
                    return Language.parse(MSG.ERROR_READY_2_TEAM_ALONE);
                }
            }

            if (this.config.getBoolean(CFG.USES_EVENTEAMS) && !TeamManager.checkEven(this)) {
                return Language.parse(MSG.NOTICE_WAITING_EQUAL);
            }
        }

        for (ArenaTeam team : this.teams) {
            for (ArenaPlayer p : team.getTeamMembers()) {
                debug(this, p.getPlayer(), "checking class: " + p.getPlayer().getName());

                if (p.getArenaClass() == null) {
                    debug(this, p.getPlayer(), "player has no class");

                    String autoClass = this.config.getDefinedString(CFG.READY_AUTOCLASS);
                    if (this.config.getBoolean(CFG.USES_PLAYER_OWN_INVENTORY) && this.getArenaClass(p.getName()) != null) {
                        autoClass = p.getName();
                    }
                    if (autoClass != null && this.getArenaClass(autoClass) != null) {
                        this.selectClass(p, autoClass, true);
                    } else {
                        // player no class!
                        PVPArena.getInstance().getLogger().warning("Player no class: " + p.getPlayer());
                        return Language.parse(MSG.ERROR_READY_5_ONE_PLAYER_NO_CLASS);
                    }
                }
            }
        }

        if(this.isFightInProgress()) {
            // Bypass ready ratio checks if joining during match
            return null;
        }

        final int readyPlayers = this.countReadyPlayers();

        if (players > readyPlayers) {
            final double ratio = this.config.getDouble(CFG.READY_NEEDEDRATIO);
            debug(this, "ratio: " + ratio);
            if (ratio > 0) {
                double aRatio = ((double) readyPlayers) / players;
                if (aRatio >= ratio) {
                    return "";
                }
            }
            return Language.parse(MSG.ERROR_READY_0_ONE_PLAYER_NOT_READY);
        }
        return this.config.getBoolean(CFG.READY_ENFORCECOUNTDOWN) ? "" : null;
    }

    /**
     * call event when a player is exiting from an arena (by plugin)
     *
     * @param player the player to remove
     */
    public void callExitEvent(final Player player) {
        final PAExitEvent exitEvent = new PAExitEvent(this, player);
        Bukkit.getPluginManager().callEvent(exitEvent);
    }

    /**
     * call event when a player is leaving an arena (on his own)
     *
     * @param player the player to remove
     */
    public void callLeaveEvent(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final PALeaveEvent event = new PALeaveEvent(this, player, aPlayer.getStatus() == PlayerStatus.FIGHT);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void removeClass(final String string) {
        this.classes.removeIf(ac -> ac.getName().equals(string));
    }

    public void removeEntity(final Entity entity) {
        this.entities.values().removeIf(uuid -> uuid.equals(entity.getUniqueId()));
    }

    /**
     * remove a player from the arena
     *  @param aPlayer the player to reset
     * @param tploc  the coord string to teleport the player to
     */
    public void removePlayer(final ArenaPlayer aPlayer, final String tploc, final boolean soft,
                             final boolean force) {
        debug(aPlayer, "removing player {}, soft: {}, tp to {}", aPlayer.getName(), soft, tploc);
        this.resetPlayer(aPlayer, tploc, soft, force);

        if (!soft && aPlayer.getArenaTeam() != null) {
            aPlayer.getArenaTeam().remove(aPlayer);
        }

        this.callExitEvent(aPlayer.getPlayer());
        if (this.config.getBoolean(CFG.USES_CLASSSIGNSDISPLAY)) {
            PAClassSign.remove(this.signs, aPlayer.getPlayer());
        }

        aPlayer.getPlayer().setNoDamageTicks(60);
    }

    /**
     * reset an arena
     *
     * @param force enforce it
     */
    public void resetPlayers(final boolean force) {
        debug(this, "resetting player manager");
        final Set<ArenaPlayer> players = new HashSet<>();
        for (ArenaTeam team : this.teams) {
            for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                debug(this, arenaPlayer.getPlayer(), "player: " + arenaPlayer.getName());
                if (arenaPlayer.getArena() == null || !arenaPlayer.getArena().equals(this)) {
                    debug(this, arenaPlayer.getPlayer(), "> skipped");
                } else {
                    debug(this, arenaPlayer.getPlayer(), "> added");
                    players.add(arenaPlayer);
                }
            }
        }

        for (ArenaPlayer arenaPlayer : players) {

            arenaPlayer.debugPrint();
            if (arenaPlayer.getStatus() != null && this.isWinner(arenaPlayer)) {
                // TODO enhance wannabe-smart exploit fix for people that
                // spam join and leave the arena to make one of them win
                final Player player = arenaPlayer.getPlayer();
                if (!force && this.fightInProgress) {
                    // if we are remaining, give reward!
                    arenaPlayer.getStats().incWins();
                    this.giveRewards(arenaPlayer);
                }
                this.callExitEvent(player);
                this.resetPlayer(arenaPlayer, this.config.getString(CFG.TP_WIN, OLD), false, force);
            } else {
                final PALoseEvent loseEvent = new PALoseEvent(this, arenaPlayer.getPlayer());
                Bukkit.getPluginManager().callEvent(loseEvent);

                final Player player = arenaPlayer.getPlayer();
                if (!force) {
                    arenaPlayer.getStats().incLosses();
                }
                this.callExitEvent(player);
                this.resetPlayer(arenaPlayer, this.config.getString(CFG.TP_LOSE, OLD), false, force);
            }

            arenaPlayer.reset();
        }
        for (ArenaPlayer player : ArenaPlayer.getAllArenaPlayers()) {
            if (this.equals(player.getArena()) && player.getStatus() == PlayerStatus.WATCH) {

                this.callExitEvent(player.getPlayer());
                this.resetPlayer(player, this.config.getString(CFG.TP_EXIT, OLD), false, force);
                player.setArena(null);
                player.reset();
            }
        }
    }

    /**
     * reset an arena
     */
    public void reset(boolean force) {

        if(!force) {
            final PAEndEvent event = new PAEndEvent(this);
            Bukkit.getPluginManager().callEvent(event);
        }

        debug(this, "resetting arena; force: " + force);
        for (PAClassSign as : this.signs) {
            as.clear();
        }
        this.signs.clear();
        this.playedPlayers.clear();
        this.resetPlayers(force);
        this.setFightInProgress(false);

        ofNullable(this.endRunner).ifPresent(BukkitRunnable::cancel);
        ofNullable(this.realEndRunner).ifPresent(BukkitRunnable::cancel);
        ofNullable(this.pvpRunner).ifPresent(BukkitRunnable::cancel);
        this.endRunner = null;
        this.realEndRunner = null;
        this.pvpRunner = null;
        ofNullable(this.timer).ifPresent(timer -> {
            timer.stop();
            this.timer = null;
        });

        ArenaModuleManager.reset(this, force);
        this.clearRegions();
        ofNullable(this.goal).ifPresent(arenaGoal -> arenaGoal.reset(force));

        try {
            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), () -> {
                Arena.this.playedPlayers.clear();
                Arena.this.startCount = 0;
            }, 30L);
        } catch (Exception ignored) {
        }
        this.scoreboard = null;
    }

    /**
     * reset a player to his pre-join values
     * @param aPlayer      the player to reset
     * @param destination the teleport location
     * @param soft        if location should be preserved (another tp incoming)
     */
    private void resetPlayer(@NotNull ArenaPlayer aPlayer, String destination, boolean soft, boolean force) {
        debug(aPlayer, "resetting player, soft: {}", soft);

        Player player = aPlayer.getPlayer();
        try {
            ArrowHack.processArrowHack(player);
        } catch (Exception ignored) {
        }

        ofNullable(aPlayer.getState()).ifPresent(playerState -> playerState.unload(aPlayer, soft));

        this.getScoreboard().reset(player, force, soft);

        ArenaModuleManager.resetPlayer(this, player, soft, force);

        if (!soft && (!aPlayer.hasCustomClass() || this.config.getBoolean(CFG.GENERAL_CUSTOMRETURNSGEAR))) {
            aPlayer.reloadInventory(true);
        }

        if(soft || !force) {
            aPlayer.saveStatistics();
        }

        TeleportManager.teleportPlayerAfterReset(this, destination, soft, force, aPlayer);
    }

    public void selectClass(final ArenaPlayer aPlayer, final String cName, boolean checkModules) {
        if (checkModules && ArenaModuleManager.cannotSelectClass(this, aPlayer.getPlayer(), cName)) {
            return;
        }
        for (ArenaClass c : this.classes) {
            if (c.getName().equalsIgnoreCase(cName)) {
                aPlayer.setArenaClass(c);
                if (aPlayer.getArenaClass() != null) {
                    aPlayer.setArena(this);
                    aPlayer.createState(aPlayer.getPlayer());
                    InventoryManager.clearInventory(aPlayer.getPlayer());
                    c.equip(aPlayer.getPlayer());
                    this.msg(aPlayer.getPlayer(), MSG.CLASS_PREVIEW, c.getName());
                }
                return;
            }
        }
        this.msg(aPlayer.getPlayer(), MSG.ERROR_CLASS_NOT_FOUND, cName);
    }

    public void setFightInProgress(final boolean fightInProgress) {
        this.fightInProgress = fightInProgress;
        debug(this, "fighting : " + fightInProgress);
    }

    public void clearSpawn(final String name, String teamName, String className) {
        this.config.clearSpawn(name, teamName, className);
        this.removeSpawn(new PASpawn(null, name, teamName, null));
    }

    public void start() {
        this.start(false);
    }

    /**
     * initiate the arena start
     */
    public void start(final boolean forceStart) {
        debug(this, "start()");
        if (this.getConfig().getBoolean(CFG.USES_SCOREBOARD)) {
            if (this.isFightInProgress()) {
                this.getScoreboard().show();
            }
        }
        this.startRunner = null;
        this.winners.clear();

        if (this.fightInProgress) {
            debug(this, "already in progress! OUT!");
            return;
        }
        int sum = 0;
        for (ArenaTeam team : this.teams) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                if (forceStart) {
                    ap.setStatus(PlayerStatus.READY);
                }
                if (ap.getStatus() == PlayerStatus.LOUNGE || ap.getStatus() == PlayerStatus.READY) {
                    sum++;
                }
            }
        }
        debug(this, "sum == " + sum);
        final String error = this.ready();

        boolean overRide = false;

        if (forceStart) {
            overRide = error == null ||
                    error.contains(Language.parse(MSG.ERROR_READY_1_ALONE)) ||
                    error.contains(Language.parse(MSG.ERROR_READY_2_TEAM_ALONE)) ||
                    error.contains(Language.parse(MSG.ERROR_READY_3_TEAM_MISSING_PLAYERS)) ||
                    error.contains(Language.parse(MSG.ERROR_READY_4_MISSING_PLAYERS));
        }

        if (overRide || StringUtils.isBlank(error)) {
            final Boolean handle = WorkflowManager.handleStart(this, null, forceStart);

            if (overRide || Boolean.TRUE.equals(handle)) {
                debug(this, "START!");
                this.setFightInProgress(true);

                this.timer = new ArenaTimer(this);
                this.timer.start();

                if (this.getConfig().getBoolean(CFG.USES_SCOREBOARD)) {
                    this.getScoreboard().show();
                }

            } else {

                // false
                PVPArena.getInstance().getLogger().info("START aborted by event cancel");
                //reset(true);
            }
        } else {
            // false
            this.broadcast(Language.parse(MSG.ERROR_ERROR, error));
            //reset(true);
        }
    }

    public void addPlayerDuringMatch(ArenaPlayer arenaPlayer) {
        arenaPlayer.setStatus(PlayerStatus.FIGHT);
        this.getGoal().initiate(arenaPlayer);
        this.getScoreboard().setupPlayer(arenaPlayer);
        SpawnManager.distributePlayer(this, arenaPlayer);
        this.msg(arenaPlayer.getPlayer(), MSG.FIGHT_BEGINS);
    }

    public void stop(final boolean force) {
        for (ArenaPlayer p : this.getFighters()) {
            this.playerLeave(p.getPlayer(), CFG.TP_EXIT, true, force, false);
        }
        this.reset(force);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Arena arena = (Arena) o;
        return this.name.equals(arena.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    public void addBlock(final PABlock paBlock) {
        this.removeBlock(paBlock);
        this.blocks.add(paBlock);
    }

    public void removeBlock(final PABlock paBlock) {
        this.blocks.removeIf(block -> block.getName().equals(paBlock.getName()));
    }

    public void setSpawns(Set<PASpawn> spawns) {
        this.spawns = spawns;
    }

    public void setBlocks(Set<PABlock> blocks) {
        this.blocks = blocks;
    }

    /**
     * Add a spawn
     *
     * @param paSpawn spawn to set
     *
     * @return true if spawn existed and has been replaced
     */
    public boolean setSpawn(final PASpawn paSpawn) {
        // remove spawn with same name, team and class before
        boolean removed = this.removeSpawn(paSpawn);
        this.spawns.add(paSpawn);
        this.config.addSpawn(paSpawn);
        return removed;
    }

    public boolean removeSpawn(final PASpawn paSpawn) {
        return this.spawns.removeIf(spawn ->
                spawn.getName().equals(paSpawn.getName())
                        && (spawn.getTeamName() == null || spawn.getTeamName().equals(paSpawn.getTeamName()))
                        && (spawn.getClassName() == null || spawn.getClassName().equals(paSpawn.getClassName()))
        );
    }

    public static void pmsg(final CommandSender sender, MSG msg, Object... args) {
        pmsg(sender, Language.parse(msg, args));
    }

    public static void pmsg(final CommandSender sender, Help.HELP helpMsg) {
        helpMsg.get().forEach(helpLine -> pmsg(sender, StringParser.colorize(helpLine)));
    }

    public static void pmsg(final CommandSender sender, final String msg) {
        if (sender != null && !StringUtils.isBlank(msg)) {
            debug(sender, "@{} : {}", sender.getName(), msg);
            String prefix = PVPArena.getInstance().getConfig().getString("globalPrefix", "PVP Arena");
            sender.sendMessage(Language.parse(MSG.MESSAGES_GENERAL, prefix, msg));
        }
    }
}
