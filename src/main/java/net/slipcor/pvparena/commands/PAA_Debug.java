package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.config.DebugOutputMode;
import net.slipcor.pvparena.config.Debugger;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.logging.Level.*;
import static net.slipcor.pvparena.config.DebugOutputMode.BOTH;
import static net.slipcor.pvparena.config.DebugOutputMode.CONSOLE;

/**
 * <pre>PVP Arena DEBUG Command class</pre>
 * <p/>
 * A command to toggle debugging
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Debug extends AbstractGlobalCommand {

    public PAA_Debug() {
        super(new String[]{"pvparena.cmds.debug"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0, 1, 2})) {
            return;
        }
        
        if (args.length == 0) {
            if (Debugger.isActive()) {
                Debugger.disable(sender);
            } else {
                Debugger.enable(sender);
            }
        }
        else if (args.length == 1)
        {
            if (StringParser.isPositiveValue(args[0])) {
                Debugger.enable(sender);
            }
            else if (StringParser.isNegativeValue(args[0]))
            {
                Debugger.disable(sender);
            } else {
                Arena.pmsg(sender, Language.MSG.ERROR_INVALID_VALUE, args[0]);
            }
        }
        else if (args.length == 2)
        {
            if ("output".equalsIgnoreCase(args[0])) {
                if (Stream.of(DebugOutputMode.values()).anyMatch(outMode -> outMode.name().equalsIgnoreCase(args[1]))) {
                    DebugOutputMode newDebugOutputMode = DebugOutputMode.valueOf(args[1].toUpperCase());
                    Debugger.setOutputMode(newDebugOutputMode);
                    Arena.pmsg(sender, String.format("debugging output was set to %s", newDebugOutputMode));
                } else {
                    Arena.pmsg(sender, Language.MSG.ERROR_INVALID_VALUE, args[1]);
                }
            } else if ("level".equalsIgnoreCase(args[0])) {
                if (asList(FINE.getName(), FINER.getName()).contains(args[1].toUpperCase())) {
                    Level newLevel = parse(args[1]);
                    Debugger.setLevel(newLevel);
                    Arena.pmsg(sender, String.format("debugging level was set to %s", newLevel));
                    if (asList(CONSOLE, BOTH).contains(Debugger.getOutputMode()) && newLevel == FINER) {
                        Arena.pmsg(sender, String.format("%sConsole logging with FINER logging is not " +
                                "recommended! That may flood your console.", ChatColor.RED));
                    }
                } else {
                    Arena.pmsg(sender, Language.MSG.ERROR_INVALID_VALUE, args[1]);
                }
            }
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("debug");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!d");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"on"});
        result.define(new String[]{"off"});
        for (DebugOutputMode debugOutputMode : DebugOutputMode.values()) {
            result.define(new String[]{"output", debugOutputMode.name()});
        }
        result.define(new String[]{"level", FINE.getName()});
        result.define(new String[]{"level", FINER.getName()});
        return result;
    }
}
