package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.ItemStackUtils.getItemStacksFromConfig;

/**
 * <pre>GlobalClasses class</pre>
 * <p/>
 * Provides a SINGLETON instance to manage global classes
 *
 * @author Eredrim
 * @version v2.1.0
 */
public final class GlobalClasses {

    private static final String PATH = "classes.yml";
    private static final String CLASSES_SECTION = "classes";
    private static final String CHESTS_SECTION = "classchests";
    private static GlobalClasses INSTANCE;

    private final YamlConfiguration yamlConfig;
    private final File yamlFile;
    private final Map<String, ArenaClass> classMap = new HashMap<>();

    private GlobalClasses() {
        // private constructor to prevent instantiation
        this.yamlFile = new File(PVPArena.getInstance().getDataFolder(), PATH);
        this.yamlConfig = YamlConfiguration.loadConfiguration(this.yamlFile);
    }

    public static GlobalClasses getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GlobalClasses();
        }
        return INSTANCE;
    }

    public @Nullable ArenaClass get(String className) {
        return this.classMap.get(className);
    }

    public Set<String> getNames() {
        return this.classMap.keySet();
    }

    public void load() {
        this.classMap.clear();

        if (this.yamlConfig.get(CLASSES_SECTION) == null) {
            this.yamlConfig.addDefault(CLASSES_SECTION, new HashMap<>());
            this.yamlConfig.options().copyDefaults(true);

            try {
                this.yamlConfig.save(this.yamlFile);
                this.yamlConfig.load(this.yamlFile);
            } catch (IOException | InvalidConfigurationException e1) {
                e1.printStackTrace();
            }
        }

        ConfigurationSection classesSection = this.yamlConfig.getConfigurationSection(CLASSES_SECTION);
        if(classesSection != null) {
            classesSection.getKeys(false).forEach(className -> {
                try {
                    ConfigurationSection classesCfg = classesSection.getConfigurationSection(className);
                    ItemStack[] items = getItemStacksFromConfig(classesCfg.getList("items"));
                    ItemStack offHand = getItemStacksFromConfig(classesCfg.getList("offhand"))[0];
                    ItemStack[] armors = getItemStacksFromConfig(classesCfg.getList("armor"));
                    this.classMap.put(className, new ArenaClass(className, items, offHand, armors));
                } catch (Exception e) {
                    PVPArena.getInstance().getLogger().warning("(classes.yml) Error while parsing class, skipping: " + className);
                    debug("Error details: {}, Cause: {}", e.getMessage(), e.getCause());
                }
            });
        }

        ConfigurationSection chestSection = this.yamlConfig.getConfigurationSection(CHESTS_SECTION);
        if (chestSection != null) {
            chestSection.getValues(false).forEach((className, rawLocation) -> {
                try {
                    PABlockLocation loc = new PABlockLocation((String) rawLocation);
                    this.classMap.put(className, loadFromChestLocation(className, loc));
                } catch (Exception e) {
                    PVPArena.getInstance().getLogger().warning("(classes.yml) Error while parsing location of classchest, skipping: " + className);
                    debug("Error details: {}, Cause: {}", e.getMessage(), e.getCause());
                }
            });
        }
    }

    public void addToArena(Arena arena) {
        this.classMap.forEach((key, value) -> arena.addClass(key, value.getItems(), value.getOffHand(), value.getArmors()));
    }

    public void serializeAndSave(String className, ItemStack[] items, ItemStack offHand, ItemStack[] armors) throws IOException {
        this.yamlConfig.set(String.format("%s.%s.items", CLASSES_SECTION, className), Arrays.asList(items));
        this.yamlConfig.set(String.format("%s.%s.offhand", CLASSES_SECTION, className), Arrays.asList(offHand));
        this.yamlConfig.set(String.format("%s.%s.armor", CLASSES_SECTION, className), Arrays.asList(armors));

        this.yamlConfig.save(this.yamlFile);
        this.classMap.put(className, new ArenaClass(className, items, offHand, armors));
    }

    public void serializeAndSaveChest(String className, PABlockLocation location) throws IOException {
        this.yamlConfig.set(String.format("%s.%s", CHESTS_SECTION, className), location.toString());

        this.yamlConfig.save(this.yamlFile);
        this.classMap.put(className, loadFromChestLocation(className, location));
    }

    public void remove(String className) throws IOException {
        this.yamlConfig.set(String.format("%s.%s", CLASSES_SECTION, className), null);
        this.yamlConfig.save(this.yamlFile);
        this.classMap.remove(className);
    }

    public void removeChest(String className) throws IOException {
        this.yamlConfig.set(String.format("%s.%s", CHESTS_SECTION, className), null);
        this.yamlConfig.save(this.yamlFile);
        this.classMap.remove(className);
    }

    private static ArenaClass loadFromChestLocation(String className, PABlockLocation location) {
        Chest c = (Chest) location.toLocation().getBlock().getState();
        ItemStack[] contents = c.getInventory().getContents();
        int chestSize = c.getInventory().getSize();
        ItemStack[] items = Arrays.copyOfRange(contents, 0, chestSize-5);
        ItemStack offHand = ofNullable(contents[chestSize-5]).orElse(new ItemStack(Material.AIR));
        ItemStack[] armors = Arrays.copyOfRange(contents, chestSize-4, chestSize);
        return new ArenaClass(className, items, offHand, armors);
    }
}
