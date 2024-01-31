package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.ConfigNodeType;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static net.slipcor.pvparena.core.Utils.getSerializableItemStacks;

/**
 * <pre>PVP Arena SET Command class</pre>
 * <p/>
 * A command to set config values
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Set extends AbstractArenaCommand {

    public PAA_Set() {
        super(new String[]{"pvparena.cmds.set"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1, 2})) {
            return;
        }

        // args[0]
        // usage: /pa {arenaname} set [page]

        if (args.length < 2) {
            try {
                int page = Integer.parseInt(args[0]);

                page = Math.max(page, 1);

                final Map<String, String> keys = new HashMap<>();

                int position = 0;

                for (String node : arena.getConfig().getYamlConfiguration()
                        .getKeys(true)) {
                    if (CFG.getByNode(node) == null) {
                        continue;
                    }
                    if (position++ >= (page - 1) * 10) {
                        final String[] split = node.split("\\.");
                        keys.put(node, split[split.length - 1]);
                    }
                    if (keys.size() >= 10) {
                        break;
                    }
                }
                arena.msg(sender, String.format("%s------ config list [%d] ------", ChatColor.GOLD, page));
                keys.forEach((key, value) -> arena.msg(sender, String.format("%s => %s", value, CFG.getByNode(key).getType())));

            } catch (final Exception e) {
                arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[0]);
            }
            return;
        }

        // args[0]
        // usage: /pa {arenaname} set [node] [value]
        this.set(sender, arena, args[0], args[1]);
    }

    private Optional<String> findNodeWithEndMatchingWith(YamlConfiguration cfg, String endOfNode) {
        return cfg.getKeys(true).stream()
                .filter(key -> key.toLowerCase().endsWith('.' + endOfNode.toLowerCase()))
                .findFirst();
    }

    private void set(final CommandSender player, final Arena arena, final String nodeToCheck, final String value) {

        YamlConfiguration configFile = arena.getConfig().getYamlConfiguration();
        String node = nodeToCheck;
        if(!configFile.contains(nodeToCheck, false)) {
            node = this.findNodeWithEndMatchingWith(configFile, nodeToCheck).orElse(null);
        }

        ConfigNodeType type = (node == null || CFG.getByNode(node) == null) ? null : CFG.getByNode(node).getType();


        if (type == ConfigNodeType.BOOLEAN) {
            if ("true".equalsIgnoreCase(value)) {
                arena.getConfig().setManually(node, Boolean.TRUE);
                arena.msg(player, Language.parse(MSG.SET_DONE, node, true));
            } else if ("false".equalsIgnoreCase(value)) {
                arena.getConfig().setManually(node, Boolean.FALSE);
                arena.msg(player, Language.parse(MSG.SET_DONE, node, false));
            } else {
                arena.msg(player, Language.parse(MSG.ERROR_ARGUMENT_TYPE, value, "boolean (true|false)"));
                return;
            }
        } else if (type == ConfigNodeType.STRING) {
            arena.getConfig().setManually(node, String.valueOf(value));
            arena.msg(player, Language.parse(MSG.SET_DONE, node, value));
        } else if (type == ConfigNodeType.GAMEMODE) {
            if(Stream.of(GameMode.values()).anyMatch(gm -> gm.name().equalsIgnoreCase(value))) {
                arena.getConfig().setManually(node, value);
                arena.msg(player, Language.parse(MSG.SET_DONE, node, value));
            } else {
                arena.msg(player, Language.parse(MSG.ERROR_ARGUMENT_TYPE, value, "Gamemode (e.g. SURVIVAL)"));
            }
        } else if (type == ConfigNodeType.INT) {
            final int iValue;

            try {
                iValue = Integer.parseInt(value);
            } catch (final Exception e) {
                arena.msg(player, MSG.ERROR_NOT_NUMERIC, value);
                return;
            }
            arena.getConfig().setManually(node, iValue);
            arena.msg(player, Language.parse(MSG.SET_DONE, node, iValue));
        } else if (type == ConfigNodeType.DOUBLE) {
            final double dValue;

            try {
                dValue = Double.parseDouble(value);
            } catch (final Exception e) {
                arena.msg(player, Language.parse(MSG.ERROR_ARGUMENT_TYPE, value, "double (e.g. 12.00)"));
                return;
            }
            arena.getConfig().setManually(node, dValue);
            arena.msg(player, Language.parse(MSG.SET_DONE, node, dValue));
        } else if (type == ConfigNodeType.MATERIAL) {
            if ("hand".equalsIgnoreCase(value)) {
                if (player instanceof Player) {

                    String itemDefinition = ((Player) player).getEquipment().getItemInMainHand().getType().name();
                    arena.getConfig().setManually(node, itemDefinition);
                    arena.msg(player, Language.parse(MSG.SET_DONE, node, itemDefinition));
                } else {
                    arena.msg(player, MSG.ERROR_ONLY_PLAYERS);
                    return;
                }
            } else {
                try {
                    final Material mat = Material.valueOf(value.toUpperCase());
                    if (mat != Material.AIR) {
                        arena.getConfig().setManually(node, mat.name());
                        arena.msg(player, MSG.SET_DONE, node, mat.name());
                    }
                } catch (final Exception e) {
                    arena.msg(player, Language.parse(MSG.ERROR_ARGUMENT_TYPE, value, "valid ENUM or item ID"));
                    return;
                }
            }
        } else if (type == ConfigNodeType.ITEMS) {
            if ("hand".equalsIgnoreCase(value)) {
                if (player instanceof Player) {

                    ItemStack item = ((Player) player).getInventory().getItemInMainHand();
                    arena.getConfig().setManually(node, getSerializableItemStacks(item));
                    arena.msg(player, MSG.SET_DONE, node, item.getType().name());
                } else {
                    arena.msg(player, MSG.ERROR_ONLY_PLAYERS);
                    return;
                }
            } else if ("inventory".equalsIgnoreCase(value)) {
                if (player instanceof Player) {

                    final ItemStack[] items = ((Player) player).getInventory().getContents();
                    arena.getConfig().setManually(node, getSerializableItemStacks(items));
                    arena.msg(player, MSG.SET_DONE, node, "inventory");
                } else {
                    arena.msg(player, MSG.ERROR_ONLY_PLAYERS);
                    return;
                }
            } else {
                arena.msg(player, MSG.SET_ITEMS_NOT);
                return;
            }
        } else {
            arena.msg(player, Language.parse(MSG.SET_UNKNOWN, node, value));
            arena.msg(player, Language.parse(MSG.SET_HELP, node, value));
            return;
        }
        arena.getConfig().save();
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.SET);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("set");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!s");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return CFG.getTabTree();
    }
}
