package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <pre>PVP Arena TELEPORT Command class</pre>
 * <p/>
 * A command to teleport to an arena spawn
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Teleport extends AbstractArenaCommand {

    public PAA_Teleport() {
        super(new String[]{"pvparena.cmds.teleport"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1, 2, 3})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        // usage: /pa {arenaname} teleport <spawnName> [teamName] [className] | tp to a spawn

        try {
            String[] parsedSpawnName = SpawnManager.parseSpawnNameArgs(arena, args);
            PALocation loc = SpawnManager.getSpawnByExactName(arena, parsedSpawnName[0], parsedSpawnName[1], parsedSpawnName[2]);

            if(loc == null) {
                String spawnNameForError = Arrays.stream(parsedSpawnName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));
                throw new GameplayException(Language.parse(MSG.ERROR_SPAWN_UNKNOWN, String.join(" ", spawnNameForError)));
            }

            ((Player) sender).teleport(loc.toLocation(), TeleportCause.PLUGIN);
            ((Player) sender).setNoDamageTicks(arena.getConfig().getInt(CFG.TIME_TELEPORTPROTECT) * 20);
        } catch(GameplayException e) {
            arena.msg(sender, e.getMessage());
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.TELEPORT);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("teleport");
    }

    @Override
    public List<String> getShort() {
        return Arrays.asList("!tp", "!t");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }

        for (PASpawn spawn : arena.getSpawns()) {
            if (spawn.hasTeamName()) {
                if(spawn.hasClassName()) {
                    result.define(new String[]{spawn.getName(), spawn.getTeamName(), spawn.getClassName()});
                } else {
                    result.define(new String[]{spawn.getName(), spawn.getTeamName()});
                }
            } else if (spawn.hasClassName()) {
                result.define(new String[]{spawn.getName(), spawn.getClassName()});
            } else {
                result.define(new String[]{spawn.getName()});
            }
        }

        return result;
    }
}
