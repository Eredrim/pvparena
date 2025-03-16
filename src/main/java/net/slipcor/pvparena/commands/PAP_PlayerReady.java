package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class PAP_PlayerReady extends AbstractSuperUserCommand {

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

        PAI_Ready cmd = new PAI_Ready();
        commitCommandParsingSelector(arena, sender, cmd, args);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
