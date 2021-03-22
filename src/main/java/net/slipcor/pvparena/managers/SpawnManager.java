package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.config.SpawnOffset;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import net.slipcor.pvparena.runnables.RespawnRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Spawn Manager class</pre>
 * <p/>
 * Provides static methods to manage Spawns
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class SpawnManager {

    private SpawnManager() {
    }

    private static String calculateFarSpawn(final String[] taken,
                                            final Set<PASpawn> available,
                                            final Set<PASpawn> total) {
        debug("--------------------");
        debug("calculating a spawn!");
        debug("--------------------");
        String far = null;
        for (final PASpawn s : available) {
            far = s.getName();
            break;
        }
        debug("last resort: {}", far);

        double diff = 0;
        for (final PASpawn s : available) {
            debug("> checking {}", s.getName());
            double tempDiff = 0;
            for (int i = 0; i < taken.length && taken[i] != null; i++) {
                for (final PASpawn tt : total) {
                    for (final PASpawn aa : available) {
                        if (tt.getName().equals(taken[i])
                                && aa.getName().equals(s.getName())) {
                            tempDiff += tt.getLocation().getDistanceSquared(aa.getLocation());
                            debug(">> tempDiff: {}", tempDiff);
                        }
                    }
                }

            }

            if (tempDiff > diff) {
                debug("-> diff");
                diff = tempDiff;
                far = s.getName();
            }
        }

        return far;
    }

    public static void distribute(final Arena arena, final ArenaTeam team) {
        final Set<ArenaRegion> ars = arena.getRegionsByType(RegionType.SPAWN);

        if (!ars.isEmpty()) {
            placeInsideSpawnRegions(arena, team.getTeamMembers(), ars);
            return;
        }

        if (arena.getConfig().getBoolean(CFG.GENERAL_QUICKSPAWN)) {
            class TeleportLater extends BukkitRunnable {
                private final Set<ArenaPlayer> teamMembers = new HashSet<>();
                private final boolean classSpawn;
                private int pos;

                private PASpawn[] locations;

                TeleportLater(final Set<ArenaPlayer> set) {
                    for (final ArenaPlayer ap : set) {
                        this.teamMembers.add(ap);
                    }
                    this.classSpawn = arena.getConfig().getBoolean(CFG.GENERAL_CLASSSPAWN);
                }

                @Override
                public void run() {

                    if (this.locations == null) {
                        final Set<PASpawn> spawns = new HashSet<>();
                        if (arena.isFreeForAll()) {
                            if ("free".equals(team.getName())) {
                                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, "spawn"));
                            } else {
                                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName()));
                            }
                        } else {
                            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName() + "spawn"));
                        }
                        debug(arena, "read spawns for '" + team.getName() + "'; size: " + spawns.size());
                        this.locations = new PASpawn[spawns.size()];
                        int pos = 0;
                        for (final PASpawn spawn : spawns) {
                            debug(arena, "- " + spawn.getName());
                            this.locations[pos++] = spawn;
                        }
                    }


                    for (final ArenaPlayer ap : this.teamMembers) {
                        if (this.classSpawn) {


                            final Set<PASpawn> spawns = SpawnManager.getPASpawnsStartingWith(arena, team.getName() + ap.getArenaClass().getName() + "spawn");

                            int pos = new Random().nextInt(spawns.size());
                            for (final PASpawn spawn : spawns) {
                                if (--pos < 0) {
                                    arena.tpPlayerToCoordName(ap, spawn.getName());
                                    break;
                                }
                            }

                        } else {
                            arena.tpPlayerToCoordName(ap, this.locations[this.pos++ % this.locations.length].getName());
                        }
                        ap.setStatus(PlayerStatus.FIGHT);
                        this.teamMembers.remove(ap);
                        return;
                    }
                    this.cancel();
                }

            }
            new TeleportLater(team.getTeamMembers()).runTaskTimer(PVPArena.getInstance(), 1L, 1L);

            return;
        }

        if (arena.getConfig().getBoolean(CFG.GENERAL_SMARTSPAWN)) {
            distributeSmart(arena, team.getTeamMembers(), team.getName());
            return;
        }
        distributeByOrder(arena, team.getTeamMembers(), team.getName());
    }

    private static void distributeByOrder(final Arena arena,
                                          final Set<ArenaPlayer> set, final String string) {
        debug(arena, "distributeByOrder: " + string);
        if (set == null || set.size() < 1) {
            return;
        }

        final Set<PASpawn> spawns = new HashSet<>();
        if (arena.isFreeForAll()) {
            if ("free".equals(string)) {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, "spawn"));
            } else {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, string));
            }
        } else {
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, string + "spawn"));
        }

        if (spawns.size() < 1) {
            return;
        }

        class TeleportLater extends BukkitRunnable {
            private final Set<ArenaPlayer> set = new HashSet<>();
            private final boolean classSpawn;

            TeleportLater(final Set<ArenaPlayer> set) {
                for (final ArenaPlayer ap : set) {
                    this.set.add(ap);
                }
                this.classSpawn = arena.getConfig().getBoolean(CFG.GENERAL_CLASSSPAWN);
            }

            @Override
            public void run() {
                for (final ArenaPlayer ap : this.set) {
                    ap.setStatus(PlayerStatus.FIGHT);
                    if (this.classSpawn) {


                        final Set<PASpawn> spawns = SpawnManager.getPASpawnsStartingWith(arena, ap.getArenaTeam().getName() + ap.getArenaClass().getName() + "spawn");

                        int pos = new Random().nextInt(spawns.size());
                        for (final PASpawn spawn : spawns) {
                            if (--pos < 0) {
                                arena.tpPlayerToCoordName(ap, spawn.getName());
                                break;
                            }
                        }

                    } else {
                        for (final PASpawn s : spawns) {
                            arena.tpPlayerToCoordName(ap, s.getName());
                            if (spawns.size() > 1) {
                                spawns.remove(s);
                            }
                            break;
                        }
                    }
                    this.set.remove(ap);
                    return;
                }
                this.cancel();
            }

        }
        new TeleportLater(set).runTaskTimer(PVPArena.getInstance(), 1L, 1L);
    }

    public static void distributeSmart(final Arena arena,
                                       final Set<ArenaPlayer> set, final String teamNName) {
        debug(arena, "distributing smart-ish");
        if (set == null || set.size() < 1) {
            return;
        }

        final Set<PASpawn> locations;
        final Set<PASpawn> total_locations;

        if (arena.isFreeForAll()) {
            if ("free".equals(teamNName)) {
                locations = getPASpawnsStartingWith(arena, "spawn");
                total_locations = getPASpawnsStartingWith(arena, "spawn");
            } else {
                locations = getPASpawnsStartingWith(arena, teamNName);
                total_locations = getPASpawnsStartingWith(arena, teamNName);
            }
        } else {
            locations = getPASpawnsStartingWith(arena, teamNName + "spawn");
            total_locations = getPASpawnsStartingWith(arena, teamNName + "spawn");
        }

        if (locations == null || locations.size() < 1) {
            debug(arena, "null or less than 1! -> OUT!");
            return;
        }

        final String[] iteratings = new String[locations.size()];

        for (int i = 0; i < total_locations.size(); i++) {
            if (i == 0) {
                PASpawn innerSpawn = null;
                for (final PASpawn ss : locations) {
                    innerSpawn = ss;
                    break;
                }
                iteratings[i] = innerSpawn.getName();
                locations.remove(innerSpawn);
                continue;
            }
            final String spawnName = calculateFarSpawn(iteratings, locations, total_locations);
            iteratings[i] = spawnName;
            for (final PASpawn spawn : locations) {
                if (spawn.getName().equals(spawnName)) {
                    locations.remove(spawn);
                    break;
                }
            }

        }

        class TeleportLater extends BukkitRunnable {
            private int pos;
            private final String[] iteratings;
            private final Set<ArenaPlayer> set = new HashSet<>();

            TeleportLater(final Set<ArenaPlayer> set, final String[] iteratings) {
                this.pos = 0;
                for (final ArenaPlayer ap : set) {
                    this.set.add(ap);
                }
                this.iteratings = iteratings.clone();
            }

            @Override
            public void run() {
                for (final ArenaPlayer ap : this.set) {
                    ap.setStatus(PlayerStatus.FIGHT);
                    final String spawnName = this.iteratings[this.pos++ % this.iteratings.length];
                    if (spawnName == null) {
                        PVPArena.getInstance().getLogger().warning("Element #" + this.pos + " is null: [" + StringParser.joinArray(this.iteratings, ",") + ']');
                    }
                    arena.tpPlayerToCoordName(ap, spawnName);
                    this.set.remove(ap);
                    return;
                }
                this.cancel();
            }

        }

        new TeleportLater(set, iteratings).runTaskTimer(PVPArena.getInstance(), 1L, 1L);
    }


    public static PABlockLocation getBlockNearest(final Set<PABlockLocation> locs,
                                                  final PABlockLocation location) {
        PABlockLocation result = null;

        for (final PABlockLocation loc : locs) {
            if (!loc.getWorldName().equals(location.getWorldName())) {
                continue;
            }
            if (result == null
                    || result.getDistanceSquared(location) > loc.getDistanceSquared(location)) {
                result = loc;
            }
        }

        return result;
    }

    public static Set<PABlockLocation> getBlocksStartingWith(final Arena arena, final String name) {
        final Set<PABlockLocation> result = new HashSet<>();

        for (final PABlock block : arena.getBlocks()) {
            if (block.getName().startsWith(name)) {
                result.add(block.getLocation());
            }
        }

        return result;
    }

    public static Set<PABlockLocation> getBlocksContaining(final Arena arena, final String name) {
        final Set<PABlockLocation> result = new HashSet<>();

        for (final PABlock block : arena.getBlocks()) {
            if (block.getName().contains(name)) {
                result.add(block.getLocation());
            }
        }

        return result;
    }

    public static Set<PABlock> getPABlocksContaining(final Arena arena, final String name) {
        final Set<PABlock> result = new HashSet<>();

        for (final PABlock block : arena.getBlocks()) {
            if (block.getName().contains(name)) {
                result.add(block);
            }
        }

        return result;
    }

    public static Set<PALocation> getSpawnsContaining(final Arena arena, final String name) {
        final Set<PALocation> result = new HashSet<>();

        for (final PASpawn spawn : arena.getSpawns()) {
            if (spawn.getName().contains(name)) {
                result.add(spawn.getLocation());
            }
        }

        return result;
    }

    public static Set<PALocation> getSpawnsStartingWith(final Arena arena, final String name) {
        final Set<PALocation> result = new HashSet<>();

        for (final PASpawn spawn : arena.getSpawns()) {
            if (spawn.getName().startsWith(name)) {
                result.add(spawn.getLocation());
            }
        }

        return result;
    }

    public static Set<PASpawn> getPASpawnsStartingWith(final Arena arena, final String name) {
        final Set<PASpawn> result = new HashSet<>();

        for (final PASpawn spawn : arena.getSpawns()) {
            if (spawn.getName().startsWith(name)) {
                result.add(spawn);
            }
        }

        return result;
    }

    public static PABlockLocation getBlockByExactName(final Arena arena, final String name) {
        for (final PABlock spawn : arena.getBlocks()) {
            if (spawn.getName().equals(name)) {
                return spawn.getLocation();
            }
        }
        return null;
    }

    public static PALocation getSpawnByExactName(final Arena arena, final String name) {
        SpawnOffset spawnOffset = PVPArena.getInstance().getSpawnOffset();

        return arena.getSpawns().stream()
                .filter(spawn -> spawn.getName().equals(name))
                .findAny()
                .map(spawn -> spawn.getLocation().add(spawnOffset.getX(), spawnOffset.getY(), spawnOffset.getZ()))
                .orElse(null);
    }

    public static PABlockLocation getRegionCenter(final Arena arena) {
        final Set<PALocation> locs = new HashSet<>();

        ArenaRegion ars = null;
        for (final ArenaRegion a : arena.getRegionsByType(RegionType.BATTLE)) {
            ars = a;
            break;
        }

        if (ars == null) {
            return new PABlockLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        final World world = Bukkit.getWorld(ars.getWorldName());

        if (world == null) {
            return new PABlockLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        locs.addAll(getSpawnsContaining(arena, "spawn"));

        long x = 0;
        long y = 0;
        long z = 0;

        for (final PALocation loc : locs) {
            if (!loc.getWorldName().equals(world.getName())) {
                continue;
            }
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
        }

        return new PABlockLocation(world.getName(),
                (int) x / locs.size(), (int) y / locs.size(), (int) z / locs.size());
    }

    private static void placeInsideSpawnRegion(final Arena arena, final ArenaPlayer aPlayer,
                                               final ArenaRegion region) {
        int x, y, z;
        final Random random = new Random();

        boolean found = false;
        int attempt = 0;

        PABlockLocation loc = null;

        while (!found && attempt < 10) {

            x = region.getShape().getMinimumLocation().getX() + random.nextInt(region.getShape().getMaximumLocation().getX() -
                    region.getShape().getMinimumLocation().getX());
            y = region.getShape().getMinimumLocation().getY() + random.nextInt(region.getShape().getMaximumLocation().getY() -
                    region.getShape().getMinimumLocation().getY());
            z = region.getShape().getMinimumLocation().getZ() + random.nextInt(region.getShape().getMaximumLocation().getZ() -
                    region.getShape().getMinimumLocation().getZ());

            loc = new PABlockLocation(region.getShape().getMinimumLocation().getWorldName(), x, y, z);
            loc.setY(loc.toLocation().getWorld().getHighestBlockYAt(x, z)+1);
            attempt++;
            found = region.getShape().contains(loc);

        }

        final PABlockLocation newLoc = loc;

        class RunLater implements Runnable {
            @Override
            public void run() {
                final PALocation temp = aPlayer.getSavedLocation();

                Location bLoc = newLoc.toLocation();
                SpawnOffset spawnOffset = PVPArena.getInstance().getSpawnOffset();
                bLoc = bLoc.add(spawnOffset.toVector());

                while (bLoc.getBlock().getType() != Material.AIR
                        && bLoc.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR
                        && bLoc.getBlock().getRelative(BlockFace.UP, 2).getType() != Material.AIR) {
                    bLoc = bLoc.add(0, 1, 0);
                }

                debug(arena, "bLoc: " + bLoc.toString());
                aPlayer.setLocation(new PALocation(bLoc));

                aPlayer.setStatus(PlayerStatus.FIGHT);

                arena.tpPlayerToCoordName(aPlayer, "old");
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new Runnable() {
                            @Override
                            public void run() {
                                aPlayer.setLocation(temp);
                                debug(arena, "temp: " + temp.toString());
                            }
                }, 6L);

            }

        }

        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 1L);

    }

    private static void placeInsideSpawnRegions(final Arena arena, final Set<ArenaPlayer> set,
                                                final Set<ArenaRegion> ars) {
        if (arena.isFreeForAll()) {
            for (final ArenaPlayer ap : set) {
                int pos = new Random().nextInt(ars.size());

                for (final ArenaRegion x : ars) {
                    if (pos-- == 0) {
                        placeInsideSpawnRegion(arena, ap, x);
                        break;
                    }
                }
            }
        } else {
            String teamName = null;
            for (final ArenaPlayer ap : set) {
                if (teamName == null) {
                    teamName = ap.getArenaTeam().getName();
                }
                boolean teleported = false;
                for (final ArenaRegion x : ars) {
                    if (x.getRegionName().contains(teamName)) {
                        placeInsideSpawnRegion(arena, ap, x);
                        teleported = true;
                        break;
                    }
                }
                if (!teleported) {
                    int pos = new Random().nextInt(ars.size());

                    for (final ArenaRegion x : ars) {
                        if (pos-- == 0) {
                            placeInsideSpawnRegion(arena, ap, x);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * is a player near a spawn?
     *
     * @param arena  the arena to check
     * @param player the player to check
     * @param diff   the distance to check
     * @return true if the player is near, false otherwise
     */
    public static boolean isNearSpawn(final Arena arena, final Player player, final int diff) {

        debug(arena, player, "checking if arena is near a spawn");
        if (!arena.hasPlayer(player)) {
            return false;
        }
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (team == null) {
            return false;
        }

        final Set<PALocation> spawns = new HashSet<>();

        if (arena.getConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            spawns.addAll(SpawnManager.getSpawnsContaining(arena, team.getName() + aPlayer.getArenaClass().getName() + "spawn"));
        } else if (arena.isFreeForAll()) {
            spawns.addAll(SpawnManager.getSpawnsStartingWith(arena, "spawn"));
        } else {
            spawns.addAll(SpawnManager.getSpawnsStartingWith(arena, team.getName() + "spawn"));
        }

        for (final PALocation loc : spawns) {
            if (loc.getDistanceSquared(new PALocation(player.getLocation())) <= diff * diff) {
                debug(arena, player, "found near spawn: " + loc);
                return true;
            }
        }
        return false;
    }

    public static void respawn(final Arena arena, final ArenaPlayer aPlayer, final String overrideSpawn) {

        if (arena == null) {
            PVPArena.getInstance().getLogger().warning("Arena is null for player " + aPlayer + " while respawning!");
            return;
        }

        // Trick to avoid death screen
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), () -> aPlayer.getPlayer().closeInventory(), 1);

        if (overrideSpawn == null) {


            if (arena.getConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)
                    && arena.isFreeForAll() == "free".equals(aPlayer.getArenaTeam().getName())) {

                // we want a class spawn and the arena is either not FFA or the player is in the FREE team

                final Set<PASpawn> spawns = SpawnManager.getPASpawnsStartingWith(arena, aPlayer.getArenaTeam().getName() + aPlayer.getArenaClass().getName() + "spawn");
                int pos = new Random().nextInt(spawns.size());
                for (final PASpawn spawn : spawns) {
                    if (--pos <= 0) {
                        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RespawnRunnable(arena, aPlayer, spawn.getName()), 2);
                        aPlayer.setStatus(PlayerStatus.FIGHT);
                        return;
                    }
                }

                return;
            }

            final Set<ArenaRegion> ars = arena.getRegionsByType(RegionType.SPAWN);
            if (!ars.isEmpty()) {
                final Set<ArenaPlayer> team = new HashSet<>();
                team.add(aPlayer);
                placeInsideSpawnRegions(arena, team, ars);

                return;
            }
            if (arena.isFreeForAll()) {


                if (!arena.getConfig().getBoolean(CFG.GENERAL_SMARTSPAWN)
                        || !"free".equals(aPlayer.getArenaTeam().getName())) {

                    // either we generally don't need smart spawning or the player is not in the "free" team ;)

                    // i.e. just put him randomly

                    final Set<PASpawn> spawns = new HashSet<>();

                    if (arena.getConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                        final String arenaClass = aPlayer.getArenaClass().getName();
                        spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, aPlayer.getArenaTeam().getName() + arenaClass + "spawn"));
                    } else if ("free".equals(aPlayer.getArenaTeam().getName())) {
                        spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, "spawn"));
                    } else {
                        spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, aPlayer.getArenaTeam().getName()));
                    }


                    int pos = new Random().nextInt(spawns.size());
                    for (final PASpawn spawn : spawns) {
                        if (--pos <= 0) {
                            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RespawnRunnable(arena, aPlayer, spawn.getName()), 2);
                            aPlayer.setStatus(PlayerStatus.FIGHT);
                            return;
                        }
                    }
                }

                // we need a smart spawn

                debug(arena, "we need smart!");

                final Set<PASpawn> spawns = SpawnManager.getPASpawnsStartingWith(arena, "spawn");

                final Set<PALocation> pLocs = new HashSet<>();

                for (final ArenaPlayer arenaPlayer : aPlayer.getArenaTeam().getTeamMembers()) {
                    if (arenaPlayer.getName().equals(aPlayer.getName())) {
                        continue;
                    }
                    pLocs.add(new PALocation(arenaPlayer.getPlayer().getLocation()));
                    debug(arena, "pos of " + arenaPlayer.getName() + new PALocation(arenaPlayer.getPlayer().getLocation()));
                }
                debug(arena, "pLocs.size: " + pLocs.size());

                // pLocs now contains the other player's positions

                final Map<PALocation, Double> diffs = new HashMap<>();

                double max = 0;

                for (final PASpawn spawnLoc : spawns) {
                    double sum = 90000;
                    for (final PALocation playerLoc : pLocs) {
                        if (spawnLoc.getLocation().getWorldName().equals(playerLoc.getWorldName())) {
                            sum = Math.min(sum, spawnLoc.getLocation().getDistanceSquared(playerLoc));
                        }
                    }
                    max = Math.max(sum, max);
                    diffs.put(spawnLoc.getLocation(), sum);
                    debug(arena, "spawnLoc: " + spawnLoc.getName() + ':' + sum);
                }
                debug(arena, "max = " + max);

                for (final Map.Entry<PALocation, Double> paLocationDoubleEntry : diffs.entrySet()) {
                    if (paLocationDoubleEntry.getValue() == max) {
                        for (final PASpawn spawn : spawns) {
                            if (spawn.getLocation().equals(paLocationDoubleEntry.getKey())) {
                                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RespawnRunnable(arena, aPlayer, spawn.getName()), 2);
                                return;
                            }
                        }

                    }
                }

                return;
            }

            final Set<PASpawn> spawns = SpawnManager.getPASpawnsStartingWith(arena, aPlayer.getArenaTeam().getName() + "spawn");
            int pos = new Random().nextInt(spawns.size());
            for (final PASpawn spawn : spawns) {
                if (--pos <= 0) {
                    Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RespawnRunnable(arena, aPlayer, spawn.getName()), 2);
                    aPlayer.setStatus(PlayerStatus.FIGHT);
                    return;
                }
            }

        }

        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RespawnRunnable(arena, aPlayer, overrideSpawn), 2);
        if (overrideSpawn == null || !overrideSpawn.toLowerCase().endsWith("relay")) {
            aPlayer.setStatus(PlayerStatus.FIGHT);
        }
    }

    /**
     * set an arena coord to a given block
     *
     * @param loc   the location to save
     * @param place the coord name to save the location to
     */
    public static void setBlock(final Arena arena, final PABlockLocation loc, final String place) {
        // "x,y,z,yaw,pitch"

        final String spawnName = Config.parseToString(loc);
        debug(arena, "setting spawn " + place + " to " + spawnName);
        arena.getConfig().setManually("spawns." + place, spawnName);
        arena.getConfig().save();
        arena.addBlock(new PABlock(loc, place));
    }

    public static void loadSpawns(final Arena arena, final Config cfg) {
        final Set<String> spawns = cfg.getKeys("spawns");
        if (spawns == null) {
            return;
        }

        for (final String name : spawns) {
            final String value = (String) cfg.getUnsafe("spawns." + name);
            final String[] parts = value.split(",");

            if (parts.length != 4 && parts.length != 6) {
                throw new IllegalArgumentException(
                        "Input string must contain world, x, y, and z: " + name);
            }

            if (parts.length == 4) {
                // PABlockLocation
                arena.addBlock(new PABlock(Config.parseBlockLocation(value), name));
            } else {
                // PALocation
                arena.addSpawn(new PASpawn(Config.parseLocation(value), name));
            }
        }

    }
}
