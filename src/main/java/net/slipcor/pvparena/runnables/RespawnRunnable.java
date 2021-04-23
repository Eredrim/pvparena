package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import org.apache.commons.lang.Validate;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import static net.slipcor.pvparena.config.Debugger.debug;

public class RespawnRunnable extends BukkitRunnable {

    private final Arena arena;
    private final ArenaPlayer player;
    private final String spawnName;

    public RespawnRunnable(@NotNull Arena arena, @NotNull ArenaPlayer player, @NotNull String spawnName) {
        Validate.notNull(arena, "Arena cannot be null!");
        debug(player, "RespawnRunnable constructor to spawn {}", spawnName);
        this.arena = arena;
        this.player = player;
        this.spawnName = spawnName;
    }

    @Override
    public void run() {
        if (this.player.getArenaTeam() == null) {
            PVPArena.getInstance().getLogger().warning("Player team null! Couldn't respawn player.");
            return;
        }
        debug(this.arena, "respawning {} to {}",  this.player.getName(), this.spawnName);
        this.arena.tpPlayerToCoordName(this.player, this.spawnName);
    }

}
