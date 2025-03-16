package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class PAP_PlayerSpectate extends AbstractSuperUserCommand {

    public PAP_PlayerSpectate() {
        super(new String[]{"pvparena.cmds.playerspectate"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1})) {
            return;
        }

        // usage: /pa {arenaname} playerspectate [playername]

        PAG_Spectate cmd = new PAG_Spectate();
        commitCommandParsingSelector(arena, sender, cmd, args);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("playerspectate");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!ps");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Player}"});
        return result;
    }
}
