package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaGoalManager;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.loadables.ModuleType;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toList;
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

        final String goalName = cfg.getString(CFG.GENERAL_GOAL);
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
            if(isOutdatedConfigVersion(cfg.getString(CFG.VERSION, ""))) {
                throw new UnsupportedClassVersionError();
            }

            cfg.createDefaults(goalName, modules);

            if (goalManager.hasLoadable(goalName)) {
                ArenaGoal goal = goalManager.getNewInstance(goalName);
                arena.setGoal(goal, false);
            } else {
                PVPArena.getInstance().getLogger().warning(String.format("Goal referenced in arena '%s' not found (uninstalled?): %s", arena.getName(), goalName));
                return false;
            }

            List<String> list = cfg.getStringList(CFG.LISTS_MODS.getNode(), new ArrayList<>());
            boolean hasJoinMod = false;
            boolean hasSpectateMod = false;
            for (String moduleName : list) {
                ArenaModuleManager moduleManager = PVPArena.getInstance().getAmm();
                if (!moduleManager.hasLoadable(moduleName)) {
                    PVPArena.getInstance().getLogger().warning(String.format("Module referenced in arena '%s' not found (uninstalled?): %s", arena.getName(), moduleName));
                    continue;
                }
                ArenaModule module = moduleManager.getNewInstance(moduleName);
                arena.addModule(module, false);
                if (module.getType() == ModuleType.JOIN) {
                    hasJoinMod = true;
                }

                if (module.getType() == ModuleType.SPECTATE) {
                    hasSpectateMod = true;
                }
            }
            if (!hasJoinMod) {
                PVPArena.getInstance().getLogger().warning(String.format("Arena '%s' has no JOIN module! Please add one or arena will not work.", arena.getName()));
            }
            if (!hasSpectateMod) {
                PVPArena.getInstance().getLogger().warning(String.format("Arena '%s' has no SPECTATE module! Please add one or arena will not work.", arena.getName()));
            }
        }

        if (config.get(CLASSITEMS) == null && PVPArena.getInstance().getConfig().get(CLASSITEMS) == null) {
            config.createSection(CLASSITEMS, generateDefaultClasses());
        }

        debug(arena, "setting default config for goal {}", arena.getGoal());
        arena.getGoal().setDefaults(config);

        final Map<String, Object> classes = config.getConfigurationSection(CLASSITEMS).getValues(false);
        arena.getClasses().clear();
        debug(arena, "reading class items");
        ArenaClass.loadGlobalClasses(arena);
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
            final Set<String> regionKeys = config.getConfigurationSection(
                    "arenaregion").getKeys(false);
            for (String rName : regionKeys) {
                debug(arena, "arenaregion '" + rName + '\'');
                try {
                    final ArenaRegion region = Config.parseRegion(arena, config, rName);

                    if (region.getWorld() == null) {
                        PVPArena.getInstance().getLogger().severe("Error while loading arena, world null: " + rName);
                    } else {
                        arena.addRegion(region);
                    }
                } catch (Exception e) {
                    PVPArena.getInstance().getLogger().warning(String.format("Can't parse region %s of arena %s. Skipping.", rName, arena.getName()));
                }

            }
        }

        if (arena.isFreeForAll()) {
            if (!arena.getConfig().getBoolean(CFG.PERMS_TEAMKILL) && !"Infect".equals(arena.getConfig().getString(CFG.GENERAL_GOAL))) {
                PVPArena.getInstance().getLogger().warning("Arena " + arena.getName() + " is running in NO-PVP mode! Make sure people can die!");
            }
        } else {
            cfg.getYamlConfiguration()
                    .getConfigurationSection("teams")
                    .getValues(false)
                    .entrySet()
                    .forEach(entry -> {
                        ArenaTeam team = new ArenaTeam(entry.getKey(), (String) entry.getValue());
                        arena.getTeams().add(team);
                        debug(arena, String.format("added team %s => %s", team.getName(), team.getColorCodeString()));
                    });
        }

        debug(arena, "loading modules for arena");
        ArenaModuleManager.configParse(arena, config);
        cfg.save();
        cfg.load();

        arena.setPrefix(cfg.getString(CFG.GENERAL_PREFIX));
        return true;
    }

    private static boolean isOutdatedConfigVersion(String versionString) {
        if(versionString.contains(".")) {
            String firstDigit = versionString.split("\\.")[0];
            try {
                return Integer.parseInt(firstDigit) < 2;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return true;
    }

    /**
     * Move configuration from previous PA version to a dedicated folder
     * @param cfgFile the config file
     */
    public static void moveOldConfig(File cfgFile) {
        PVPArena pluginInstance = PVPArena.getInstance();
        try {
            File oldArenasDir = new File(String.format("%s/arenas_old", pluginInstance.getDataFolder().getPath()));
            pluginInstance.getLogger().warning(String.format("Config file %s is outdated! It has been skipped and moved to %s", cfgFile.getName(), oldArenasDir.getPath()));
            oldArenasDir.mkdirs();
            Files.move(cfgFile.toPath(), oldArenasDir.toPath().resolve(cfgFile.getName()), REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Setup new Arena config
     *
     * @param arena new Arena
     * @param cfg   config
     */
    private static void createNewConfig(Arena arena, Config cfg) {
        cfg.set(Config.CFG.ID, UUID.randomUUID().toString());
        cfg.set(Config.CFG.GENERAL_PREFIX, arena.getName());
        List<String> modules = arena.getMods().stream()
                .map(ArenaModule::getName)
                .collect(toList());
        debug(arena, "set config goal {}", arena.getGoal().getName());
        debug(arena, "set config default modules {}", arena.getGoal().getName());
        cfg.set(CFG.GENERAL_GOAL, arena.getGoal().getName());
        cfg.set(CFG.LISTS_MODS, modules);
        cfg.createDefaults(arena.getGoal().getName(), modules);
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

        Optional.ofNullable(SpawnManager.isSpawnsSetup(arena)).ifPresent(errors::add);

        if(errors.isEmpty()) {
            Optional.ofNullable(SpawnManager.areSpawnsInBattlefield(arena)).ifPresent(errors::add);
        }

        Optional.ofNullable(SpawnManager.isBlocksSetup(arena)).ifPresent(errors::add);

        return errors;
    }

    public static Map<String, Object> generateDefaultClasses() {
        Map<String, Object> classItems = new HashMap<>();

        classItems.put("Ranger", new HashMap<String, List<?>>() {{
            put("items", Utils.getItemStacksFromMaterials(Material.BOW, Material.ARROW));
            put("offhand", Utils.getItemStacksFromMaterials(Material.AIR));
            put("armor", Utils.getItemStacksFromMaterials(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS));
        }});

        classItems.put("Swordsman", new HashMap<String, List<?>>() {{
            put("items", Utils.getItemStacksFromMaterials(Material.DIAMOND_SWORD));
            put("offhand", Utils.getItemStacksFromMaterials(Material.AIR));
            put("armor", Utils.getItemStacksFromMaterials(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS));
        }});

        classItems.put("Tank", new HashMap<String, List<?>>() {{
            put("items", Utils.getItemStacksFromMaterials(Material.STONE_SWORD));
            put("offhand", Utils.getItemStacksFromMaterials(Material.AIR));
            put("armor", Utils.getItemStacksFromMaterials(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS));
        }});

        classItems.put("Pyro", new HashMap<String, List<?>>() {{
            put("items", Utils.getItemStacksFromMaterials(Material.FLINT_AND_STEEL, Material.TNT, Material.TNT, Material.TNT));
            put("offhand", Utils.getItemStacksFromMaterials(Material.AIR));
            put("armor", Utils.getItemStacksFromMaterials(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS));
        }});

        return classItems;
    }
}
