package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ConfigurationManager;
import net.slipcor.pvparena.managers.PermissionManager;
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
    private static final String CMD_JOIN_PERM = "pvparena.cmds.join";
    private static final String JOIN = "join";
    private static final String JOIN_SHORT = "-j";

    public PAG_Join() {
        super(new String[]{CMD_JOIN_PERM});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!(this.hasPerms(sender, arena) && PermissionManager.hasExplicitArenaPerm(sender, arena, JOIN))) {
            debug(sender, "Insufficient perms to join '{}'. Current perms: {}", arena, String.join(", ", this.perms));
            arena.msg(sender, MSG.ERROR_NOPERM_JOIN);
            return;
        }

        if (!arena.isValid()) {
            arena.msg(sender, Language.parse(MSG.ERROR_DISABLED));
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        if (!(sender instanceof Player)) {
            arena.msg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        if (arena.isFightInProgress() && !arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH) &&
                (!arena.getConfig().getBoolean(CFG.JOIN_ALLOW_REJOIN) || !arena.hasAlreadyPlayed(sender.getName()))) {
            arena.msg(sender, MSG.ERROR_FIGHT_IN_PROGRESS);
            return;
        }

        // Only check the first player who joins the arena
        if(arena.getEveryone().isEmpty()) {
            final Set<String> errors = ConfigurationManager.isSetup(arena);
            if (CollectionUtils.isNotEmpty(errors)) {
                errors.forEach(error -> arena.msg(sender, MSG.ERROR_ERROR, error));
                return;
            }
        }

        if (PAA_Region.activeSelections.containsKey(sender.getName())) {
            PAA_Region.activeSelections.remove(sender.getName());
            arena.msg(sender, MSG.NOTICE_CLOSED_SELECTION);
        }

        if (ArenaRegion.tooFarAway(arena, (Player) sender)) {
            arena.msg(sender, MSG.ERROR_JOIN_RANGE);
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer((Player) sender);

        if (aPlayer.getArena() == null) {
            if (!arena.getGoal().allowsJoinInBattle() &&
                    !arena.getConfig().getBoolean(CFG.JOIN_ALLOW_REJOIN) && arena.hasAlreadyPlayed(aPlayer.getName())) {
                debug(arena, aPlayer.getPlayer(), "Join_2");
                arena.msg(aPlayer.getPlayer(), MSG.ERROR_ARENA_ALREADY_PART_OF, arena.getName());
            } else {
                WorkflowManager.handleJoin(arena, aPlayer.getPlayer(), args);
            }
        } else {
            final Arena pArena = aPlayer.getArena();
            debug(arena, sender, "Join_1");
            pArena.msg(sender, MSG.ERROR_ARENA_ALREADY_PART_OF, pArena.getName());
        }

    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
            for (String team : arena.getTeamNames()) {
                result.define(new String[]{team});
            }
        }
        return result;
    }
}
