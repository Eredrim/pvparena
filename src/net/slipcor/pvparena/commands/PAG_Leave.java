package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena LEAVE Command class</pre>
 * <p/>
 * A command to leave an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAG_Leave extends AbstractArenaCommand {

    public PAG_Leave() {
        super(new String[]{"pvparena.cmds.leave"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, Language.parse(arena, MSG.ERROR_ONLY_PLAYERS));
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        // Handle modules which need to leave even if players aren't in an arena
        for (final ArenaModule mod : arena.getMods()) {
            if(mod.handleSpecialLeave(aPlayer)) {
                return;
            }
        }

        if (!arena.hasPlayer(aPlayer.get())) {
            if(PAA_Edit.activeEdits.containsKey(sender.getName())) {
                new PAA_Edit().commit(arena, sender, args);
            } else {
                arena.msg(sender, Language.parse(arena, MSG.ERROR_NOT_IN_ARENA));
            }
            return;
        }
        arena.callLeaveEvent(aPlayer.get());
        arena.playerLeave(aPlayer.get(), CFG.TP_EXIT, false, false, false);
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.LEAVE));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("leave");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-l");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
