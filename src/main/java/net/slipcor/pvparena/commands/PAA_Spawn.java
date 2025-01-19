package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.managers.SpawnManager.parseSpawnNameArgs;

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

    public PAA_Spawn() {
        super(new String[]{"pvparena.cmds.spawn"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{2, 3, 4})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        Player player = (Player) sender;

        try {
            switch (args[0]) {
                case "set":
                    // usage: /pa {arenaname} spawn set <spawnname> [teamName] [className]
                    this.addSpawn(arena, player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "remove":
                    // usage: /pa {arenaname} spawn remove <spawnname> [teamName] [className]
                    this.removeSpawn(arena, player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                default:
                    break;
            }
        } catch (GameplayException e) {
            arena.msg(player, e.getMessage());
        }
    }

    private void removeSpawn(Arena arena, Player player, String[] args) throws GameplayException {
        // usage: /pa {arenaname} spawn remove <spawnname> [team] [class] | remove a spawn
        String[] parsedArgs = parseSpawnNameArgs(arena, args);
        String spawnName = parsedArgs[0];
        String teamName = parsedArgs[1];
        String className = parsedArgs[2];

        final PALocation location = SpawnManager.getSpawnByExactName(arena, spawnName, teamName, className);
        if (location == null) {
            arena.msg(player, MSG.CMD_SPAWN_NOTSET, spawnName);
        } else {
            arena.msg(player, MSG.CMD_SPAWN_REMOVED, spawnName);
            arena.clearSpawn(spawnName, teamName, className);
        }
    }

    private void addSpawn(Arena arena, Player player, String[] args) throws GameplayException {
        // usage: /pa {arenaname} spawn set <spawnname> [team] [class] | set a spawn (for team) (for a specific class)
        String[] parsedArgs = parseSpawnNameArgs(arena, args);
        this.addSpawn(player, arena, parsedArgs[0], parsedArgs[1], parsedArgs[2]);
    }

    private void addSpawn(Player player, Arena arena, String spawnName, String teamName, String className) {
        debug("Adding spawn \"{}\" for team \"{}\" and class \"{}\"", spawnName, teamName, className);
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        PASpawn newSpawn = new PASpawn(new PALocation(arenaPlayer.getPlayer().getLocation()), spawnName, teamName, className);
        if (DEFAULTS_SPAWNS.stream().anyMatch(spawnName::startsWith)) {
            this.commitSet(arena, player, newSpawn);
            return;
        }

        if (arena.getGoal() != null && arena.getGoal().hasSpawn(spawnName, teamName)) {
            this.commitSet(arena, player, newSpawn);
            return;
        }

        for (ArenaModule mod : arena.getMods()) {
            if (mod.hasSpawn(spawnName, teamName)) {
                this.commitSet(arena, player, newSpawn);
                return;
            }
        }

        arena.msg(player, MSG.CMD_SPAWN_UNKNOWN, spawnName);
    }

    private void commitSet(@NotNull Arena arena, @NotNull CommandSender sender, @NotNull PASpawn spawn) {
        boolean replaced = arena.setSpawn(spawn);
        if (replaced) {
            arena.msg(sender, MSG.CMD_SPAWN_SET_AGAIN, getFormattedSpawnName(arena, spawn));
        } else {
            arena.msg(sender, MSG.CMD_SPAWN_SET, getFormattedSpawnName(arena, spawn));
        }
    }

    private static String getFormattedSpawnName(Arena arena, PASpawn spawn) {
        String coloredTeam = ofNullable(spawn.getTeamName())
                .map(teamName -> arena.getTeam(teamName).getColoredName())
                .orElse("");
        return String.join(" ", coloredTeam, spawn.getName(), ofNullable(spawn.getClassName()).orElse("")).trim();
    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
            if (spawn.hasTeamName()) {
                if(spawn.hasClassName()) {
                    result.define(new String[]{"set", spawn.getName(), spawn.getTeamName(), spawn.getClassName()});
                    result.define(new String[]{"remove", spawn.getName(), spawn.getTeamName(), spawn.getClassName()});
                } else {
                    result.define(new String[]{"set", spawn.getName(), spawn.getTeamName(),});
                    result.define(new String[]{"remove", spawn.getName(), spawn.getTeamName(),});
                }
            } else if (spawn.hasClassName()) {
                result.define(new String[]{"set", spawn.getName(), spawn.getClassName()});
                result.define(new String[]{"remove", spawn.getName(), spawn.getClassName()});
            } else {
                result.define(new String[]{"set", spawn.getName()});
                result.define(new String[]{"remove", spawn.getName()});
            }
        }

        // default spawns
        for (String spawn : DEFAULTS_SPAWNS) {
            result.define(new String[]{"set", spawn});
        }

        Set<PASpawn> complexSpawns = ofNullable(arena.getGoal())
                .map(goal -> goal.checkForMissingSpawns(arena.getSpawns()))
                .orElse(new HashSet<>());

        arena.getMods().stream()
                .map(mod -> mod.checkForMissingSpawns(arena.getSpawns()))
                .forEach(complexSpawns::addAll);

        complexSpawns.forEach(paSpawn -> {
            if(paSpawn.hasTeamName()) {
                result.define(new String[]{"set", paSpawn.getName(), paSpawn.getTeamName()});
            } else {
                result.define(new String[]{"set", paSpawn.getName()});
            }
        });

        return result;
    }
}
