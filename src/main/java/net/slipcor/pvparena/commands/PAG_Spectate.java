package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ConfigurationManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
        super(new String[]{"pvparena.user", "pvparena.cmds.spectate"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        final Set<String> errors = ConfigurationManager.isSetup(arena);
        if (CollectionUtils.isNotEmpty(errors)) {
            errors.forEach(error -> arena.msg(sender, MSG.ERROR_ERROR, error));
            return;
        }

        WorkflowManager.handleSpectate(arena, (Player) sender);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.SPECTATE);
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
