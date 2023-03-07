package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import org.bukkit.command.CommandSender;

import static net.slipcor.pvparena.config.Debugger.debug;

public class PermissionManager {

    public static final String CREATE_PERM = "pvparena.create";
    public static final String ADMIN_PERM = "pvparena.admin";

    public static final String PERM_NODE_PREFIX = "nulang.nopermto.";


    /**
     * Get the permission deny message (translated and colored)
     *
     * @param perm permission string
     * @return message to display to user
     */
    public static String getMissingPermissionMessage(String perm) {
        String permString = perm.substring(perm.indexOf(".") + 1);
        Language.MSG node = Language.MSG.getByNode(PERM_NODE_PREFIX + permString);
        if (node != null) {
            return Language.parse(Language.MSG.ERROR_NOPERM, Language.parse(node));
        } else {
            return Language.parse(Language.MSG.ERROR_NOPERM, Language.parse(Language.MSG.ERROR_NOPERM_X_USER));
        }
    }

    /**
     * Allowed player to administrate the arena if he's the owner
     *
     * @param sender sender
     * @param arena  arena
     * @return true if player has builder permission for this arena
     */
    public static boolean hasBuilderPerm(CommandSender sender, Arena arena) {
        return sender.hasPermission(CREATE_PERM)
                && sender.getName().equals(arena.getOwner());
    }

    /**
     * Check if a CommandSender has admin permissions
     *
     * @param sender the CommandSender to check
     * @return true if a CommandSender has admin permissions, false otherwise
     */
    public static boolean hasAdminPerm(CommandSender sender) {
        return sender.hasPermission(ADMIN_PERM);
    }


    /**
     * Check if a CommandSender has permission for an arena
     *
     * @param sender the CommandSender to check
     * @param arena  the arena to check
     * @return true if explicit permission not needed or granted, false
     * otherwise
     */
    public static boolean hasExplicitArenaPerm(CommandSender sender, Arena arena, String command) {
        if (arena.getConfig().getBoolean(Config.CFG.PERMS_EXPLICIT_PER_ARENA)) {
            final String perm = String.format("pvparena.%s.%s", command, arena.getName().toLowerCase());
            debug(sender, "explicit arena perm: {}", sender.hasPermission(perm));

            return sender.hasPermission(perm);
        }

        // explicit permissions not enabled
        return true;
    }

    /**
     * Check if a CommandSender has permission for an class
     *
     * @param sender the CommandSender to check
     * @param arena  the arena to check
     * @param aClass the class
     * @return true if explicit permission not needed or granted, false
     * otherwise
     */
    public static boolean hasExplicitClassPerm(CommandSender sender, Arena arena, ArenaClass aClass) {

        if (arena.getConfig().getBoolean(Config.CFG.PERMS_EXPLICITCLASS)) {
            final String perm = String.format("pvparena.class.%s", aClass.getName().toLowerCase());
            debug(sender, "explicit class perm: {}", sender.hasPermission(perm));

            return sender.hasPermission(perm);
        }

        // explicit permissions not enabled
        return true;
    }
}
