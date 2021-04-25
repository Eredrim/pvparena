package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.ConfigurationManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>PVP Arena JOIN Command class</pre>
 * <p/>
 * A command to join an arena
 *
 * @author slipcor
 * @version v0.10.2
 */

public class PAG_Join extends AbstractArenaCommand {

    //private final Debugger debug = Debugger.getInstance();

    public PAG_Join() {
        super(new String[]{"pvparena.user", "pvparena.cmds.join"});
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
            arena.msg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        if (arena.isFightInProgress()
                && (
                !arena.getConfig().getBoolean(CFG.PERMS_JOININBATTLE)
                        ||
                        arena.getConfig().getBoolean(CFG.JOIN_ONLYIFHASPLAYED)
                                && !arena.hasAlreadyPlayed(sender.getName()))) {

            arena.msg(sender, MSG.ERROR_FIGHT_IN_PROGRESS);
            return;
        }

        if (!PVPArena.hasPerms(sender, arena)) {
            arena.msg(sender, MSG.ERROR_NOPERM_JOIN);
            return;
        }

        final Set<String> errors = ConfigurationManager.isSetup(arena);
        if (CollectionUtils.isNotEmpty(errors)) {
            errors.forEach(error -> arena.msg(sender, MSG.ERROR_ERROR, error));
            return;
        }

        if (ArenaRegion.tooFarAway(arena, (Player) sender)) {
            arena.msg(sender, MSG.ERROR_JOIN_RANGE);
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer((Player) sender);

        if (aPlayer.getArena() == null) {
            if (!arena.getConfig().getBoolean(CFG.PERMS_ALWAYSJOININBATTLE) &&
                    !arena.getConfig().getBoolean(CFG.JOIN_ONLYIFHASPLAYED) &&
                    arena.hasAlreadyPlayed(aPlayer.getName())) {
                debug(arena, aPlayer.getPlayer(), "Join_2");
                arena.msg(aPlayer.getPlayer(), MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(arena));
            } else {
                WorkflowManager.handleJoin(arena, aPlayer.getPlayer(), args);
            }
        } else {
            final Arena pArena = aPlayer.getArena();
            debug(arena, sender, "Join_1");
            pArena.msg(sender, MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(pArena));
        }

    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.JOIN);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("join");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-j");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }
        if (!arena.isFreeForAll()) {
            for (String team : arena.getTeamNames()) {
                result.define(new String[]{team});
            }
        }
        return result;
    }
}
