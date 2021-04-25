package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Runnable class "SpawnCamp"</pre>
 * <p/>
 * An arena timer to punish spawn campers
 *
 * @author slipcor
 * @version v0.9.8
 */

public class SpawnCampRunnable extends BukkitRunnable {
    private final Arena arena;
    private final int spawnCampDamage;
    private final Set<PALocation> spawns;

    /**
     * create a spawn camp runnable
     *
     * @param arena the arena we are running in
     */
    public SpawnCampRunnable(final Arena arena) {
        debug(arena, "SpawnCampRunnable constructor");
        this.arena = arena;
        this.spawnCampDamage = this.arena.getConfig().getInt(CFG.DAMAGE_SPAWNCAMP);
        this.spawns = this.arena.getTeams().stream()
                .filter(ArenaTeam::isNotEmpty)
                .flatMap(team -> {
                    if (this.arena.getConfig().getBoolean(CFG.GENERAL_SPAWN_PER_CLASS)) {
                        return SpawnManager.getSpawnsContaining(this.arena, "spawn").stream();
                    }
                    String spawnStartingWith = this.arena.isFreeForAll() ? "spawn" : team.getName() + "spawn";
                    return SpawnManager.getSpawnsLocationStartingWith(this.arena, spawnStartingWith).stream();
                })
                .collect(Collectors.toSet());
    }

    /**
     * the run method, commit arena end
     */
    @Override
    public void run() {
        debug(this.arena, "SpawnCampRunnable commiting");
        if (this.arena.isFightInProgress() && this.arena.getConfig().getBoolean(CFG.PROTECT_PUNISH)) {
            this.spawnCampPunish();
        } else {
            // deactivate the auto saving task
            this.cancel();
        }
    }

     /**
     * damage every actively fighting player for being near a spawn
     */
     private void spawnCampPunish() {
        this.spawns.forEach(spawnLoc ->
            this.arena.getFighters().stream()
                    .filter(ap -> ap.getStatus() == PlayerStatus.FIGHT)
                    .filter(ap -> spawnLoc.getDistanceSquared(ap.getLocation()) < 9)
                    .map(ArenaPlayer::getPlayer)
                    .forEach(player -> {
                        player.setLastDamageCause(new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, 1002));
                        player.damage(this.spawnCampDamage);
                    })
        );
    }
}
