package net.slipcor.pvparena.compatibility;

import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

/**
 * Compatibility class to make PVPArena compatible with Bukkit 1.16.5 - 1.20.4
 */
public abstract class EffectTypeAdapter {

    public static final PotionEffectType SPEED = PotionEffectType.SPEED;
    public static final PotionEffectType SLOWNESS = Objects.requireNonNull(PotionEffectType.getByName("slowness"));
    public static final PotionEffectType HASTE = Objects.requireNonNull(PotionEffectType.getByName("haste"));
    public static final PotionEffectType MINING_FATIGUE = Objects.requireNonNull(PotionEffectType.getByName("mining_fatigue"));
    public static final PotionEffectType STRENGTH = Objects.requireNonNull(PotionEffectType.getByName("strength"));
    public static final PotionEffectType INSTANT_HEALTH = Objects.requireNonNull(PotionEffectType.getByName("instant_health"));
    public static final PotionEffectType INSTANT_DAMAGE = Objects.requireNonNull(PotionEffectType.getByName("instant_damage"));
    public static final PotionEffectType JUMP_BOOST = Objects.requireNonNull(PotionEffectType.getByName("jump_boost"));
    public static final PotionEffectType NAUSEA = Objects.requireNonNull(PotionEffectType.getByName("nausea"));
    public static final PotionEffectType REGENERATION = PotionEffectType.REGENERATION;
    public static final PotionEffectType RESISTANCE = Objects.requireNonNull(PotionEffectType.getByName("resistance"));
    public static final PotionEffectType FIRE_RESISTANCE = PotionEffectType.FIRE_RESISTANCE;
    public static final PotionEffectType WATER_BREATHING = PotionEffectType.WATER_BREATHING;
    public static final PotionEffectType INVISIBILITY = PotionEffectType.INVISIBILITY;
    public static final PotionEffectType BLINDNESS = PotionEffectType.BLINDNESS;
    public static final PotionEffectType NIGHT_VISION = PotionEffectType.NIGHT_VISION;
    public static final PotionEffectType HUNGER = PotionEffectType.HUNGER;
    public static final PotionEffectType WEAKNESS = PotionEffectType.WEAKNESS;
    public static final PotionEffectType POISON = PotionEffectType.POISON;
    public static final PotionEffectType WITHER = PotionEffectType.WITHER;
    public static final PotionEffectType HEALTH_BOOST = PotionEffectType.HEALTH_BOOST;
    public static final PotionEffectType ABSORPTION = PotionEffectType.ABSORPTION;
    public static final PotionEffectType SATURATION = PotionEffectType.SATURATION;
    public static final PotionEffectType GLOWING = PotionEffectType.GLOWING;
    public static final PotionEffectType LEVITATION = PotionEffectType.LEVITATION;
    public static final PotionEffectType LUCK = PotionEffectType.LUCK;
    public static final PotionEffectType UNLUCK = PotionEffectType.UNLUCK;
    public static final PotionEffectType SLOW_FALLING = PotionEffectType.SLOW_FALLING;
    public static final PotionEffectType CONDUIT_POWER = PotionEffectType.CONDUIT_POWER;
    public static final PotionEffectType DOLPHINS_GRACE = PotionEffectType.DOLPHINS_GRACE;
    public static final PotionEffectType BAD_OMEN = PotionEffectType.BAD_OMEN;
    public static final PotionEffectType HERO_OF_THE_VILLAGE = PotionEffectType.HERO_OF_THE_VILLAGE;
}

