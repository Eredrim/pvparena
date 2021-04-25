package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.managers.StatisticsManager.Type;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena STATS Command class</pre>
 * <p/>
 * A command to display the player statistics
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_GlobalStats extends AbstractGlobalCommand {

    public PAI_GlobalStats() {
        super(new String[]{"pvparena.user", "pvparena.cmds.stats"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{1})) {
            return;
        }

        new PAI_Stats().commit(null, sender, args);
    }

    @Override
    public boolean hasVersionForArena() {
        return true;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.STATS);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("stats");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-s");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        for (Type val : Type.values()) {
            result.define(new String[]{val.name()});
        }
        return result;
    }
}
