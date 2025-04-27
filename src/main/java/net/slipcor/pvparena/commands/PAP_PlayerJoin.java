package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class PAP_PlayerJoin extends AbstractSuperUserCommand {

    public PAP_PlayerJoin() {
        super(new String[]{"pvparena.cmds.playerjoin"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!arena.isValid()) {
            arena.msg(sender, Language.parse(MSG.ERROR_DISABLED));
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1, 2})) {
            return;
        }

        // usage: /pa {arenaname} playerjoin [playername] {team}

        PAG_Join cmd = new PAG_Join();
        commitCommandParsingSelector(arena, sender, cmd, args);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("playerjoin");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!pj");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena != null) {
            arena.getTeamNames().forEach(team -> result.define(new String[]{"{Player}", team}));
        }
        return result;
    }
}
