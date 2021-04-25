package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.command.CommandSender;

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
    private final String[] perms;

    AbstractArenaCommand(final String[] permissions) {
        this.perms = permissions.clone();
    }

    public static boolean argCountValid(final CommandSender sender, final Arena arena,
                                        final String[] args, final Integer[] validCounts) {

        for (int i : validCounts) {
            if (i == args.length) {
                return true;
            }
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

    @Override
    public boolean hasPerms(final CommandSender sender, final Arena arena) {
        if (sender.hasPermission("pvparena.admin")) {
            return true;
        }

        if (arena != null && sender.hasPermission("pvparena.create")
                && sender.getName().equals(arena.getOwner())) {
            return true;
        }

        for (String perm : this.perms) {
            if (sender.hasPermission(perm)) {
                return true;
            }
        }

        return false;
    }

    public abstract void displayHelp(final CommandSender sender);
}
