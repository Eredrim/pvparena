package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.command.CommandSender;

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

        for (int i : validCounts) {
            if (i == args.length) {
                return true;
            }
        }

        Arena.pmsg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), StringParser.joinArray(validCounts, "|"));
        return false;
    }

    public abstract void commit(CommandSender sender, String[] args);

    public abstract String getName();

    /**
     * Check if the global command also exists in arena context
     * @return true if there is the same command for arena context
     */
    public boolean hasVersionForArena() {
        return false;
    }

    @Override
    public boolean hasPerms(final CommandSender sender, final Arena arena) {
        // tabComplete check
        if (PVPArena.hasAdminPerms(sender)) {
            return true;
        }

        for (String perm : this.perms) {
            if (sender.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    boolean hasPerms(final CommandSender sender) {
        if (PVPArena.hasAdminPerms(sender)) {
            return true;
        }

        boolean done = false;

        for (String perm : this.perms) {
            if (sender.hasPermission(perm)) {
                return true;
            }
            final String[] split = perm.split("\\.");
            String permString = split[1];
            try {
                if (split.length > 2) {
                    permString = split[1]+"."+split[2];
                }
                Arena.pmsg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.getByNode("nulang.nopermto." + permString)));
            } catch (final Exception e) {
                PVPArena.getInstance().getLogger().warning("Unknown MSG for pvparena." + permString);
                Arena.pmsg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_USER));
            }
            done = true;
        }

        if (!done) {
            Arena.pmsg(sender, MSG.ERROR_NOPERM, MSG.ERROR_NOPERM_X_ADMIN.toString());
        }

        return false;
    }

    protected abstract void displayHelp(CommandSender sender);
}
