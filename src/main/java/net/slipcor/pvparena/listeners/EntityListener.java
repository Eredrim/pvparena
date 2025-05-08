package net.slipcor.pvparena.listeners;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.compatibility.EffectTypeAdapter;
import net.slipcor.pvparena.compatibility.ParticleAdapter;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.StatisticsManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.RegionFlag;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.runnables.DamageResetRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

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
        TEAMEFFECT.put(EffectTypeAdapter.BLINDNESS, false);
        TEAMEFFECT.put(EffectTypeAdapter.NAUSEA, false);
        TEAMEFFECT.put(EffectTypeAdapter.RESISTANCE, true);
        TEAMEFFECT.put(EffectTypeAdapter.HASTE, true);
        TEAMEFFECT.put(EffectTypeAdapter.FIRE_RESISTANCE, true);
        TEAMEFFECT.put(EffectTypeAdapter.INSTANT_DAMAGE, false);
        TEAMEFFECT.put(EffectTypeAdapter.INSTANT_HEALTH, true);
        TEAMEFFECT.put(EffectTypeAdapter.HUNGER, false);
        TEAMEFFECT.put(EffectTypeAdapter.STRENGTH, true);
        TEAMEFFECT.put(EffectTypeAdapter.JUMP_BOOST, true);
        TEAMEFFECT.put(EffectTypeAdapter.POISON, false);
        TEAMEFFECT.put(EffectTypeAdapter.REGENERATION, true);
        TEAMEFFECT.put(EffectTypeAdapter.SLOWNESS, false);
        TEAMEFFECT.put(EffectTypeAdapter.MINING_FATIGUE, false);
        TEAMEFFECT.put(EffectTypeAdapter.SPEED, true);
        TEAMEFFECT.put(EffectTypeAdapter.WATER_BREATHING, true);
        TEAMEFFECT.put(EffectTypeAdapter.WEAKNESS, false);
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

        Location eventLocation = event.getLocation();
        Arena arena = ArenaManager.getArenaByRegionLocation(new PABlockLocation(eventLocation));

        if(arena == null) {
            return;
        }

        if(arena.hasRegionsProtectingLocation(eventLocation, RegionProtection.TNT)) {
            debug(arena, "explosion inside an TNT protected arena, TNT should be blocked");
            event.setCancelled(true);
        } if(arena.hasRegionsProtectingLocation(eventLocation, RegionProtection.TNTBREAK)) {
            debug(arena, "explosion inside an TNTBREAK protected arena, TNT should be blocked");
            Location location = event.getLocation();
            location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            event.setCancelled(true);
        } else {
            debug(arena, "explosion allowed inside this arena");

            try {
                arena.getGoal().checkExplode(event);
            } catch (GameplayException e) {
                debug(arena, "onEntityExplode cancelled by goal: {}", arena.getGoal().getName());
                event.setCancelled(true);
                return;
            }

            ArenaModuleManager.onEntityExplode(arena, event);
        }
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        Entity eDamager = event.getDamager();
        final Entity eDamagee = event.getEntity();

        debug("onEntityDamageByEntity: cause: {} : {} => {}", event.getCause().name(), event.getDamager(), event.getEntity());
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

        ArenaModuleManager.onEntityDamageByEntity(arena, attacker, defender, event);

        StatisticsManager.damage(arena, attacker, defender, event.getDamage());

        if (arena.getConfig().getBoolean(CFG.DAMAGE_BLOODPARTICLES)) {
            apDefender.showBloodParticles();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileHitEvent(final ProjectileHitEvent event) {
        ProjectileSource eDamager = event.getEntity().getShooter();
        final Entity eDamagee = event.getHitEntity();

        if(eDamager instanceof Player && eDamagee instanceof Player) {
            final Player attacker = (Player) eDamager;
            final Player defender = (Player) eDamagee;
            final ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender.getName());
            final ArenaPlayer apAttacker = ArenaPlayer.fromPlayer(attacker.getName());
            final Arena arena = apDefender.getArena();

            if (arena == null || apAttacker.getArena() == null || !arena.isFightInProgress() || apDefender.getStatus() == PlayerStatus.LOST) {
                return;
            }

            debug(arena, "onProjectileHitEvent: fighting player");
            ArenaModuleManager.onProjectileHit(arena, attacker, defender, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onEntityDamage(final EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Player)) {
            return;
        }

        Player defender = (Player) entity;
        ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender);
        Arena arena = apDefender.getArena();

        if (arena != null) {

            debug("onEntityDamage: cause: {} : {} => {}", event.getCause().name(), entity, entity.getLocation());

            PlayerStatus status = apDefender.getStatus();
            if (arena.realEndRunner != null || Stream.of(PlayerStatus.FIGHT, PlayerStatus.NULL).noneMatch(status::equals)) {
                event.setCancelled(true);
                return;
            }

            boolean isInNoDamageRegion = arena.getRegions().stream()
                    .anyMatch(reg -> reg.getFlags().contains(RegionFlag.NODAMAGE) && reg.containsLocation(apDefender.getLocation()));

            if(isInNoDamageRegion) {
                event.setCancelled(true);
                return;
            }

            // Faking death if damage is higher than player health
            if ((defender.getHealth() - event.getFinalDamage()) <= 0) {
                // Event is not cancelled to keep attack effects, we set damage to 0 instead
                Arrays.stream(DamageModifier.values())
                        .filter(event::isApplicable)
                        .forEach(modifier -> event.setDamage(modifier, 0));

                playFakeDeathEffects(defender);
                WorkflowManager.handlePlayerDeath(arena, defender, event);
            }
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
        player.getWorld().spawnParticle(ParticleAdapter.CLOUD.getValue(), particlesSpawnLoc, count, xzOffset, yOffset, xzOffset, speed);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityTargetLivingEntityEvent event) {
        if (event.getTarget() != null) {
            ArenaManager.getArenas().stream()
                    .filter(arena -> arena.hasEntity(event.getEntity()))
                    .findAny()
                    .ifPresent(arena -> {
                        Player entityOwner = arena.getEntityOwner(event.getEntity());
                        ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(entityOwner);

                        if (event.getTarget().equals(entityOwner)) {
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
                    });
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent event) {
        ArenaManager.getArenas().stream()
                .filter(arena -> arena.hasEntity(event.getEntity()))
                .findAny()
                .ifPresent(arena -> arena.removeEntity(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityTeleport(final EntityTeleportEvent event) {
        if (event.getEntity() instanceof Tameable) {
            Tameable t = (Tameable) event.getEntity();

            if (t.isTamed() && t.getOwner() instanceof Player) {
                ArenaPlayer ap = ArenaPlayer.fromPlayer((Player) t.getOwner());
                if (ap.getArena() != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onPotionSplash(final PotionSplashEvent event) {

        ProjectileSource projectileSource = event.getEntity().getShooter();
        if(!(projectileSource instanceof Player)) {
            return;
        }

        debug("onPotionSplash");
        ArenaPlayer shooter = ArenaPlayer.fromPlayer(((Player) projectileSource).getName());

        if (shooter.getArena() != null && shooter.getArenaTeam() != null && shooter.getStatus().equals(PlayerStatus.FIGHT)) {
            debug(shooter, "is a regular arena player");

            if (shooter.getArena().getConfig().getBoolean(CFG.PERMS_TEAMKILL)) {
                return; // if teamkill allowed, don't check, just ignore
            }

            boolean hasOnlyNegativeEffects = event.getPotion().getEffects().stream()
                    .noneMatch(effect -> TEAMEFFECT.get(effect.getType()) == true);

            // If potion has only negative effects, cancel the effect for teammates
            if (hasOnlyNegativeEffects) {

                /* some people obviously allow non arena players to mess with potions around arena players
                  these checks should ignore (and not cancel) any of external entities to the arena/team */

                event.getAffectedEntities().stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> ArenaPlayer.fromPlayer((Player) e))
                        .filter(damagee -> damagee.getArena() != null
                                && damagee.getArena().equals(shooter.getArena())
                                && damagee.getStatus() == PlayerStatus.FIGHT
                                && Objects.equals(damagee.getArenaTeam(), shooter.getArenaTeam()))
                        .forEach(sameTeamDamagee -> {
                            // Do not affect same team players
                            debug("setting intensity to 0 for {}", sameTeamDamagee);
                            event.setIntensity(sameTeamDamagee.getPlayer(), 0);
                        });
            }
        }
    }
}
