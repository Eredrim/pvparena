package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
import org.apache.commons.lang.Validate;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import static net.slipcor.pvparena.config.Debugger.debug;

public class RespawnRunnable extends BukkitRunnable {

    private final Arena arena;
    private final ArenaPlayer arenaPlayer;
    private final String spawnName;

    public RespawnRunnable(@NotNull Arena arena, @NotNull ArenaPlayer arenaPlayer, @NotNull String spawnName) {
        Validate.notNull(arena, "Arena cannot be null!");
        debug(arenaPlayer, "RespawnRunnable constructor to spawn {}", spawnName);
        this.arena = arena;
        this.arenaPlayer = arenaPlayer;
        this.spawnName = spawnName;
    }

    @Override
    public void run() {
        if (this.arenaPlayer.getArenaTeam() == null) {
            PVPArena.getInstance().getLogger().warning("Player team null! Couldn't respawn player.");
            return;
        }
        debug(this.arena, "respawning {} to {}", this.arenaPlayer.getName(), this.spawnName);
        TeleportManager.teleportPlayerToRandomSpawn(this.arena, this.arenaPlayer,
                SpawnManager.selectSpawnsForPlayer(this.arena, this.arenaPlayer, this.spawnName));
    }

}
