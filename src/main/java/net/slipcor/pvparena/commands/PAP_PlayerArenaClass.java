package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class PAP_PlayerArenaClass extends AbstractSuperUserCommand {

    public PAP_PlayerArenaClass() {
        super(new String[]{"pvparena.cmds.playerarenaclass"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{2})) {
            return;
        }

        // usage: /pa {arenaname} playerarenaclass [playername] [class]

        PAG_Arenaclass cmd = new PAG_Arenaclass();
        commitCommandParsingSelector(arena, sender, cmd, args);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("playerarenaclass");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!pac");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena != null) {
            arena.getClasses().stream()
                    .filter(aClass -> !"custom".equalsIgnoreCase(aClass.getName()))
                    .forEach(aClass -> result.define(new String[]{"{Player}", aClass.getName()}));
        }
        return result;
    }
}
