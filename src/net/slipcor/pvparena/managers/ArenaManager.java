package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.commands.PAA_Setup;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.loadables.ArenaRegion.RegionProtection;
import net.slipcor.pvparena.loadables.ArenaRegion.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.util.*;

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
    private static final Debug DEBUG = new Debug(24);

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
        arena.getDebugger().i("checking for arena end");
        if (!arena.isFightInProgress()) {
            arena.getDebugger().i("no fight, no end ^^");
            return false;
        }

        return PACheck.handleEnd(arena, force);
    }

    /**
     * try loading an arena
     *
     * @param name the arena name to load
     * @return the loaded module name if something is missing, null otherwise
     */
    private static String checkForMissingGoals(final String name) {
        DEBUG.i("check for missing goals: " + name);
        final File file = new File(PVPArena.instance.getDataFolder() + "/arenas/"
                + name + ".yml");
        if (!file.exists()) {
            return name + " (file does not exist)";
        }
        final Config cfg = new Config(file);

        cfg.load();
        final List<String> list = cfg.getStringList(CFG.LISTS_GOALS.getNode(),
                new ArrayList<String>());

        if (list.size() < 1) {
            return null;
        }

        for (final String goal : list) {

            final ArenaGoal type = PVPArena.instance.getAgm().getGoalByName(goal);

            if (type == null) {
                return goal;
            }

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
            if (!arena.getArenaConfig().getBoolean(CFG.PROTECT_ENABLED)) {
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
    public static void load_arenas() {
        DEBUG.i("loading arenas...");
        try {
            final File path = new File(PVPArena.instance.getDataFolder().getPath(),
                    "arenas");
            final File[] file = path.listFiles();
            for (File aFile : file) {
                if (!aFile.isDirectory() && aFile.getName().contains(".yml")) {
                    String sName = aFile.getName().replace("config_", "");
                    sName = sName.replace(".yml", "");
                    final String error = checkForMissingGoals(sName);
                    if (error == null) {
                        DEBUG.i("arena: " + sName);
                        if (!ARENAS.containsKey(sName.toLowerCase())) {
                            Arena arena = new Arena(sName);
                            loadArena(arena);
                        }
                    } else {
                        PVPArena.instance.getLogger().warning(Language.parse(MSG.ERROR_GOAL_NOTFOUND, error, StringParser.joinSet(PVPArena.instance.getAgm().getAllGoalNames(), ", ")));
                        PVPArena.instance.getLogger().warning(Language.parse(MSG.GOAL_INSTALLING, error));
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Backwards compatible arena loading
     *
     * @deprecated use {@link #loadArena(Arena arena)} } instead.
     */
    @Deprecated
    public static void loadArena(final String configFile) {
        DEBUG.i("loading arena " + configFile);
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
        DEBUG.i("loading arena " + arena.getName());

        if (!arena.isValid()) {
            Arena.pmsg(Bukkit.getConsoleSender(), Language.parse(arena, MSG.ERROR_ARENACONFIG, arena.getName()));
            return false;
        }

        ARENAS.put(arena.getName().toLowerCase(), arena);
        return true;
    }

    public static void removeArena(final Arena arena, final boolean deleteConfig) {
        arena.stop(true);
        ARENAS.remove(arena.getName().toLowerCase());
        if (deleteConfig) {
            arena.getArenaConfig().delete();
        }
    }

    /**
     * reset all arenas
     */
    public static void reset(final boolean force) {
        for (final Arena arena : ARENAS.values()) {
            DEBUG.i("resetting arena " + arena.getName());
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
        DEBUG.i("onInteract: sign check", player);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block block = event.getClickedBlock();
            if (block.getState() instanceof Sign) {
                final Sign sign = (Sign) block.getState();
                if ("[arena]".equalsIgnoreCase(sign.getLine(0))) {
                    final String sName = sign.getLine(1).toLowerCase();
                    String[] newArgs = new String[0];
                    final Arena arena = ARENAS.get(sName);
                    if (sign.getLine(2) != null
                            && arena.getTeam(sign.getLine(2)) != null) {
                        newArgs = new String[1];
                        newArgs[0] = sign.getLine(2);
                    }
                    if (arena == null) {
                        Arena.pmsg(player,
                                Language.parse(MSG.ERROR_ARENA_NOTFOUND, sName));
                        return;
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
            if (!a.isLocked() && !(a.isFightInProgress() && !a.allowsJoinInBattle())) {
                return a;
            }
        }
        return null;
    }

    public static void readShortcuts(final ConfigurationSection cs) {
        DEBUG.i("reading shortcuts!");
        usingShortcuts = false;
        DEF_VALUES.clear();
        DEF_LISTS.clear();
        if (cs == null) {
            PVPArena.instance.getLogger().warning("'shortcuts' node is null!!");
            DEBUG.i("'shortcuts' node is null!!");
            return;
        }

        for (final String key : cs.getKeys(false)) {
            DEBUG.i("key: " + key);
            final List<String> strings = cs.getStringList(key);

            if (strings == null) {
                PVPArena.instance.getLogger().warning("'shortcuts=>" + key + "' node is null!!");
                DEBUG.i("'shortcuts=>\" + key + \"' node is null!!");
                continue;
            }
            if (PVPArena.instance.getConfig().getBoolean("shortcut_shuffle")) {
                DEBUG.i("shuffling shortcuts!");
                Collections.shuffle(strings);
            }

            boolean error = false;
            for (final String arena : strings) {
                if (!ARENAS.containsKey(arena.toLowerCase())) {
                    PVPArena.instance.getLogger().warning("Arena not found: " + arena);
                    DEBUG.i("Arena not found: " + arena);
                    error = true;
                } else {
                    DEBUG.i("added "+key+">"+arena);
                }
            }
            if (error || strings.size() < 1) {
                PVPArena.instance.getLogger().warning("shortcut '" + key + "' will be skipped!!");
                DEBUG.i("shortcut '" + key + "' will be skipped!!");
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


        if (PVPArena.instance.getConfig().getBoolean("allow_ungrouped")) {
            nextArena: for (Arena a : ArenaManager.getArenas()) {
                if (!DEF_VALUES.keySet().contains(a.getName())) {
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
        if (usingShortcuts && PVPArena.instance.getConfig().getBoolean("only_shortcuts")) {
            for (final Map.Entry<String, Arena> stringArenaEntry : DEF_VALUES.entrySet()) {
                if (stringArenaEntry.getValue().equals(arena)) {
                    return stringArenaEntry.getKey();
                }
            }
        }
        return arena.getName();
    }

    public static Arena getIndirectArenaByName(final CommandSender sender, String string) {
        DEBUG.i("getIndirect(" + sender.getName() + "): " + string);
        if (!usingShortcuts || PermissionManager.hasOverridePerm(sender)) {
            DEBUG.i("out1");
            return getArenaByName(string);
        }

        // 1) exact match - case sensitive

        if (!DEF_LISTS.containsKey(string) && !PVPArena.instance.getConfig().getBoolean("only_shortcuts")) {
            // if we have not found an EXACT match for a shortcut, and we allow shortcuts -> find an exact arena in a shortcut
            for (final String temp : DEF_LISTS.keySet()) {
                if (temp.contains(string)) {
                    Arena a = ArenaManager.getArenaByName(temp);
                    if (a.isLocked()) {
                        continue;
                    }
                    DEBUG.i("found exact CS " + temp);
                    string = temp;
                    break;
                }
            }
        }
        DEBUG.i("temporary #1 " + string);

        boolean isUngrouped = true;
        String preciseArenaName = null;

        deflists: for (List<String> values : DEF_LISTS.values()) {
            for (String item : values) {
                if (item.equals(string)) {
                    if (!PVPArena.instance.getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false;
                    break deflists; // exact case match, out!
                }
                if (item.equalsIgnoreCase(string)) {
                    if (!PVPArena.instance.getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false; // case insensitive match, continue to eventually find a better match
                }
                if (item.toLowerCase().startsWith(string.toLowerCase())) {
                    if (!PVPArena.instance.getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false; // partial match, continue to eventually find a better match
                }
                if (item.toLowerCase().endsWith(string.toLowerCase())) {
                    if (!PVPArena.instance.getConfig().getBoolean("only_shortcuts")) {
                        preciseArenaName = item;
                    }
                    isUngrouped = false; // partial match, continue to eventually find a better match
                }
            }
        } // if ungrouped = false -> we have found the arena in a shortcut definition (CI)
        DEBUG.i("temporary #2 " + string);

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
        DEBUG.i("temporary #3 " + string);

        if (!foundExactCI) {
            // not found via exact check, ignoring case
            if (preciseArenaName == null && PVPArena.instance.getConfig().getBoolean("only_shortcuts") &&
                    !(isUngrouped && PVPArena.instance.getConfig().getBoolean("allow_ungrouped"))) {
                // 1) only allowing shortcuts
                // 2) it's either grouped and we thus don't show it, or it is ungrouped and we don't allow ungrouped
                DEBUG.i("not a shortcut or allowed ungrouped");

                return null;
            } else {
                // A: allowing more then shortcuts
                // OR
                // B: ungrouped and allowing ungrouped
                if (preciseArenaName != null) {
                    DEBUG.i("priorizing actual arena name "+ preciseArenaName + " over "+ string);
                    string = preciseArenaName;
                }
                DEBUG.i("out getArenaByName: " + string);
                return getArenaByName(string);
            }
        }

/*
        DEBUG.i("advance " + string);
        advance(string);
*/
        if (DEF_VALUES.get(string) == null) {
            DEBUG.i("out null -.-");
        } else {
            DEBUG.i("out : " + DEF_VALUES.get(string).getName());
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
        DEBUG.i("Sorting!");
        for (final String s : ARENAS.keySet()) {
            DEBUG.i(s);
        }
        final Map<String, Arena> sorted = new TreeMap<>(ARENAS);
        final List<Arena> result = new ArrayList<>();
        DEBUG.i("Sorted!");
        for (final Map.Entry<String, Arena> stringArenaEntry : sorted.entrySet()) {
            result.add(stringArenaEntry.getValue());
            DEBUG.i(stringArenaEntry.getKey());
        }
        return result;
    }
}
