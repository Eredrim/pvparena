package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.managers.InventoryManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <pre>Arena Runnable class "DamageReset"</pre>
 * <p/>
 * An arena timer to reset people's armor / weapon
 *
 * @author slipcor
 * @version v0.9.8
 */

public class DamageResetRunnable implements Runnable {

    private final Arena arena;
    private final Player attacker;
    private final Player defender;

    public DamageResetRunnable(final Arena arena, final Player attacker, final Player defender) {
        this.arena = arena;
        this.attacker = attacker;
        this.defender = defender;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        if (!this.arena.getConfig().getBoolean(CFG.DAMAGE_WEAPONS)) {
            try {
                if (InventoryManager.receivesDamage(this.attacker.getItemInHand())) {
                    this.attacker.getItemInHand().setDurability((short) 0);
                    this.attacker.updateInventory();
                }
            } catch (final Exception e) {
            }
        }

        if (!this.arena.getConfig().getBoolean(CFG.DAMAGE_ARMOR)) {
            try {
                final ItemStack[] items = this.defender.getInventory().getArmorContents();

                for (ItemStack is : items) {
                    if (is == null || !is.getType().name().endsWith("_HELMET")) {
                        continue;
                    }
                    is.setDurability((short) 0);
                }
                this.defender.updateInventory();
            } catch (final Exception e) {
            }
        }
    }

}
