package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * PVP Arena Global Command class
 * </pre>
 * <p/>
 * The abstract class of a general command, including perm check
 *
 * @author slipcor
 * @version v0.10.0
 */

public abstract class AbstractGlobalCommand implements IArenaCommandHandler {
    private final String[] perms;

    AbstractGlobalCommand(final String[] permissions) {
        this.perms = permissions.clone();
    }

    static boolean argCountValid(final CommandSender sender, final String[] args,
                                 final Integer[] validCounts) {

        if (Arrays.stream(validCounts).anyMatch(count -> count == args.length)) {
            return true;
        }

        Arena.pmsg(
                sender,
                Language.parse(MSG.ERROR_INVALID_ARGUMENT_COUNT,
                        String.valueOf(args.length),
                        StringParser.joinArray(validCounts, "|")));
        return false;
    }

    public abstract void commit(CommandSender sender, String[] args);

    public abstract String getName();

    /**
     * Check if the global command also exists in arena context
     *
     * @return true if there is the same command for arena context
     */
    public boolean hasVersionForArena() {
        return false;
    }

    public boolean hasPerms(final CommandSender sender) {
        return hasPerms(sender, null, false);
    }

    @Override
    public boolean hasPerms(final CommandSender sender, final Arena arena, final boolean silent) {

        if (PermissionManager.hasAdminPerm(sender)) {
            return true;
        }

        boolean hasNotPermission = false;
        List<String> messages = new ArrayList<>();

        for (String perm : this.perms) {
            if (sender.hasPermission(perm)) {
                return true;
            }

            // Get the permission deny message
            if (!silent) {
                messages.add(PermissionManager.getMissingPermissionMessage(perm));
                hasNotPermission = true;
            }
        }

        if (!silent) {
            if (hasNotPermission) {
                messages.forEach(message -> Arena.pmsg(sender, message));
            } else {
                // perms is empty
                Arena.pmsg(
                        sender,
                        Language.parse(MSG.ERROR_NOPERM,
                                MSG.ERROR_NOPERM_X_ADMIN.toString()));
            }
        }

        return false;
    }

    protected abstract void displayHelp(CommandSender sender);
}
