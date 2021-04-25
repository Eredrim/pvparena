package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.commands.PAA_Setup;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaGoalManager;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.ItemStackUtils.getItemStacksFromConfig;

/**
 * <pre>
 * Configuration Manager class
 * </pre>
 * <p/>
 * Provides static methods to manage Configurations
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class ConfigurationManager {

    public static final String CLASSITEMS = "classitems";

    private ConfigurationManager() {
    }

    /**
     * create a config manager instance
     *
     * @param arena the arena to load
     * @param cfg   the configuration
     */
    public static boolean configParse(final Arena arena, final Config cfg) {
        if (!cfg.load()) {
            return false;
        }
        final YamlConfiguration config = cfg.getYamlConfiguration();

        final String goalConfig = cfg.getString(CFG.GENERAL_GOAL);
        final List<String> modules = cfg.getStringList("mods", new ArrayList<>());

        ArenaGoalManager goalManager = PVPArena.getInstance().getAgm();
        if (config.getKeys(false).isEmpty()) {

            if (arena.getGoal() == null) {
                PVPArena.getInstance().getLogger().warning(String.format("Unable to find goal for arena %s", arena.getName()));
                return false;
            }
            createNewConfig(arena, cfg);
        } else {
            // opening existing arena

            values:
            for (CFG c : CFG.getValues()) {
                if (c.hasModule()) {
                    if (goalConfig.equals(c.getGoalOrModule())) {
                        if (cfg.getUnsafe(c.getNode()) == null) {
                            cfg.createDefaults(goalConfig, modules);
                            break values;
                        }
                    }

                    for (String mod : modules) {
                        if (mod.equals(c.getGoalOrModule())) {
                            if (cfg.getUnsafe(c.getNode()) == null) {
                                cfg.createDefaults(goalConfig, modules);
                                break values;
                            }
                        }
                    }
                    continue; // node unused, don't check for existence!
                }
                if (cfg.getUnsafe(c.getNode()) == null) {
                    cfg.createDefaults(goalConfig, modules);
                    break;
                }
            }

            String goalName = cfg.getString(CFG.GENERAL_GOAL);
            if (goalManager.hasLoadable(goalName)) {
                ArenaGoal goal = goalManager.getNewInstance(goalName);
                arena.setGoal(goal, false);
            } else {
                PVPArena.getInstance().getLogger().warning(String.format("Goal referenced in arena '%s' not found (uninstalled?): %s", arena.getName(), goalName));
                return false;
            }

            List<String> list = cfg.getStringList(CFG.LISTS_MODS.getNode(), new ArrayList<>());
            for (String moduleName : list) {
                ArenaModuleManager moduleManager = PVPArena.getInstance().getAmm();
                if (!moduleManager.hasLoadable(moduleName)) {
                    PVPArena.getInstance().getLogger().warning(String.format("Module referenced in arena '%s' not found (uninstalled?): %s", arena.getName(), moduleName));
                    continue;
                }
                ArenaModule module = moduleManager.getNewInstance(moduleName);
                arena.addModule(module, false);
            }
        }

        if (config.get(CLASSITEMS) == null && PVPArena.getInstance().getConfig().get(CLASSITEMS) == null) {
            config.addDefault(CLASSITEMS + ".Ranger.items",
                    Utils.getItemStacksFromMaterials(Material.BOW, Material.ARROW));
            config.addDefault(CLASSITEMS + ".Ranger.offhand",
                    Utils.getItemStacksFromMaterials(Material.AIR));
            config.addDefault(CLASSITEMS + ".Ranger.armor",
                    Utils.getItemStacksFromMaterials(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS));

            config.addDefault(CLASSITEMS + ".Swordsman.items",
                    Utils.getItemStacksFromMaterials(Material.DIAMOND_SWORD));
            config.addDefault(CLASSITEMS + ".Swordsman.offhand",
                    Utils.getItemStacksFromMaterials(Material.AIR));
            config.addDefault(CLASSITEMS + ".Swordsman.armor",
                    Utils.getItemStacksFromMaterials(Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS));

            config.addDefault(CLASSITEMS + ".Tank.items",
                    Utils.getItemStacksFromMaterials(Material.STONE_SWORD));
            config.addDefault(CLASSITEMS + ".Tank.offhand",
                    Utils.getItemStacksFromMaterials(Material.AIR));
            config.addDefault(CLASSITEMS + ".Tank.armor",
                    Utils.getItemStacksFromMaterials(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS));

            config.addDefault(CLASSITEMS + ".Pyro.items",
                    Utils.getItemStacksFromMaterials(Material.FLINT_AND_STEEL, Material.TNT, Material.TNT, Material.TNT));
            config.addDefault(CLASSITEMS + ".Pyro.offhand",
                    Utils.getItemStacksFromMaterials(Material.AIR));
            config.addDefault(CLASSITEMS + ".Pyro.armor",
                    Utils.getItemStacksFromMaterials(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS));

        }

        if (config.get("time_intervals") == null) {
            String prefix = "time_intervals.";
            config.addDefault(prefix + "1", "1..");
            config.addDefault(prefix + "2", "2..");
            config.addDefault(prefix + "3", "3..");
            config.addDefault(prefix + "4", "4..");
            config.addDefault(prefix + "5", "5..");
            config.addDefault(prefix + "10", "10 %s");
            config.addDefault(prefix + "20", "20 %s");
            config.addDefault(prefix + "30", "30 %s");
            config.addDefault(prefix + "60", "60 %s");
            config.addDefault(prefix + "120", "2 %m");
            config.addDefault(prefix + "180", "3 %m");
            config.addDefault(prefix + "240", "4 %m");
            config.addDefault(prefix + "300", "5 %m");
            config.addDefault(prefix + "600", "10 %m");
            config.addDefault(prefix + "1200", "20 %m");
            config.addDefault(prefix + "1800", "30 %m");
            config.addDefault(prefix + "2400", "40 %m");
            config.addDefault(prefix + "3000", "50 %m");
            config.addDefault(prefix + "3600", "60 %m");
        }

        debug(arena, "setting default config for goal {}", arena.getGoal());
        arena.getGoal().setDefaults(config);

        config.options().copyDefaults(true);

        cfg.set(CFG.Z, "1.3.3.217");
        cfg.save();
        cfg.load();

        final Map<String, Object> classes = config.getConfigurationSection(CLASSITEMS).getValues(false);
        arena.getClasses().clear();
        debug(arena, "reading class items");
        ArenaClass.addGlobalClasses(arena);
        for (Map.Entry<String, Object> stringObjectEntry1 : classes.entrySet()) {

            ItemStack[] items;
            ItemStack offHand;
            ItemStack[] armors;

            try {
                items = getItemStacksFromConfig(config.getList(CLASSITEMS + "." + stringObjectEntry1.getKey() + ".items"));
                offHand = getItemStacksFromConfig(config.getList(CLASSITEMS + "." + stringObjectEntry1.getKey() + ".offhand"))[0];
                armors = getItemStacksFromConfig(config.getList(CLASSITEMS + "." + stringObjectEntry1.getKey() + ".armor"));
            } catch (final Exception e) {
                Bukkit.getLogger().severe(
                        "[PVP Arena] Error while parsing class, skipping: "
                                + stringObjectEntry1.getKey());
                debug(arena, e.getMessage());
                continue;
            }
            try {

                String classChest = (String) config.getConfigurationSection("classchests").get(stringObjectEntry1.getKey());
                PABlockLocation loc = new PABlockLocation(classChest);
                Chest c = (Chest) loc.toLocation().getBlock().getState();
                ItemStack[] contents = c.getInventory().getContents();

                items = Arrays.copyOfRange(contents, 0, contents.length - 5);
                offHand = contents[contents.length - 5];
                armors = Arrays.copyOfRange(contents, contents.length - 4, contents.length);

                arena.addClass(stringObjectEntry1.getKey(), items, offHand, armors);
                debug(arena, "adding class chest items to class " + stringObjectEntry1.getKey());

            } catch (Exception e) {
                arena.addClass(stringObjectEntry1.getKey(), items, offHand, armors);
                debug(arena, "adding class items to class " + stringObjectEntry1.getKey());
            }
        }
        arena.addClass("custom",
                new ItemStack[]{new ItemStack(Material.AIR, 1)},
                new ItemStack(Material.AIR, 1),
                new ItemStack[]{new ItemStack(Material.AIR, 1)});
        arena.setOwner(cfg.getString(CFG.GENERAL_OWNER));
        arena.setLocked(!cfg.getBoolean(CFG.GENERAL_ENABLED));
        if (config.getConfigurationSection("arenaregion") == null) {
            debug(arena, "arenaregion null");
        } else {
            debug(arena, "arenaregion not null");
            final Map<String, Object> regs = config.getConfigurationSection(
                    "arenaregion").getValues(false);
            for (String rName : regs.keySet()) {
                debug(arena, "arenaregion '" + rName + '\'');
                final ArenaRegion region = Config.parseRegion(arena, config,
                        rName);

                if (region.getWorld() == null) {
                    PVPArena.getInstance().getLogger().severe(
                            "Error while loading arena, world null: " + rName);
                } else {
                    arena.addRegion(region);
                }
            }
        }

        cfg.save();

        arena.getGoal().configParse(config);

        if (cfg.getYamlConfiguration().getConfigurationSection("teams") == null) {
            if (arena.isFreeForAll()) {
                config.set("teams.free", "WHITE");
            } else {
                config.set("teams.red", "RED");
                config.set("teams.blue", "BLUE");
            }
        }

        cfg.reloadMaps();

        final Map<String, Object> tempMap = cfg
                .getYamlConfiguration().getConfigurationSection("teams")
                .getValues(true);

        if (arena.isFreeForAll()) {
            if (!arena.getConfig().getBoolean(CFG.PERMS_TEAMKILL) && !"Infect".equals(arena.getConfig().getString(CFG.GENERAL_GOAL))) {
                PVPArena.getInstance().getLogger().warning("Arena " + arena.getName() + " is running in NO-PVP mode! Make sure people can die!");
            }
        } else {
            for (Map.Entry<String, Object> stringObjectEntry : tempMap.entrySet()) {
                final ArenaTeam team = new ArenaTeam(stringObjectEntry.getKey(),
                        (String) stringObjectEntry.getValue());
                arena.getTeams().add(team);
                debug(arena, "added team " + team.getName() + " => "
                        + team.getColorCodeString());
            }
        }

        debug(arena, "loading modules for arena");
        ArenaModuleManager.configParse(arena, config);
        cfg.save();
        cfg.reloadMaps();

        arena.setPrefix(cfg.getString(CFG.GENERAL_PREFIX));
        return true;
    }

    /**
     * Setup new Arena config
     *
     * @param arena new Arena
     * @param cfg   config
     */
    private static void createNewConfig(Arena arena, Config cfg) {
        cfg.set(Config.CFG.GENERAL_PREFIX, arena.getName());
        debug(arena, "set config goal {}", arena.getGoal().getName());
        cfg.set(CFG.GENERAL_GOAL, arena.getGoal().getName());
        cfg.createDefaults(arena.getGoal().getName(), new ArrayList<>());
    }

    /**
     * check if an arena is configured completely
     *
     * @param arena the arena to check
     * @return an error string if there is something missing, null otherwise
     */
    public static Set<String> isSetup(final Arena arena) {
        Set<String> errors = new HashSet<>();

        for (String editor : PAA_Edit.activeEdits.keySet()) {
            if (PAA_Edit.activeEdits.get(editor).getName().equals(
                    arena.getName())) {
                errors.add(Language.parse(MSG.ERROR_EDIT_MODE));
            }
        }

        for (String setter : PAA_Setup.activeSetups.keySet()) {
            if (PAA_Setup.activeSetups.get(setter).getName().equals(
                    arena.getName())) {
                errors.add(Language.parse(MSG.ERROR_SETUP_MODE));
            }
        }

        Optional.ofNullable(SpawnManager.isSpawnsSetup(arena)).ifPresent(errors::add);
        Optional.ofNullable(SpawnManager.isBlocksSetup(arena)).ifPresent(errors::add);

        return errors;
    }


}
