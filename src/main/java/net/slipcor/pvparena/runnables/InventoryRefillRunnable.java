package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.*;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.managers.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Runnable class "Inventory"</pre>
 * <p/>
 * An arena timer to restore a player's inventory
 *
 * @author slipcor
 * @version v0.10.2
 */

public class InventoryRefillRunnable implements Runnable {
    private final Player player;
    private final List<ItemStack> additions = new ArrayList<>();
    private final Arena arena;
    private final boolean refill;

    public InventoryRefillRunnable(@NotNull Arena arena, final Player player, final List<ItemStack> keptItems) {
        this.arena = arena;
        this.additions.addAll(keptItems);
        this.player = player;
        this.refill = this.arena.getConfig().getBoolean(CFG.PLAYER_REFILLINVENTORY);

        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), this, 3L);
    }

    @Override
    public void run() {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(this.player.getName());
        debug(this.arena, "refilling " + this.player.getName());
        if (aPlayer.getStatus() == PlayerStatus.FIGHT) {
            if (aPlayer.hasCustomClass() && !this.arena.getConfig().getBoolean(CFG.PLAYER_REFILLCUSTOMINVENTORY) || !this.refill) {
                if (this.refill) {
                    if(!this.additions.isEmpty()){
                        ItemStack[] items = this.additions.toArray(new ItemStack[0]);
                        ArenaClass.equip(this.player, items);
                    } else {
                        String message = String.format("Can't refill inventory, please set %s, %s or %s parameter",
                                CFG.ITEMS_KEEPONRESPAWN.getNode(), CFG.ITEMS_KEEPALLONRESPAWN.getNode(),
                                CFG.PLAYER_REFILLCUSTOMINVENTORY.getNode());
                        PVPArena.getInstance().getLogger().info(message);
                    }
                }
                if (this.arena.getConfig().getBoolean(CFG.USES_WOOLHEAD)) {
                    final ArenaTeam aTeam = aPlayer.getArenaTeam();
                    final ChatColor chatColor = aTeam.getColor();
                    debug(aPlayer, "forcing woolhead: {} / {}", aTeam.getName(), chatColor.name());
                    this.player.getInventory().setHelmet(
                            new ItemStack(ColorUtils.getWoolMaterialFromChatColor(chatColor), 1));
                    this.arena.getGoal().refillInventory(this.player);
                }
            } else if (aPlayer.hasCustomClass()) {
                aPlayer.reloadInventory(false);

                for (final ItemStack item : this.additions) {
                    this.player.getInventory().addItem(item);
                }
            } else {
                InventoryManager.clearInventory(this.player);
                aPlayer.equipPlayerFightItems();

                for (final ItemStack item : this.additions) {
                    this.player.getInventory().addItem(item);
                }
            }
        } else {
            debug(aPlayer, "calls refillInventory but DOESN'T FIGHT");
        }
        this.player.setFireTicks(0);
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if (InventoryRefillRunnable.this.player.getFireTicks() > 0) {
                    InventoryRefillRunnable.this.player.setFireTicks(0);
                }
            }, 5L);
        } catch (Exception ignored) {
        }
    }
}
