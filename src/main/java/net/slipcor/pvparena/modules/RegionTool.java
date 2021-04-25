package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Module class "RegionTool"</pre>
 * <p/>
 * Enables region debug via WAND item
 *
 * @author slipcor
 */

public class RegionTool extends ArenaModule {
    public RegionTool() {
        super("RegionTool");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getPlayer().getEquipment().getItemInMainHand() == null
                || event.getPlayer().getEquipment().getItemInMainHand().getType() == Material.AIR) {
            return false;
        }

        if (event.getPlayer().getEquipment().getItemInMainHand().getType() == Material.AIR) {
            return false;
        }

        if (!PVPArena.hasAdminPerms(event.getPlayer())) {
            return false;
        }

        if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            debug(event.getPlayer(), "exiting: offhand");
            return false;
        }

        final Material wand = PVPArena.getInstance().getWandItem();
        debug(arena, event.getPlayer(), "wand is " + wand.name());

        if (event.getPlayer().getEquipment().getItemInMainHand().getType() == wand) {
            PABlockLocation loc = new PABlockLocation(event.getPlayer().getLocation());
            if (event.getClickedBlock() != null) {
                loc = new PABlockLocation(event.getClickedBlock().getLocation());
            }
            for (Arena arena : ArenaManager.getArenas()) {
                for (ArenaRegion region : arena.getRegions()) {
                    if (region.getShape().contains(loc)) {
                        arena.msg(event.getPlayer(), String.format("%sArena %s%s%s: region %s%s", ChatColor.WHITE,
                                ChatColor.AQUA, arena.getName(), ChatColor.WHITE, ChatColor.AQUA, region.getRegionName()));
                    }
                }
            }
        }

        return false;
    }
}
