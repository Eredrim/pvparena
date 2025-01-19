package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena INFO Command class</pre>
 * <p/>
 * A command to display the active modules of an arena and settings
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_Help extends AbstractGlobalCommand {

    public PAI_Help() {
        super(new String[]{"pvparena.cmds.help"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0})) {
            return;
        }

        /*
            /pvparena help
		*/
        Arena.pmsg(sender, Language.MSG.CMD_HELP_MESSAGE, Language.MSG.CMD_HELP_LINK);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("help");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-h");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        return new CommandTree<>(null);
    }
}
