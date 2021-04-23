package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.loader.JarLoader;
import net.slipcor.pvparena.loader.Loadable;
import net.slipcor.pvparena.modules.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <pre>Arena Module Manager class</pre>
 * <p/>
 * Loads and manages arena modules
 */
public class ArenaModuleManager {
    private Set<Loadable<? extends ArenaModule>> modLoadables;
    private final JarLoader<ArenaModule> loader;

    /**
     * create an arena module manager instance
     *
     * @param plugin the plugin instance
     */
    public ArenaModuleManager(final PVPArena plugin) {
        final File path = new File(plugin.getDataFolder(), "/mods");
        if (!path.exists()) {
            path.mkdir();
        }
        this.loader = new JarLoader<>(path, ArenaModule.class);
        this.modLoadables = this.loader.loadClasses();
        this.addInternalMods();
    }

    private void addInternalMods() {
        this.addInternalLoadable(BattlefieldJoin.class);
        this.addInternalLoadable(CustomSpawn.class);
        this.addInternalLoadable(RegionTool.class);
        this.addInternalLoadable(StandardLounge.class);
        this.addInternalLoadable(StandardSpectate.class);
        this.addInternalLoadable(WarmupJoin.class);
    }

    public static void announce(final Arena arena, final String message, final String type) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.announce(message, type);
        }
    }

    public static boolean cannotSelectClass(final Arena arena, final Player player,
                                            final String className) {
        for (final ArenaModule mod : arena.getMods()) {
            if (mod.cannotSelectClass(player, className)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkCountOverride(Arena arena, Player player, String message) {
        for (final ArenaModule mod : arena.getMods()) {
            if (mod.checkCountOverride(player, message)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> checkForMissingSpawns(final Arena arena, final Set<String> spawnsNames) {
        return arena.getMods().stream()
                .map(arenaModule -> arenaModule.checkForMissingSpawns(spawnsNames))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public static void choosePlayerTeam(final Arena arena, final Player player, final String coloredTeam) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.choosePlayerTeam(player, coloredTeam);
        }
    }

    public static boolean commitEnd(final Arena arena, final ArenaTeam aTeam) {
        for (final ArenaModule mod : arena.getMods()) {
            if (mod.commitEnd(aTeam)) {
                return true;
            }
        }
        return false;
    }

    public static void configParse(final Arena arena, final YamlConfiguration config) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.configParse(config);
        }
    }

    public static void giveRewards(final Arena arena, final Player player) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.giveRewards(player);
        }
    }

    public static void initiate(final Arena arena, final Player player) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.initiate(player);
        }
    }

    public static void lateJoin(final Arena arena, final Player player) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.lateJoin(player);
        }
    }

    public static void onBlockBreak(final Arena arena, final Block block) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onBlockBreak(block);
        }
    }

    public static void onBlockChange(final Arena arena, final Block block, final BlockState state) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onBlockChange(block, state);
        }
    }

    public static void onBlockPiston(final Arena arena, final Block block) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onBlockPiston(block);
        }
    }

    public static void onBlockPlace(final Arena arena, final Block block, final Material mat) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onBlockPlace(block, mat);
        }
    }

    public static void onEntityDamageByEntity(final Arena arena, final Player attacker,
                                              final Player defender, final EntityDamageByEntityEvent event) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onEntityDamageByEntity(attacker, defender, event);
        }
    }

    public static void onProjectileHit(final Arena arena, final Player attacker, final Player defender, final ProjectileHitEvent event) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onProjectileHit(attacker, defender, event);
        }
    }

    public static void onEntityExplode(final Arena arena, final EntityExplodeEvent event) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onEntityExplode(event);
        }
    }

    public static void onEntityRegainHealth(final Arena arena, final EntityRegainHealthEvent event) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onEntityRegainHealth(event);
        }
    }

    public static void onPaintingBreak(final Arena arena, final Hanging painting, final EntityType type) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onPaintingBreak(painting, type);
        }
    }

    public static boolean onPlayerInteract(final Arena arena, final PlayerInteractEvent event) {
        for (final ArenaModule mod : arena.getMods()) {
            if (mod.onPlayerInteract(event)) {
                return true;
            }
        }
        return false;
    }

    public static void onPlayerPickupItem(final Arena arena, final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            for (final ArenaModule mod : arena.getMods()) {
                mod.onPlayerPickupItem(event);
            }
        }
    }

    public static void onPlayerVelocity(final Arena arena, final PlayerVelocityEvent event) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.onPlayerVelocity(event);
        }
    }

    public static void parseClassChange(Arena arena, Player player, ArenaClass aClass) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.parseClassChange(player, aClass);
        }
    }

    public static Integer parseStartCountDown(Integer seconds, String message, Arena arena, Boolean global) {
        if (arena == null) {
            return seconds;
        }
        Integer result = seconds;
        for (final ArenaModule mod : arena.getMods()) {
            result = mod.parseStartCountDown(result, message, global);
        }
        return result;
    }

    public static void parseJoin(final Arena arena, final Player player,
                                 final ArenaTeam team) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.parseJoin(player, team);
        }
    }

    public static void parsePlayerDeath(final Arena arena, final Player player,
                                        final EntityDamageEvent cause) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.parsePlayerDeath(player, cause);
        }
    }

    public static void parsePlayerLeave(final Arena arena, final Player player, final ArenaTeam team) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.parsePlayerLeave(player, team);
        }
    }

    public static void parseRespawn(Arena arena, Player player, ArenaTeam team, PADeathInfo deathInfo) {
        for (final ArenaModule mod : arena.getMods()) {
            try {
                mod.parseRespawn(player, team, deathInfo.getCause(), deathInfo.getDamager());
            } catch (final Exception e) {
                PVPArena.getInstance().getLogger().warning("Module had NPE on Respawn: " + mod.getName());
            }
        }
    }

    public static void reset(final Arena arena, final boolean force) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.reset(force);
        }
    }

    public static void resetPlayer(final Arena arena, final Player player, final boolean soft, final boolean force) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.resetPlayer(player, soft, force);
        }
    }

    public static void timedEnd(final Arena arena, final Set<String> result) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.timedEnd(result);
        }
    }

    public static void tpPlayerToCoordName(final Arena arena, final Player player, final String place) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.tpPlayerToCoordName(player, place);
        }
    }

    public static void unload(final Arena arena, final Player player) {
        for (final ArenaModule mod : arena.getMods()) {
            mod.unload(player);
        }
    }

    public Set<Loadable<? extends ArenaModule>> getAllLoadables() {
        return this.modLoadables;
    }

    public boolean hasLoadable(final String name) {
        return this.modLoadables.stream().anyMatch(l -> l.getName().equalsIgnoreCase(name));
    }

    public Loadable<? extends ArenaModule> getLoadableByName(String name) {
        return this.modLoadables.stream()
                .filter(l -> l.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public ArenaModule getNewInstance(String name) {
        try {
            Loadable<? extends ArenaModule> modLoadable = this.getLoadableByName(name);

            if(modLoadable != null) {
                return modLoadable.getNewInstance();
            }

        } catch (ReflectiveOperationException e) {
            PVPArena.getInstance().getLogger().severe(String.format("Mod '%s' seems corrupted", name));
            e.printStackTrace();
        }
        return null;
    }

    public void reload() {
        this.modLoadables = this.loader.reloadClasses();
        this.addInternalMods();
    }

    private void addInternalLoadable(Class<? extends ArenaModule> loadableClass) {
        this.modLoadables.add(new Loadable<>(loadableClass.getSimpleName(), true, loadableClass));
    }
}
