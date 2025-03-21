package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PAClassSign;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.arena.PlayerStatus.*;

/**
 * <pre>PVP Arena JOIN Command class</pre>
 * <p/>
 * A command to join an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAG_Arenaclass extends AbstractArenaCommand {
    public PAG_Arenaclass() {
        super(new String[]{"pvparena.cmds.arenaclass"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0,1})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer((Player) sender);
        PlayerStatus pStatus = arenaPlayer.getStatus();

        // Player can change arena class only in lounge or in fight with ingameClassSwith parameter set to true
        if(!arena.equals(arenaPlayer.getArena()) || !asList(LOUNGE, READY, FIGHT, DEAD).contains(pStatus) ||
                (pStatus == FIGHT && !arena.getConfig().getBoolean(CFG.USES_INGAMECLASSSWITCH))) {
            return;
        }

        if (args.length < 1) {
            Set<String> classes = new TreeSet<>();
            for (ArenaClass ac : arena.getClasses()) {
                if (ac.getName().equals("custom")) {
                    continue;
                }
                classes.add(ChatColor.GREEN + ac.getName() + ChatColor.WHITE);
            }
            arena.msg(sender, MSG.CMD_CLASS_LIST, StringParser.joinSet(classes, ", "));
            return;
        }

        final ArenaClass arenaClass = arena.getArenaClass(args[0]);
        if (arenaClass == null) {
            sender.sendMessage(Language.parse(MSG.ERROR_CLASS_NOT_FOUND, args[0]));
            return;
        }

        if (!PermissionManager.hasExplicitClassPerm(sender, arena, arenaClass)) {
            arena.msg(sender, MSG.ERROR_NOPERM_CLASS, arenaClass.getName());
            return;
        }

        if (ArenaModuleManager.cannotSelectClass(arena, (Player) sender, args[0])) {
            return;
        }
        PAClassSign.remove(arena.getSigns(), (Player) sender);

        PAClassSign oldSign = null;
        boolean error = false;

        for (PAClassSign sign : arena.getSigns()) {
            try {
                Sign s = (Sign) sign.getLocation().toLocation().getBlock().getState();
                if (arenaPlayer.getArenaClass().getName().equals(s.getLine(0))) {
                    oldSign = sign;
                }
                if (arenaClass.getName().equals(s.getLine(0))) {
                    if (!sign.add((Player) sender)) {
                        error = true;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (error) {
            if (oldSign != null) {
                oldSign.add((Player) sender);
            }
            arena.msg(sender, MSG.ERROR_CLASS_FULL, arenaClass.getName());
            return;
        }

        if (!arena.getConfig().getBoolean(CFG.GENERAL_CLASSSWITCH_AFTER_RESPAWN) || !arena.isFightInProgress()) {
            InventoryManager.clearInventory(arenaPlayer.getPlayer());
            arenaPlayer.setArenaClass(arenaClass);
            if (arenaPlayer.getArenaClass() != null) {
                arenaPlayer.equipPlayerFightItems();
                arena.msg(sender, MSG.CMD_ARENACLASS_SELECTED, arenaClass.getName());
            }
        } else if (arena.getConfig().getBoolean(CFG.GENERAL_CLASSSWITCH_AFTER_RESPAWN) || arenaPlayer.getStatus() == DEAD) {
            arena.msg(sender, MSG.CMD_ARENACLASS_SELECTED_RESPAWN, arenaClass.getName());
            arenaPlayer.setNextArenaClass(arenaClass);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("arenaclass");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-ac");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);

        if (arena != null) {
            arena.getClasses().stream()
                    .filter(aClass -> !"custom".equalsIgnoreCase(aClass.getName()))
                    .forEach(aClass -> result.define(new String[]{aClass.getName()}));
        }

        return result;
    }
}
