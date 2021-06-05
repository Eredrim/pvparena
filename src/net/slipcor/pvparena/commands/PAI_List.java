package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * <pre>PVP Arena LIST Command class</pre>
 * <p/>
 * A command to display the players of an arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_List extends AbstractArenaCommand {

    public PAI_List() {
        super(new String[]{"pvparena.cmds.list"});
    }

    private static final Map<ArenaPlayer.Status, Character> colorMap = new HashMap<>();

    static {

        colorMap.put(ArenaPlayer.Status.NULL, 'm'); // error? strike through
        colorMap.put(ArenaPlayer.Status.WARM, '6'); // warm = gold
        colorMap.put(ArenaPlayer.Status.LOUNGE, 'b'); // readying up = aqua
        colorMap.put(ArenaPlayer.Status.READY, 'a'); // ready = green
        colorMap.put(ArenaPlayer.Status.FIGHT, 'f'); // fighting = white
        colorMap.put(ArenaPlayer.Status.WATCH, 'e'); // watching = yellow
        colorMap.put(ArenaPlayer.Status.DEAD, '7'); // dead = silver
        colorMap.put(ArenaPlayer.Status.LOST, 'c'); // lost = red
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        if (args.length < 1) {


            for (final ArenaTeam teams : arena.getTeams()) {
                final Set<String> names = new HashSet<>();

                for (final ArenaPlayer player : teams.getTeamMembers()) {
                    names.add("&" + colorMap.get(player.getStatus()) + player.getName() + "&r");
                }

                if (arena.isFreeForAll() && "free".equals(teams.getName())) {
                    arena.msg(sender, Language.parse(arena, MSG.LIST_PLAYERS, StringParser.joinSet(names, ", ")));
                } else {
                    final int count = teams.getTeamMembers().size();
                    final String sCount = " &r(" + count + ')';
                    arena.msg(sender, Language.parse(arena, MSG.LIST_TEAM, teams.getColoredName() + sCount, StringParser.joinSet(names, ", ")));
                }
            }
            return;
        }

        final Map<ArenaPlayer.Status, Set<String>> stats = new HashMap<>();

        for (final ArenaPlayer player : arena.getEveryone()) {
            final Set<String> players = stats.containsKey(player.getStatus()) ? stats.get(player.getStatus()) : new HashSet<String>();

            players.add(player.getName());
            stats.put(player.getStatus(), players);
        }

        for (final Map.Entry<ArenaPlayer.Status, Set<String>> statusSetEntry : stats.entrySet()) {
            arena.msg(sender, Language.parse(arena, MSG.getByNode("LIST_" + statusSetEntry.getKey().name()), "&" + colorMap.get(statusSetEntry.getKey()) + StringParser.joinSet(statusSetEntry.getValue(), ", ")));
        }

    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.LIST));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("list");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-ls");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
