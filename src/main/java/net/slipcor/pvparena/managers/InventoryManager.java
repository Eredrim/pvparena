package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Config.CFG;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Inventory Manager class</pre>
 * <p/>
 * Provides static methods to manage Inventories
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class InventoryManager {

    private static final String[] TOOLSUFFIXES = {"_AXE", "_PICKAXE", "_SPADE", "_HOE", "_SWORD", "BOW", "SHEARS"};

    private InventoryManager() {
    }

    /**
     * fully clear a player's inventory
     *
     * @param player the player to clear
     */
    public static void clearInventory(final Player player) {
        debug(player, "fully clear player inventory: " + player.getName());

        player.closeInventory();

        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setBoots(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
    }

    /**
     * drop a player's inventory
     *
     * @param player the player to empty
     * @return a list of the items that could be returned
     */
    public static List<ItemStack> drop(final Player player) {
        List<ItemStack> keptItems = new ArrayList<>();

        debug(player, "dropping player inventory: " + player.getName());
        List<Material> excludedItems = new ArrayList<>();
        List<ItemStack> toKeepItems = new ArrayList<>();

        ArenaPlayer ap = ArenaPlayer.fromPlayer(player);

        boolean keepAll = false;

        if (ap != null && ap.getArena() != null) {
            excludedItems = Arrays.stream(ap.getArena().getConfig().getItems(CFG.ITEMS_EXCLUDEFROMDROPS))
                    .filter(Objects::nonNull)
                    .map(ItemStack::getType)
                    .collect(Collectors.toList());

            keepAll = ap.getArena().getConfig().getBoolean(CFG.ITEMS_KEEPALLONRESPAWN);
            if (!keepAll) {
                toKeepItems = Arrays.asList(ap.getArena().getConfig().getItems(CFG.ITEMS_KEEPONRESPAWN));
            }
        }

        for (ItemStack dropped : player.getInventory().getContents()) {

            if (dropped == null || dropped.getType() == Material.AIR) {
                continue;
            }

            if (excludedItems.contains(dropped.getType())) {
                continue;
            }

            if (keepAll) {
                keptItems.add(dropped.clone());
                continue;
            }

            List<ItemStack> selectedKeptItems = toKeepItems.stream()
                    .filter(item -> dropped.getType() == item.getType())
                    .filter(item -> !item.hasItemMeta() || item.getItemMeta().getDisplayName().equals(dropped.getItemMeta().getDisplayName()))
                    .filter(item -> !item.hasItemMeta() || !item.getItemMeta().hasLore() || item.getItemMeta().getLore().equals(dropped.getItemMeta().getLore()))
                    .map(ItemStack::clone)
                    .collect(Collectors.toList());

            if(!selectedKeptItems.isEmpty()) {
                keptItems.addAll(selectedKeptItems);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), dropped);
            }
        }
        player.getInventory().clear();
        ap.setMayDropInventory(false);
        return keptItems;
    }

    public static boolean receivesDamage(final ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        for (String s : TOOLSUFFIXES) {
            if (item.getType().name().endsWith(s)) {
                return true;
            }
        }

        return false;
    }

    public static void dropExp(final Player player, final int exp) {
        if (exp < 1) {
            return;
        }
        final Location loc = player.getLocation();

        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                final ExperienceOrb orb = loc.getWorld().spawn(loc, ExperienceOrb.class);
                orb.setExperience(exp);
            }, 20L);
        } catch (final Exception ignored) {

        }
    }

    public static void transferItems(final Player player, final Inventory blockInventory) {
        final ItemStack[] oldItems = blockInventory.getContents().clone();
        for (ItemStack items : oldItems) {
            final Map<Integer, ItemStack> remaining = player.getInventory().addItem(items);
            blockInventory.remove(items);
            if (!remaining.isEmpty()) {
                for (ItemStack item : remaining.values()) {
                    blockInventory.addItem(item);
                }
            }
        }
    }
}
