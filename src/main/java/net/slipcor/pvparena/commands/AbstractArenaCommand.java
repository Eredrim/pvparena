package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * <pre>
 * PVP Arena ArenaCommand class
 * </pre>
 * <p/>
 * The abstract class of a command belonging to an arena, including perm check
 *
 * @author slipcor
 * @version v0.10.0
 */

public abstract class AbstractArenaCommand implements IArenaCommandHandler {
    protected final String[] perms;

    AbstractArenaCommand(final String[] permissions) {
        this.perms = permissions.clone();
    }

    public static boolean argCountValid(final CommandSender sender, final Arena arena,
                                        final String[] args, final Integer[] validCounts) {

        if (Arrays.stream(validCounts).anyMatch(count -> count == args.length)) {
            return true;
        }

        final String msg = Language.parse(MSG.ERROR_INVALID_ARGUMENT_COUNT,
                String.valueOf(args.length),
                StringParser.joinArray(validCounts, "|"));

        if (arena == null) {
            Arena.pmsg(sender, msg);
            return false;
        }
        arena.msg(sender, msg);
        return false;
    }

    public abstract void commit(Arena arena, CommandSender sender, String[] args);

    public abstract String getName();

    public boolean hasPerms(CommandSender sender, Arena arena) {
        return this.hasPerms(sender, arena, false);
    }

    @Override
    public boolean hasPerms(CommandSender sender, Arena arena, boolean silent) {

        if (arena != null && PermissionManager.hasBuilderPerm(sender, arena)) {
            return true;
        }

        boolean hasPermission = Arrays.stream(this.perms).anyMatch(sender::hasPermission);
        if (!silent && !hasPermission) {
            Arrays.stream(this.perms)
                    .forEach(perm -> Arena.pmsg(sender, PermissionManager.getMissingPermissionMessage(perm)));
        }

        return hasPermission;
    }
}
