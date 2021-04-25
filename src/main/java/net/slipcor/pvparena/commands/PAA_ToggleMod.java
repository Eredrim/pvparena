package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.loader.Loadable;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena ACTIVATE Command class</pre>
 * <p/>
 * A command to activate modules
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_ToggleMod extends AbstractArenaCommand {

    public PAA_ToggleMod() {
        super(new String[]{"pvparena.cmds.togglemod"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args,
                new Integer[]{1})) {
            return;
        }

        // pa [arenaname] togglemod [module]

        final String name = args[0].toLowerCase();
        ArenaModuleManager moduleManager = PVPArena.getInstance().getAmm();
        if (moduleManager.hasLoadable(name)) {
            boolean isEnabling = !arena.hasMod(name);
            if(isEnabling) {
                ArenaModule module = moduleManager.getNewInstance(name);
                arena.addModule(module, true);
                if (module.isMissingBattleRegion(arena)) {
                    arena.msg(sender, MSG.TOGGLEMOD_NOTICE);
                }
                arena.msg(sender, MSG.INFO_MOD_ENABLED, name);
            } else {
                arena.removeModule(name);
                arena.msg(sender, MSG.INFO_MOD_DISABLED, name);
            }
            return;
        }
        arena.msg(sender, MSG.ERROR_UNKNOWN_MODULE, args[0]);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.TOGGLEMOD);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("togglemod");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!tm");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        for (String string : PVPArena.getInstance().getAgm().getAllGoalNames()) {
            result.define(new String[]{string});
        }
        for (Loadable<?> mod : PVPArena.getInstance().getAmm().getAllLoadables()) {
            result.define(new String[]{mod.getName()});
        }
        return result;
    }
}
