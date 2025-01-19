package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
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
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer((Player) sender);

        if (args.length < 1) {
            // toggle
            if (aPlayer.isIgnoringAnnouncements()) {
                aPlayer.setIgnoreAnnouncements(false);
                arena.msg(sender, MSG.MODULE_ANNOUNCEMENTS_IGNOREOFF);
            } else {
                aPlayer.setIgnoreAnnouncements(true);
                arena.msg(sender, MSG.MODULE_ANNOUNCEMENTS_IGNOREON);
            }
            return;
        }

        if (StringParser.isPositiveValue(args[0])) {
            aPlayer.setIgnoreAnnouncements(true);
            arena.msg(sender, MSG.MODULE_ANNOUNCEMENTS_IGNOREON);
        }

        if (StringParser.isNegativeValue(args[0])) {
            aPlayer.setIgnoreAnnouncements(false);
            arena.msg(sender, MSG.MODULE_ANNOUNCEMENTS_IGNOREOFF);
            return;
        }

        // usage: /pa {arenaname} shutup {value}

        arena.msg(sender, MSG.ERROR_INVALID_VALUE, args[0]);
        arena.msg(sender, MSG.ERROR_POSITIVES, StringParser.joinSet(StringParser.positive, " | "));
        arena.msg(sender, MSG.ERROR_NEGATIVES, StringParser.joinSet(StringParser.negative, " | "));

    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
