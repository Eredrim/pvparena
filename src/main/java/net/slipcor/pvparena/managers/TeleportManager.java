package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.ArrowHack;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

import static net.slipcor.pvparena.classes.PASpawn.OLD;
import static net.slipcor.pvparena.classes.PASpawn.SPECTATOR;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.modules.StandardLounge.LOUNGE;

/**
 * Manage Teleportation
 */
public final class TeleportManager {

    private TeleportManager() {
    }

    /**
     * Teleport player to a random spawn
     *
     * @param arena       arena
     * @param arenaPlayer player to tp
     * @param spawn       spawns
     * @return the spawn where player have been teleported
     */
    public static PASpawn teleportPlayerToSpawn(Arena arena, ArenaPlayer arenaPlayer, PASpawn spawn) {
        debug(arenaPlayer, "teleport player to spawn {}", PASpawn.serialize(spawn));
        PASpawn destination;
        destination = prepareTeleportation(arena, arenaPlayer, spawn);
        teleportPlayer(arena, arenaPlayer, destination);
        executePostTeleportationFixes(arena, arenaPlayer);

        return destination;
    }

    /**
     * Teleport player to a random spawn
     *
     * @param arena       arena
     * @param arenaPlayer player to tp
     * @param spawns      list of all available spawns
     * @return the spawn where player have been teleported
     */
    public static PASpawn teleportPlayerToRandomSpawn(Arena arena, ArenaPlayer arenaPlayer, Collection<PASpawn> spawns) {
        debug(arenaPlayer, "teleport player to a random spawn ({} spawns available)", spawns.size());
        PASpawn destination = RandomUtils.getRandom(spawns, new Random());
        if (destination == null) {
            PVPArena.getInstance().getLogger().severe(String.format("No spawn found to teleport player %s", arenaPlayer.getName()));
            return null;
        }
        prepareTeleportation(arena, arenaPlayer, destination);
        teleportPlayer(arena, arenaPlayer, destination);
        executePostTeleportationFixes(arena, arenaPlayer);

        return destination;
    }

    /**
     * teleport a given player to the given coord string
     *
     * @param arenaPlayer the player to teleport
     * @param spawns      available spawns
     * @return the spawn where player have been teleported
     */
    public static PASpawn teleportPlayerToSpawnForJoin(Arena arena, final ArenaPlayer arenaPlayer,
                                                       final Set<PASpawn> spawns, boolean async) {

        debug(arenaPlayer, "teleport player for join ({} spawns available)", spawns.size());
        PASpawn destination = RandomUtils.getRandom(spawns, new Random());
        if (destination == null) {
            PVPArena.getInstance().getLogger().severe(String.format("No spawn found to teleport player %s", arenaPlayer.getName()));
            return null;
        }
        prepareTeleportation(arena, arenaPlayer, destination);
        int delay = async ? 2 : 0;
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
            teleportPlayer(arena, arenaPlayer, destination);
            arena.getScoreboard().setupPlayer(arenaPlayer);
            executePostTeleportationFixes(arena, arenaPlayer);
        }, delay);
        return destination;
    }

    private static PASpawn prepareTeleportation(Arena arena, ArenaPlayer arenaPlayer, PASpawn spawn) {
        Player player = arenaPlayer.getPlayer();
        debug(player, "prepare teleport of player to {}", PASpawn.serialize(spawn));

        if (player.isInsideVehicle()) {
            player.getVehicle().eject();
        }

        // spectator or spectator1 or spectator2
        if (spawn.getName().startsWith(SPECTATOR)) {
            // @TODO Player status must be handled by goal/module. Not here ...
            if (arena.getFighters().contains(arenaPlayer)) {
                arenaPlayer.setStatus(PlayerStatus.LOST);
            } else {
                arenaPlayer.setStatus(PlayerStatus.WATCH);
            }
        }

        // Check if spawn is inside a battle region (if player status == fight)
        // to ensure player will not be kicked right after teleport :')
        if (PlayerStatus.FIGHT.equals(arenaPlayer.getStatus())
                && CollectionUtils.isNotEmpty(arena.getRegionsByType(RegionType.BATTLE))
                && arena.getRegionsByType(RegionType.BATTLE).stream()
                .noneMatch(arenaRegion -> arenaRegion.containsLocation(spawn.getPALocation()))
        ) {
            PVPArena.getInstance().getLogger().severe(String.format("Can't teleport player %s because spawnpoint %s is not in a battle region.", arenaPlayer.getName(), spawn.getName()));
        }

        debug("raw location: {}", spawn);

        spawn.setOffset(arena.getConfig().getOffset(spawn.getName()));
        debug("offset location: {}", spawn.getOffset());

        arenaPlayer.setTeleporting(true);
        arenaPlayer.setTelePass(true);
        return spawn;
    }

    private static void executePostTeleportationFixes(Arena arena, ArenaPlayer arenaPlayer) {
        // remove arrows in player body
        try {
            ArrowHack.processArrowHack(arenaPlayer.getPlayer());
        } catch (Exception ignored) {
        }

        // set player visible
        if (arena.getConfig().getBoolean(Config.CFG.USES_INVISIBILITYFIX)
                && Arrays.asList(PlayerStatus.FIGHT, PlayerStatus.LOUNGE).contains(arenaPlayer.getStatus())) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () ->
                            arena.getFighters()
                                    .forEach(ap -> ap.getPlayer()
                                            .showPlayer(PVPArena.getInstance(), arenaPlayer.getPlayer()))
                    , 5L);
        }

        // disable player fly
        if (!arena.getConfig().getBoolean(Config.CFG.PERMS_FLY)) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                arenaPlayer.getPlayer().setAllowFlight(false);
                arenaPlayer.getPlayer().setFlying(false);
            }, 5L);
        }
    }

    private static void teleportPlayer(Arena arena, final ArenaPlayer arenaPlayer, final PASpawn spawn) {

        debug(arenaPlayer, "teleporting player to {}", spawn.getPALocation().toLocation());
        Player player = arenaPlayer.getPlayer();
        player.teleport(spawn.getPALocation().toLocation());
        ArenaModuleManager.teleportPlayer(arena, player, spawn);

        int noDamageTicks = arena.getConfig().getInt(Config.CFG.TIME_TELEPORTPROTECT) * 20;
        player.setNoDamageTicks(noDamageTicks);
        if (spawn.getName().contains(LOUNGE)) {
            debug(arena, "setting TelePass later!");
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                arenaPlayer.setTelePass(false);
                arenaPlayer.setTeleporting(false);
            }, noDamageTicks);

        } else {
            debug(arena, "setting TelePass now!");
            arenaPlayer.setTelePass(false);
            arenaPlayer.setTeleporting(false);
        }
    }


    public static void teleportPlayerAfterReset(Arena arena, final String destination, final boolean soft, final boolean force, final ArenaPlayer arenaPlayer) {
        final Player player = arenaPlayer.getPlayer();
        class RunLater implements Runnable {

            @Override
            public void run() {
                debug(arena, player, "string = " + destination);
                arenaPlayer.setTelePass(true);

                int noDamageTicks = arena.getConfig().getInt(Config.CFG.TIME_TELEPORTPROTECT) * 20;
                if (OLD.equalsIgnoreCase(destination)) {
                    debug(arena, player, "tping to old");
                    if (arenaPlayer.getSavedLocation() != null) {
                        debug(arena, player, "location is fine");
                        final PALocation loc = arenaPlayer.getSavedLocation();
                        player.teleport(loc.toLocation());
                        player.setNoDamageTicks(noDamageTicks);
                        arenaPlayer.setTeleporting(false);
                    }
                } else {
                    Vector offset = arena.getConfig().getOffset(destination);
                    PALocation loc = SpawnManager.getSpawnByExactName(arena, destination);
                    if (loc == null) {
                        new Exception("RESET Spawn null: " + arena.getName() + "->" + destination).printStackTrace();
                    } else {
                        player.teleport(loc.toLocation().add(offset));
                        arenaPlayer.setTelePass(false);
                        arenaPlayer.setTeleporting(false);
                    }
                    player.setNoDamageTicks(noDamageTicks);
                }
                if (soft || !force) {
                    StatisticsManager.update(arena, arenaPlayer);
                }
                if (!soft) {
                    arenaPlayer.setLocation(null);
                    arenaPlayer.clearFlyState();
                }
            }
        }

        final RunLater runLater = new RunLater();

        arenaPlayer.setTeleporting(true);
        if (arena.getConfig().getInt(Config.CFG.TIME_RESETDELAY) > 0 && !force) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), runLater,
                    arena.getConfig().getInt(Config.CFG.TIME_RESETDELAY) * 20L);
        } else {
            runLater.run();
        }
    }

}
