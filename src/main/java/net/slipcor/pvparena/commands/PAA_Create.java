package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaGoalManager;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>PVP Arena CREATE Command class</pre>
 * <p/>
 * A command to create an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Create extends AbstractGlobalCommand {

    public PAA_Create() {
        super(new String[]{"pvparena.create", "pvparena.cmds.create"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{1, 2})) {
            return;
        }

        // usage: /pa create [arenaname] {legacy_arenatype}

        Arena arena = ArenaManager.getArenaByName(args[0]);

        if (arena != null) {
            Arena.pmsg(sender, MSG.ERROR_ARENA_EXISTS, arena.getName());
            return;
        }

        arena = new Arena(args[0]);

        if (!sender.hasPermission("pvparena.admin")) {
            // no admin perms => create perms => set owner
            arena.setOwner(sender.getName());
        }

        ArenaGoalManager goalManager = PVPArena.getInstance().getAgm();
        if (args.length > 1) {
            if (goalManager.hasLoadable(args[1])) {
                ArenaGoal goal = goalManager.getNewInstance(args[1]);
                arena.setGoal(goal, false);
            } else {
                arena.msg(sender, MSG.ERROR_GOAL_NOTFOUND, args[0],
                        goalManager.getAllGoalNames().stream().sorted().collect(Collectors.joining(", ")));
                return;
            }
        } else {
            ArenaGoal goal = goalManager.getNewInstance("TeamLives");
            arena.setGoal(goal, false);
        }

        debug(arena, "creating new config file for arena");
        File file = new File(String.format("%s/arenas/%s.yml", PVPArena.getInstance().getDataFolder().getPath(), arena.getName()));
        try {
            if (!file.createNewFile()) {
                PVPArena.getInstance().getLogger().severe(String.format("Can't create new file for arena %s: file already exists", arena.getName()));
                return;
            }
        } catch (IOException ex) {
            PVPArena.getInstance().getLogger().severe(String.format("Can't create new file for arena %s: %s", arena.getName(), ex.getMessage()));
            return;
        }

        if (ArenaManager.loadArena(arena)) {
            Arena.pmsg(sender, MSG.ARENA_CREATE_DONE, arena.getName(), arena.getGoal().getName());
            final PAA_ToggleMod cmd = new PAA_ToggleMod();
            cmd.commit(arena, sender, new String[]{"standardspectate"});
            cmd.commit(arena, sender, new String[]{"standardlounge"});
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.CREATE);
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("create", "new");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!c");
    }

    /**
     * Auto-fill goals name when creating new arena
     *
     * @param nothing not used
     * @return goal names
     */
    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        final ArenaGoalManager goalManager = PVPArena.getInstance().getAgm();
        final CommandTree<String> result = new CommandTree<>(null);
        goalManager.getAllGoalNames().forEach(goalName ->
                result.define(new String[]{"{String}", goalName})
        );
        return result;
    }
}
