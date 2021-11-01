package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PAClassSign;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
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
import static net.slipcor.pvparena.arena.ArenaPlayer.Status.FIGHT;
import static net.slipcor.pvparena.arena.ArenaPlayer.Status.LOUNGE;

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
        if (!hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0,1})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, Language.parse(arena, MSG.ERROR_ONLY_PLAYERS));
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        ArenaPlayer.Status pStatus = aPlayer.getStatus();

        // Player can change arena class only in lounge or in fight with ingameClassSwith parameter set to true
        if(!arena.equals(aPlayer.getArena()) || !asList(LOUNGE, FIGHT).contains(pStatus) ||
                (pStatus == FIGHT && !arena.getArenaConfig().getBoolean(CFG.USES_INGAMECLASSSWITCH))) {
            return;
        }

        if (args.length < 1 || "custom".equalsIgnoreCase(args[0])) {
            Set<String> classes = new TreeSet<>();
            for (ArenaClass ac : arena.getClasses()) {
                if ("custom".equalsIgnoreCase(ac.getName())) {
                    continue;
                }
                classes.add(ChatColor.GREEN + ac.getName() + ChatColor.WHITE);
            }
            arena.msg(sender, Language.parse(arena, MSG.CLASS_LIST, StringParser.joinSet(classes, ", ")));
            return;
        }

        final ArenaClass aClass = arena.getClass(args[0]);

        if (aClass == null) {
            sender.sendMessage(Language.parse(arena, MSG.ERROR_CLASS_NOT_FOUND, args[0]));
            return;
        }

        if (!PermissionManager.hasExplicitClassPerm(sender, arena, aClass)) {
            arena.msg(sender,
                    Language.parse(arena, MSG.ERROR_NOPERM_CLASS, aClass.getName()));
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
                if (aPlayer.getArenaClass().getName().equals(s.getLine(0))) {
                    oldSign = sign;
                }
                if (aClass.getName().equals(s.getLine(0))) {
                    if (!sign.add((Player) sender)) {
                        error = true;
                    }
                }
            } catch (Exception e) {

            }
        }

        if (error) {
            if (oldSign != null) {
                oldSign.add((Player) sender);
            }
            arena.msg(sender,
                    Language.parse(arena, MSG.ERROR_CLASS_FULL, aClass.getName()));
            return;
        }

        if (!arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSWITCH_AFTER_RESPAWN) || !arena.isFightInProgress()) {
            InventoryManager.clearInventory(aPlayer.get());
            aPlayer.setArenaClass(aClass);
            if (aPlayer.getArenaClass() != null) {
                ArenaPlayer.givePlayerFightItems(arena, aPlayer.get());

                arena.msg(sender,
                        Language.parse(arena, MSG.CLASS_SELECTED, aClass.getName()));
            }
        } else if (aPlayer.getArenaClass() != null) {
            arena.msg(sender,
                    Language.parse(arena, MSG.CLASS_SELECTED_RESPAWN, aClass.getName()));
            aPlayer.setNextArenaClass(aClass);
        }
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.ARENACLASS));
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
