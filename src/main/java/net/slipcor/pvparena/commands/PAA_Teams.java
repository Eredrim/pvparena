package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena TEAMS Command class</pre>
 * <p/>
 * A command to manage arena teams
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_Teams extends AbstractArenaCommand {

    public PAA_Teams() {
        super(new String[]{"pvparena.cmds.teams"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 2, 3})) {
            this.displayHelp(sender);
            return;
        }

        // usage: /pa {arenaname} teams set [name] [value]
        // usage: /pa {arenaname} teams add [name] [value]
        // usage: /pa {arenaname} teams remove [name]
        // usage: /pa {arenaname} teams

        if (args.length == 0) {
            // show teams
            arena.msg(sender, MSG.TEAMS_LIST, StringParser.joinSet(arena.getTeamNamesColored(), ChatColor.COLOR_CHAR + "f,"));
            return;
        }

        if (!argCountValid(sender, arena, args, "remove".equals(args[0]) ? new Integer[]{2} : new Integer[]{3})) {
            this.displayHelp(sender);
            return;
        }

        final ArenaTeam team = arena.getTeam(args[1]);

        if (team == null && !"add".equals(args[0])) {
            arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, args[1]);
            return;
        }

        if ("remove".equals(args[0])) {
            arena.msg(sender, MSG.TEAMS_REMOVE, team.getColoredName());
            arena.getTeams().remove(team);
            arena.getConfig().setManually("teams." + team.getName(), null);
            arena.getConfig().save();
        } else if ("add".equals(args[0])) {
            try {

                final ChatColor color = ChatColor.valueOf(args[2].toUpperCase());
                final ArenaTeam newTeam = new ArenaTeam(args[1], color.name());
                arena.getTeams().add(newTeam);
                arena.getConfig().setManually("teams." + newTeam.getName(), color.name());
                arena.getConfig().save();

                arena.msg(sender, MSG.TEAMS_ADD, newTeam.getColoredName());
            } catch (final Exception e) {
                arena.msg(sender, MSG.ERROR_ARGUMENT, args[2], StringParser.joinArray(ChatColor.values(), ","));
            }
        } else if ("set".equals(args[0])) {
            try {
                final ChatColor color = ChatColor.valueOf(args[2].toUpperCase());
                final ArenaTeam newTeam = new ArenaTeam(args[1], color.name());
                arena.getTeams().remove(arena.getTeam(args[1]));
                arena.getTeams().add(newTeam);
                arena.getConfig().setManually("teams." + newTeam.getName(), color.name());
                arena.getConfig().save();

                arena.msg(sender, MSG.TEAMS_REMOVE, newTeam.getColoredName());
            } catch (final Exception e) {
                arena.msg(sender, MSG.ERROR_ARGUMENT, args[2], StringParser.joinArray(ChatColor.values(), ","));
            }
        } else {
            this.displayHelp(sender);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.TEAMS);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("teams");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!ts");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"add"});
        if (arena == null) {
            return result;
        }
        for (String team : arena.getTeamNames()) {
            result.define(new String[]{"remove", team});
            Arrays.stream(ChatColor.values()).forEach(color ->
                    result.define(new String[]{"set", team, color.name()})
            );
        }
        return result;
    }
}
