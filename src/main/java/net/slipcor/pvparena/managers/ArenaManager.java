package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.commands.PAA_Setup;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Manager class
 * </pre>
 * <p/>
 * Provides static methods to manage Arenas
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class ArenaManager {
    private static final Map<String, Arena> ARENAS = new HashMap<>();
    private static final Map<String, Arena> DEF_VALUES = new HashMap<>();
    private static final Map<String, List<String>> DEF_LISTS = new HashMap<>();

    private static boolean usingShortcuts;

    private ArenaManager() {
    }

    /**
     * check for arena end and commit it, if true
     *
     * @param arena the arena to check
     * @return true if the arena ends
     */
    public static boolean checkAndCommit(final Arena arena, final boolean force) {
        debug(arena, "checking for arena end");
        if (!arena.isFightInProgress()) {
            debug(arena, "no fight, no end ^^");
            return false;
        }

        return WorkflowManager.handleEnd(arena, force);
    }

    /**
     * try loading an arena
     *
     * @param name the arena name to load
     * @return the loaded module name if is missing, null otherwise
     */
    private static String checkIfMissingGoals(final String name) {
        debug("check for missing goals: {}", name);
        final File file = new File(String.format("%s/arenas/%s.yml", PVPArena.getInstance().getDataFolder(), name));
        if (!file.exists()) {
            return String.format("%s (file does not exist)", name);
        }
        final Config cfg = new Config(file);

        cfg.load();
        String goalName = cfg.getString(CFG.GENERAL_GOAL);
        if (!PVPArena.getInstance().getAgm().hasLoadable(goalName)) {
            return goalName;
        }

        return null;
    }

    /**
     * check if join region is set and if player is inside, if so
     *
     * @param player the player to check
     * @return true if not set or player inside, false otherwise
     */
    public static boolean checkJoin(final Player player, final Arena arena) {
        boolean found = false;
        for (final ArenaRegion region : arena.getRegions()) {
            if (region.getType() == RegionType.JOIN) {
                found = true;
                if (region.getShape().contains(new PABlockLocation(player.getLocation()))) {
                    return true;
                }
            }
        }
        return !found; // no join region set
    }

    /**
     * check if an arena has interfering regions with other arenas
     *
     * @param arena the arena to check
     * @return true if no running arena interfering, false otherwise
     */
    public static boolean checkRegions(final Arena arena) {
        for (final Arena a : ARENAS.values()) {
            if (a.equals(arena)) {
                continue;
            }
            if (a.isFightInProgress()
                    && !ArenaRegion.checkRegion(a, arena)) {
                return false;
            }
        }
        return true;
    }

    /**
     * count the arenas
     *
     * @return the arena count
     */
    public static int count() {
        return ARENAS.size();
    }

    /**
     * search the arenas by arena name
     *
     * @param name the arena name
     * @return an arena instance if found, null otherwise
     */
    public static Arena getArenaByName(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        final String sName = name.toLowerCase();
        final Arena arena = ARENAS.get(sName);
        if (arena != null) {
            return arena;
        }
        for (final Map.Entry<String, Arena> stringArenaEntry2 : ARENAS.entrySet()) {
            if (stringArenaEntry2.getKey().endsWith(sName)) {
                return stringArenaEntry2.getValue();
            }
        }
        for (final Map.Entry<String, Arena> stringArenaEntry1 : ARENAS.entrySet()) {
            if (stringArenaEntry1.getKey().startsWith(sName)) {
                return stringArenaEntry1.getValue();
            }
        }
        for (final Map.Entry<String, Arena> stringArenaEntry : ARENAS.entrySet()) {
            if (stringArenaEntry.getKey().contains(sName)) {
                return stringArenaEntry.getValue();
            }
        }
        return null;
    }

    public static Arena getArenaByExactName(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        final String sName = name.toLowerCase();
        final Arena arena = ARENAS.get(sName);
        if (arena != null) {
            return arena;
        }
        return ARENAS.entrySet().stream()
                .filter(e -> name.equalsIgnoreCase(e.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /**
     * search the arenas by location
     *
     * @param location the location to find
     * @return an arena instance if found, null otherwise
     */
    public static Arena getArenaByRegionLocation(final PABlockLocation location) {
        for (final Arena arena : ARENAS.values()) {
            if (arena.isLocked()) {
                continue;
            }
            for (final ArenaRegion region : arena.getRegions()) {
                if (region.getShape().contains(location)) {
                    return arena;
                }
            }
        }
        return null;
    }

    public static Arena getArenaByProtectedRegionLocation(
            final PABlockLocation location, final RegionProtection regionProtection) {
        for (final Arena arena : ARENAS.values()) {
            if (!arena.getConfig().getBoolean(CFG.PROTECT_ENABLED)) {
                continue;
            }
            for (final ArenaRegion region : arena.getRegions()) {
                if (region.getShape().contains(location)
                        && region.getProtections().contains(regionProtection)) {
                    return arena;
                }
            }
        }
        return null;
    }

    public static Set<Arena> getArenasByRegionLocation(
            final PABlockLocation location) {
        final Set<Arena> result = new HashSet<>();
        for (final Arena arena : ARENAS.values()) {
            if (arena.isLocked()) {
                continue;
            }
            for (final ArenaRegion region : arena.getRegions()) {
                if (region.getShape().contains(location)) {
                    result.add(arena);
                }
            }
        }
        return result;
    }

    /**
     * return the arenas
     *
     * @return a Set of Arena
     */
    public static Set<Arena> getArenas() {
        return new HashSet<>(ARENAS.values());
    }

    /**
     * return the first arena
     *
     * @return the first arena instance
     */
    public static Arena getFirst() {
        for (final Arena arena : ARENAS.values()) {
            return arena;
        }
        return null;
    }

    /**
     * get all arena names
     *
     * @return a string with all arena names joined with comma
     */
    public static String getNames() {
        return StringParser.joinSet(ARENAS.keySet(), ", ");
    }

    public static Map<String, List<String>> getShortcutDefinitions() {
        return DEF_LISTS;
    }

    public static Map<String, Arena> getShortcutValues() {
        return DEF_VALUES;
    }

    /**
     * load all configs in the PVP Arena folder
     */
    public static void loadAllArenas() {

        debug("reading 'arenas' folder...");
        File[] files = null;
        try {
            final File path = new File(PVPArena.getInstance().getDataFolder().getPath(),
                    "arenas");
            files = path.listFiles();
        } catch (final Exception e) {
            PVPArena.getInstance().getLogger().severe(String.format("Can't create PvpArena folder: %s.", e.getMessage()));
            return;
        }

        if (CollectionUtils.isNotEmpty(files)) {
            for (File arenaConfigFile : files) {
                if (!arenaConfigFile.isDirectory() && arenaConfigFile.getName().contains(".yml")) {
                    String sName = arenaConfigFile.getName().replace("config_", "");
                    sName = sName.replace(".yml", "");

                    debug("arena: {}", sName);
                    if (!ARENAS.containsKey(sName.toLowerCase())) {
                        Arena arena = new Arena(sName);
                        loadArena(arena);
                    }
                }
            }
        }
    }

    /**
     * Backwards compatible arena loading
     *
     * @deprecated use {@link #loadArena(Arena arena)} } instead.
     */
    @Deprecated
    public static void loadArena(final String configFile) {
        debug("loading arena {}", configFile);
        final Arena arena = new Arena(configFile);
        ARENAS.put(arena.getName().toLowerCase(), arena);
    }

    /**
     * load a specific arena
     *
     * @param arena the arena to load
     * @return whether the operation succeeded
     */
    public static boolean loadArena(final Arena arena) {
        if (arena == null) {
            return false;
        }
        debug(arena, "loading arena");

        File file = new File(String.format("%s/arenas/%s.yml", PVPArena.getInstance().getDataFolder().getPath(), arena.getName()));
        if (!file.exists()) {
            PVPArena.getInstance().getLogger().severe(String.format("Can't load arena %s: file %s not found.", arena.getName(), file.getName()));
            return false;
        }
        final Config cfg = new Config(file);
        arena.setConfig(cfg);
        arena.setValid(ConfigurationManager.configParse(arena, cfg));
        debug(arena, "valid: {}", arena.isValid());
        if (arena.isValid()) {
            StatisticsManager.loadStatistics(arena);
            SpawnManager.loadSpawns(arena, cfg);
        } else {
            // not valid arena config file
            Arena.pmsg(Bukkit.getConsoleSender(), MSG.ERROR_ARENACONFIG, arena.getName());
            // force enabled to false to prevent players using it
            arena.getConfig().set(CFG.GENERAL_ENABLED, false);
            arena.getConfig().save();
            arena.setLocked(true);
        }

        ARENAS.put(arena.getName().toLowerCase(), arena);
        return arena.isValid();
    }

    public static void removeArena(final Arena arena, final boolean deleteConfig) {
        arena.stop(true);
        ARENAS.remove(arena.getName().toLowerCase());
        if (deleteConfig) {
            arena.getConfig().delete();
        }
    }

    /**
     * reset all arenas
     */
    public static void reset(final boolean force) {
        for (final Arena arena : ARENAS.values()) {
            debug("resetting arena {}", arena);
            arena.reset(force);
        }
    }

    /**
     * try to join an arena via sign click
     *
     * @param event  the PlayerInteractEvent
     * @param player the player trying to join
     */
    public static void trySignJoin(final PlayerInteractEvent event, final Player player) {
        debug(player, "onInteract: sign check");
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block block = event.getClickedBlock();
            if (block.getState() instanceof Sign) {
                String[] lines = ((Sign) block.getState()).getLines();
                List<String> signHeaders = PVPArena.getInstance().getConfig().getStringList("signHeaders");
                if (CollectionUtils.containsIgnoreCase(signHeaders, lines[0])) {
                    final String sName = ChatColor.stripColor(lines[1]).toLowerCase();
                    String[] newArgs = new String[0];
                    final Arena arena = ARENAS.get(sName);

                    if (arena == null) {
                        Arena.pmsg(player, MSG.ERROR_ARENA_NOTFOUND, sName);
                        return;
                    }

                    String secondLine = ChatColor.stripColor(lines[2]);
                    if (StringUtils.notBlank(secondLine) && arena.getTeam(secondLine) != null) {
                        newArgs = new String[]{secondLine};
                    }

                    final AbstractArenaCommand command = new PAG_Join();
                    command.commit(arena, player, newArgs);
                }
            }
        }
    }

    public static int countAvailable() {
        int sum = 0;
        for (final Arena a : getArenas()) {
            if (!a.isLocked() && !a.isFightInProgress()) {
                sum++;
            }
        }
        return sum;
    }

    public static Arena getAvailable() {
        for (final Arena a : getArenas()) {
            if (!a.isLocked() && !(a.isFightInProgress() && !a.getGoal().allowsJoinInBattle())) {
                return a;
            }
        }
        return null;
    }

    public static void readShortcuts(final ConfigurationSection cs) {
        debug("reading shortcuts!");
        usingShortcuts = false;
        DEF_VALUES.clear();
        DEF_LISTS.clear();
        if (cs == null) {
            PVPArena.getInstance().getLogger().warning("'shortcuts' node is null!!");
            debug("'shortcuts' node is null!!");
            return;
        }

        for (final String key : cs.getKeys(false)) {
            debug("key: {}", key);
            final List<String> strings = cs.getStringList(key);

            if (strings == null) {
                PVPArena.getInstance().getLogger().warning("'shortcuts=>" + key + "' node is null!!");
                debug("'shortcuts=>\" + key + \"' node is null!!");
                continue;
            }
            if (PVPArena.getInstance().getConfig().getBoolean("shortcut_shuffle")) {
                debug("shuffling shortcuts!");
                Collections.shuffle(strings);
            }

            boolean error = false;
            for (final String arena : strings) {
                if (!ARENAS.containsKey(arena.toLowerCase())) {
                    PVPArena.getInstance().getLogger().warning("Arena not found: " + arena);
                    debug("Arena not found: {}", arena);
                    error = true;
                } else {
                    debug("added {} > {}", key, arena);
                }
            }
            if (error || strings.size() < 1) {
                PVPArena.getInstance().getLogger().warning("shortcut '" + key + "' will be skipped!!");
                debug("shortcut '{}' will be skipped!!", key);
                continue;
            }
            usingShortcuts = true;
            DEF_LISTS.put(key, strings);
            advance(key);
        }
    }

    public static boolean isUsingShortcuts() {
        return usingShortcuts;
    }

    public static List<String> getColoredShortcuts() {
        final Set<String> sorted = new TreeSet<>(DEF_LISTS.keySet());


        if (PVPArena.getInstance().getConfig().getBoolean("allow_ungrouped")) {
            nextArena:
            for (Arena a : ArenaManager.getArenas()) {
                if (!DEF_VALUES.containsKey(a.getName())) {
                    for (List<String> list : DEF_LISTS.values()) {
                        if (list.contains(a.getName())) {
                            continue nextArena;
                        }
                    }
                    sorted.add(a.getName());
                }
            }
        }

        final List<String> result = new ArrayList<>();

        for (final String definition : sorted) {
            if (DEF_VALUES.containsKey(definition)) {
                final Arena a = DEF_VALUES.get(definition);
                result.add((a.isLocked() ? "&c" : PAA_Edit.activeEdits.containsValue(a) || PAA_Setup.activeSetups.containsValue(a) ? "&e" : a.isFightInProgress() ? "&a" : "&f") + definition + "&r");
            } else {
                try {
                    final Arena a = ArenaManager.getArenaByName(definition);
                    result.add((a.isLocked() ? "&c" : PAA_Edit.activeEdits.containsValue(a) || PAA_Setup.activeSetups.containsValue(a) ? "&e" : a.isFightInProgress() ? "&a" : "&f") + definition + "&r");
                } catch (Exception e) {
                    result.add("&f" + definition + "&r");
                }
            }
        }

        return result;
    }

    public static String getIndirectArenaName(final Arena arena) {
        if (usingShortcuts && PVPArena.getInstance().getConfig().getBoolean("only_shortcuts")) {
            for (final Map.Entry<String, Arena> stringArenaEntry : DEF_VALUES.entrySet()) {
                if (stringArenaEntry.getValue().equals(arena)) {
                    return stringArenaEntry.getKey();
                }
            }
        }
        return arena.getName();
    }

    public static Arena getIndirectArenaByName(final CommandSender sender, String string) {
        debug("getIndirect({}): {}", sender.getName(), string);
        if (!usingShortcuts || PVPArena.hasOverridePerms(sender)) {
            debug("out1");
            return getArenaByName(string);
        }

        // 1) exact match - case sensitive

        if (!DEF_LISTS.containsKey(string) && !PVPArena.getInstance().getConfig().getBoolean("only_shortcuts")) {
            // if we have not found an EXACT match for a shortcut, and we allow shortcuts -> find an exact arena in a shortcut
            for (final String temp : DEF_LISTS.keySet()) {
                if (temp.contains(string)) {
                    Arena a = ArenaManager.getArenaByName(temp);
                    if (a.isLocked()) {
                        continue;
                    }
                    debug("found exact CS {}", temp);
                    string = temp;
                    break;
                }
            }
        }
        debug("temporary #1 {}", string);

        boolean isUngrouped = true;
        String preciseArenaName = null;

        deflists:
        for (List<String> values : DEF_LISTS.values()) {
            for (String item : values) {
                if (item.equals(string)) {
                    if (!PVPArena.getInstance().getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false;
                    break deflists; // exact case match, out!
                }
                if (item.equalsIgnoreCase(string)) {
                    if (!PVPArena.getInstance().getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false; // case insensitive match, continue to eventually find a better match
                }
                if (item.toLowerCase().startsWith(string.toLowerCase())) {
                    if (!PVPArena.getInstance().getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false; // partial match, continue to eventually find a better match
                }
                if (item.toLowerCase().endsWith(string.toLowerCase())) {
                    if (!PVPArena.getInstance().getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false; // partial match, continue to eventually find a better match
                }
            }
        } // if ungrouped = false -> we have found the arena in a shortcut definition (CI)
        debug("temporary #2 {}", string);

        boolean foundExactCI = false;

        for (String key : DEF_LISTS.keySet()) {
            if (key.equals(string)) {
                foundExactCI = true;
                string = key;
                break;
            }
            if (key.equalsIgnoreCase(string)) {
                foundExactCI = true;
                string = key; // case insensitive match, continue to eventually find a better match
            }
            if (key.toLowerCase().startsWith(string.toLowerCase())) {
                foundExactCI = true;
                string = key; // partial match, continue to eventually find a better match
            }
            if (key.toLowerCase().endsWith(string.toLowerCase())) {
                foundExactCI = true;
                string = key; // partial match, continue to eventually find a better match
            }
        }
        debug("temporary #3 {}", string);

        if (!foundExactCI) {
            // not found via exact check, ignoring case
            if (preciseArenaName == null && PVPArena.getInstance().getConfig().getBoolean("only_shortcuts") &&
                    !(isUngrouped && PVPArena.getInstance().getConfig().getBoolean("allow_ungrouped"))) {
                // 1) only allowing shortcuts
                // 2) it's either grouped and we thus don't show it, or it is ungrouped and we don't allow ungrouped
                debug("not a shortcut or allowed ungrouped");

                return null;
            } else {
                // A: allowing more then shortcuts
                // OR
                // B: ungrouped and allowing ungrouped
                if (preciseArenaName != null) {
                    debug("priorizing actual arena name {} over {}", preciseArenaName, string);
                    string = preciseArenaName;
                }
                debug("out getArenaByName: {}", string);
                return getArenaByName(string);
            }
        }

/*
        DEBUG.i("advance " + string);
        advance(string);
*/
        if (DEF_VALUES.get(string) == null) {
            debug("out null -.-");
        } else {
            debug("out : {}", DEF_VALUES.get(string).getName());
        }

        return DEF_VALUES.get(string);
    }

    public static void advance(final Arena arena) {
        if (usingShortcuts) {
            for (final Map.Entry<String, Arena> stringArenaEntry : DEF_VALUES.entrySet()) {
                if (stringArenaEntry.getValue().equals(arena)) {
                    advance(stringArenaEntry.getKey());
                    return;
                }
            }
        }
    }

    public static void advance(final String string) {
        if (!usingShortcuts) {
            return;
        }
        final List<String> defs = DEF_LISTS.get(string);

        if (DEF_VALUES.containsKey(string)) {
            final Arena arena = DEF_VALUES.get(string);
            boolean found = false;
            for (final String arenaName : defs) {
                if (found) {
                    // we just found it, this is the one!
                    final Arena nextArena = ARENAS.get(arenaName.toLowerCase());

                    if (nextArena.isLocked()) {
                        continue;
                    }

                    DEF_VALUES.put(string, nextArena);
                    return;
                } else {
                    if (arenaName.equalsIgnoreCase(arena.getName())) {
                        found = true;
                    }
                }
            }
        }
        // get the first available!
        for (final String arenaName : defs) {
            final Arena arena = ARENAS.get(arenaName.toLowerCase());
            if (arena.isFightInProgress() || arena.isLocked()) {
                continue;
            }

            DEF_VALUES.put(string, arena);
            return;
        }
        if (!DEF_VALUES.containsKey(string)) {
            DEF_VALUES.put(string, ArenaManager.getArenaByName(defs.get(0)));
        }
    }

    public static List<Arena> getArenasSorted() {
        debug("Sorting!");
        for (final String s : ARENAS.keySet()) {
            debug(s);
        }
        final Map<String, Arena> sorted = new TreeMap<>(ARENAS);
        final List<Arena> result = new ArrayList<>();
        debug("Sorted!");
        for (final Map.Entry<String, Arena> stringArenaEntry : sorted.entrySet()) {
            result.add(stringArenaEntry.getValue());
            debug(stringArenaEntry.getKey());
        }
        return result;
    }
}
