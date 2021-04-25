package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.IllegalPluginAccessException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.ItemStackUtils.getItemStacksFromConfig;

/**
 * <pre>Arena Class class</pre>
 * <p/>
 * contains Arena Class methods and variables for quicker access
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class ArenaClass {

    private final String name;
    private final ItemStack[] items;
    private final ItemStack offHand;
    private final ItemStack[] armors;

    private static final Map<String, ArenaClass> globals = new HashMap<>();

    private static final List<Material> OTHER_HELMET_LIST = asList(Material.PUMPKIN, Material.JACK_O_LANTERN, Material.PLAYER_HEAD);


    public static void addGlobalClasses() {
        globals.clear();
        final File classFile = new File(PVPArena.getInstance().getDataFolder(), "classes.yml");
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(classFile);

        cfg.addDefault("classes.Ranger",
                "BOW,ARROW:64,LEATHER_HELMET,LEATHER_CHESTPLATE,LEATHER_LEGGINGS,LEATHER_BOOTS");
        cfg.addDefault("classes.Swordsman",
                "DIAMOND_SWORD,IRON_HELMET,IRON_CHESTPLATE,IRON_LEGGINGS,IRON_BOOTS");
        cfg.addDefault("classes.Tank",
                "STONE_SWORD,DIAMOND_HELMET,DIAMOND_CHESTPLATE,DIAMOND_LEGGINGS,DIAMOND_BOOTS");
        cfg.addDefault("classes.Pyro",
                "FLINT_AND_STEEL,TNT:3,LEATHER_HELMET,LEATHER_CHESTPLATE,LEATHER_LEGGINGS,LEATHER_BOOTS");

        cfg.options().copyDefaults();
        try {
            cfg.save(classFile);
        } catch (final IOException e1) {
            e1.printStackTrace();
        }

        for (String className : cfg.getConfigurationSection("classes").getKeys(false)) {
            ItemStack[] items;
            ItemStack offHand;
            ItemStack[] armors;

            try {
                ConfigurationSection classesCfg = cfg.getConfigurationSection("classes").getConfigurationSection(className);
                items = getItemStacksFromConfig(classesCfg.getList("items"));
                offHand = getItemStacksFromConfig(classesCfg.getList("items"))[0];
                armors = getItemStacksFromConfig(classesCfg.getList("items"));
            } catch (final Exception e) {
                Bukkit.getLogger().severe(
                        "[PVP Arena] Error while parsing class, skipping: "
                                + className);
                e.printStackTrace();
                continue;
            }
            final String classChest;
            try {
                classChest = (String) cfg.getConfigurationSection("classchests").get(className);
                PABlockLocation loc = new PABlockLocation(classChest);
                Chest c = (Chest) loc.toLocation().getBlock().getState();
                ItemStack[] contents = c.getInventory().getContents();
                items = Arrays.copyOfRange(contents, 0, contents.length-5);
                offHand = contents[contents.length-5];
                armors = Arrays.copyOfRange(contents, contents.length-4, contents.length);
                globals.put(className, new ArenaClass(className, items, offHand, armors));
            } catch (Exception e) {
                globals.put(className, new ArenaClass(className, items, offHand, armors));
            }
        }
    }

    public static void addGlobalClasses(final Arena arena) {
        for (Map.Entry<String, ArenaClass> stringArenaClassEntry : globals.entrySet()) {
            arena.addClass(stringArenaClassEntry.getKey(), stringArenaClassEntry.getValue().items, stringArenaClassEntry.getValue().offHand, stringArenaClassEntry.getValue().armors);
        }
    }

    public static void equip(final Player player, final ItemStack[] items) {
        int i = 0;
        for (ItemStack item : items) {
            if (isArmorItem(item.getType())) {
                equipArmor(item, player.getInventory());
            } else {
                if (i == items.length - 1) {
                    player.getInventory().setItemInOffHand(item);
                    continue;
                }
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && "SPAWN".equals(item.getItemMeta().getDisplayName())) {
                    final String eggType = item.getType().name().replace("_SPAWN_EGG", "");

                    try {
                        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new Runnable(){
                            @Override
                            public void run() {
                                ArenaPlayer.fromPlayer(player).getArena().addEntity(
                                        player, player.getWorld().spawnEntity(player.getLocation(), EntityType.valueOf(eggType)));
                            }
                        }, 20L);
                    } catch(final IllegalPluginAccessException e) {

                    }
                } else {
                    player.getInventory().addItem(item);
                }
            }
            i++;
        }
    }

    public static void equip(final Player player, final ItemStack[][] itemArray) {
        try {
            player.getInventory().setItemInOffHand(itemArray[1][0]);
        } catch(ArrayIndexOutOfBoundsException e) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }

        for(ItemStack itemStack : itemArray[2]) {
            equipArmor(itemStack, player.getInventory());
        }

        for (ItemStack item : itemArray[0]) {
            if (item.getType().name().endsWith("_SPAWN_EGG")) {
                final String eggType = item.getType().name().replace("_SPAWN_EGG", "");

                try {
                    Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () ->
                            ArenaPlayer.fromPlayer(player).getArena().addEntity(
                                player, player.getWorld().spawnEntity(player.getLocation(), EntityType.valueOf(eggType))), 20L);
                } catch(final IllegalPluginAccessException ignored) {

                }
            } else {
                player.getInventory().addItem(item);
            }
        }
    }

    public void equip(final Player player) {
        debug(player, "Equipping player " + player.getName() + " with items!");
        for (ItemStack item : this.armors) {
            if (item != null) {
                equipArmor(item, player.getInventory());
            }
        }
        for (ItemStack item : this.items) {
            if (item == null) {
                continue;
            }
            if (isArmorItem(item.getType())) {
                equipArmor(item, player.getInventory());
            } else {
                player.getInventory().addItem(item);
            }
        }
        player.getInventory().setItemInOffHand(this.offHand);
    }

    private static void equipArmor(final ItemStack stack, final PlayerInventory inv) {
        final Material type = stack.getType();
        if (isHelmetItem(type)) {
            if (inv.getHelmet() != null && inv.getHelmet().getType() != Material.AIR) {
                inv.addItem(stack);
            } else {
                inv.setHelmet(stack);
            }
        } else if (isChestplateItem(type)) {
            if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) {
                inv.addItem(stack);
            } else {
                inv.setChestplate(stack);
            }
        } else if (isLeggingsItem(type)) {
            if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) {
                inv.addItem(stack);
            } else {
                inv.setLeggings(stack);
            }
        } else if (isBootsItem(type)) {
            if (inv.getBoots() != null && inv.getBoots().getType() != Material.AIR) {
                inv.addItem(stack);
            } else {
                inv.setBoots(stack);
            }
        }
    }

    public ArenaClass(final String className, final ItemStack[] classItems, final ItemStack offHand, final ItemStack[] armors) {
        this.name = className;
        this.offHand = offHand;
        this.items = classItems.clone();
        this.armors = armors.clone();
    }

    public String getName() {
        return this.name;
    }

    public ItemStack[] getArmors() {
        return this.armors.clone();
    }

    public ItemStack[] getItems() {
        return this.items.clone();
    }

    private static boolean isHelmetItem(Material material) {
        return material.name().endsWith("_HELMET") || material.name().endsWith("_WOOL") ||
                OTHER_HELMET_LIST.contains(material);
    }

    private static boolean isChestplateItem(Material material) {
        return material.name().endsWith("_CHESTPLATE") || material == Material.ELYTRA;
    }

    private static boolean isLeggingsItem(Material material) {
        return material.name().endsWith("_LEGGINGS");
    }

    private static boolean isBootsItem(Material material) {
        return material.name().endsWith("_BOOTS");
    }

    private static boolean isArmorItem(Material material) {
        return isBootsItem(material) || isLeggingsItem(material) || isChestplateItem(material) || isHelmetItem(material);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
