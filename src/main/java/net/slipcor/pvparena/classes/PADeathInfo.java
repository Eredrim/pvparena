package net.slipcor.pvparena.classes;

import net.slipcor.pvparena.arena.ArenaPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

public class PADeathInfo {
    private final EntityDamageEvent.DamageCause cause;
    private Entity damager;
    private Player killer;

    public PADeathInfo(EntityDamageEvent deathEvent) {
        this.cause = deathEvent.getCause();
        this.killer = ArenaPlayer.getLastDamagingPlayer(deathEvent);

        if(deathEvent instanceof EntityDamageByEntityEvent) {
            damager = ((EntityDamageByEntityEvent) deathEvent).getDamager();
        }
    }

    /**
     * Constructor for deaths directly triggered by the plugin
     * @param cause death cause
     */
    public PADeathInfo(EntityDamageEvent.DamageCause cause) {
        this.cause = cause;
    }

    public EntityDamageEvent.DamageCause getCause() {
        return this.cause;
    }

    @Nullable
    public Entity getDamager() {
        return this.damager;
    }

    @Nullable
    public Player getKiller() {
        return this.killer;
    }
}
