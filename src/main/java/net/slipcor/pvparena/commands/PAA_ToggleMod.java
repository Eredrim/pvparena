package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.loadables.ModuleType;
import net.slipcor.pvparena.loader.Loadable;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
                this.replaceSameTypeModule(arena, module, sender);
                arena.addModule(module, true);
                if (module.isMissingBattleRegion(arena)) {
                    arena.msg(sender, MSG.CMD_TOGGLEMOD_BATTLE);
                }
                arena.msg(sender, MSG.CMD_TOGGLEMOD_ENABLED, name);
            } else {
                ArenaModule toRemove = arena.getMods().stream().filter(mod -> name.equalsIgnoreCase(mod.getName())).findAny().get();
                if(toRemove.getType() == ModuleType.JOIN || toRemove.getType() == ModuleType.SPECTATE) {
                    arena.msg(sender, MSG.CMD_TOGGLEMOD_NOT_REMOVABLE, name, toRemove.getType().name());
                } else {
                    arena.removeModule(name);
                    arena.msg(sender, MSG.CMD_TOGGLEMOD_DISABLED, name);
                }
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
        for (Loadable<?> mod : PVPArena.getInstance().getAmm().getAllLoadables()) {
            result.define(new String[]{mod.getName()});
        }
        return result;
    }

    private void replaceSameTypeModule(Arena arena, ArenaModule newMod, CommandSender sender) {
        if(newMod.getType() == ModuleType.SPECTATE || newMod.getType() == ModuleType.JOIN) {
            Optional<ArenaModule> sameTypeMod = arena.getMods().stream()
                    .filter(mod -> mod.getType() == newMod.getType())
                    .findAny();

            sameTypeMod.ifPresent(mod -> {
                arena.removeModule(mod.getName());
                arena.msg(sender, MSG.CMD_TOGGLEMOD_REPLACEMENT, newMod.getName(), mod.getName());
            });
        }
    }
}
