package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena PLAYERTEAM Command class</pre>
 * <p/>
 * A command to put a player into an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAP_PlayerReady extends AbstractArenaCommand {

    public PAP_PlayerReady() {
        super(new String[]{"pvparena.cmds.playerready"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1})) {
            return;
        }

        // usage: /pa {arenaname} playerready [playername]

        final Player player = Bukkit.getPlayer(args[0]);

        if (player == null) {
            arena.msg(sender, MSG.ERROR_PLAYER_NOTFOUND, args[0]);
            return;
        }

        final PAI_Ready cmd = new PAI_Ready();
        cmd.commit(arena, player, new String[0]);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.PLAYERREADY);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("playerready");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!pr");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Player}"});
        return result;
    }
}
