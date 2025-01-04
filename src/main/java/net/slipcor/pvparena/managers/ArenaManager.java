package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * check if join region is set and if player is inside, if so
     *
     * @param player the player to check
     * @return true if not set or player inside, false otherwise
     */
    public static boolean checkJoinRegion(final Player player, final Arena arena) {
        boolean found = false;
        for (ArenaRegion region : arena.getRegions()) {
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
        for (Arena a : ARENAS.values()) {
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
        for (Map.Entry<String, Arena> stringArenaEntry2 : ARENAS.entrySet()) {
            if (stringArenaEntry2.getKey().endsWith(sName)) {
                return stringArenaEntry2.getValue();
            }
        }
        for (Map.Entry<String, Arena> stringArenaEntry1 : ARENAS.entrySet()) {
            if (stringArenaEntry1.getKey().startsWith(sName)) {
                return stringArenaEntry1.getValue();
            }
        }
        for (Map.Entry<String, Arena> stringArenaEntry : ARENAS.entrySet()) {
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
        for (Arena arena : ARENAS.values()) {
            if (arena.isLocked()) {
                continue;
            }
            for (ArenaRegion region : arena.getRegions()) {
                if (region.getShape().contains(location)) {
                    return arena;
                }
            }
        }
        return null;
    }

    public static Arena getArenaByProtectedRegionLocation(
            final PABlockLocation location, final RegionProtection regionProtection) {
        for (Arena arena : ARENAS.values()) {
            if (!arena.getConfig().getBoolean(CFG.PROTECT_ENABLED)) {
                continue;
            }
            for (ArenaRegion region : arena.getRegions()) {
                if (region.getShape().contains(location)
                        && region.getProtections().contains(regionProtection)) {
                    return arena;
                }
            }
        }
        return null;
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
        for (Arena arena : ARENAS.values()) {
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
                if (!arenaConfigFile.isDirectory() && arenaConfigFile.getName().endsWith(".yml")) {
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
        try {
            final Config cfg = new Config(file);
            arena.setConfig(cfg);
            arena.setValid(ConfigurationManager.configParse(arena, cfg));
            debug(arena, "valid: {}", arena.isValid());
            if (arena.isValid()) {
                SpawnManager.loadSpawns(arena, cfg);
                SpawnManager.loadBlocks(arena, cfg);
            } else {
                // not valid arena config file
                Arena.pmsg(Bukkit.getConsoleSender(), MSG.ERROR_ARENACONFIG, arena.getName());
                arena.setLocked(true);
            }

            ARENAS.put(arena.getName().toLowerCase(), arena);
        } catch (UnsupportedClassVersionError e) {
            arena.setValid(false);
            ConfigurationManager.moveOldConfig(file);
        }

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
        for (Arena arena : ARENAS.values()) {
            debug("resetting arena {}", arena);
            arena.reset(force);
        }
    }

    public static int countAvailable() {
        int sum = 0;
        for (Arena a : getArenas()) {
            if (!a.isLocked() && !a.isFightInProgress()) {
                sum++;
            }
        }
        return sum;
    }

    public static Arena getAvailable() {
        for (Arena a : getArenas()) {
            if (!a.isLocked() && !(a.isFightInProgress() && !a.getGoal().allowsJoinInBattle())) {
                return a;
            }
        }
        return null;
    }
}
