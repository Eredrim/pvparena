package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.exceptions.GameplayExceptionNotice;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        if (!arena.hasPlayer(aPlayer.getPlayer())) {

            arena.msg(sender, MSG.ERROR_NOT_IN_ARENA);
            return;
        }

        if (args.length < 1) {
            try {
                if(arena.isFightInProgress()) {
                    checkReadyRequirementsDuringFight(arena, aPlayer);
                } else {
                    checkReadyRequirementsBeforeFight(arena, aPlayer);
                }
            } catch (GameplayException e) {
                arena.msg(sender, e.getMessage());
            }
        } else {
            // /pa ready list
            final Set<String> names = new HashSet<>();

            for (ArenaPlayer player : arena.getEveryone()) {
                if (player.getStatus() == PlayerStatus.LOUNGE) {
                    names.add("&7" + player.getName() + "&r");
                } else if (player.getStatus() == PlayerStatus.READY) {
                    names.add("&a" + player.getName() + "&r");
                }
            }
            arena.msg(sender, MSG.READY_LIST, StringParser.joinSet(names, ", "));
        }
    }

    public static void checkReadyRequirementsBeforeFight(Arena arena, ArenaPlayer aPlayer) throws GameplayException {
        if (aPlayer.getStatus() != PlayerStatus.LOUNGE) {
            return;
        }

        if (aPlayer.getArenaClass() == null) {
            throw new GameplayException(MSG.ERROR_READY_NOCLASS);
        }

        arena.msg(aPlayer.getPlayer(), MSG.READY_DONE);
        arena.broadcast(Language.parse(MSG.ANNOUNCE_PLAYER_READY, aPlayer.getArenaTeam().colorizePlayer(aPlayer)));
        aPlayer.setStatus(PlayerStatus.READY);

        if (aPlayer.getArenaTeam().isEveryoneReady()) {
            arena.broadcast(Language.parse(MSG.TEAM_READY, aPlayer.getArenaTeam().getColoredName()));
        }

        if (arena.getConfig().getBoolean(CFG.USES_EVENTEAMS) && !TeamManager.checkEven(arena)) {
            // even teams desired, not done => announce
            throw new GameplayExceptionNotice(MSG.NOTICE_WAITING_EQUAL);
        }

        if (!ArenaRegion.checkRegions(arena)) {
            throw new GameplayExceptionNotice(MSG.NOTICE_WAITING_FOR_ARENA);
        }

        final String error = arena.ready();

        if (error == null) {
            arena.start();
        } else if (error.isEmpty()) {
            arena.countDown();
        } else {
            throw new GameplayException(error);
        }
    }

    public static void checkReadyRequirementsDuringFight(Arena arena, ArenaPlayer aPlayer) throws GameplayException {
        if (aPlayer.getStatus() != PlayerStatus.LOUNGE) {
            return;
        }

        if (aPlayer.getArenaClass() == null) {
            throw new GameplayException(MSG.ERROR_READY_NOCLASS);
        }

        if (arena.getConfig().getBoolean(CFG.USES_EVENTEAMS) && !TeamManager.checkEven(arena)) {
            // even teams desired, not done => announce
            throw new GameplayExceptionNotice(MSG.NOTICE_WAITING_EQUAL);
        }

        arena.msg(aPlayer.getPlayer(), MSG.READY_DONE);
        aPlayer.setStatus(PlayerStatus.READY);

        final String error = arena.ready();

        if (error == null) {
            arena.addPlayerDuringMatch(aPlayer);
        } else {
            throw new GameplayException(error);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
