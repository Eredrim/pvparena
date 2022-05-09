package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.classes.*;
import net.slipcor.pvparena.core.*;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.*;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.loadables.ArenaRegion.RegionType;
import net.slipcor.pvparena.managers.*;
import net.slipcor.pvparena.runnables.StartRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

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

    private static Debug DEBUG = new Debug(3);
    private Debug debug;
    private final Set<ArenaClass> classes = new HashSet<>();
    private final Set<ArenaGoal> goals = new HashSet<>();
    private final Set<ArenaModule> mods = new HashSet<>();
    private final Set<ArenaRegion> regions = new HashSet<>();
    private final Set<PAClassSign> signs = new HashSet<>();
    private final Set<ArenaTeam> teams = new HashSet<>();
    private final Set<String> playedPlayers = new HashSet<>();

    private final Set<PABlock> blocks = new HashSet<>();
    private final Set<PASpawn> spawns = new HashSet<>();

    private final Map<Player, UUID> entities = new HashMap<>();

    private PARoundMap rounds;

    private final String name;
    private String prefix = "PVP Arena";
    private String owner = "%server%";

    // arena status
    private boolean fightInProgress;
    private boolean locked;
    private boolean free;
    private boolean valid;
    private int startCount;
    private int round;

    // Runnable IDs
    public BukkitRunnable endRunner;
    public BukkitRunnable pvpRunner;
    public BukkitRunnable realEndRunner;
    public BukkitRunnable startRunner;
    public int spawnCampRunnerID = -1;

    private boolean gaveRewards;

    private Config cfg;
    private YamlConfiguration language = new YamlConfiguration();
    private long startTime;
    private Scoreboard scoreboard = null;

    public Arena(final String name) {
        this.name = name;

        getDebugger().i("loading Arena " + name);
        final File file = new File(PVPArena.instance.getDataFolder().getPath()
                + "/arenas/" + name + ".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        cfg = new Config(file);
        valid = ConfigurationManager.configParse(this, cfg);
        if (valid) {
            StatisticsManager.loadStatistics(this);
            SpawnManager.loadSpawns(this, cfg);

            final String langName = (String) cfg.getUnsafe("general.lang");
            if (langName == null || "none".equals(langName)) {
                return;
            }

            final File langFile = new File(PVPArena.instance.getDataFolder(), langName);
            language = new YamlConfiguration();
            try {
                language.load(langFile);
            } catch (final InvalidConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addClass(String className, ItemStack[] items, ItemStack offHand, ItemStack[] armors) {
        if (getClass(className) != null) {
            removeClass(className);
        }

        classes.add(new ArenaClass(className, items, offHand, armors));
    }

    public void addClass(ArenaClass newClass) {
        if (getClass(newClass.getName()) != null) {
            removeClass(newClass.getName());
        }

        classes.add(newClass);
    }

    public boolean addCustomScoreBoardEntry(final ArenaModule module, final String key, final int value) {
        debug.i("module "+module+" tries to set custom scoreboard value '"+key+"' to score "+value);
        if (key == null || key.isEmpty()) {
            debug.i("empty -> remove");
            return removeCustomScoreBoardEntry(module, value);
        }
        if (scoreboard == null) {
            debug.i("scoreboard is not setup!");
            return false;
        }
        try {
            Team mTeam = null;
            String string;
            String prefix;
            String suffix;

            if (key.length() < 17) {
                string = key;
                prefix = "";
                suffix = "";
            } else {
                String split[]= StringParser.splitForScoreBoard(key);
                prefix = split[0];
                string = split[1];
                suffix = split[2];
            }
            for (Team team : scoreboard.getTeams()) {
                if (team.getName().equals("pa_msg_"+value)) {
                    mTeam = team;
                }
            }

            if (mTeam == null) {
                mTeam = scoreboard.registerNewTeam("pa_msg_"+value);
            }
            mTeam.setPrefix(prefix);
            mTeam.setSuffix(suffix);

            for (String entry : scoreboard.getEntries()) {
                if (scoreboard.getObjective("lives").getScore(entry).getScore() == value) {
                    mTeam.removeEntry(entry);
                    scoreboard.getObjective("lives").getScore(string).setScore(0);
                    scoreboard.resetScores(entry);
                    break;
                }
            }
            mTeam.addEntry(string);
            scoreboard.getObjective("lives").getScore(string).setScore(value);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void addEntity(final Player player, final Entity entity) {
        entities.put(player, entity.getUniqueId());
    }

    public void addRegion(final ArenaRegion region) {
        regions.add(region);
        getDebugger().i("loading region: " + region.getRegionName());
        if (region.getType() == RegionType.JOIN) {
            if (cfg.getBoolean(CFG.JOIN_FORCE)) {
                region.initTimer();
            }
        } else if (region.getType() == RegionType.WATCH || region.getType() == RegionType.LOUNGE) {
            region.initTimer();
        }
    }

    public void broadcast(final String msg) {
        getDebugger().i("@all: " + msg);
        final Set<ArenaPlayer> players = getEveryone();
        for (final ArenaPlayer p : players) {
            if (p.getArena() == null || !p.getArena().equals(this)) {
                continue;
            }
            msg(p.get(), msg);
        }
    }

    /**
     * send a message to every player, prefix player name and ChatColor
     *
     * @param msg    the message to send
     * @param color  the color to use
     * @param player the player to prefix
     */
    public void broadcastColored(final String msg, final ChatColor color,
                                 final Player player) {
        final String sColor = cfg.getBoolean(CFG.CHAT_COLORNICK)?color.toString():"";
        synchronized (this) {
            broadcast(sColor + player.getName() + ChatColor.WHITE + ": " + msg.replace("&", "%%&%%"));
        }
    }

    /**
     * send a message to every player except the given one
     *
     * @param sender the player to exclude
     * @param msg    the message to send
     */
    public void broadcastExcept(final CommandSender sender, final String msg) {
        getDebugger().i("@all/" + sender.getName() + ": " + msg, sender);
        final Set<ArenaPlayer> players = getEveryone();
        for (final ArenaPlayer p : players) {
            if (p.getArena() == null || !p.getArena().equals(this)) {
                continue;
            }
            if (p.getName().equals(sender.getName())) {
                continue;
            }
            msg(p.get(), msg);
        }
    }

    public void chooseClass(final Player player, final Sign sign, final String className) {

        getDebugger().i("choosing player class", player);

        getDebugger().i("checking class perms", player);
        if (cfg.getBoolean(CFG.PERMS_EXPLICITCLASS)
                && !player.hasPermission("pvparena.class." + className)) {
            msg(player,
                    Language.parse(this, MSG.ERROR_NOPERM_CLASS, className));
            return; // class permission desired and failed =>
            // announce and OUT
        }

        if (sign != null) {
            if (cfg.getBoolean(CFG.USES_CLASSSIGNSDISPLAY)) {
                PAClassSign.remove(signs, player);
                final Block block = sign.getBlock();
                PAClassSign classSign = PAClassSign.used(block.getLocation(), signs);
                if (classSign == null) {
                    classSign = new PAClassSign(block.getLocation());
                    signs.add(classSign);
                }
                if (!classSign.add(player)) {
                    msg(player,
                            Language.parse(this, MSG.ERROR_CLASS_FULL, className));
                    return;
                }
            }

            if (ArenaModuleManager.cannotSelectClass(this, player, className)) {
                return;
            }
            if (startRunner != null) {
                ArenaPlayer.parsePlayer(player.getName()).setStatus(Status.READY);
            }
        }
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        if (aPlayer.getArena() == null) {
            PVPArena.instance.getLogger().warning(
                    "failed to set class " + className + " to player "
                            + player.getName());
        } else if (!ArenaModuleManager.cannotSelectClass(this, player, className)) {
            aPlayer.setArenaClass(className);
            if (aPlayer.getArenaClass() != null) {
                if ("custom".equalsIgnoreCase(className)) {
                    // if custom, give stuff back
                    ArenaPlayer.reloadInventory(this, player, false);
                } else {
                    InventoryManager.clearInventory(player);
                    ArenaPlayer.givePlayerFightItems(this, player);
                }
            }
            return;
        }
        InventoryManager.clearInventory(player);
    }

    public void clearRegions() {
        for (final ArenaRegion region : regions) {
            region.reset();
        }
    }

    /**
     * initiate the arena start countdown
     */
    public void countDown() {
        if (startRunner != null || fightInProgress) {

            if (!cfg.getBoolean(CFG.READY_ENFORCECOUNTDOWN) && getClass(cfg.getString(CFG.READY_AUTOCLASS)) == null && !fightInProgress) {
                startRunner.cancel();
                startRunner = null;
                broadcast(Language.parse(this, MSG.TIMER_COUNTDOWN_INTERRUPTED));
            }
            return;
        }

        new StartRunnable(this, cfg
                .getInt(CFG.TIME_STARTCOUNTDOWN));
    }

    /**
     * count all players being ready
     *
     * @return the number of ready players
     */
    public int countReadyPlayers() {
        int sum = 0;
        for (final ArenaTeam team : teams) {
            for (final ArenaPlayer p : team.getTeamMembers()) {
                if (p.getStatus() == Status.READY) {
                    sum++;
                }
            }
        }
        getDebugger().i("counting ready players: " + sum);
        return sum;
    }

    public Config getArenaConfig() {
        return cfg;
    }

    public Set<PABlock> getBlocks() {
        return blocks;
    }

    public ArenaClass getClass(final String className) {
        for (final ArenaClass ac : classes) {
            if (ac.getName().equalsIgnoreCase(className)) {
                return ac;
            }
        }
        return null;
    }

    public Set<ArenaClass> getClasses() {
        return classes;
    }

    public Debug getDebugger() {
        if (debug == null) {
            debug = new Debug(this);
        }
        return debug;
    }

    public Player getEntityOwner(final Entity entity) {
        for (final Map.Entry<Player, UUID> playerUUIDEntry : entities.entrySet()) {
            if (playerUUIDEntry.getValue().equals(entity.getUniqueId())) {
                return playerUUIDEntry.getKey();
            }
        }
        return null;
    }

    /**
     * hand over everyone being part of the arena
     */
    public Set<ArenaPlayer> getEveryone() {
        return ArenaPlayer.getAllArenaPlayers().stream()
                .filter(ap -> this.equals(ap.getArena()))
                .collect(Collectors.toSet());
    }

    /**
     * hand over all players being member of a team
     */
    public Set<ArenaPlayer> getFighters() {

        final Set<ArenaPlayer> players = new HashSet<>();

        for (final ArenaTeam team : teams) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                players.add(ap);
            }
        }
        return players;
    }

    public Set<ArenaGoal> getGoals() {
        return round == 0 ? goals : rounds.getGoals(round);
    }

    public Set<ArenaModule> getMods() {
        return mods;
    }

    public String getName() {
        return name;
    }

    public Location getOffset(String spawnName) {
        List<String> offsets = getArenaConfig().getStringList(CFG.TP_OFFSETS.getNode(), new ArrayList<String>());
        for (String value : offsets) {
            if (value != null && value.contains(":")) {
                String[] split = value.split(":");
                if (spawnName.equals(split[0])) {
                    String[] vals = split[1].split(";");
                    try {
                        return new Location(
                                Bukkit.getServer().getWorlds().get(0),
                                Double.parseDouble(vals[0]),
                                Double.parseDouble(vals[1]),
                                Double.parseDouble(vals[2])
                        );
                    } catch (Exception e) {

                    }
                }
            }
        }
        return null;
    }

    public String getOwner() {
        return owner;
    }

    public Set<String> getPlayedPlayers() {
        return playedPlayers;
    }

    public String getPrefix() {
        return prefix;
    }

    public Material getReadyBlock() {
        getDebugger().i("reading ready block");
        try {
            Material mMat = cfg.getMaterial(CFG.READY_BLOCK, Material.STICK);
            getDebugger().i("mMat now is " + mMat.name());
            return mMat;
        } catch (final Exception e) {
            Language.logWarn(MSG.ERROR_MAT_NOT_FOUND, "ready block");
        }
        return Material.IRON_BLOCK;
    }

    public ArenaRegion getRegion(final String name) {
        for (final ArenaRegion region : regions) {
            if (region.getRegionName().equalsIgnoreCase(name)) {
                return region;
            }
        }
        return null;
    }

    public Set<ArenaRegion> getRegions() {
        return regions;
    }

    public int getRound() {
        return round;
    }

    public int getRoundCount() {
        return rounds.getCount();
    }

    public PARoundMap getRounds() {
        return rounds;
    }

    public Set<PAClassSign> getSigns() {
        return signs;
    }

    public Set<PASpawn> getSpawns() {
        return spawns;
    }

    private Scoreboard getSpecialScoreboard() {
        if (this.scoreboard == null) {
            this.scoreboard = this.getCommonScoreboard(true);

            // length = 18 without arena name
            String sbHeaderPrefix = ChatColor.GREEN + "PVP Arena" + ChatColor.RESET + " - " + ChatColor.YELLOW;
            String sbHeaderName = sbHeaderPrefix + this.getName();

            if (sbHeaderName.length() > 32) {
                if (this.prefix.length() <= 14) {
                    sbHeaderName = sbHeaderPrefix + this.prefix;
                } else {
                    sbHeaderName = sbHeaderName.substring(0, 32);
                }
            }

            if (this.scoreboard.getObjective("lives") != null) {
                this.scoreboard.getObjective("lives").unregister();
                if (this.scoreboard.getObjective(DisplaySlot.SIDEBAR) != null) {
                    this.scoreboard.getObjective(DisplaySlot.SIDEBAR).unregister();
                }
            }
            Objective obj = this.scoreboard.registerNewObjective("lives", "dummy", sbHeaderName); //deathCount

            if (this.isFightInProgress()) {
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        }
        return this.scoreboard;
    }

    private Scoreboard getStandardScoreboard() {
        if (this.scoreboard == null) {
            return this.getCommonScoreboard(false);
        }
        return this.scoreboard;
    }

    private Scoreboard getCommonScoreboard(boolean addTeamEntry) {
        if (this.scoreboard == null) {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            for (final ArenaTeam team : this.getTeams()) {
                final Team sbTeam = this.scoreboard.registerNewTeam(team.getName());
                sbTeam.setPrefix(team.getColor().toString());
                sbTeam.setSuffix(ChatColor.RESET.toString());
                sbTeam.setColor(team.getColor());
                sbTeam.setCanSeeFriendlyInvisibles(!this.isFreeForAll());
                sbTeam.setAllowFriendlyFire(this.getArenaConfig().getBoolean(CFG.PERMS_TEAMKILL));
                if (!this.getArenaConfig().getBoolean(CFG.PLAYER_COLLISION)) {
                    sbTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                }

                if(addTeamEntry) {
                    sbTeam.addEntry(team.getName());
                }
            }
        }
        return this.scoreboard;
    }

    public ArenaTeam getTeam(final String name) {
        for (final ArenaTeam team : teams) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    /**
     * hand over all teams
     *
     * @return the arena teams
     */
    public Set<ArenaTeam> getTeams() {
        return teams;
    }

    /**
     * hand over all teams
     *
     * @return the arena teams
     */
    public Set<String> getTeamNames() {
        final Set<String> result = new HashSet<>();
        for (final ArenaTeam team : teams) {
            result.add(team.getName());
        }
        return result;
    }

    /**
     * hand over all teams
     *
     * @return the arena teams
     */
    public Set<String> getTeamNamesColored() {
        final Set<String> result = new HashSet<>();
        for (final ArenaTeam team : teams) {
            result.add(team.getColoredName());
        }
        return result;
    }

    public String getWorld() {
        ArenaRegion ars = null;

        for (final ArenaRegion arss : getRegionsByType(RegionType.BATTLE)) {
            ars = arss;
            break;
        }

        if (ars != null) {
            return ars.getWorldName();
        }

        return Bukkit.getWorlds().get(0).getName();
    }

    /**
     * give customized rewards to players
     *
     * @param player the player to give the reward
     */
    public void giveRewards(final Player player) {
        if (gaveRewards) {
            return;
        }

        getDebugger().i("giving rewards to " + player.getName(), player);

        ArenaModuleManager.giveRewards(this, player);
        ItemStack[] items = cfg.getItems(CFG.ITEMS_REWARDS);

        final boolean isRandom = cfg.getBoolean(CFG.ITEMS_RANDOM);
        final Random rRandom = new Random();

        final PAWinEvent dEvent = new PAWinEvent(this, player, items);
        Bukkit.getPluginManager().callEvent(dEvent);
        items = dEvent.getItems();

        getDebugger().i("start " + startCount + " - minplayers: " + cfg.getInt(CFG.ITEMS_MINPLAYERS), player);

        if (items == null || items.length < 1
                || cfg.getInt(CFG.ITEMS_MINPLAYERS) > startCount) {
            return;
        }

        final int randomItem = rRandom.nextInt(items.length);

        for (int i = 0; i < items.length; ++i) {
            if (items[i] == null) {
                continue;
            }
            final ItemStack stack = items[i];
            if (stack == null) {
                PVPArena.instance.getLogger().warning(
                        "unrecognized item: " + items[i]);
                continue;
            }
            if (isRandom && i != randomItem) {
                continue;
            }
            try {
                player.getInventory().setItem(
                        player.getInventory().firstEmpty(), stack);
            } catch (final Exception e) {
                msg(player, Language.parse(this, MSG.ERROR_INVENTORY_FULL));
                return;
            }
        }
    }

    public void goalAdd(final ArenaGoal goal) {
        final ArenaGoal nugoal = (ArenaGoal) goal.clone();

        for (final ArenaGoal g : goals) {
            if (goal.getName().equals(g.getName())) {
                return;
            }
        }

        nugoal.setArena(this);

        goals.add(nugoal);
        updateGoals();
    }

    public void goalRemove(final ArenaGoal goal) {
        final ArenaGoal nugoal = (ArenaGoal) goal.clone();
        nugoal.setArena(this);

        goals.remove(nugoal);
        updateGoals();
    }

    public boolean goalToggle(final ArenaGoal goal) {
        final ArenaGoal nugoal = (ArenaGoal) goal.clone();
        nugoal.setArena(this);

        boolean contains = false;
        ArenaGoal removeGoal = nugoal;

        for (final ArenaGoal g : goals) {
            if (g.getName().equals(goal.getName())) {
                contains = true;
                removeGoal = g;
                break;
            }
        }

        if (contains) {
            goals.remove(removeGoal);
            updateGoals();
            return false;
        }
        goals.add(nugoal);
        updateGoals();
        return true;
    }

    public boolean hasEntity(final Entity entity) {
        return entities.containsValue(entity.getUniqueId());
    }

    /**
     * check if a custom class player is alive
     *
     * @return true if there is a custom class player alive, false otherwise
     *
     * @deprecated - checking this method is obsolete due to preventdrops and region checks
     */
    @Deprecated
    public boolean isCustomClassAlive() {
        return false;
    }

    public boolean hasAlreadyPlayed(final String playerName) {
        return playedPlayers.contains(playerName);
    }

    public void hasNotPlayed(final ArenaPlayer player) {
        if (cfg.getBoolean(CFG.JOIN_ONLYIFHASPLAYED)) {
            return;
        }
        playedPlayers.remove(player.getName());
    }

    public boolean hasPlayer(final Player player) {
        for (final ArenaTeam team : teams) {
            if (team.hasPlayer(player)) {
                return true;
            }
        }
        return equals(ArenaPlayer.parsePlayer(player.getName()).getArena());
    }

    public void increasePlayerCount() {
        startCount++;
    }

    public boolean isFightInProgress() {
        return fightInProgress;
    }

    public boolean isFreeForAll() {
        return this.free;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isValid() {
        return valid;
    }

    public void markPlayedPlayer(final String playerName) {
        playedPlayers.add(playerName);
    }

    public void modAdd(final ArenaModule mod) {
        mods.add(mod);
        updateMods();
    }

    public void modRemove(final ArenaModule mod) {
        mods.remove(mod);
        updateMods();
    }

    public void msg(final CommandSender sender, final String[] msg) {
        for (final String string : msg) {
            msg(sender, string);
        }
    }

    public void msg(final CommandSender sender, final String msg) {
        if (sender == null || msg == null || msg.length() < 1 ||
                " ".equals(msg)) {
            return;
        }
        getDebugger().i('@' + sender.getName() + ": " + msg);

        sender.sendMessage(Language.parse(this, MSG.MESSAGES_GENERAL, prefix, msg));
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
            return Language.parse(this, MSG.DEATHCAUSE_CUSTOM);
        }

        getDebugger().i("return a damage name for : " + cause.toString(), player);

        getDebugger().i("damager: " + damager, player);

        ArenaPlayer aPlayer = null;
        ArenaTeam team = null;
        if (damager instanceof Player) {
            aPlayer = ArenaPlayer.parsePlayer(((Player) damager).getName());
            team = aPlayer.getArenaTeam();
        }

        final EntityDamageEvent lastDamageCause = player.getLastDamageCause();

        switch (cause) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                if (damager instanceof Player && team != null) {
                    return team.colorizePlayer(aPlayer.get()) + ChatColor.YELLOW;
                }

                try {
                    getDebugger().i("last damager: "
                            + ((EntityDamageByEntityEvent) lastDamageCause)
                            .getDamager().getType(), player);
                    return Language.parse(this, MSG.getByName("DEATHCAUSE_"
                            + ((EntityDamageByEntityEvent) lastDamageCause)
                            .getDamager().getType().name()));
                } catch (final Exception e) {

                    return Language.parse(this, MSG.DEATHCAUSE_CUSTOM);
                }
            case ENTITY_EXPLOSION:
                try {
                    getDebugger().i("last damager: "
                            + ((EntityDamageByEntityEvent) lastDamageCause)
                            .getDamager().getType(), player);
                    return Language.parse(this, MSG.getByName("DEATHCAUSE_"
                            + ((EntityDamageByEntityEvent) lastDamageCause)
                            .getDamager().getType().name()));
                } catch (final Exception e) {

                    return Language.parse(this, MSG.DEATHCAUSE_ENTITY_EXPLOSION);
                }
            case PROJECTILE:
                if (damager instanceof Player && team != null) {
                    return team.colorizePlayer(aPlayer.get()) + ChatColor.YELLOW;
                }
                try {

                    final ProjectileSource source = ((Projectile) ((EntityDamageByEntityEvent) lastDamageCause)
                            .getDamager()).getShooter();

                    final LivingEntity lEntity = (LivingEntity) source;

                    getDebugger().i("last damager: "
                            + lEntity.getType(), player);

                    return Language
                            .parse(this, MSG
                                    .getByName("DEATHCAUSE_"
                                            + lEntity.getType().name()));
                } catch (final Exception e) {

                    return Language.parse(this, MSG.DEATHCAUSE_PROJECTILE);
                }
            default:
                break;
        }
        MSG string = MSG.getByName("DEATHCAUSE_" + cause.toString());
        if (string == null) {
            PVPArena.instance.getLogger().warning("Unknown cause: " + cause.toString());
            string = MSG.DEATHCAUSE_VOID;
        }
        return Language.parse(this, string);
    }

    public static void pmsg(final CommandSender sender, final String msg) {
        if (sender == null || msg == null || msg.length() < 1 ||
                " ".equals(msg)) {
            return;
        }
        DEBUG.i('@' + sender.getName() + ": " + msg, sender);
        sender.sendMessage(Language.parse(MSG.MESSAGES_GENERAL, PVPArena.instance.getConfig().getString("globalPrefix", "PVP Arena"), msg));
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
        for (final ArenaGoal goal : getGoals()) {
            goal.parseLeave(player);
        }

        if (!fightInProgress) {
            startCount--;
            playedPlayers.remove(player.getName());
        }
        getDebugger().i("fully removing player from arena", player);
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        if (!silent) {

            final ArenaTeam team = aPlayer.getArenaTeam();
            if (team == null) {

                broadcastExcept(
                        player,
                        Language.parse(this, MSG.FIGHT_PLAYER_LEFT, player.getName()
                                + ChatColor.YELLOW));
            } else {
                ArenaModuleManager.parsePlayerLeave(this, player, team);

                broadcastExcept(
                        player,
                        Language.parse(this, MSG.FIGHT_PLAYER_LEFT,
                                team.colorizePlayer(player) + ChatColor.YELLOW));
            }
            msg(player, Language.parse(this, MSG.NOTICE_YOU_LEFT));
        }

        removePlayer(player, cfg.getString(location), soft, force);

        if (!cfg.getBoolean(CFG.READY_ENFORCECOUNTDOWN) && startRunner != null && cfg.getInt(CFG.READY_MINPLAYERS) > 0 &&
                getFighters().size() <= cfg.getInt(CFG.READY_MINPLAYERS)) {
            startRunner.cancel();
            broadcast(Language.parse(this, MSG.TIMER_COUNTDOWN_INTERRUPTED));
            startRunner = null;
        }

        if (fightInProgress) {
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
        getDebugger().i("ready check !!");

        final int players = TeamManager.countPlayersInTeams(this);
        if (players < 2) {
            return Language.parse(this, MSG.ERROR_READY_1_ALONE);
        }
        if (players < cfg.getInt(CFG.READY_MINPLAYERS)) {
            return Language.parse(this, MSG.ERROR_READY_4_MISSING_PLAYERS);
        }

        if (cfg.getBoolean(CFG.READY_CHECKEACHPLAYER)) {
            for (final ArenaTeam team : teams) {
                for (final ArenaPlayer ap : team.getTeamMembers()) {
                    if (ap.getStatus() != Status.READY) {
                        return Language
                                .parse(this, MSG.ERROR_READY_0_ONE_PLAYER_NOT_READY);
                    }
                }
            }
        }

        if (!free) {
            final Set<String> activeTeams = new HashSet<>();

            for (final ArenaTeam team : teams) {
                for (final ArenaPlayer ap : team.getTeamMembers()) {
                    if (!cfg.getBoolean(CFG.READY_CHECKEACHTEAM)
                            || ap.getStatus() == Status.READY) {
                        activeTeams.add(team.getName());
                        break;
                    }
                }
            }

            if (cfg.getBoolean(CFG.USES_EVENTEAMS)
                    && !TeamManager.checkEven(this)) {
                return Language.parse(this, MSG.NOTICE_WAITING_EQUAL);
            }

            if (activeTeams.size() < 2) {
                return Language.parse(this, MSG.ERROR_READY_2_TEAM_ALONE);
            }
        }

        final String error = PVPArena.instance.getAgm().ready(this);
        if (error != null) {
            return error;
        }

        for (final ArenaTeam team : teams) {
            for (final ArenaPlayer p : team.getTeamMembers()) {
                if (p.get() == null) {
                    continue;
                }
                getDebugger().i("checking class: " + p.get().getName(), p.get());

                if (p.getArenaClass() == null) {
                    getDebugger().i("player has no class", p.get());


                    final String autoClass =
                            cfg.getBoolean(CFG.USES_PLAYERCLASSES) ?
                                    getClass(p.getName()) != null ? p.getName() : cfg.getString(CFG.READY_AUTOCLASS)
                                    : cfg.getString(CFG.READY_AUTOCLASS);
                    final ArenaClass aClass = getClass(autoClass);

                    if (aClass != null) {
                        selectClass(p, aClass.getName());
                    } else {
                        // player no class!
                        PVPArena.instance.getLogger().warning("Player no class: " + p.get());
                        return Language
                                .parse(this, MSG.ERROR_READY_5_ONE_PLAYER_NO_CLASS);
                    }
                }
            }
        }
        final int readyPlayers = countReadyPlayers();

        if (players > readyPlayers) {
            final double ratio = cfg.getDouble(CFG.READY_NEEDEDRATIO);
            getDebugger().i("ratio: " + ratio);
            if (ratio > 0) {
                final double aRatio = ((double) readyPlayers)
                        / players;
                if (players > 0 && aRatio >= ratio) {
                    return "";
                }
            }
            return Language.parse(this, MSG.ERROR_READY_0_ONE_PLAYER_NOT_READY);
        }
        return cfg.getBoolean(CFG.READY_ENFORCECOUNTDOWN) ? "" : null;
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
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        final PALeaveEvent event = new PALeaveEvent(this, player, aPlayer.getStatus() == Status.FIGHT);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void removeClass(final String string) {
        for (final ArenaClass ac : classes) {
            if (ac.getName().equals(string)) {
                classes.remove(ac);
                return;
            }
        }
    }

    public void removeEntity(final Entity entity) {
        for (final Map.Entry<Player, UUID> playerUUIDEntry : entities.entrySet()) {
            if (playerUUIDEntry.getValue().equals(entity.getUniqueId())) {
                entities.remove(playerUUIDEntry.getKey());
                return;
            }
        }
    }

    public void removeOffset(final String spawnName) {
        final List<String> offsets = getArenaConfig().getStringList(CFG.TP_OFFSETS.getNode(), new ArrayList<String>());
        final List<String> removals = new ArrayList<>();
        for (String value : offsets) {
            if (value != null && value.contains(":")) {
                String[] split = value.split(":");
                if (spawnName.equals(split[0])) {
                    removals.add(value);
                }
            }
        }
        for (String rem : removals) {
            offsets.remove(rem);
        }
        getArenaConfig().setManually(CFG.TP_OFFSETS.getNode(), offsets);
        getArenaConfig().save();
    }

    /**
     * remove a player from the arena
     *
     * @param player the player to reset
     * @param tploc  the coord string to teleport the player to
     */
    public void removePlayer(final Player player, final String tploc, final boolean soft,
                             final boolean force) {
        getDebugger().i("removing player " + player.getName() + (soft ? " (soft)" : "")
                + ", tp to " + tploc, player);
        resetPlayer(player, tploc, soft, force);

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        if (!soft && aPlayer.getArenaTeam() != null) {
            aPlayer.getArenaTeam().remove(aPlayer);
        }

        callExitEvent(player);
        if (cfg.getBoolean(CFG.USES_CLASSSIGNSDISPLAY)) {
            PAClassSign.remove(signs, player);
        }

//		if (getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE)) {
//			aPlayer.setArena(null);
//		}

        player.setNoDamageTicks(60);
    }

    public void renewDebugger() {
        debug = null;
        debug = new Debug(this);
        DEBUG = null;
        DEBUG = new Debug(3);
    }

    /**
     * reset an arena
     *
     * @param force enforce it
     */
    public void resetPlayers(final boolean force) {
        getDebugger().i("resetting player manager");
        final Set<ArenaPlayer> players = new HashSet<>();
        for (final ArenaTeam team : teams) {
            for (final ArenaPlayer p : team.getTeamMembers()) {
                getDebugger().i("player: " + p.getName(), p.get());
                if (p.getArena() == null || !p.getArena().equals(this)) {
                    /*
					if (p.getArenaTeam() != null) {
						p.getArenaTeam().remove(p);
						getDebugger().info("> removed", p.get());
					}*/
                    getDebugger().i("> skipped", p.get());
                } else {
                    getDebugger().i("> added", p.get());
                    players.add(p);
                }
            }
        }

        // pre-parsing for "whole team winning"
        for (final ArenaPlayer p : players) {
            if (p.getStatus() != null && p.getStatus() == Status.FIGHT) {
                final Player player = p.get();
                if (player == null) {
                    continue;
                }
                if (!force && p.getStatus() == Status.FIGHT
                        && fightInProgress && !gaveRewards && !free && cfg.getBoolean(CFG.USES_TEAMREWARDS)) {
                    players.removeAll(p.getArenaTeam().getTeamMembers());
                    giveRewardsLater(p.getArenaTeam()); // this removes the players from the arena
                    break;
                }
            }
        }

        for (final ArenaPlayer p : players) {

            p.debugPrint();
            if (p.getStatus() != null && p.getStatus() == Status.FIGHT) {
                // TODO enhance wannabe-smart exploit fix for people that
                // spam join and leave the arena to make one of them win
                final Player player = p.get();
                if (player == null) {
                    continue;
                }
                if (!force) {
                    p.addWins();
                }
                callExitEvent(player);
                resetPlayer(player, cfg.getString(CFG.TP_WIN, "old"),
                        false, force);
                if (!force && p.getStatus() == Status.FIGHT
                        && fightInProgress && !gaveRewards) {
                    // if we are remaining, give reward!
                    giveRewards(player);
                }
            } else if (p.getStatus() != null
                    && (p.getStatus() == Status.DEAD || p.getStatus() == Status.LOST)) {

                final PALoseEvent loseEvent = new PALoseEvent(this, p.get());
                Bukkit.getPluginManager().callEvent(loseEvent);

                final Player player = p.get();
                if (!force) {
                    p.addLosses();
                }
                callExitEvent(player);
                resetPlayer(player, cfg.getString(CFG.TP_LOSE, "old"),
                        false, force);
            } else {
                callExitEvent(p.get());
                resetPlayer(p.get(),
                        cfg.getString(CFG.TP_LOSE, "old"), false,
                        force);
            }

            p.reset();
        }
        for (final ArenaPlayer player : ArenaPlayer.getAllArenaPlayers()) {
            if (equals(player.getArena()) && player.getStatus() == Status.WATCH) {

                callExitEvent(player.get());
                resetPlayer(player.get(),
                        cfg.getString(CFG.TP_EXIT, "old"), false,
                        force);
                player.setArena(null);
                player.reset();
            }
        }
    }

    private void resetScoreboard(final Player player, final boolean force, final boolean soft) {
        if (this.getArenaConfig().getBoolean(CFG.USES_SCOREBOARD)) {
            this.getDebugger().i("ScoreBoards: "+(soft?"(soft) ":"")+"remove: " + player.getName(), player);
            try {
                if (this.scoreboard != null) {
                    for (final Team team : this.scoreboard.getTeams()) {
                        if (team.hasEntry(player.getName())) {
                            team.removeEntry(player.getName());
                            if (soft) {
                                this.updateScoreboards();
                                return;
                            }
                            this.scoreboard.resetScores(player.getName());
                        }
                    }
                } else {
                    this.getDebugger().i("ScoreBoards: scoreboard is null!");
                    return;
                }

                final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
                if (ap.hasBackupScoreboard()) {
                    this.getDebugger().i("ScoreBoards: restoring " + ap.get());

                    class RunLater extends BukkitRunnable {
                        @Override
                        public void run() {
                            Scoreboard backupScoreboard = ap.getBackupScoreboard();
                            if (ap.getBackupScoreboardTeam() != null && !force) {
                                backupScoreboard.getTeam(ap.getBackupScoreboardTeam()).addEntry(ap.getName());
                            }
                            player.setScoreboard(backupScoreboard);
                            ap.setBackupScoreboardTeam(null);
                            ap.setBackupScoreboard(null);
                        }
                    }

                    if (force) {
                        new RunLater().run();
                    } else {
                        try {
                            new RunLater().runTaskLater(PVPArena.instance, 2L);
                        } catch (IllegalStateException ignored) {

                        }
                    }
                }

            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            Team team = this.getStandardScoreboard().getEntryTeam(player.getName());
            if (team != null) {
                team.removeEntry(player.getName());
                if (soft) {
                    return;
                }
            }
            final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
            if (ap.hasBackupScoreboard()) {
                try {
                    Bukkit.getScheduler().runTaskLater(PVPArena.instance, () -> {
                        Scoreboard backupScoreboard = ap.getBackupScoreboard();
                        if (ap.getBackupScoreboardTeam() != null) {
                            backupScoreboard.getTeam(ap.getBackupScoreboardTeam()).addEntry(ap.getName());
                        }
                        player.setScoreboard(backupScoreboard);
                        ap.setBackupScoreboardTeam(null);
                    }, 3L);
                } catch (IllegalPluginAccessException ignored) {

                }
            }
        }
    }

    private void giveRewardsLater(final ArenaTeam arenaTeam) {
        debug.i("Giving rewards to the whole team!");
        if (arenaTeam == null) {
            debug.i("team is null");
            return; // this one failed. try next time...
        }

        final Set<ArenaPlayer> players = new HashSet<>();
        players.addAll(arenaTeam.getTeamMembers());

        for (final ArenaPlayer ap : players) {
            ap.addWins();
            callExitEvent(ap.get());
            resetPlayer(ap.get(), cfg.getString(CFG.TP_WIN, "old"),
                    false, false);
            ap.reset();
        }
        debug.i("Giving rewards to team " + arenaTeam.getName() + '!');

        Bukkit.getScheduler().runTaskLater(PVPArena.instance, new Runnable(){
            @Override
            public void run() {
                for (final ArenaPlayer ap : players) {
                    debug.i("Giving rewards to " + ap.get().getName() + '!');
                    try {
                        giveRewards(ap.get());
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
                gaveRewards = true;
            }
        }, 1L);

    }

    /**
     * reset an arena
     */
    public void reset(final boolean force) {

        final PAEndEvent event = new PAEndEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        getDebugger().i("resetting arena; force: " + force);
        for (final PAClassSign as : signs) {
            as.clear();
        }
        signs.clear();
        playedPlayers.clear();
        resetPlayers(force);
        setFightInProgress(false);

        if (endRunner != null) {
            endRunner.cancel();
        }
        endRunner = null;
        if (realEndRunner != null) {
            realEndRunner.cancel();
        }
        realEndRunner = null;
        if (pvpRunner != null) {
            pvpRunner.cancel();
        }
        pvpRunner = null;

        ArenaModuleManager.reset(this, force);
        ArenaManager.advance(Arena.this);
        clearRegions();
        PVPArena.instance.getAgm().reset(this, force);

        round = 0;
        StatisticsManager.save();

        try {
            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.instance, new Runnable() {
                @Override
                public void run() {
                    playedPlayers.clear();
                    startCount = 0;
                }
            }, 30L);
        } catch (final Exception e) {
            // maybe shutting down?
        }
        scoreboard = null;
    }

    public boolean removeCustomScoreBoardEntry(final ArenaModule module, final int value) {
        debug.i("module "+module+" tries to unset custom scoreboard value '"+value+"'");
        if (scoreboard == null) {
            debug.i("scoreboard is not setup!");
            return false;
        }
        try {
            Team mTeam = null;

            for (Team team : scoreboard.getTeams()) {
                if (team.getName().equals("pa_msg_"+value)) {
                    mTeam = team;
                }
            }

            if (mTeam == null) {
                return true;
            }

            for (String entry : scoreboard.getEntries()) {
                if (scoreboard.getObjective("lives").getScore(entry).getScore() == value) {
                    scoreboard.getObjective("lives").getScore(entry).setScore(0);
                    scoreboard.resetScores(entry);
                    mTeam.removeEntry(entry);
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * reset a player to his pre-join values
     *
     * @param player the player to reset
     * @param destination the teleport location
     * @param soft   if location should be preserved (another tp incoming)
     */
    private void resetPlayer(final Player player, final String destination, final boolean soft,
                             final boolean force) {
        if (player == null) {
            return;
        }
        getDebugger().i("resetting player: " + player.getName() + (soft ? "(soft)" : ""),
                player);

        try {
            new ArrowHack(player);
        } catch (final Exception e) {
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        if (aPlayer.getState() != null) {
            aPlayer.getState().unload(soft);
        }
        this.resetScoreboard(player, force, soft);

        //noinspection deprecation
        ArenaModuleManager.resetPlayer(this, player, soft, force);

        if (!soft && (!aPlayer.hasCustomClass() || cfg.getBoolean(CFG.GENERAL_CUSTOMRETURNSGEAR))) {
            ArenaPlayer.reloadInventory(this, player, true);
        }

        this.teleportPlayerAfterReset(destination, soft, force, aPlayer);
    }

    private void teleportPlayerAfterReset(final String destination, final boolean soft, final boolean force, final ArenaPlayer aPlayer) {
        final Player player = aPlayer.get();
        class RunLater implements Runnable {

            @Override
            public void run() {
                getDebugger().i("string = " + destination, player);
                aPlayer.setTelePass(true);

                if ("old".equalsIgnoreCase(destination)) {
                    getDebugger().i("tping to old", player);
                    if (aPlayer.getSavedLocation() != null) {
                        getDebugger().i("location is fine", player);
                        final PALocation loc = aPlayer.getSavedLocation();
                        player.teleport(loc.toLocation());
                        player
                                .setNoDamageTicks(
                                        getArenaConfig().getInt(
                                                CFG.TIME_TELEPORTPROTECT) * 20);
                        aPlayer.setTeleporting(false);
                    }
                } else {
                    Location offset = getOffset(destination);
                    if (offset == null) {
                        offset = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                    }
                    final PALocation loc = SpawnManager.getSpawnByExactName(Arena.this, destination);
                    if (loc == null) {
                        new Exception("RESET Spawn null: " + getName() + "->" + destination).printStackTrace();
                    } else {
                        player.teleport(loc.toLocation().add(offset.toVector()));
                        aPlayer.setTelePass(false);
                        aPlayer.setTeleporting(false);
                    }
                    player.setNoDamageTicks(
                            getArenaConfig().getInt(
                                    CFG.TIME_TELEPORTPROTECT) * 20);
                }
                if (soft || !force) {
                    StatisticsManager.update(Arena.this, aPlayer);
                }
                if (!soft) {
                    aPlayer.setLocation(null);
                    aPlayer.clearFlyState();
                }
            }
        }

        final RunLater runLater = new RunLater();

        aPlayer.setTeleporting(true);
        if (cfg.getInt(CFG.TIME_RESETDELAY) > 0 && !force) {
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, runLater, cfg.getInt(CFG.TIME_RESETDELAY) * 20);
        } else if (PVPArena.instance.isShuttingDown()) {
            runLater.run();
        } else {
            // Waiting two ticks in order to avoid player death bug
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, runLater, 2);
        }
    }

    public void setupScoreboard(final ArenaPlayer ap) {
        Player player = ap.get();

        this.getDebugger().i("ScoreBoards: Initiating scoreboard for player " + player.getName());
        this.getDebugger().i("ScoreBoards: has backup: " + ap.hasBackupScoreboard());
        this.getDebugger().i("ScoreBoards: player.getScoreboard == null: " + (player.getScoreboard() == null));
        if (!ap.hasBackupScoreboard() && player.getScoreboard() != null) {
            ap.setBackupScoreboard(player.getScoreboard());
            ofNullable(player.getScoreboard().getEntryTeam(ap.getName())).ifPresent(team ->
                    ap.setBackupScoreboardTeam(team.getName())
            );
        }

        if (this.getArenaConfig().getBoolean(CFG.USES_SCOREBOARD)) {
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, () -> {
                final Scoreboard board = this.getSpecialScoreboard();

                Optional<Team> optBoardTeam = ofNullable(ap.getArenaTeam()).map(team -> board.getTeam(team.getName()));
                optBoardTeam.ifPresent(boardTeam -> boardTeam.addEntry(player.getName()));

                this.updateScoreboard(player);

                if (this.getArenaConfig().getBoolean(CFG.USES_SCOREBOARDROUNDDISPLAY)) {
                    this.addCustomScoreBoardEntry(null, Language.parse(MSG.ROUNDS_DISPLAY,
                            String.valueOf(this.getRound()),
                            String.valueOf(this.getRoundCount())),  199);
                    this.addCustomScoreBoardEntry(null, Language.parse(MSG.ROUNDS_DISPLAYSEPARATOR), 198);
                }
            }, 1L);
        } else {
            final Scoreboard board = this.getStandardScoreboard();

            player.setScoreboard(board);
            Optional<Team> optBoardTeam = ofNullable(ap.getArenaTeam()).map(team -> board.getTeam(team.getName()));
            optBoardTeam.ifPresent(boardTeam -> boardTeam.addEntry(player.getName()));
        }
    }

    /**
     * reset player variables
     *
     * @param player the player to access
     */
    public void unKillPlayer(final Player player, final DamageCause cause, final Entity damager) {

        getDebugger().i("respawning player " + player.getName(), player);
        double iHealth = cfg.getInt(CFG.PLAYER_HEALTH, -1);

        if (iHealth < 1) {
            iHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        }

        PlayerState.playersetHealth(player, iHealth);
        player.setFoodLevel(cfg.getInt(CFG.PLAYER_FOODLEVEL, 20));
        player.setSaturation(cfg.getInt(CFG.PLAYER_SATURATION, 20));
        player.setExhaustion((float) cfg.getDouble(CFG.PLAYER_EXHAUSTION, 0.0));
        player.setVelocity(new Vector());
        player.setFallDistance(0);

        if (cfg.getBoolean(CFG.PLAYER_DROPSEXP)) {
            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0);
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        final ArenaTeam team = aPlayer.getArenaTeam();

        if (team == null) {
            return;
        }

        PlayerState.removeEffects(player);

        if (aPlayer.getNextArenaClass() != null) {
            InventoryManager.clearInventory(aPlayer.get());
            aPlayer.setArenaClass(aPlayer.getNextArenaClass());
            if (aPlayer.getArenaClass() != null) {
                ArenaPlayer.givePlayerFightItems(this, aPlayer.get());
                aPlayer.setMayDropInventory(true);
            }
            aPlayer.setNextArenaClass(null);
        }

        ArenaModuleManager.parseRespawn(this, player, team, cause, damager);
        player.setFireTicks(0);
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, new Runnable() {
                @Override
                public void run() {
                    if (player.getFireTicks() > 0) {
                        player.setFireTicks(0);
                    }
                }
            }, 5L);
        } catch (Exception e) {
        }
        player.setNoDamageTicks(cfg.getInt(CFG.TIME_TELEPORTPROTECT) * 20);
    }

    public void selectClass(final ArenaPlayer aPlayer, final String cName) {
        if (ArenaModuleManager.cannotSelectClass(this, aPlayer.get(), cName)) {
            return;
        }
        for (final ArenaClass c : classes) {
            if (c.getName().equalsIgnoreCase(cName)) {
                aPlayer.setArenaClass(c);
                if (aPlayer.getArenaClass() != null) {
                    aPlayer.setArena(this);
                    aPlayer.createState(aPlayer.get());
                    InventoryManager.clearInventory(aPlayer.get());
                    c.equip(aPlayer.get());
                    msg(aPlayer.get(), Language.parse(this, MSG.CLASS_PREVIEW, c.getName()));
                }
                return;
            }
        }
        msg(aPlayer.get(), Language.parse(this, MSG.ERROR_CLASS_NOT_FOUND, cName));
    }

    public void setArenaConfig(final Config cfg) {
        this.cfg = cfg;
    }

    public void setFightInProgress(final boolean fightInProgress) {
        this.fightInProgress = fightInProgress;
        getDebugger().i("fighting : " + fightInProgress);
    }

    public void setFree(final boolean isFree) {
        free = isFree;
        if (free && cfg.getUnsafe("teams.free") == null) {
            teams.clear();
            teams.add(new ArenaTeam("free", "WHITE"));
        } else if (free) {
            teams.clear();
            teams.add(new ArenaTeam("free", (String) cfg
                    .getUnsafe("teams.free")));
        }
        cfg.set(CFG.GENERAL_TYPE, isFree ? "free" : "none");
        cfg.save();
    }

    public void setOffset(final String spawnName, final double x, final double y, final double z) {
        final List<String> offsets = getArenaConfig().getStringList(CFG.TP_OFFSETS.getNode(), new ArrayList<String>());

        offsets.add(spawnName + ':' +
                String.format("%.1f", x)+ ";" +
                String.format("%.1f", y)+ ";" +
                String.format("%.1f", z));

        getArenaConfig().setManually(CFG.TP_OFFSETS.getNode(), offsets);
        getArenaConfig().save();
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    /**
     * damage every actively fighting player for being near a spawn
     */
    public void spawnCampPunish() {

        final Map<Location, ArenaPlayer> players = new HashMap<>();

        for (final ArenaPlayer ap : getFighters()) {
            if (ap.getStatus() != Status.FIGHT) {
                continue;
            }
            players.put(ap.get().getLocation(), ap);
        }

        for (final ArenaTeam team : teams) {
            if (team.getTeamMembers().size() < 1) {
                continue;
            }
            final String sTeam = team.getName();
            final Set<PALocation> spawns;


            if (cfg.getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                spawns = SpawnManager.getSpawnsContaining(this, "spawn");
            } else {
                spawns = SpawnManager.getSpawnsStartingWith(this, this.free ?"spawn":sTeam + "spawn");
            }


            for (final PALocation spawnLoc : spawns) {
                for (final Map.Entry<Location, ArenaPlayer> locationArenaPlayerEntry : players.entrySet()) {
                    if (spawnLoc.getDistanceSquared(new PALocation(locationArenaPlayerEntry.getKey())) < 9) {
                        locationArenaPlayerEntry.getValue()
                                .get()
                                .setLastDamageCause(
                                        new EntityDamageEvent(locationArenaPlayerEntry.getValue().get(),
                                                DamageCause.CUSTOM,
                                                1002));
                        locationArenaPlayerEntry.getValue()
                                .get()
                                .damage(cfg.getInt(
                                        CFG.DAMAGE_SPAWNCAMP));
                    }
                }
            }
        }
    }

    public void spawnSet(final String node, final PALocation paLocation) {
        final String string = Config.parseToString(paLocation);

        // the following conversion is needed because otherwise the arena will add
        // too much offset until the next restart, where the location is loaded based
        // on the BLOCK position of the given location plus the player orientation
        final PALocation location = Config.parseLocation(string);

        cfg.setManually("spawns." + node, string);
        cfg.save();
        addSpawn(new PASpawn(location, node));
    }

    public void spawnUnset(final String node) {
        cfg.setManually("spawns." + node, null);
        cfg.save();
    }

    public void start() {
        start(false);
    }

    /**
     * initiate the arena start
     */
    public void start(final boolean forceStart) {
        getDebugger().i("start()");
        if (getArenaConfig().getBoolean(CFG.USES_SCOREBOARD) && scoreboard != null) {
            Objective obj = scoreboard.getObjective("lives");
            if (this.isFightInProgress()) {
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        }
        gaveRewards = false;
        startRunner = null;
        if (fightInProgress) {
            getDebugger().i("already in progress! OUT!");
            return;
        }
        int sum = 0;
        for (final ArenaTeam team : teams) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (forceStart) {
                    ap.setStatus(Status.READY);
                }
                if (ap.getStatus() == Status.LOUNGE
                        || ap.getStatus() == Status.READY) {
                    sum++;
                }
            }
        }
        getDebugger().i("sum == " + sum);
        final String errror = ready();

        boolean overRide = false;

        if (forceStart) {
            overRide = errror == null ||
                    errror.contains(Language.parse(MSG.ERROR_READY_1_ALONE)) ||
                    errror.contains(Language.parse(MSG.ERROR_READY_2_TEAM_ALONE)) ||
                    errror.contains(Language.parse(MSG.ERROR_READY_3_TEAM_MISSING_PLAYERS)) ||
                    errror.contains(Language.parse(MSG.ERROR_READY_4_MISSING_PLAYERS));
        }

        if (overRide || errror == null || errror.isEmpty()) {
            final Boolean handle = PACheck.handleStart(this, null, forceStart);

            if (overRide || handle) {
                getDebugger().i("START!");
                setFightInProgress(true);

                if (getArenaConfig().getBoolean(CFG.USES_SCOREBOARD)) {
                    Objective obj = getSpecialScoreboard().getObjective("lives");
                    obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                }

            } else if (handle) {
                if (errror != null) {
                    PVPArena.instance.getLogger().info(errror);
                }
				/*
				for (ArenaPlayer ap : getFighters()) {
					getDebugger().i("removing player " + ap.getName());
					playerLeave(ap.get(), CFG.TP_EXIT, false);
				}
				reset(false);*/
            } else {

                // false
                PVPArena.instance.getLogger().info("START aborted by event cancel");
                //reset(true);
            }
        } else {
            // false
            broadcast(Language.parse(MSG.ERROR_ERROR, errror));
            //reset(true);
        }
    }

    public void stop(final boolean force) {
        for (final ArenaPlayer p : getFighters()) {
            playerLeave(p.get(), CFG.TP_EXIT, true, force, false);
        }
        reset(force);
    }

    /**
     * send a message to every player of a given team
     *
     * @param sTeam the team to send to
     * @param msg    the message to send
     * @param color  the color to use
     * @param player the player to prefix
     */
    public void tellTeam(final String sTeam, final String msg, final ChatColor color,
                         final Player player) {
        final ArenaTeam team = getTeam(sTeam);
        if (team == null) {
            return;
        }
        getDebugger().i('@' + team.getName() + ": " + msg, player);
        synchronized (this) {
            for (final ArenaPlayer p : team.getTeamMembers()) {
                final String reset = cfg.getBoolean(CFG.CHAT_COLORNICK)?"":ChatColor.RESET.toString();
                if (player == null) {
                    p.get().sendMessage(
                            color + "[" + team.getName() + ']' + ChatColor.RESET
                                    + ": " + msg);
                } else {
                    p.get().sendMessage(
                            color + "[" + team.getName() + "] " + reset + player.getName() + ChatColor.RESET
                                    + ": " + msg);
                }
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public void tpPlayerToCoordName(ArenaPlayer player, String place) {
        Location destination = this.prepareTeleportation(player, place);
        this.teleportPlayer(place, player, destination);
        this.execPostTeleportationFixes(player);
    }

    /**
     * teleport a given player to the given coord string
     *
     * @param player the player to teleport
     * @param place  the coord string
     */
    public void tpPlayerToCoordNameForJoin(final ArenaPlayer player, final String place, boolean async) {
        Location destination = this.prepareTeleportation(player, place);
        int delay = async ? 2 : 0;
        Bukkit.getScheduler().runTaskLater(PVPArena.instance, () -> {
            teleportPlayer(place, player, destination);
            setupScoreboard(player);
        }, delay);
        this.execPostTeleportationFixes(player);
    }

    private Location prepareTeleportation(ArenaPlayer aPlayer, String place) {
        Player player = aPlayer.get();
        getDebugger().i("teleporting " + player + " to coord " + place, player);

        if (player == null) {
            PVPArena.instance.getLogger().severe("Player null!");
            throw new RuntimeException("Player null!");
        }

        if (player.isInsideVehicle()) {
            player.getVehicle().eject();
        }

        ArenaModuleManager.tpPlayerToCoordName(this, player, place);

        if ("spectator".equals(place)) {
            if (getFighters().contains(aPlayer)) {
                aPlayer.setStatus(Status.LOST);
            } else {
                aPlayer.setStatus(Status.WATCH);
            }
        }
        PALocation loc = SpawnManager.getSpawnByExactName(this, place);
        if ("old".equals(place)) {
            loc = aPlayer.getSavedLocation();
        }
        if (loc == null) {
            throw new RuntimeException("TP Spawn null: " + name + "->" + place);
        }

        debug.i("raw location: " + loc.toString());

        Location offset = this.getOffset(place);
        if (offset == null) {
            offset = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
        }
        debug.i("offset location: " + offset.toString());

        aPlayer.setTeleporting(true);
        aPlayer.setTelePass(true);
        return loc.toLocation().add(offset.getX(), offset.getY(), offset.getZ());
    }

    private void execPostTeleportationFixes(ArenaPlayer aPlayer) {
        if (cfg.getBoolean(CFG.PLAYER_REMOVEARROWS)) {
            try {
                new ArrowHack(aPlayer.get());
            } catch (final Exception e) {
            }
        }

        if (cfg.getBoolean(CFG.USES_INVISIBILITYFIX) &&
                aPlayer.getStatus() == Status.FIGHT ||
                aPlayer.getStatus() == Status.LOUNGE) {

            Bukkit.getScheduler().runTaskLater(PVPArena.instance, new Runnable() {
                @Override
                public void run() {
                    for (final ArenaPlayer player : getFighters()) {
                        if (player.get() != null) {
                            player.get().showPlayer(PVPArena.instance, aPlayer.get());
                        }
                    }
                }
            }, 5L);
        }

        if (!cfg.getBoolean(CFG.PERMS_FLY)) {
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, new Runnable() {
                @Override
                public void run() {
                    aPlayer.get().setAllowFlight(false);
                    aPlayer.get().setFlying(false);
                }
            }, 5L);
        }
    }

    private void teleportPlayer(String place, final ArenaPlayer aPlayer, Location location) {
        Player player = aPlayer.get();
        player.teleport(location);
        player.setNoDamageTicks(cfg.getInt(CFG.TIME_TELEPORTPROTECT) * 20);
        if (place.contains("lounge")) {
            getDebugger().i("setting TelePass later!");
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, new Runnable() {
                @Override
                public void run() {
                    aPlayer.setTelePass(false);
                    aPlayer.setTeleporting(false);
                }
            }, cfg.getInt(CFG.TIME_TELEPORTPROTECT) * 20);

        } else {
            getDebugger().i("setting TelePass now!");
            aPlayer.setTelePass(false);
            aPlayer.setTeleporting(false);
        }
    }

    /**
     * last resort to put a player into an arena (when no goal/module wants to)
     *
     * @param player the player to put
     * @param team   the arena team to put into
     * @return true if joining successful
     */
    public boolean tryJoin(final Player player, final ArenaTeam team) {
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        getDebugger().i("trying to join player " + player.getName(), player);

        final String clear = cfg.getString(CFG.PLAYER_CLEARINVENTORY);

        if ("ALL".equals(clear) || clear.contains(player.getGameMode().name())) {
            player.getInventory().clear();
            ArenaPlayer.backupAndClearInventory(this, player);
            aPlayer.dump();
        }

        final PAJoinEvent event = new PAJoinEvent(this, player, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            debug.i("! Join event cancelled by a plugin !");
            return false;
        }

        if (aPlayer.getStatus() == Status.NULL) {
            // joining DIRECTLY - save loc !!
            aPlayer.setLocation(new PALocation(player.getLocation()));
        } else {
            // should not happen; just make sure it does not. If noone reports this
            // for some time, we can remove this check. It should never happen
            // anything different. Just saying.
            PVPArena.instance.getLogger().warning("Status not null for tryJoin: " + player.getName());
        }

        if (aPlayer.getArenaClass() == null) {
            String autoClass =
                    cfg.getBoolean(CFG.USES_PLAYERCLASSES) ?
                            getClass(player.getName()) != null ? player.getName() : cfg.getString(CFG.READY_AUTOCLASS)
                            : cfg.getString(CFG.READY_AUTOCLASS);

            if (autoClass != null && autoClass.contains(":") && autoClass.contains(";")) {
                final String[] definitions = autoClass.split(";");
                autoClass = definitions[definitions.length - 1]; // set default

                final Map<String, ArenaClass> classes = new HashMap<>();

                for (final String definition : definitions) {
                    if (!definition.contains(":")) {
                        continue;
                    }
                    final String[] var = definition.split(":");
                    final ArenaClass aClass = getClass(var[1]);
                    if (aClass != null) {
                        classes.put(var[0], aClass);
                    }
                }

                if (classes.containsKey(team.getName())) {
                    autoClass = classes.get(team.getName()).getName();
                }
            }

            if (autoClass != null && !"none".equals(autoClass)
                    && getClass(autoClass) == null) {
                msg(player, Language.parse(this, MSG.ERROR_CLASS_NOT_FOUND,
                        "autoClass"));
                return false;
            }
        }

        aPlayer.setArena(this);
        team.add(aPlayer);
        aPlayer.setStatus(Status.FIGHT);

        final Set<PASpawn> spawns = new HashSet<>();
        if (cfg.getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            final String arenaClass =
                    cfg.getBoolean(CFG.USES_PLAYERCLASSES) ?
                            getClass(player.getName()) != null ? player.getName() : cfg.getString(CFG.READY_AUTOCLASS)
                            : cfg.getString(CFG.READY_AUTOCLASS);
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, team.getName() + arenaClass + "spawn"));
        } else if (free) {
            if ("free".equals(team.getName())) {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, "spawn"));
            } else {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, team.getName()));
            }
        } else {
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, team.getName() + "spawn"));
        }

        int pos = new Random().nextInt(spawns.size());

        for (final PASpawn spawn : spawns) {
            if (--pos < 0) {
                this.tpPlayerToCoordName(aPlayer, spawn.getName());
                break;
            }
        }

        if (aPlayer.getState() == null) {

            final Arena arena = aPlayer.getArena();


            ArenaPlayer.backupAndClearInventory(arena, player);
            aPlayer.createState(player);
            aPlayer.dump();


            if (aPlayer.getArenaTeam() != null && aPlayer.getArenaClass() == null) {
                final String autoClass =
                        arena.cfg.getBoolean(CFG.USES_PLAYERCLASSES) ?
                                arena.getClass(player.getName()) != null ? player.getName() : arena.cfg.getString(CFG.READY_AUTOCLASS)
                                : arena.cfg.getString(CFG.READY_AUTOCLASS);
                if (autoClass != null && !"none".equals(autoClass) && arena.getClass(autoClass) != null) {
                    arena.chooseClass(player, null, autoClass);
                }
                if (autoClass == null) {
                    arena.msg(player, Language.parse(this, MSG.ERROR_CLASS_NOT_FOUND, "autoClass"));
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Setup an arena based on legacy goals:
     * <p/>
     * <pre>
     * teams - team lives arena
     * teamdm - team deathmatch arena
     * dm - deathmatch arena
     * free - deathmatch arena
     * ctf - capture the flag arena
     * ctp - capture the pumpkin arena
     * spleef - free for all with teamkill off
     * sabotage - destroy TNT inside the other team's base
     * tank - all vs one!
     * liberation - free willy!
     * infect - infect (catchy, huh?)!
     * food - food!
     * </pre>
     *
     * @param goalName legacy goal
     */
    public boolean getLegacyGoals(final String goalName) {
        setFree(false);
        final String lcName = goalName.toLowerCase();

        if ("teams".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("TeamLives"));
        } else if ("teamdm".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("TeamDeathMatch"));
        } else if ("dm".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm()
                    .getGoalByName("PlayerDeathMatch"));
            setFree(true);
        } else if ("free".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("PlayerLives"));
            setFree(true);
        } else if ("spleef".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("PlayerLives"));
            setFree(true);
            cfg.set(CFG.PERMS_TEAMKILL, false);
        } else if ("ctf".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("Flags"));
        } else if ("ctp".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("Flags"));
            cfg.set(CFG.GOAL_FLAGS_FLAGTYPE, "PUMPKIN");
            cfg.save();
        } else if ("tank".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("Tank"));
            setFree(true);
            cfg.save();
        } else if ("sabotage".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("Sabotage"));
            cfg.save();
        } else if ("infect".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("Infect"));
            setFree(true);
            cfg.save();
        } else if ("liberation".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("Liberation"));
            cfg.save();
        } else if ("food".equals(lcName)) {
            goalAdd(PVPArena.instance.getAgm().getGoalByName("Food"));
            cfg.save();
        } else {
            return false;
        }

        updateGoals();
        return true;
    }

    public Set<ArenaRegion> getRegionsByType(final RegionType regionType) {
        final Set<ArenaRegion> result = new HashSet<>();
        for (final ArenaRegion rs : regions) {
            if (rs.getType() == regionType) {
                result.add(rs);
            }
        }
        return result;
    }

    public void setRoundMap(final List<String> list) {
        if (list == null) {
            rounds = new PARoundMap(this, new ArrayList<Set<String>>());
        } else {
            final List<Set<String>> outer = new ArrayList<>();
            for (final String round : list) {
                final String[] split = round.split("|");
                final Set<String> inner = new HashSet<>();
                Collections.addAll(inner, split);
                outer.add(inner);
            }
            rounds = new PARoundMap(this, outer);
        }
    }

    public void setRound(final int value) {
        round = value;
    }

    public static void pmsg(final CommandSender sender, final String[] msgs) {
        for (final String s : msgs) {
            pmsg(sender, s);
        }
    }

    private void updateGoals() {
        final List<String> list = new ArrayList<>();

        for (final ArenaGoal goal : goals) {
            list.add(goal.getName());
        }

        cfg.set(CFG.LISTS_GOALS, list);
        cfg.save();
    }

    private void updateMods() {
        final List<String> list = new ArrayList<>();

        for (final ArenaModule mod : mods) {
            list.add(mod.getName());
        }

        cfg.set(CFG.LISTS_MODS, list);
        cfg.save();
    }

    public void updateRounds() {
        final List<String> result = new ArrayList<>();

        for (int i = 0; i < rounds.getCount(); i++) {
            List<String> names = new ArrayList<>();
            for (ArenaGoal goal : rounds.getGoals(i)) {
                names.add(goal.getName());
            }
            result.add(StringParser.joinList(names, "|"));
        }

        cfg.setManually("rounds", result);
        cfg.save();
    }

    public void updateScoreboards() {
        if (this.getArenaConfig().getBoolean(CFG.USES_SCOREBOARD)) {
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, () -> {
                final Scoreboard currentScoreboard = this.getSpecialScoreboard();
                if (this.isFreeForAll()) {
                    for (ArenaPlayer ap : this.getEveryone()) {
                        int value = PACheck.handleGetLives(this, ap);
                        if (value >= 0 && ap.getStatus() != Status.WATCH) {
                            currentScoreboard.getObjective("lives").getScore(ap.getName()).setScore(value);
                        }
                        Player player = ap.get();
                        if (player != null && !currentScoreboard.equals(player.getScoreboard())) {
                            player.setScoreboard(currentScoreboard);
                        }
                    }
                } else {
                    for (ArenaTeam team : this.getTeams()) {
                        team.getTeamMembers().stream().findFirst().ifPresent(randomTeamPlayer ->
                            currentScoreboard.getObjective("lives")
                                .getScore(team.getName())
                                .setScore(PACheck.handleGetLives(this, randomTeamPlayer))
                        );
                    }
                    for (ArenaPlayer ap : this.getEveryone()) {
                        Player player = ap.get();
                        if (player != null && !currentScoreboard.equals(player.getScoreboard())) {
                            player.setScoreboard(currentScoreboard);
                        }
                    }
                }
            }, 1L);
        }
    }

    private void updateScoreboard(final Player player) {
        if (this.getArenaConfig().getBoolean(CFG.USES_SCOREBOARD)) {
            Scoreboard currentScoreboard = this.getSpecialScoreboard();
            final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());

            // if player is a spectator, special case. Just update and do not add to the scores
            if (ap.getArenaTeam() != null) {
                currentScoreboard.getObjective("lives")
                        .getScore(this.isFreeForAll() ? player.getName() : ap.getArenaTeam().getName())
                        .setScore(PACheck.handleGetLives(this, ap));
            }

            player.setScoreboard(currentScoreboard);
        }
    }

    public void updateScoreboardTeam(final Player player, final ArenaTeam oldTeam, final ArenaTeam newTeam) {
        if (this.getArenaConfig().getBoolean(CFG.USES_SCOREBOARD)) {
            final Scoreboard board = this.getSpecialScoreboard();

            Bukkit.getScheduler().runTaskLater(PVPArena.instance, () -> {
                board.getTeam(oldTeam.getName()).removeEntry(player.getName());

                Team sTeam = board.getTeams().stream()
                        .filter(t -> t.getName().equals(newTeam.getName()))
                        .findFirst()
                        .orElseGet(() -> this.addNewTeam(board, newTeam));
                sTeam.addEntry(player.getName());

                this.updateScoreboard(player);
            }, 1L);
        } else {
            Scoreboard board = this.getStandardScoreboard();
            board.getTeam(oldTeam.getName()).removeEntry(player.getName());

            Team sTeam = board.getTeams().stream()
                    .filter(t -> t.getName().equals(newTeam.getName()))
                    .findFirst()
                    .orElseGet(() -> this.addNewTeam(board, newTeam));
            sTeam.addEntry(player.getName());
        }
    }

    private Team addNewTeam(Scoreboard board, ArenaTeam newTeam) {
        final Team sTeam = board.registerNewTeam(newTeam.getName());
        sTeam.setPrefix(newTeam.getColor().toString());
        sTeam.setSuffix(ChatColor.RESET.toString());
        sTeam.setColor(newTeam.getColor());
        sTeam.setCanSeeFriendlyInvisibles(!this.isFreeForAll());
        return sTeam;
    }

    public YamlConfiguration getLanguage() {
        return language;
    }

    public void setStartingTime() {
        startTime = System.currentTimeMillis();
    }

    public int getPlayedSeconds() {
        final int seconds = (int) (System.currentTimeMillis() - startTime);
        return seconds / 1000;
    }

    public void addBlock(final PABlock paBlock) {
        for (PABlock block : blocks) {
            if (block.getName().equals(paBlock.getName())) {
                blocks.remove(block);
                break;
            }
        }
        blocks.add(paBlock);
    }

    public void addSpawn(final PASpawn paSpawn) {
        for (PASpawn spawn : spawns) {
            if (spawn.getName().equals(paSpawn.getName())) {
                spawns.remove(spawn);
                break;
            }
        }
        spawns.add(paSpawn);
    }

    public boolean allowsJoinInBattle() {
        for (final ArenaGoal goal : getGoals()) {
            if (!goal.allowsJoinInBattle()) {
                return false;
            }
        }
        return true;
    }
}
