package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>PVP Arena REMOVE Command class</pre>
 * <p/>
 * A command to remove an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Remove extends AbstractArenaCommand {

    private static String removal;

    public PAA_Remove() {
        super(new String[]{"pvparena.cmds.remove"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        final String name = arena.getName();

        if (PVPArena.getInstance().getConfig().getBoolean("safeadmin", true)) {
            if (removal == null || !removal.equals(name)) {
                Arena.pmsg(sender, MSG.NOTICE_REMOVE, name);
                removal = name;
                return;
            }
            removal = null;
        }

        ArenaManager.removeArena(arena, true);
        Arena.pmsg(sender, MSG.ARENA_REMOVE_DONE, name);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("remove", "delete");
    }

    @Override
    public List<String> getShort() {
        return Arrays.asList("!rem", "!del");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
