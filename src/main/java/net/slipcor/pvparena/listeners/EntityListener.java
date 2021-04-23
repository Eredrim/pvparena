package net.slipcor.pvparena.listeners;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionFlag;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.StatisticsManager;
import net.slipcor.pvparena.runnables.DamageResetRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Entity Listener class
 * </pre>
 *
 * @author slipcor
 * @version v0.10.2
 */

public class EntityListener implements Listener {
    private static final Map<PotionEffectType, Boolean> TEAMEFFECT = new HashMap<>();

    static {
        TEAMEFFECT.put(PotionEffectType.BLINDNESS, false);
        TEAMEFFECT.put(PotionEffectType.CONFUSION, false);
        TEAMEFFECT.put(PotionEffectType.DAMAGE_RESISTANCE, true);
        TEAMEFFECT.put(PotionEffectType.FAST_DIGGING, true);
        TEAMEFFECT.put(PotionEffectType.FIRE_RESISTANCE, true);
        TEAMEFFECT.put(PotionEffectType.HARM, false);
        TEAMEFFECT.put(PotionEffectType.HEAL, true);
        TEAMEFFECT.put(PotionEffectType.HUNGER, false);
        TEAMEFFECT.put(PotionEffectType.INCREASE_DAMAGE, true);
        TEAMEFFECT.put(PotionEffectType.JUMP, true);
        TEAMEFFECT.put(PotionEffectType.POISON, false);
        TEAMEFFECT.put(PotionEffectType.REGENERATION, true);
        TEAMEFFECT.put(PotionEffectType.SLOW, false);
        TEAMEFFECT.put(PotionEffectType.SLOW_DIGGING, false);
        TEAMEFFECT.put(PotionEffectType.SPEED, true);
        TEAMEFFECT.put(PotionEffectType.WATER_BREATHING, true);
        TEAMEFFECT.put(PotionEffectType.WEAKNESS, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onCreatureSpawn(final CreatureSpawnEvent event) {
        debug("onCreatureSpawn: {}", event.getSpawnReason().name());
        final Set<SpawnReason> naturals = new HashSet<>();
        naturals.add(SpawnReason.CHUNK_GEN);
        naturals.add(SpawnReason.DEFAULT);
        naturals.add(SpawnReason.NATURAL);
        naturals.add(SpawnReason.SLIME_SPLIT);
        naturals.add(SpawnReason.VILLAGE_INVASION);
        naturals.add(SpawnReason.LIGHTNING);

        if (!naturals.contains(event.getSpawnReason())) {
            // custom generation, this is not our business!
            debug(">not natural");
            return;
        }

        final Arena arena = ArenaManager
                .getArenaByProtectedRegionLocation(
                        new PABlockLocation(event.getLocation()),
                        RegionProtection.MOBS);
        if (arena == null) {
            debug("not part of an arena");
            return; // no arena => out
        }
        debug(arena, "cancel CreatureSpawnEvent!");
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onEntityExplode(final EntityExplodeEvent event) {
        debug("explosion");

        Arena arena = ArenaManager.getArenaByProtectedRegionLocation(
                new PABlockLocation(event.getLocation()), RegionProtection.TNT);
        if (arena == null) {

            arena = ArenaManager.getArenaByProtectedRegionLocation(
                    new PABlockLocation(event.getLocation()), RegionProtection.TNTBREAK);
            if (arena == null) {

                arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(event.getLocation()));
                if (arena != null) {
                    ArenaModuleManager.onEntityExplode(arena, event);
                }

                return; // no arena => out
            }
        }
        debug(arena, "explosion inside an arena, TNT should be blocked");
        if (!arena.getConfig().getBoolean(CFG.PROTECT_ENABLED)
                || !(event.getEntity() instanceof TNTPrimed)
                && !(event.getEntity() instanceof Creeper)) {

            try {
                arena.getGoal().checkExplode(event);
            } catch (GameplayException e) {
                debug(arena, "onEntityExplode cancelled by goal: {}", arena.getGoal().getName());
            }

            ArenaModuleManager.onEntityExplode(arena, event);
            return;
        }

        event.setCancelled(true); // ELSE => cancel event
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        final Entity entity = event.getEntity();

        if ((!(entity instanceof Player))) {
            return; // no player
        }
        final Arena arena = ArenaPlayer.fromPlayer(entity.getName())
                .getArena();
        if (arena == null) {
            return;
        }
        final Player player = (Player) entity;
        debug(arena, player, "onEntityRegainHealth => fighing player");
        debug(arena, "reason: " + event.getRegainReason());
        if (!arena.isFightInProgress()) {
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();

        if (team == null) {
            return;
        }

        ArenaModuleManager.onEntityRegainHealth(arena, event);

    }

    /**
     * parsing of damage: Entity vs Entity
     *
     * @param event the triggering event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public static void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        Entity eDamager = event.getDamager();
        final Entity eDamagee = event.getEntity();

        debug("onEntityDamageByEntity: cause: {}", event.getCause().name()
                + " : " + event.getDamager().toString() + " => "
                + event.getEntity().toString());
        debug("damage: {}", event.getDamage());

        if (eDamager instanceof Projectile) {
            debug("parsing projectile");

            ProjectileSource p = ((Projectile) eDamager).getShooter();

            if (p instanceof LivingEntity) {

                eDamager = (LivingEntity) p;

            }
            debug("=> {}", eDamager);
        }

        if (eDamager instanceof Player && ArenaPlayer.fromPlayer(eDamager.getName()).getStatus() == PlayerStatus.LOST) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Wolf) {
            final Wolf wolf = (Wolf) event.getEntity();
            if (wolf.getOwner() != null) {
                try {
                    eDamager = (Entity) wolf.getOwner();
                } catch (Exception e) {
                    // wolf belongs to dead player or whatnot
                }
            }
        }

        if (eDamager instanceof Player && eDamagee instanceof Player
                && PVPArena.getInstance().getConfig().getBoolean("onlyPVPinArena")) {
            event.setCancelled(true);
            // cancel events for regular no PVP servers
        }

        if (!(eDamagee instanceof Player)) {
            return;
        }

        final Arena arena = ArenaPlayer.fromPlayer(eDamagee.getName())
                .getArena();
        if (arena == null) {
            // defender no arena player => out
            return;
        }
        debug(arena, "onEntityDamageByEntity: fighting player");

        if ((!(eDamager instanceof Player))) {
            // attacker no player => out!
            if (arena.getConfig().getBoolean(CFG.DAMAGE_FROMOUTSIDERS)) {
                event.setCancelled(false);
            }
            return;
        }

        debug(arena, eDamager, "both entities are players");
        final Player attacker = (Player) eDamager;
        final Player defender = (Player) eDamagee;

        if (attacker.equals(defender)) {
            // player attacking himself. ignore!
            return;
        }

        boolean defTeam = false;
        boolean attTeam = false;
        final ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender.getName());
        final ArenaPlayer apAttacker = ArenaPlayer.fromPlayer(attacker.getName());

        for (ArenaTeam team : arena.getTeams()) {
            defTeam = defTeam || team.getTeamMembers().contains(
                    apDefender);
            attTeam = attTeam || team.getTeamMembers().contains(
                    apAttacker);
        }

        if (!defTeam || !attTeam || arena.realEndRunner != null) {
            // special case: attacker has no team (might not be in the arena)
            event.setCancelled(attTeam || !arena.getConfig().getBoolean(CFG.DAMAGE_FROMOUTSIDERS)
                    || !defTeam || arena.realEndRunner != null);
            return;
        }

        debug(arena, attacker, "both players part of the arena");
        debug(arena, defender, "both players part of the arena");

        if (PVPArena.getInstance().getConfig().getBoolean("onlyPVPinArena")) {
            event.setCancelled(false); // uncancel events for regular no PVP
            // servers
        }

        if ((!arena.getConfig().getBoolean(CFG.PERMS_TEAMKILL))
                && (apAttacker.getArenaTeam())
                .equals(apDefender.getArenaTeam())) {
            // no team fights!
            debug(arena, attacker, "team hit, cancel!");
            debug(arena, defender, "team hit, cancel!");
            if (!(event.getDamager() instanceof Snowball)) {
                event.setCancelled(true);
            }
            return;
        }

        if (!arena.isFightInProgress() || (arena.pvpRunner != null)) {
            debug(arena, attacker, "fight not started, cancel!");
            debug(arena, defender, "fight not started, cancel!");
            event.setCancelled(true);
            return;
        }

        // cancel if defender or attacker are not fighting
        if (apAttacker.getStatus() != PlayerStatus.FIGHT || apDefender.getStatus() != PlayerStatus.FIGHT ) {
            debug(arena, attacker, "player or target is not fighting, cancel!");
            debug(arena, defender, "player or target is not fighting, cancel!");
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(),
                new DamageResetRunnable(arena, attacker, defender), 1L);

        if (arena.getConfig().getInt(CFG.PROTECT_SPAWN) > 0
                && SpawnManager.isNearSpawn(arena, defender, arena
                .getConfig().getInt(CFG.PROTECT_SPAWN))) {
            // spawn protection!
            debug(arena, attacker, "spawn protection! damage cancelled!");
            debug(arena, defender, "spawn protection! damage cancelled!");
            event.setCancelled(true);
            return;
        }

        // here it comes, process the damage!

        debug(arena, attacker, "processing damage!");
        debug(arena, defender, "processing damage!");

        ArenaModuleManager.onEntityDamageByEntity(arena, attacker, defender,
                event);

        StatisticsManager.damage(arena, attacker, defender, event.getDamage());

        if (arena.getConfig().getBoolean(CFG.DAMAGE_BLOODPARTICLES)) {
            apDefender.showBloodParticles();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileHitEvent(final ProjectileHitEvent event) {
        ProjectileSource eDamager = event.getEntity().getShooter();
        final Entity eDamagee = event.getHitEntity();


        if (eDamager instanceof Player && ArenaPlayer.fromPlayer(((Player) eDamager).getName()).getStatus() == PlayerStatus.LOST) {
            return;
        }

        if(eDamager instanceof Player && eDamagee instanceof Player) {
            final Player attacker = (Player) eDamager;
            final Player defender = (Player) eDamagee;
            final ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender.getName());
            final ArenaPlayer apAttacker = ArenaPlayer.fromPlayer(attacker.getName());
            final Arena arena = apDefender.getArena();

            if (arena == null || apAttacker.getArena() == null || apDefender.getStatus() == PlayerStatus.LOST || !arena.isFightInProgress()) {
                return;
            }

            debug(arena, "onProjectileHitEvent: fighting player");
            ArenaModuleManager.onProjectileHit(arena, attacker, defender, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public static void onEntityDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();

        debug("onEntityDamage: cause: {} : {} => {}", event.getCause().name(), event.getEntity().toString(),
                event.getEntity().getLocation());

        if (!(entity instanceof Player)) {
            return;
        }

        if (ArenaPlayer.fromPlayer(entity.getName()).getStatus() == PlayerStatus.LOST) {
            event.setCancelled(true);
            return;
        }

        final Arena arena = ArenaPlayer.fromPlayer(entity.getName()).getArena();
        if (arena == null) {
            // defender no arena player => out
            return;
        }

        final Player defender = (Player) entity;

        final ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender.getName());

        if (arena.realEndRunner != null
                || (!apDefender.getStatus().equals(PlayerStatus.NULL) && !apDefender
                .getStatus().equals(PlayerStatus.FIGHT))) {
            event.setCancelled(true);
            return;
        }

        for (ArenaRegion ars : arena.getRegions()) {
            if (ars.getFlags().contains(RegionFlag.NODAMAGE)
                    && ars.getShape().contains(new PABlockLocation(defender.getLocation()))) {
                event.setCancelled(true);
                return;
            }
        }

        // Faking death if damage is higher than player health
        if ((defender.getHealth() - event.getFinalDamage()) <= 0) {
            // Event is not cancelled to keep attack effects, we set damage to 0 instead
            event.setDamage(0);

            playFakeDeathEffects(defender);
            WorkflowManager.handlePlayerDeath(arena, defender, event);
        }
    }

    /**
     * Play a fake death effect (particles and sound) to simulate player death
     * @param player The player who should be dead
     */
    private static void playFakeDeathEffects(Player player) {

        final float volume = 1f; // default value
        final float pitch = 1f; // default value
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, volume, pitch);

        final Location particlesSpawnLoc = player.getLocation().add(0, 0.5, 0);
        final int count = 40;
        final double xzOffset = 0.3;
        final double yOffset = 0.6;
        final double speed = 0.02;
        player.getWorld().spawnParticle(Particle.CLOUD, particlesSpawnLoc, count, xzOffset, yOffset, xzOffset, speed);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityTargetLivingEntityEvent event) {
        for (Arena arena : ArenaManager.getArenas()) {
            if (arena.hasEntity(event.getEntity())) {

                Player player = arena.getEntityOwner(event.getEntity());
                ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

                if (event.getEntity().equals(player)) {
                    event.setCancelled(true);
                    return;
                }

                if (!arena.getConfig().getBoolean(CFG.PERMS_TEAMKILL)) {
                    for (ArenaPlayer ap : aPlayer.getArenaTeam().getTeamMembers()) {
                        if (event.getTarget().equals(ap.getPlayer())) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent event) {
        for (Arena arena : ArenaManager.getArenas()) {
            if (arena.hasEntity(event.getEntity())) {

                arena.removeEntity(event.getEntity());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityTeleport(final EntityTeleportEvent event) {
        if (event.getEntity() instanceof Tameable) {
            Tameable t = (Tameable) event.getEntity();

            if (t.isTamed()) {
                ArenaPlayer ap = ArenaPlayer.fromPlayer(t.getOwner().getName());
                if (ap.getArena() != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public static void onPotionSplash(final PotionSplashEvent event) {

        debug("onPotionSplash");
        boolean affectTeam = true;

        final Collection<PotionEffect> pot = event.getPotion().getEffects();
        for (PotionEffect eff : pot) {
            debug(">{}", eff.getType().getName());
            if (TEAMEFFECT.containsKey(eff.getType())) {
                affectTeam = TEAMEFFECT.get(eff.getType());
                debug(">{}", affectTeam);
                break;
            }
        }

        ArenaPlayer shooter;

        try {
            shooter = ArenaPlayer.fromPlayer(((Player) event.getEntity()
                    .getShooter()).getName());
        } catch (Exception e) {
            return;
        }

        debug(shooter, "legit player");

        if (shooter.getArena() == null
                || !shooter.getStatus().equals(PlayerStatus.FIGHT)) {
            debug(shooter, "something is null!");
            return;
        }

        if (shooter.getArena().getConfig().getBoolean(CFG.PERMS_TEAMKILL)) {
            return; // if teamkill allowed, don't check, just ignore
        }

        final Collection<LivingEntity> entities = event.getAffectedEntities();
        for (LivingEntity e : entities) {
            if (!(e instanceof Player)) {
                debug("skipping non-player {}", e.getName());
                continue;
            }
            final ArenaPlayer damagee = ArenaPlayer.fromPlayer(e.getName());

            if (damagee.getArena() == null || shooter.getArena() == null ||
                    (damagee.getArena() != shooter.getArena()) ||
                    damagee.getArenaTeam() == null || shooter.getArenaTeam() == null) {
                /*
                  some people obviously allow non arena players to mess with potions around arena players

                  this check should cover any of the entities not being in the same arena, or not arena at all
                 */
                debug("skipping {}", e.getName());
                continue;
            }

            final boolean sameTeam = damagee.getArenaTeam().equals(shooter.getArenaTeam());


            if (sameTeam != affectTeam) {
                // different team and only team should be affected
                // same team and the other team should be affected
                // ==> cancel!
                event.setIntensity(e, 0);
                debug("setting intensity to 0 for {}", e.getName());
                break;
            }
        }
    }
}
