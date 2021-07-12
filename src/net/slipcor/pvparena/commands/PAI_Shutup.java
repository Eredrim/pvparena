package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena SHUTUP Command class</pre>
 * <p/>
 * A command to toggle announcement receiving
 *
 * @author slipcor
 */

public class PAI_Shutup extends AbstractArenaCommand {

    public PAI_Shutup() {
        super(new String[]{"pvparena.cmds.shutup"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, Language.parse(arena, MSG.ERROR_ONLY_PLAYERS));
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        if (args.length < 1) {
            // toggle
            if (aPlayer.isIgnoringAnnouncements()) {
                aPlayer.setIgnoreAnnouncements(false);
                arena.msg(sender, Language.parse(arena, MSG.MODULE_ANNOUNCEMENTS_IGNOREOFF));
            } else {
                aPlayer.setIgnoreAnnouncements(true);
                arena.msg(sender, Language.parse(arena, MSG.MODULE_ANNOUNCEMENTS_IGNOREON));
            }
            return;
        }

        if (StringParser.positive.contains(args[0].toLowerCase())) {
            aPlayer.setIgnoreAnnouncements(true);
            arena.msg(sender, Language.parse(arena, MSG.MODULE_ANNOUNCEMENTS_IGNOREON));
        }

        if (StringParser.negative.contains(args[0].toLowerCase())) {
            aPlayer.setIgnoreAnnouncements(false);
            arena.msg(sender, Language.parse(arena, MSG.MODULE_ANNOUNCEMENTS_IGNOREOFF));
            return;
        }

        // usage: /pa {arenaname} shutup {value}

        arena.msg(sender, Language.parse(arena, MSG.ERROR_INVALID_VALUE, args[0]));
        arena.msg(sender, Language.parse(arena, MSG.ERROR_POSITIVES, StringParser.joinSet(StringParser.positive, " | ")));
        arena.msg(sender, Language.parse(arena, MSG.ERROR_NEGATIVES, StringParser.joinSet(StringParser.negative, " | ")));

    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.SHUTUP));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("shutup");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-su");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Boolean}"});
        return result;
    }
}
