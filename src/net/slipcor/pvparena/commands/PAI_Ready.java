package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.managers.TeamManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>PVP Arena READY Command class</pre>
 * <p/>
 * A command to ready up inside the arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_Ready extends AbstractArenaCommand {

    public PAI_Ready() {
        super(new String[]{"pvparena.cmds.ready"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        if (!arena.hasPlayer(aPlayer.get())) {

            arena.msg(sender, Language.parse(arena, MSG.ERROR_NOT_IN_ARENA));
            return;
        }

        if (args.length < 1) {

            if (aPlayer.getStatus() != Status.LOUNGE) {
                return;
            }

            if (aPlayer.getArenaClass() == null) {
                arena.msg(sender, Language.parse(arena, MSG.ERROR_READY_NOCLASS));
                return;
            }

            if (aPlayer.getStatus() != Status.READY) {
                arena.msg(sender, Language.parse(arena, MSG.READY_DONE));
                arena.broadcast(Language.parse(arena, MSG.PLAYER_READY, aPlayer.getArenaTeam().colorizePlayer(aPlayer.get())));
            }

            aPlayer.setStatus(Status.READY);
            if (aPlayer.getArenaTeam().isEveryoneReady()) {
                arena.broadcast(Language.parse(arena, MSG.TEAM_READY, aPlayer.getArenaTeam().getColoredName()));
            }

            if (arena.getArenaConfig().getBoolean(CFG.USES_EVENTEAMS)
                    && !TeamManager.checkEven(arena)) {
                arena.msg(sender,
                        Language.parse(arena, MSG.NOTICE_WAITING_EQUAL));
                return; // even teams desired, not done => announce
            }

            if (!ArenaRegion.checkRegions(arena)) {
                arena.msg(sender,
                        Language.parse(arena, MSG.NOTICE_WAITING_FOR_ARENA));
                return;
            }

            final String error = arena.ready();

            if (error == null) {
                arena.start();
            } else if (error.isEmpty()) {
                arena.countDown();
            } else {
                arena.msg(sender, error);
            }
            return;
        }

        final Set<String> names = new HashSet<>();

        for (final ArenaPlayer player : arena.getEveryone()) {
            if (player.getStatus() == Status.LOUNGE) {
                names.add("&7" + player.getName() + "&r");
            } else if (player.getStatus() == Status.READY) {
                names.add("&a" + player.getName() + "&r");
            }
        }
        arena.msg(sender, Language.parse(arena, MSG.READY_LIST, StringParser.joinSet(names, ", ")));
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.READY));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("ready");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-r");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
