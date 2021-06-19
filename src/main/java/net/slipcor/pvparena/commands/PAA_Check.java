package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena CHECK Command class</pre>
 * <p/>
 * A command to check an arena config
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Check extends AbstractArenaCommand {

    public PAA_Check() {
        super(new String[]{"pvparena.cmds.check"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        boolean hasError = false;

        for (CFG c : CFG.getValues()) {
            if (c == null || c.getNode() == null) {
                continue;
            }
            try {
                if ("string".equals(c.getType())) {
                    final String value = arena.getConfig().getString(c);
                    arena.msg(sender, "correct " + c.getType() + ": " + value);
                } else if ("boolean".equals(c.getType())) {
                    final boolean value = arena.getConfig().getBoolean(c);
                    arena.msg(sender, "correct " + c.getType() + ": " + value);
                } else if ("int".equals(c.getType())) {
                    final int value = arena.getConfig().getInt(c);
                    arena.msg(sender, "correct " + c.getType() + ": " + value);
                } else if ("double".equals(c.getType())) {
                    final double value = arena.getConfig().getDouble(c);
                    arena.msg(sender, "correct " + c.getType() + ": " + value);
                }
            } catch (final Exception e) {
                arena.msg(sender, MSG.ERROR_ERROR, c.getNode());
                hasError = true;
            }
        }

        if (!hasError) {
            arena.msg(sender, MSG.CHECK_DONE);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.CHECK);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("check");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!ch");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
