package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.ConfigurationManager;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena JOIN Command class</pre>
 * <p/>
 * A command to join an arena
 *
 * @author slipcor
 * @version v0.10.2
 */

public class PAG_Join extends AbstractArenaCommand {

    public static final String JOIN = "join";
    public static final String JOIN_SHORT = "-j";

    //private final Debug debug = new Debug(200);

    public PAG_Join() {
        super(new String[]{"pvparena.cmds.join"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!(hasPerms(sender, arena) && PermissionManager.hasExplicitArenaPerm(sender, arena, JOIN))) {
            arena.getDebugger().i(String.join(", ", this.perms));
            arena.msg(sender, Language.parse(arena, MSG.ERROR_NOPERM_JOIN));
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        if (!(sender instanceof Player)) {
            arena.msg(sender, Language.parse(arena, MSG.ERROR_ONLY_PLAYERS));
            return;
        }

        if (arena.isFightInProgress()
                && (
                !arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE)
                        ||
                        arena.getArenaConfig().getBoolean(CFG.JOIN_ONLYIFHASPLAYED)
                                && !arena.hasAlreadyPlayed(sender.getName()))) {

            arena.msg(sender, Language.parse(arena, MSG.ERROR_FIGHT_IN_PROGRESS));
            return;
        }

        final String error = ConfigurationManager.isSetup(arena);
        if (error != null) {
            arena.msg(sender, Language.parse(arena, MSG.ERROR_ERROR, error));
            return;
        }

        if (ArenaRegion.tooFarAway(arena, (Player) sender)) {
            arena.msg(sender, Language.parse(arena, MSG.ERROR_JOIN_RANGE));
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        if (aPlayer.getArena() == null) {
            if (!arena.getArenaConfig().getBoolean(CFG.PERMS_ALWAYSJOININBATTLE) &&
                    !arena.getArenaConfig().getBoolean(CFG.JOIN_ONLYIFHASPLAYED) &&
                    arena.hasAlreadyPlayed(aPlayer.getName())) {
                arena.getDebugger().i("Join_2", sender);
                arena.msg(sender, Language.parse(arena, MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(arena)));
            } else {
                PACheck.handleJoin(arena, sender, args);
            }
        } else {
            final Arena pArena = aPlayer.getArena();
            arena.getDebugger().i("Join_1", sender);
            pArena.msg(sender, Language.parse(arena, MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(pArena)));
        }

    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.JOIN));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList(JOIN);
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList(JOIN_SHORT);
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }
        if (!arena.isFreeForAll()) {
            for (final String team : arena.getTeamNames()) {
                result.define(new String[]{team});
            }
        }
        return result;
    }

}
