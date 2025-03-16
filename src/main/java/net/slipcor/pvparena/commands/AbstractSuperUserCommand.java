package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * <pre>
 * PVP Arena SuperUserCommand class
 * </pre>
 * <p/>
 * The abstract class of commands that are executed as a player like playerjoin, playerleave, etc
 */

public abstract class AbstractSuperUserCommand extends AbstractArenaCommand {
    AbstractSuperUserCommand(String[] permissions) {
        super(permissions);
    }

    protected static void commitCommandParsingSelector(Arena arena, CommandSender sender, AbstractArenaCommand cmd, String[] args) {
        String[] shiftedArgs = StringParser.shiftArrayBy(args, 1);

        if (isPlayerTargetSelector(args[0])) {
            try {
                Bukkit.selectEntities(sender, args[0]).forEach(entity -> cmd.commit(arena, entity, shiftedArgs));
            } catch (IllegalArgumentException e) {
                arena.msg(sender, Language.MSG.ERROR_INVALID_VALUE, args[0]);
            }
        } else {
            Player player = Bukkit.getPlayer(args[0]);

            if (player == null) {
                arena.msg(sender, Language.MSG.ERROR_PLAYER_NOTFOUND, args[0]);
                return;
            }

            cmd.commit(arena, player, shiftedArgs);
        }
    }

    private static boolean isPlayerTargetSelector(String string) {
        return string.matches("^@[anprs].*");
    }
}
