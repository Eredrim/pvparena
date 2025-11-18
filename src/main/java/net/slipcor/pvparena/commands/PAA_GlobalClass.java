package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.GlobalClasses;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.managers.InventoryManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.slipcor.pvparena.core.ItemStackUtils.cloneItemStacks;

/**
 * <pre>PVP Arena globalclass command</pre>
 * <p/>
 * A command to edit global classes
 *
 * @author Eredrim
 * @version v2.1.0
 */

public class PAA_GlobalClass extends AbstractGlobalCommand {

    public static final Map<UUID, String> activeGlobalClassEdits = new HashMap<>();
    private static final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    public PAA_GlobalClass() {
        super(new String[]{"pvparena.cmds.globalclass"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{1, 2})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, Language.MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        // /pa {arenaname} !gc save [name]
        // /pa {arenaname} !gc load [name]
        // /pa {arenaname} !gc remove [name]
        // /pa {arenaname} !gc savechest [name]
        // /pa {arenaname} !gc removechest [name]

        final Player player = (Player) sender;
        String classname;

        if (args.length == 1) {
            // when no 2nd arg, save/remove/load class with name of player's current class

            String currentClass = activeGlobalClassEdits.get(player.getUniqueId());
            if (currentClass == null) {
                Arena.pmsg(player, Language.MSG.ERROR_CLASS_NOT_GIVEN);
                return;
            }
            classname = currentClass;
        } else {
            classname = args[1];
        }

        try {
            if ("load".equalsIgnoreCase(args[0])) {
                savedInventories.putIfAbsent(player.getUniqueId(), cloneItemStacks(player.getInventory().getContents()));
                InventoryManager.clearInventory(player);
                equipGlobalClass(player, classname);
            } else if ("leave".equalsIgnoreCase(args[0])) {
                if (activeGlobalClassEdits.containsKey(player.getUniqueId())) {
                    activeGlobalClassEdits.remove(player.getUniqueId());
                    InventoryManager.clearInventory(player);
                    ItemStack[] savedInventory = savedInventories.remove(player.getUniqueId());
                    player.getInventory().setContents(savedInventory);
                    Arena.pmsg(sender, Language.MSG.CMD_GLOBALCLASS_QUIT);
                }
            } else {
                GlobalClasses globalClasses = GlobalClasses.getInstance();
                if ("save".equalsIgnoreCase(args[0])) {
                    ItemStack[] storage = player.getInventory().getStorageContents();
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    ItemStack[] armor = player.getInventory().getArmorContents();

                    globalClasses.serializeAndSave(classname, storage, offhand, armor);
                    Arena.pmsg(player, Language.MSG.CMD_CLASS_SAVED, classname);

                } else if ("remove".equalsIgnoreCase(args[0])) {
                    if (globalClasses.get(classname) == null) {
                        Arena.pmsg(player, Language.MSG.ERROR_CLASS_NOT_FOUND, classname);
                    } else {
                        globalClasses.remove(classname);
                        Arena.pmsg(player, Language.MSG.CMD_CLASS_REMOVED, classname);
                    }
                } else if ("savechest".equalsIgnoreCase(args[0])) {
                    Block b = player.getTargetBlock(null, 10);
                    if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST) {
                        Arena.pmsg(sender, Language.MSG.ERROR_NO_CHEST);
                        return;
                    }
                    PABlockLocation loc = new PABlockLocation(b.getLocation());

                    globalClasses.serializeAndSaveChest(classname, loc);
                    Arena.pmsg(player, Language.MSG.CMD_CLASS_SAVED, classname);

                } else if ("removechest".equalsIgnoreCase(args[0])) {
                    if (globalClasses.get(classname) == null) {
                        Arena.pmsg(player, Language.MSG.ERROR_CLASS_NOT_FOUND, classname);
                    } else {
                        globalClasses.removeChest(classname);
                        Arena.pmsg(player, Language.MSG.CMD_CLASS_REMOVED, classname);
                    }
                } else {
                    Arena.pmsg(sender, Language.MSG.ERROR_ARGUMENT, args[0], "load, save, remove, saveChest, removeChest");
                }
            }
        } catch (IOException e) {
            Arena.pmsg(player, Language.MSG.ERROR_ERROR, "Unable to save global classes file");
            e.printStackTrace();
        }

    }

    @Override
    public boolean hasVersionForArena() {
        return true;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("globalclass");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!gc");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"load"});
        result.define(new String[]{"save"});
        result.define(new String[]{"saveChest"});
        result.define(new String[]{"remove"});
        result.define(new String[]{"leave"});
        GlobalClasses.getInstance().getNames().forEach(className -> {
            result.define(new String[]{"load", className});
            result.define(new String[]{"save", className});
            result.define(new String[]{"remove", className});
            result.define(new String[]{"saveChest", className});
            result.define(new String[]{"removeChest", className});
        });
        return result;
    }

    private static void equipGlobalClass(Player player, String className) {
        ArenaClass globalClass = GlobalClasses.getInstance().get(className);
        if (globalClass == null) {
            Arena.pmsg(player, Language.MSG.ERROR_CLASS_NOT_FOUND, className);
        } else {
            Arena.pmsg(player, Language.MSG.CMD_GLOBALCLASS_PREVIEW, className);
            activeGlobalClassEdits.put(player.getUniqueId(), className);
            globalClass.equip(player);
        }
    }
}
