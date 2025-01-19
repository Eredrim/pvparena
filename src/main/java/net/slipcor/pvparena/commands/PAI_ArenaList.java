package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>PVP Arena ARENALIST Command class</pre>
 * <p/>
 * A command to display the available arenas
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_ArenaList extends AbstractGlobalCommand {

    public PAI_ArenaList() {
        super(new String[]{"pvparena.cmds.arenalist"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0})) {
            return;
        }
        String arenaListStr = ArenaManager.getArenas().stream()
                .sorted(Comparator.comparing(Arena::getName))
                .map(a -> {
                    ChatColor colorPrefix = ChatColor.WHITE;
                    if(a.isLocked()) {
                        colorPrefix = ChatColor.RED;
                    } else if (PAA_Edit.activeEdits.containsValue(a)) {
                        colorPrefix = ChatColor.YELLOW;
                    } else if (a.isFightInProgress()) {
                        colorPrefix = ChatColor.GREEN;
                    }
                    return colorPrefix + a.getName() + ChatColor.RESET;
                })
                .collect(Collectors.joining(", "));

        Arena.pmsg(sender, MSG.ARENA_LIST, arenaListStr);
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
    public List<String> getMain() {
        return Collections.singletonList("list");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-l");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        return new CommandTree<>(null);
    }
}
