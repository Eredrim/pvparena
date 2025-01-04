package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.compatibility.AttributeAdapter;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.Collection;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Player State class</pre>
 * <p/>
 * Saves and loads player data before and after the match, respectively
 *
 * @author slipcor
 */

public final class PlayerState {

    private final String name;

    private int fireticks;
    private int foodlevel;
    private double health;
    private double maxhealth;
    private int explevel;
    private float walkSpeed;
    private float flySpeed;

    private float exhaustion;
    private float experience;
    private float saturation;

    private boolean collides;
    private boolean allowedFlight;

    private String displayname;
    private Collection<PotionEffect> potionEffects;
    private GameMode gamemode;

    public PlayerState(final Player player) {
        this.name = player.getName();
        debug(player, "creating PlayerState of {}", this.name);

        this.fireticks = player.getFireTicks();
        this.foodlevel = player.getFoodLevel();
        this.gamemode = player.getGameMode();
        this.health = player.getHealth();
        this.maxhealth = player.getAttribute(AttributeAdapter.MAX_HEALTH.getValue()).getBaseValue();

        this.exhaustion = player.getExhaustion();
        this.experience = player.getExp();
        this.explevel = player.getLevel();
        this.saturation = player.getSaturation();

        this.walkSpeed = player.getWalkSpeed();
        this.flySpeed = player.getFlySpeed();

        this.potionEffects = player.getActivePotionEffects();
        this.collides = player.isCollidable();
        this.allowedFlight = player.getAllowFlight();

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final Arena arena = aPlayer.getArena();

        aPlayer.setFlyState(player.isFlying());

        if (arena.getConfig().getBoolean(CFG.CHAT_COLORNICK)) {
            this.displayname = player.getDisplayName();
        }

        fullReset(arena, player);
        final int time = arena.getConfig().getInt(CFG.PLAYER_TIME);
        if (time != -1) {
            player.setPlayerTime(time, false);
        }
    }

    public void dump(final YamlConfiguration cfg) {
        debug("backing up PlayerState of {}", this.name);
        cfg.set("state.fireticks", this.fireticks);
        cfg.set("state.foodlevel", this.foodlevel);
        cfg.set("state.gamemode", this.gamemode.toString());
        cfg.set("state.health", this.health);
        cfg.set("state.maxhealth", this.maxhealth);
        cfg.set("state.exhaustion", this.exhaustion);
        cfg.set("state.experience", this.experience);
        cfg.set("state.explevel", this.explevel);
        cfg.set("state.saturation", this.saturation);
        cfg.set("state.displayname", this.displayname);
        cfg.set("state.flying", ArenaPlayer.fromPlayer(this.name).getFlyState());
        cfg.set("state.allowedFlight", this.allowedFlight);
        cfg.set("state.walkSpeed", this.walkSpeed);
        cfg.set("state.flySpeed", this.flySpeed);
        cfg.set("state.collides", this.collides);
    }

    public static void fullReset(final Arena arena, final Player player) {
        int iHealth = arena.getConfig().getInt(CFG.PLAYER_HEALTH);

        AttributeInstance playerMaxHealth = player.getAttribute(AttributeAdapter.MAX_HEALTH.getValue());
        if (iHealth < 1) {
            iHealth = (int) playerMaxHealth.getBaseValue();
        }

        if (arena.getConfig().getInt(CFG.PLAYER_MAXHEALTH) > 0) {
             playerMaxHealth.setBaseValue(arena.getConfig().getInt(CFG.PLAYER_MAXHEALTH));
        }

        if (iHealth > playerMaxHealth.getBaseValue()) {
            player.setHealth(playerMaxHealth.getBaseValue());
        } else {
            playersetHealth(player, iHealth);
        }
        player.setFireTicks(0);
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
            }, 5L);
        } catch (Exception e) {
        }
        player.setFallDistance(0);
        player.setVelocity(new Vector());
        player.setFoodLevel(arena.getConfig().getInt(CFG.PLAYER_FOODLEVEL));
        player.setSaturation(arena.getConfig().getInt(CFG.PLAYER_SATURATION));
        player.setExhaustion((float) arena.getConfig().getDouble(CFG.PLAYER_EXHAUSTION));
        player.setLevel(0);
        player.setExp(0);
        if (arena.getConfig().getGameMode(CFG.GENERAL_GAMEMODE) != null) {
            player.setGameMode(arena.getConfig().getGameMode(CFG.GENERAL_GAMEMODE));
        }
        player.setCollidable(arena.getConfig().getBoolean(CFG.PLAYER_COLLISION));
        PlayerState.removeEffects(player);

        if (arena.getConfig().getBoolean(CFG.CHAT_COLORNICK)) {
            final ArenaTeam team = ArenaPlayer.fromPlayer(player).getArenaTeam();
            String n;
            if (team == null) {
                n = player.getName();
            } else {
                n = team.getColorCodeString() + player.getName();
            }
            n = ChatColor.translateAlternateColorCodes('&', n);

            player.setDisplayName(n);
        }
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.2F);
    }

    public void unload(final ArenaPlayer aPlayer, final boolean soft) {
        final Player player = Bukkit.getPlayerExact(this.name);

        if (player == null) {
            aPlayer.getArena().getGoal().disconnect(aPlayer);
            return;
        }
        debug(player, "restoring PlayerState of {}", this.name);

        player.setFireTicks(this.fireticks);
        player.setFoodLevel(this.foodlevel);

        player.setFoodLevel(this.foodlevel);
        if (aPlayer.getArena().getConfig().getGameMode(CFG.GENERAL_GAMEMODE) != null) {
            player.setGameMode(this.gamemode);
        }

        AttributeInstance playerMaxHealth = player.getAttribute(AttributeAdapter.MAX_HEALTH.getValue());
        if (aPlayer.getArena().getConfig().getInt(CFG.PLAYER_MAXHEALTH) > 0) {
            playerMaxHealth.setBaseValue(this.maxhealth);
        }

        if (playerMaxHealth.getBaseValue() == this.maxhealth) {
            player.setHealth(Math.min(this.health, this.maxhealth));
        } else {
            final double newHealth = playerMaxHealth.getBaseValue() * this.health / this.maxhealth;
            if (newHealth > playerMaxHealth.getBaseValue()) {
                player.setHealth(playerMaxHealth.getBaseValue());
            } else {
                player.setHealth(newHealth);
            }

        }
        player.setSaturation(this.saturation);
        player.setLevel(this.explevel);
        player.setExp(this.experience);
        player.setExhaustion(this.exhaustion);
        player.setFallDistance(0);
        player.setVelocity(new Vector());
        if (aPlayer.getArena() != null && aPlayer.getArena().getConfig().getBoolean(CFG.CHAT_COLORNICK)) {
            player.setDisplayName(this.displayname);
        }

        if (aPlayer.getArena() != null) {
            ArenaModuleManager.unload(aPlayer.getArena(), player);
            aPlayer.getArena().getGoal().unload(aPlayer);
        }


        removeEffects(player);
        player.addPotionEffects(this.potionEffects);

        aPlayer.setTelePass(false);
        player.setFireTicks(0);
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if (player.getFireTicks() > 0) {
                        player.setFireTicks(0);
                    }
                }
            }, 5L);
        } catch (Exception e) {
        }

        if (aPlayer.getArena() != null) {
            player.setNoDamageTicks(aPlayer.getArena().getConfig().getInt(CFG.TIME_TELEPORTPROTECT) * 20);
        }
        player.resetPlayerTime();
        player.setCollidable(this.collides);
        player.setAllowFlight(this.allowedFlight);
        if (!soft) {
            if (aPlayer.getFlyState() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            player.setFlying(aPlayer.getFlyState());
        }
        player.setFlySpeed(this.flySpeed);
        player.setWalkSpeed(this.walkSpeed);
    }

    /**
     * health setting method. Implemented for heroes to work right
     *
     * @param player the player to set
     * @param value  the health value
     */
    public static void playersetHealth(final Player player, final double value) {
        debug(player, "setting health to " + value + "/20");
        if (Bukkit.getServer().getPluginManager().getPlugin("Heroes") == null) {
            player.setHealth(value);
        }
        final double current = player.getHealth();
        final double regain = value - current;

        final EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, regain, RegainReason.CUSTOM);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void reset() {
        debug("clearing PlayerState of {}", this.name);
        this.fireticks = 0;
        this.foodlevel = 0;
        this.gamemode = GameMode.SURVIVAL;
        this.health = 0;
        this.maxhealth = -1;

        this.exhaustion = 0;
        this.experience = 0;
        this.explevel = 0;
        this.saturation = 0;
        this.displayname = null;
        this.potionEffects = null;
        this.collides = false;
        this.allowedFlight = false;
        this.walkSpeed = 0.2f;
        this.flySpeed = 0.2f;
    }

    public static void removeEffects(final Player player) {
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    public static PlayerState undump(final YamlConfiguration cfg, final String pName) {
        debug("restoring backed up PlayerState of {}", pName, pName);
        final PlayerState pState = new PlayerState(Bukkit.getPlayer(pName));

        pState.fireticks = cfg.getInt("state.fireticks", 0);
        pState.foodlevel = cfg.getInt("state.foodlevel", 0);
        pState.gamemode = GameMode.valueOf(cfg.getString("state.gamemode", GameMode.SURVIVAL.name()));
        pState.health = cfg.getInt("state.health", 1);
        pState.maxhealth = cfg.getInt("state.maxhealth", -1);
        pState.exhaustion = (float) cfg.getDouble("state.exhaustion", 1);
        pState.experience = (float) cfg.getDouble("state.experience", 0);
        pState.explevel = cfg.getInt("state.explevel", 0);
        pState.saturation = (float) cfg.getDouble("state.saturation", 0);
        pState.displayname = cfg.getString("state.displayname", pName);
        ArenaPlayer.fromPlayer(pName).setFlyState(cfg.getBoolean("state.flying", false));
        pState.collides = cfg.getBoolean("state.collides", false);
        pState.allowedFlight = cfg.getBoolean("state.allowedFlight", false);
        pState.walkSpeed = (float) cfg.getDouble("state.walkSpeed", 0.2f);
        pState.flySpeed = (float) cfg.getDouble("state.flySpeed", 0.2f);

        return pState;
    }
}
