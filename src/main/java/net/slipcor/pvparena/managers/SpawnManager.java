package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.*;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.config.SpawnOffset;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.classes.PASpawn.OLD;
import static net.slipcor.pvparena.classes.PASpawn.FIGHT;
import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;
import static net.slipcor.pvparena.managers.TeamManager.FREE;

/**
 * <pre>Spawn Manager class</pre>
 * <p/>
 * Provides static methods to manage Spawns
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class SpawnManager {

    public static final String ROOT_SPAWNS_NODE = "spawns";
    public static final String ROOT_BLOCKS_NODE = "blocks";

    private SpawnManager() {
    }

    /**
     * Method used to spawn a new player during a match or to respawn a player
     * @param arena The arena which handle the game
     * @param arenaPlayer The player to spawn
     */
    public static void distributePlayer(Arena arena, ArenaPlayer arenaPlayer) {
        Set<ArenaRegion> arenaRegions = arena.getRegionsByType(RegionType.SPAWN);

        if (!arenaRegions.isEmpty()) {
            placeInsideSpawnRegions(arena, Collections.singleton(arenaPlayer), arenaRegions);

        } else if (arena.getConfig().getBoolean(CFG.GENERAL_SMART_SPAWN)) {
            spawnPlayerSmartly(arena, arenaPlayer, arenaPlayer.getArenaTeam());

        } else {
            quickSpawn(arena, Collections.singleton(arenaPlayer), arenaPlayer.getArenaTeam());
        }
    }

    public static void distributeTeams(Arena arena, ArenaTeam arenaTeam) {
        Set<ArenaRegion> arenaRegions = arena.getRegionsByType(RegionType.SPAWN);

        if (!arenaRegions.isEmpty()) {
            placeInsideSpawnRegions(arena, arenaTeam.getTeamMembers(), arenaRegions);

        } else if (arena.getConfig().getBoolean(CFG.GENERAL_QUICK_SPAWN)) {
            quickSpawn(arena, arenaTeam.getTeamMembers(), arenaTeam);

        } else if (arena.getConfig().getBoolean(CFG.GENERAL_SMART_SPAWN)) {
            distributeSmart(arena, arenaTeam.getTeamMembers(), arenaTeam);

        } else {
            distributeByOrder(arena, arenaTeam.getTeamMembers(), arenaTeam);
        }
    }

    private static void distributeByOrder(Arena arena,
                                          Set<ArenaPlayer> arenaPlayers, ArenaTeam arenaTeam) {
        debug(arena, "distributeByOrder: {}", arenaTeam);
        if (CollectionUtils.isEmpty(arenaPlayers)) {
            return;
        }

        Set<PASpawn> spawns = selectSpawnsForTeam(arena, arenaTeam, FIGHT);

        if (spawns.isEmpty()) {
            return;
        }

        class TeleportLater extends BukkitRunnable {
            private Set<ArenaPlayer> teamMembers = new HashSet<>();
            private final boolean classSpawn;
            private List<PASpawn> spawns = new ArrayList<>();
            private Map<String, List<PASpawn>> spawnsPerClass = new HashMap<>();
            private Integer lastSpawnIndex;
            private final Map<String, Integer> lastSpawnsPerClassIndex = new HashMap<>();

            TeleportLater(Set<ArenaPlayer> arenaPlayers) {
                this.teamMembers.addAll(arenaPlayers);
                this.classSpawn = arena.getConfig().getBoolean(CFG.GENERAL_SPAWN_PER_CLASS);

                // spawns sorted by spawnNameComparator
                if (this.classSpawn) {
                    this.spawnsPerClass = arena.getClasses().stream()
                            .map(ArenaClass::getName)
                            .collect(Collectors.toMap(
                                    arenaClassName -> arenaClassName,
                                    arenaClassName -> SpawnManager
                                            .getPASpawnsStartingWith(arena, FIGHT, arenaTeam.getName(), arenaClassName)
                                            .stream().sorted(spawnNameComparator())
                                            .collect(Collectors.toList())
                            ));
                } else {
                    this.spawns = selectSpawnsForTeam(arena, arenaTeam, FIGHT).stream()
                            .sorted(spawnNameComparator())
                            .collect(Collectors.toList());
                }
            }

            private Comparator<PASpawn> spawnNameComparator() {
                return (spawn1, spawn2) -> spawn1.getName().compareToIgnoreCase(spawn2.getName());
            }

            @Override
            public void run() {
                this.teamMembers.forEach(arenaPlayer -> {
                    arenaPlayer.setStatus(PlayerStatus.FIGHT);
                    final List<PASpawn> spawnsForPlayerClass = this.spawnsPerClass.get(arenaPlayer.getArenaClass().getName());
                    Integer lastSpawnsForPlayerClassIndex = this.lastSpawnsPerClassIndex.get(arenaPlayer.getArenaClass().getName());
                    if (this.classSpawn && CollectionUtils.isNotEmpty(spawnsForPlayerClass)) {
                        // reset index to collection start
                        if (lastSpawnsForPlayerClassIndex == null || lastSpawnsForPlayerClassIndex == spawnsForPlayerClass.size()) {
                            lastSpawnsForPlayerClassIndex = 0;
                        }
                        TeleportManager.teleportPlayerToSpawn(arena, arenaPlayer, spawnsForPlayerClass.get(lastSpawnsForPlayerClassIndex));
                        // spawn used, increment index.
                        this.lastSpawnsPerClassIndex.compute(arenaPlayer.getArenaClass().getName(),
                                (className, index) -> (index == null) ? 1 : index++);
                    } else if (CollectionUtils.isNotEmpty(this.spawns)) {
                        // reset index to collection start
                        if (this.lastSpawnIndex == null || this.lastSpawnIndex == this.spawns.size()) {
                            this.lastSpawnIndex = 0;
                        }
                        TeleportManager.teleportPlayerToSpawn(arena, arenaPlayer, this.spawns.get(this.lastSpawnIndex));
                        // spawn used, increment index.
                        this.lastSpawnIndex++;
                    } else {
                        PVPArena.getInstance().getLogger().severe("Not enough spawns to distribute players !");
                    }
                });
            }

        }
        new TeleportLater(arenaPlayers).runTaskLater(PVPArena.getInstance(), 1L);
    }

    /**
     * Spread players on spawn points in a balanced way
     *
     * @param arena        arena
     * @param arenaPlayers players
     * @param arenaTeam    team
     */
    public static void distributeSmart(Arena arena, Set<ArenaPlayer> arenaPlayers, ArenaTeam arenaTeam) {
        debug(arena, "distributing smart-ish");
        if (CollectionUtils.isEmpty(arenaPlayers)) {
            return;
        }

        Set<PASpawn> locations = selectSpawnsForTeam(arena, arenaTeam, FIGHT);
        Set<PASpawn> totalLocations = new HashSet<>(locations);

        if (locations.isEmpty()) {
            PVPArena.getInstance().getLogger().severe("Not enough spawns to smart distribute players !");
            return;
        }

        PASpawn[] iterations = new PASpawn[locations.size()];

        for (int i = 0; i < totalLocations.size(); i++) {
            if (i == 0) {
                PASpawn firstSpawn = locations.iterator().next();
                iterations[i] = firstSpawn;
                locations.remove(firstSpawn);
                continue;
            }
            PASpawn farSpawn = calculateFarSpawn(iterations, locations, totalLocations);
            iterations[i] = farSpawn;
            locations.remove(farSpawn);
        }

        class TeleportLater extends BukkitRunnable {
            private int index;
            private final PASpawn[] spawns;
            private Set<ArenaPlayer> arenaPlayers = new HashSet<>();

            TeleportLater(Set<ArenaPlayer> arenaPlayers, final PASpawn[] spawns) {
                this.index = 0;
                this.arenaPlayers = arenaPlayers;
                this.spawns = spawns.clone();
            }

            @Override
            public void run() {
                this.arenaPlayers.forEach(arenaPlayer -> {
                    final PASpawn spawn = this.spawns[this.index++ % this.spawns.length];
                    if (spawn == null) {
                        PVPArena.getInstance().getLogger().warning(String.format("Element #%s is null: [%s]",
                                this.index, StringParser.joinArray(this.spawns, ",")));
                    } else {
                        arenaPlayer.setStatus(PlayerStatus.FIGHT);
                        TeleportManager.teleportPlayerToSpawn(arena, arenaPlayer, spawn);
                    }
                });
            }

        }

        new TeleportLater(arenaPlayers, iterations).runTaskLater(PVPArena.getInstance(), 1L);
    }

    /**
     * Spawn a player as far as possible from other players of its team.
     * For FFA, from other players of the free team (i.e. everyone)
     * @param arena The applicable arena
     * @param arenaPlayer The player to (re)spawn
     * @param arenaTeam The player team to use
     */
    public static void spawnPlayerSmartly(Arena arena, ArenaPlayer arenaPlayer, ArenaTeam arenaTeam) {
        debug(arena, "(re)spawn smart-ish");

        Set<PASpawn> availableSpawns = selectSpawnsForTeam(arena, arenaTeam, FIGHT);
        List<PALocation> playersLocation = arenaTeam.getTeamMembers().stream()
                .filter(ap -> !ap.equals(arenaPlayer))
                .filter(ap -> ap.getStatus() == PlayerStatus.FIGHT)
                .map(ArenaPlayer::getLocation)
                .collect(Collectors.toList());


        double xSum = 0;
        double ySum = 0;
        double zSum = 0;
        int posNumber = playersLocation.size();

        for(PALocation loc : playersLocation) {
            xSum += loc.getX();
            ySum += loc.getY();
            zSum += loc.getZ();
        }

        PALocation otherPlayersCentroid = new PALocation(new Location(arena.getWorld(), xSum / posNumber, ySum / posNumber, zSum / posNumber));

        PASpawn fartherSpawn = availableSpawns.stream()
                .max(Comparator.comparingDouble(paSpawn -> paSpawn.getPALocation().getDistanceSquared(otherPlayersCentroid)))
                .get();

        class TeleportLater extends BukkitRunnable {
            private final PASpawn spawn;
            private final ArenaPlayer arenaPlayer;

            TeleportLater(ArenaPlayer arenaPlayer, PASpawn spawn) {
                this.arenaPlayer = arenaPlayer;
                this.spawn = spawn;
            }

            @Override
            public void run() {
                this.arenaPlayer.setStatus(PlayerStatus.FIGHT);
                TeleportManager.teleportPlayerToSpawn(arena, this.arenaPlayer, this.spawn);
            }

        }

        new TeleportLater(arenaPlayer, fartherSpawn).runTaskLater(PVPArena.getInstance(), 1L);
    }

    /**
     * Get spawn as far as possible of others spawns already taken
     *
     * @param takenSpawns     spawn already taken
     * @param availableSpawns available spawns
     * @param totalSpawns     all spawns
     * @return the far spawn (not already taken of course)
     */
    private static PASpawn calculateFarSpawn(PASpawn[] takenSpawns,
                                             Set<PASpawn> availableSpawns,
                                             Set<PASpawn> totalSpawns) {
        trace("--------------------");
        trace("calculating a spawn!");
        trace("--------------------");
        PASpawn far = availableSpawns.iterator().next();

        trace("last resort: {}", far);

        double diff = 0;
        for (PASpawn spawn : availableSpawns) {
            trace("> checking {}", spawn.getName());
            double tempDiff = 0;
            for (int index = 0; index < takenSpawns.length && takenSpawns[index] != null; index++) {
                for (PASpawn totalSpawn : totalSpawns) {
                    for (PASpawn availableSpawn : availableSpawns) {
                        if (totalSpawn.equals(takenSpawns[index]) && availableSpawn.equals(spawn)) {
                            tempDiff += totalSpawn.getPALocation().getDistanceSquared(availableSpawn.getPALocation());
                            trace(">> tempDiff: {}", tempDiff);
                        }
                    }
                }
            }

            if (tempDiff > diff) {
                trace("-> diff");
                diff = tempDiff;
                far = spawn;
            }
        }

        return far;
    }

    /**
     * Spawn all players at the same time.
     *
     * @param arena        arena
     * @param arenaPlayers team's members
     * @param arenaTeam    team
     */
    private static void quickSpawn(Arena arena, Set<ArenaPlayer> arenaPlayers, ArenaTeam arenaTeam) {
        class TeleportLater extends BukkitRunnable {
            private Set<ArenaPlayer> teamMembers = new HashSet<>();
            private final boolean classSpawn;
            private Set<PASpawn> spawns = new HashSet<>();
            private Map<String, Set<PASpawn>> spawnsPerClass = new HashMap<>();

            TeleportLater(Set<ArenaPlayer> arenaPlayers, ArenaTeam arenaTeam) {
                this.teamMembers.addAll(arenaPlayers);
                this.classSpawn = arena.getConfig().getBoolean(CFG.GENERAL_SPAWN_PER_CLASS);

                if (this.classSpawn) {
                    this.spawnsPerClass = arena.getClasses().stream()
                            .map(ArenaClass::getName)
                            .collect(Collectors.toMap(
                                    arenaClassName -> arenaClassName,
                                    arenaClassName -> SpawnManager.getPASpawnsStartingWith(arena, FIGHT, arenaTeam.getName(), arenaClassName)));
                } else {
                    this.spawns = selectSpawnsForTeam(arena, arenaTeam, FIGHT);
                }
            }

            @Override
            public void run() {
                this.teamMembers.forEach(arenaPlayer -> {
                    arenaPlayer.setStatus(PlayerStatus.FIGHT);
                    if (this.classSpawn &&
                            CollectionUtils.isNotEmpty(this.spawnsPerClass.get(arenaPlayer.getArenaClass().getName()))) {
                        TeleportManager.teleportPlayerToRandomSpawn(arena, arenaPlayer, this.spawnsPerClass.get(arenaPlayer.getArenaClass().getName()));
                    } else {
                        TeleportManager.teleportPlayerToRandomSpawn(arena, arenaPlayer, this.spawns);
                    }
                });
            }

        }
        new TeleportLater(arenaPlayers, arenaTeam).runTaskLater(PVPArena.getInstance(), 1L);
    }

    /**
     * Select spawns by name, team and per class if config enabled
     *
     * @param arena     arena
     * @param team      team
     * @param spawnName spawn name
     * @return spawns matching criteria
     */
    public static Set<PASpawn> selectSpawnsForTeam(Arena arena, ArenaTeam team, String spawnName) {
        Set<PASpawn> spawns = new HashSet<>();

        if (arena.isFreeForAll()) {
            if (FREE.equals(team.getName())) {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, spawnName));
            } else {
                // Special spawns for fake team goals (like infect or tank)
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName()));
            }
        } else {
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, spawnName, team.getName()));
        }
        return spawns;
    }

    /**
     * Select spawns by name, team and per class if config enabled
     *
     * @param arena       arena
     * @param arenaPlayer arenaPlayer
     * @param spawnName   spawn name
     * @return spawns matching criteria
     */
    public static Set<PASpawn> selectSpawnsForPlayer(Arena arena, ArenaPlayer arenaPlayer, String spawnName) {
        Set<PASpawn> spawns = new HashSet<>();

        if (arena.getConfig().getBoolean(CFG.GENERAL_SPAWN_PER_CLASS)) {
            final String arenaClass = arenaPlayer.getArenaClass().getName();
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, spawnName, arenaPlayer.getArenaTeam().getName(), arenaClass));

        } else if (arena.isFreeForAll()) {
            if (FREE.equals(arenaPlayer.getArenaTeam().getName())) {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, spawnName));
            } else {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, spawnName, arenaPlayer.getArenaTeam().getName()));
            }
        } else {
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, spawnName, arenaPlayer.getArenaTeam().getName()));
        }
        return spawns;
    }

    public static PABlockLocation getBlockNearest(Set<PABlockLocation> locs,
                                                  PABlockLocation location) {
        PABlockLocation result = null;

        for (PABlockLocation loc : locs) {
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

    public static Set<PABlockLocation> getBlocksStartingWith(@NotNull Arena arena, @NotNull String name, String teamName) {
        return arena.getBlocks().stream()
                .filter(block -> block.getName().startsWith(name) && Objects.equals(block.getTeamName(), teamName))
                .map(PABlock::getLocation)
                .collect(Collectors.toSet());
    }

    public static Set<PABlock> getPABlocksStartingWith(@NotNull Arena arena, @NotNull String name, String teamName) {
        return arena.getBlocks().stream()
                .filter(block -> block.getName().startsWith(name) && Objects.equals(block.getTeamName(), teamName))
                .collect(Collectors.toSet());
    }

    public static Set<PABlockLocation> getBlocksContaining(@NotNull Arena arena, @NotNull String name) {
        return getPABlocksContaining(arena, name).stream()
                .map(PABlock::getLocation)
                .collect(Collectors.toSet());
    }

    public static Set<PABlock> getPABlocksContaining(Arena arena, String name) {
        return arena.getBlocks().stream()
                .filter(block -> block.getName().startsWith(name))
                .collect(Collectors.toSet());
    }

    public static Set<PALocation> getSpawnsContaining(Arena arena, String name) {
        Set<PALocation> result = new HashSet<>();

        for (PASpawn spawn : arena.getSpawns()) {
            if (spawn.getName().contains(name)) {
                result.add(spawn.getPALocation());
            }
        }

        return result;
    }

    public static Set<PALocation> getSpawnsLocationStartingWith(Arena arena, String name) {
        return arena.getSpawns().stream()
                .filter(paSpawn -> paSpawn.getName().startsWith(name))
                .map(PASpawn::getPALocation)
                .collect(Collectors.toSet());
    }

    public static Set<PASpawn> getPASpawnsStartingWith(Arena arena, String name, String team) {
        return arena.getSpawns().stream()
                .filter(paSpawn -> paSpawn.getName().startsWith(name)
                        && paSpawn.getTeamName().equals(team)
                )
                .collect(Collectors.toSet());
    }

    public static Set<PASpawn> getPASpawnsStartingWith(Arena arena, String name, String team, String className) {
        return arena.getSpawns().stream()
                .filter(paSpawn -> paSpawn.getName().startsWith(name)
                        && paSpawn.getTeamName().equals(team)
                        && paSpawn.getClassName().equals(className))
                .collect(Collectors.toSet());
    }

    public static Set<PASpawn> getPASpawnsStartingWith(Arena arena, String name) {
        return arena.getSpawns().stream()
                .filter(paSpawn -> paSpawn.getName().startsWith(name))
                .collect(Collectors.toSet());
    }

    public static PABlockLocation getBlockByExactName(Arena arena, String name) {
        for (PABlock spawn : arena.getBlocks()) {
            if (spawn.getName().equals(name)) {
                return spawn.getLocation();
            }
        }
        return null;
    }

    public static PABlockLocation getBlockByExactName(Arena arena, String name, String teamName) {
        return arena.getBlocks().stream()
                .filter(block -> block.getName().equalsIgnoreCase(name)
                        && block.getTeamName().equalsIgnoreCase(teamName))
                .findAny()
                .map(PABlock::getLocation)
                .orElse(null);
    }

    public static PALocation getSpawnByExactName(Arena arena, String name) {
        return arena.getSpawns().stream()
                .filter(spawn -> spawn.getName().equals(name))
                .findAny()
                .map(PASpawn::getPALocationWithOffset)
                .orElse(null);
    }

    public static PASpawn getPASpawnByExactName(Arena arena, String name) {
        return arena.getSpawns().stream()
                .filter(spawn -> spawn.getName().equals(name))
                .findAny()
                .orElse(null);
    }

    public static PALocation getSpawnByExactName(Arena arena, String name, String teamName, String className) {
        return arena.getSpawns().stream()
                .filter(spawn -> StringUtils.equalsIgnoreCase(spawn.getName(), name)
                        && StringUtils.equalsIgnoreCase(spawn.getTeamName(), teamName)
                        && StringUtils.equalsIgnoreCase(spawn.getClassName(), className))
                .findAny()
                .map(PASpawn::getPALocationWithOffset)
                .orElse(null);
    }

    /**
     * Return location of Arena exit or World spawn as fallback
     *
     * @param arena Arena containing exit spawn
     * @return Bukkit location object of the exit
     */
    public static Location getExitSpawnLocation(Arena arena) {
        return ofNullable(getSpawnByExactName(arena, "exit"))
                .map(PALocation::toLocation)
                .orElse(arena.getWorld().getSpawnLocation());
    }

    public static PABlockLocation getRegionCenter(Arena arena) {

        ArenaRegion arenaRegion = arena.getRegionsByType(RegionType.BATTLE).stream().findFirst().orElse(null);

        if (arenaRegion == null) {
            return new PABlockLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        final World world = Bukkit.getWorld(arenaRegion.getWorldName());

        if (world == null) {
            return new PABlockLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        Set<PALocation> locs = new HashSet<>(getSpawnsContaining(arena, FIGHT));

        long x = 0;
        long y = 0;
        long z = 0;

        for (PALocation loc : locs) {
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

    public static String[] parseSpawnNameFromConfig(Arena arena, String spawnNode) throws GameplayException {
        String[] args = spawnNode.split("_");
        String teamName = null;
        String className;
        String spawnName;

        if (arena.getTeam(args[0]) == null) {
            spawnName = args[0];
            className = parseSpawnClassNameArg(arena, args, 1);
        } else {
            teamName = args[0];
            spawnName = args[1];
            className = parseSpawnClassNameArg(arena, args, 2);
        }

        return new String[]{spawnName, teamName, className};
    }

    public static String[] parseSpawnNameArgs(Arena arena, String[] args) throws GameplayException {
        String teamName = null;
        String className;
        String spawnName = args[0];

        if (args.length < 2 || arena.getTeam(args[1]) == null) {
            className = parseSpawnClassNameArg(arena, args, 1);
        } else {
            teamName = args[1];
            className = parseSpawnClassNameArg(arena, args, 2);
        }

        return new String[]{spawnName, teamName, className};
    }

    private static String parseSpawnClassNameArg(Arena arena, String[] args, int offset) throws GameplayException {
        if(args.length > offset) {
            String candidate = args[offset];
            if(arena.getArenaClass(candidate) == null) {
                throw new GameplayException(Language.parse(Language.MSG.ERROR_CLASS_NOT_FOUND, candidate));
            }
            return candidate;
        }
        return null;
    }

    private static void placeInsideSpawnRegion(Arena arena, ArenaPlayer arenaPlayer, ArenaRegion region) {
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
            loc.setY(loc.toLocation().getWorld().getHighestBlockYAt(x, z) + 1);
            attempt++;
            found = region.getShape().contains(loc);

        }

        final PABlockLocation newLoc = loc;

        class RunLater implements Runnable {
            @Override
            public void run() {
                final PALocation temp = arenaPlayer.getSavedLocation();

                Location bLoc = newLoc.toLocation();
                SpawnOffset spawnOffset = PVPArena.getInstance().getSpawnOffset();
                bLoc = bLoc.add(spawnOffset.toVector());

                while (bLoc.getBlock().getType() != Material.AIR
                        && bLoc.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR
                        && bLoc.getBlock().getRelative(BlockFace.UP, 2).getType() != Material.AIR) {
                    bLoc = bLoc.add(0, 1, 0);
                }

                debug(arena, "bLoc: " + bLoc.toString());
                arenaPlayer.setLocation(new PALocation(bLoc));

                arenaPlayer.setStatus(PlayerStatus.FIGHT);

                TeleportManager.teleportPlayerToRandomSpawn(arena, arenaPlayer,
                        Collections.singleton(new PASpawn(arenaPlayer.getSavedLocation(), OLD, null, null)));
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                    arenaPlayer.setLocation(temp);
                    debug(arena, "temp: " + temp.toString());
                }, 6L);

            }

        }

        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 1L);

    }

    private static void placeInsideSpawnRegions(Arena arena, Set<ArenaPlayer> set, Set<ArenaRegion> ars) {
        if (arena.isFreeForAll()) {
            for (ArenaPlayer ap : set) {
                ArenaRegion randomRegion = RandomUtils.getRandom(ars, new Random());
                placeInsideSpawnRegion(arena, ap, randomRegion);
            }
        } else {
            String teamName = null;
            for (ArenaPlayer ap : set) {
                if (teamName == null) {
                    teamName = ap.getArenaTeam().getName();
                }
                boolean teleported = false;
                for (ArenaRegion x : ars) {
                    if (x.getRegionName().contains(teamName)) {
                        placeInsideSpawnRegion(arena, ap, x);
                        teleported = true;
                        break;
                    }
                }
                if (!teleported) {
                    ArenaRegion randomRegion = RandomUtils.getRandom(ars, new Random());
                    placeInsideSpawnRegion(arena, ap, randomRegion);
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
    public static boolean isNearSpawn(Arena arena, final Player player, final int diff) {

        debug(arena, player, "checking if arena is near a spawn");
        if (!arena.hasPlayer(player)) {
            return false;
        }
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (team == null) {
            return false;
        }

        Set<PALocation> spawns = new HashSet<>();

        if (arena.getConfig().getBoolean(CFG.GENERAL_SPAWN_PER_CLASS)) {
            spawns.addAll(SpawnManager.getSpawnsContaining(arena, team.getName() + aPlayer.getArenaClass().getName() + FIGHT));
        } else if (arena.isFreeForAll()) {
            spawns.addAll(SpawnManager.getSpawnsLocationStartingWith(arena, FIGHT));
        } else {
            spawns.addAll(SpawnManager.getSpawnsLocationStartingWith(arena, team.getName() + FIGHT));
        }

        for (PALocation loc : spawns) {
            if (loc.getDistanceSquared(new PALocation(player.getLocation())) <= diff * diff) {
                debug(arena, player, "found near spawn: " + loc);
                return true;
            }
        }
        return false;
    }

    public static void respawn(ArenaPlayer arenaPlayer, String overrideSpawn) {
        final Arena arena = arenaPlayer.getArena();

        if (arena == null) {
            PVPArena.getInstance().getLogger().warning("Arena is null for player " + arenaPlayer + " while respawning!");
            return;
        }

        if (overrideSpawn == null) {
            distributePlayer(arena, arenaPlayer);
        } else {
            if (!overrideSpawn.toLowerCase().endsWith("relay")) {
                arenaPlayer.setStatus(PlayerStatus.FIGHT);
            }
            TeleportManager.teleportPlayerToRandomSpawn(arena, arenaPlayer, selectSpawnsForPlayer(arena, arenaPlayer, overrideSpawn));
        }
    }

    /**
     * set an arena coord to a given block
     *
     * @param loc       the location to save
     * @param blockName the name of the block
     * @param teamName  the team name blocks. Set GLOBAL is none
     */
    public static void setBlock(Arena arena, PABlockLocation loc, String blockName, String teamName) {
        // "x,y,z,yaw,pitch"

        final String location = Config.parseToString(loc);
        final PABlock paBlock = new PABlock(loc, blockName, teamName);
        debug(arena, "setting block " + paBlock.getFullName() + " to " + location);
        arena.addBlock(paBlock);
        arena.getConfig().addBlock(paBlock);
    }

    public static void removeBlock(Arena arena, PABlock paBlock) {
        debug(arena, "removing block " + paBlock.getFullName() + " to " + paBlock.getLocation());
        arena.removeBlock(paBlock);
        arena.getConfig().clearBlock(paBlock);
    }

    public static void loadSpawns(Arena arena, Config cfg) {

        if (cfg.getConfigurationSection(ROOT_SPAWNS_NODE) == null) {
            return;
        }

        Set<PASpawn> spawns = new HashSet<>();
        for (String spawnNode : cfg.getKeys(ROOT_SPAWNS_NODE)) {
            loadSpawnNode(arena, cfg, spawns, spawnNode);
        }
        arena.setSpawns(spawns);

    }

    private static void loadSpawnNode(Arena arena, Config cfg, Set<PASpawn> spawns, String spawnNode) {
        String location = (String) cfg.getUnsafe(String.format("%s.%s", ROOT_SPAWNS_NODE, spawnNode));
        Optional.ofNullable(PASpawn.deserialize(spawnNode, location, arena)).ifPresent(spawns::add);
    }

    public static String isSpawnsSetup(Arena arena) {

        final Config arenaConfig = arena.getConfig();
        // @TODO Why reload config here ? Arena should already have spawn in memory
        loadSpawns(arena, arenaConfig);

        Set<PASpawn> spawns = arena.getSpawns();
        Set<PASpawn> missingSpawns = new HashSet<>();

        final CFG[] specialSpawns = new CFG[]{CFG.TP_WIN, CFG.TP_LOSE, CFG.TP_DEATH};
        // specials config with a spawn as value: require spawn set
        for (CFG specialSpawn : specialSpawns) {
            final String specialSpawnName = arenaConfig.getString(specialSpawn);
            if (!OLD.equals(specialSpawnName)) {
                missingSpawns.addAll(getMissingSpawns(spawns, specialSpawnName));
            }
        }

        // custom mods spawns
        missingSpawns.addAll(ArenaModuleManager.checkForMissingSpawns(arena, spawns));
        // custom goal spawns
        missingSpawns.addAll(arena.getGoal().checkForMissingSpawns(spawns));

        // display all missing spawns in one message
        if (CollectionUtils.isNotEmpty(missingSpawns)) {
            String message = missingSpawns.stream()
                    .sorted(Comparator.comparing(PASpawn::getName))
                    .map(PASpawn::getPrettyName)
                    .collect(Collectors.joining(", "));

            return Language.parse(Language.MSG.ERROR_MISSING_SPAWN, message);
        }
        return null;
    }

    public static String isBlocksSetup(Arena arena) {

        final Config arenaConfig = arena.getConfig();
        // @TODO Why reload config here ? Arena should already have blocks in memory
        loadBlocks(arena, arenaConfig);

        Set<PABlock> blocks = arena.getBlocks();
        Set<PABlock> missingBlocks = new HashSet<>();

        // custom mods spawns
        missingBlocks.addAll(ArenaModuleManager.checkForMissingBlocks(arena, blocks));
        // custom goal spawns
        missingBlocks.addAll(arena.getGoal().checkForMissingBlocks(blocks));

        // display all missing spawns in one message
        if (CollectionUtils.isNotEmpty(missingBlocks)) {
            String message = missingBlocks.stream()
                    .sorted(Comparator.comparing(PABlock::getName))
                    .map(PABlock::getPrettyName)
                    .collect(Collectors.joining(", "));
            return Language.parse(Language.MSG.ERROR_MISSING_BLOCK, message);
        }
        return null;
    }

    public static String areSpawnsInBattlefield(Arena arena) {
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.BATTLE);

        if(!regions.isEmpty()) {
            String errorMsg = arena.getSpawns().stream()
                    .filter(spawn -> StringUtils.startsWithIgnoreCase(spawn.getName(), FIGHT))
                    .filter(spawn -> regions.stream().noneMatch(rg -> rg.containsLocation(spawn.getPALocation())))
                    .map(PASpawn::getPrettyName)
                    .collect(Collectors.joining(", "));

            if(StringUtils.notBlank(errorMsg)) {
                return Language.parse(Language.MSG.ERROR_OUT_OF_BOUNDS_SPAWN, errorMsg);
            }
        }

        return null;
    }

    /**
     * Get missing spawn with name, empty otherwise
     *
     * @param name spawn name to check
     * @return missing spawn, empty otherwise
     */
    public static Set<PASpawn> getMissingSpawns(Set<PASpawn> spawns, final String name) {
        Set<PASpawn> missing = new HashSet<>();
        if (spawns.stream().noneMatch(spawn ->
                spawn.getName().startsWith(name) && spawn.getTeamName() == null)) {
            missing.add(new PASpawn(null, name, null, null));
        }
        return missing;
    }

    /**
     * check if necessary FFA spawns are set
     *
     * @return null if ready, error message otherwise
     */
    public static Set<PASpawn> getMissingFFASpawn(Arena arena, Set<PASpawn> spawns) {
        Set<PASpawn> missing = new HashSet<>();
        int minPlayers = arena.getConfig().getInt(CFG.READY_MINPLAYERS);
        for (int i = 1; i <= minPlayers; i++) {
            missing.addAll(getMissingSpawns(spawns, FIGHT + i));
        }
        return missing;
    }

    /**
     * check if necessary custom FFA spawns areList.of set
     * <p>
     * + check if a spawn start with custom
     *
     * @return null if ready, error message otherwise
     */
    public static Set<PASpawn> getMissingFFACustom(Set<PASpawn> spawns, final String custom) {
        Set<PASpawn> missing = new HashSet<>();
        if (spawns.stream().noneMatch(spawn ->
                (spawn.getName().equals(custom) || spawn.getName().startsWith(custom))
                        && spawn.getTeamName() == null)) {
            missing.add(new PASpawn(null, custom, null, null));
        }
        return missing;
    }

    /**
     * check if necessary team spawns are set
     *
     * @return empty if ready, team(s) missing otherwise
     */
    public static Set<PASpawn> getMissingTeamSpawn(Arena arena, Set<PASpawn> spawns) {
        return arena.getTeams().stream()
                .map(team -> new PASpawn(null, FIGHT, team.getName(), null))
                .filter(teamSpawn -> spawns.stream()
                        .noneMatch(spawn -> spawn.getName().startsWith(teamSpawn.getName())
                                && spawn.getTeamName().equals(teamSpawn.getTeamName())))
                .collect(Collectors.toSet());
    }

    /**
     * check if necessary custom team spawns are set
     * <p>
     * + check if a spawn start with team's name + custom
     *
     * @return empty if ready, team(s) missing otherwise
     */
    public static Set<PASpawn> getMissingTeamCustom(Arena arena, Set<PASpawn> spawns, final String custom) {
        return arena.getTeams().stream()
                .map(team -> new PASpawn(null, custom, team.getName(), null))
                .filter(teamSpawn -> spawns.stream()
                        .noneMatch(spawn -> spawn.getName().startsWith(teamSpawn.getName())
                                && spawn.getTeamName().equals(teamSpawn.getTeamName())))
                .collect(Collectors.toSet());
    }

    /**
     * check if necessary custom team blocks are set
     * <p>
     * + check if a spawn start with team's name + custom
     *
     * @return missing blocks
     */
    public static Set<PABlock> getMissingBlocksTeamCustom(Arena arena, Set<PABlock> blocks, final String custom) {
        return arena.getTeams().stream()
                .map(team -> new PABlock(null, custom, team.getName()))
                .filter(teamBlock -> blocks.stream()
                        .noneMatch(block ->
                                (block.getName().equals(teamBlock.getName())
                                        || block.getName().startsWith(custom))
                                        && block.getTeamName().equals(teamBlock.getTeamName())))
                .collect(Collectors.toSet());
    }

    /**
     * check if necessary custom team blocks are set
     * <p>
     * + check if a spawn start with team's name + custom
     *
     * @return missing blocks
     */
    public static Set<PABlock> getMissingBlocksCustom(Arena arena, Set<PABlock> blocks, final String custom) {
        Set<PABlock> missing = new HashSet<>();
        if (blocks.stream().noneMatch(block ->
                (block.getName().equals(custom) || block.getName().startsWith(custom))
                        && block.getTeamName() == null)) {
            missing.add(new PABlock(null, custom, null));
        }
        return missing;
    }

    public static void loadBlocks(Arena arena, Config cfg) {
        debug(arena, "loading blocks");
        if (cfg.getConfigurationSection(ROOT_BLOCKS_NODE) == null) {
            return;
        }

        Set<PABlock> spawns = new HashSet<>();
        for (String spawnNode : cfg.getKeys(ROOT_BLOCKS_NODE)) {
            loadBlockNode(arena, cfg, spawns, spawnNode);
        }
        arena.setBlocks(spawns);

    }

    private static void loadBlockNode(Arena arena, Config cfg, Set<PABlock> spawns, String blockNode) {
        final String location = (String) cfg.getUnsafe(String.format("%s.%s", ROOT_BLOCKS_NODE, blockNode));
        // format: (team_)<name>: world,x,y,z
        // tnt: event,3408,76,135
        // red_button: event,3459,62,104
        String[] blockArgs = blockNode.split("_");
        String arenaTeamName = null;
        String blockName = blockArgs[0];
        if (blockArgs.length > 1) {
            ArenaTeam arenaTeam = arena.getTeam(blockArgs[0]);
            blockName = blockArgs[1];
            arenaTeamName = ofNullable(arenaTeam).map(ArenaTeam::getName).orElse(blockArgs[0].toLowerCase());
        }
        try {
            spawns.add(new PABlock(Config.parseBlockLocation(location), blockName, arenaTeamName));
        } catch (IllegalArgumentException ex) {
            PVPArena.getInstance().getLogger().warning(String.format("Can't load %s's block %s.%s: %s",
                    arena.getName(), ROOT_BLOCKS_NODE, blockNode, ex.getMessage()));
        }
    }
}
