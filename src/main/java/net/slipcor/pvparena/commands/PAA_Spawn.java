package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>PVP Arena SPAWN Command class</pre>
 * <p/>
 * A command to set / remove arena spawns
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Spawn extends AbstractArenaCommand {

    private static final List<String> DEFAULTS_SPAWNS = Stream.of("exit", "spectator").collect(Collectors.toList());

    public static final String DECIMAL = "decimal";

    public PAA_Spawn() {
        super(new String[]{"pvparena.cmds.spawn"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1, 2, 3, 4, 5})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        Player player = (Player) sender;
        String spawnName = args[0];

        if (args.length == 1) {
            // usage: /pa {arenaname} spawn [spawnname] | set a spawn
            addSpawn(player, arena, spawnName, null, null);
        } else {
            if ("remove".equals(args[1])) {
                removeSpawn(arena, args, player, spawnName);
            } else if ("offset".equals(args[1])) {
                setOffset(arena, args, player, spawnName);
            } else {
                addSpawn(arena, args, player, spawnName);
            }
        }
    }

    private void setOffset(Arena arena, String[] args, Player player, String spawnName) {
        // usage: /pa {arenaname} spawn [spawnname] offset X Y Z | offset a spawn
        final PALocation loc = SpawnManager.getSpawnByExactName(arena, spawnName);
        if (loc == null) {
            arena.msg(player, MSG.SPAWN_UNKNOWN, spawnName);
        } else {
            double x, y, z;

            try {
                x = Double.parseDouble(args[2]);
            } catch (Exception e) {
                arena.msg(player, MSG.ERROR_ARGUMENT_TYPE, args[2], DECIMAL);
                return;
            }

            try {
                y = Double.parseDouble(args[3]);
            } catch (Exception e) {
                arena.msg(player, MSG.ERROR_ARGUMENT_TYPE, args[3], DECIMAL);
                return;
            }

            try {
                z = Double.parseDouble(args[4]);
            } catch (Exception e) {
                arena.msg(player, MSG.ERROR_ARGUMENT_TYPE, args[4], DECIMAL);
                return;
            }

            arena.getConfig().setOffset(spawnName, x, y, z);

            arena.msg(player, Language.parse(MSG.SPAWN_OFFSET, spawnName,
                    String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + " (x, y, z)"));
        }
    }

    private void removeSpawn(Arena arena, String[] args, Player player, String spawnName) {
        String teamName = null;
        if (args.length > 2) {
            teamName = args[2];
            if (arena.getTeam(teamName) == null) {
                arena.msg(player, MSG.ERROR_TEAM_NOT_FOUND, teamName);
                return;
            }
        }
        String className = null;
        if (args.length > 3) {
            className = args[3];
            if (arena.getClass(className) == null) {
                arena.msg(player, MSG.ERROR_CLASS_NOT_FOUND, className);
                return;
            }
        }
        // usage: /pa {arenaname} spawn [spawnname] remove (team) (class) | remove a spawn
        final PALocation location = SpawnManager.getSpawnByExactName(arena, spawnName, teamName, className);
        if (location == null) {
            arena.msg(player, MSG.SPAWN_NOTSET, spawnName);
        } else {
            arena.msg(player, MSG.SPAWN_REMOVED, spawnName);
            arena.clearSpawn(spawnName, teamName, className);
        }
    }

    private void addSpawn(Arena arena, String[] args, Player player, String spawnName) {
        String teamName;
        String className;
        // usage: /pa {arenaname} spawn [spawnname] (teamName) (className) | set a spawn (for team) (for a specific class)
        teamName = args[1];
        if (arena.getTeam(teamName) == null) {
            arena.msg(player, MSG.ERROR_TEAM_NOT_FOUND, teamName);
            return;
        }
        className = null;
        if (args.length > 2) {
            className = args[2];
            if (arena.getClass(className) == null) {
                arena.msg(player, MSG.ERROR_CLASS_NOT_FOUND, className);
                return;
            }
        }
        addSpawn(player, arena, spawnName, teamName, className);
    }

    private void addSpawn(Player player, Arena arena, String spawnName, String teamName, String className) {
        debug("Adding spawn \"{}\" for team \"{}\" and class \"{}\"", spawnName, teamName, className);
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        if (DEFAULTS_SPAWNS.stream().anyMatch(spawnName::startsWith)) {
            this.commitSet(arena, player, new PALocation(arenaPlayer.getPlayer().getLocation()), spawnName, teamName, className);
            return;
        }

        if (arena.getGoal() != null && arena.getGoal().hasSpawn(spawnName, teamName)) {
            this.commitSet(arena, player, new PALocation(arenaPlayer.getPlayer().getLocation()), spawnName, teamName, className);
            return;
        }

        for (ArenaModule mod : arena.getMods()) {
            if (mod.hasSpawn(spawnName, teamName)) {
                this.commitSet(arena, player, new PALocation(arenaPlayer.getPlayer().getLocation()), spawnName, teamName, className);
                return;
            }
        }

        arena.msg(player, MSG.ERROR_SPAWN_UNKNOWN, spawnName);
    }

    private void commitSet(@NotNull  Arena arena, @NotNull  CommandSender sender, @NotNull  PALocation loc, @NotNull String name, String teamName, String className) {
        boolean replaced = arena.setSpawn(name, loc, teamName, className);
        if (replaced) {
            arena.msg(sender, MSG.SPAWN_REMOVED, name, ofNullable(teamName)
                    .map(optTeamName -> arena.getTeam(optTeamName).getColoredName()).orElse(""), ofNullable(className).orElse(""));
        }
        arena.msg(sender, MSG.SPAWN_SET, name, ofNullable(teamName)
                .map(optTeamName -> arena.getTeam(optTeamName).getColoredName()).orElse(""), ofNullable(className).orElse(""));
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.SPAWN);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("spawn");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!sp");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);

        if (arena == null) {
            return result;
        }

        // spawns already set to arena
        for (PASpawn spawn : arena.getSpawns()) {
            result.define(new String[]{spawn.getName()});
        }

        // default spawns
        for (String spawn : DEFAULTS_SPAWNS) {
            result.define(new String[]{spawn});
        }

        // missing goal spawns
        if (arena.getGoal() != null) {
            for (PASpawn spawn : arena.getGoal().checkForMissingSpawns(arena.getSpawns())) {
                result.define(new String[]{spawn.getName()});
            }
        }

        // missing module spawns
        for (ArenaModule mod : arena.getMods()) {
            for (PASpawn spawn : mod.checkForMissingSpawns(arena.getSpawns())) {
                result.define(new String[]{spawn.getName()});
            }
        }

        return result;
    }
}
