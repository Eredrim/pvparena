package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ConfigurationManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena SPECTATE Command class</pre>
 * <p/>
 * A command to join an arena as spectator
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAG_Spectate extends AbstractArenaCommand {

    public PAG_Spectate() {
        super(new String[]{"pvparena.cmds.spectate"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, Language.parse(arena, MSG.ERROR_ONLY_PLAYERS));
            return;
        }

        final String error = ConfigurationManager.isSetup(arena);
        if (error != null) {
            Arena.pmsg(sender, Language.parse(arena, MSG.ERROR_ERROR, error));
            return;
        }

        PACheck.handleSpectate(arena, sender);
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.SPECTATE));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("spectate");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-s");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
