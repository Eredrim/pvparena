package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
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
        super(new String[]{"pvparena.user", "pvparena.cmds.list"});
    }

    private static final Map<PlayerStatus, Character> colorMap = new HashMap<>();

    static {

        colorMap.put(PlayerStatus.NULL, 'm'); // error? strike through
        colorMap.put(PlayerStatus.WARM, '6'); // warm = gold
        colorMap.put(PlayerStatus.LOUNGE, 'b'); // readying up = aqua
        colorMap.put(PlayerStatus.READY, 'a'); // ready = green
        colorMap.put(PlayerStatus.FIGHT, 'f'); // fighting = white
        colorMap.put(PlayerStatus.WATCH, 'e'); // watching = yellow
        colorMap.put(PlayerStatus.DEAD, '7'); // dead = silver
        colorMap.put(PlayerStatus.LOST, 'c'); // lost = red
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        if (args.length < 1) {


            for (ArenaTeam teams : arena.getTeams()) {
                final Set<String> names = new HashSet<>();

                for (ArenaPlayer player : teams.getTeamMembers()) {
                    names.add("&" + colorMap.get(player.getStatus()) + player.getName() + "&r");
                }

                if (arena.isFreeForAll() && "free".equals(teams.getName())) {
                    arena.msg(sender, MSG.LIST_PLAYERS, StringParser.joinSet(names, ", "));
                } else {
                    final int count = teams.getTeamMembers().size();
                    final String sCount = " &r(" + count + ')';
                    arena.msg(sender, MSG.LIST_TEAM, teams.getColoredName() + sCount, StringParser.joinSet(names, ", "));
                }
            }
            return;
        }

        final Map<PlayerStatus, Set<String>> stats = new HashMap<>();

        for (ArenaPlayer player : arena.getEveryone()) {
            final Set<String> players = stats.containsKey(player.getStatus()) ? stats.get(player.getStatus()) : new HashSet<String>();

            players.add(player.getName());
            stats.put(player.getStatus(), players);
        }

        for (Map.Entry<PlayerStatus, Set<String>> statusSetEntry : stats.entrySet()) {
            arena.msg(sender, Language.parse(MSG.getByNode("LIST_" + statusSetEntry.getKey().name()), "&" + colorMap.get(statusSetEntry.getKey()) + StringParser.joinSet(statusSetEntry.getValue(), ", ")));
        }

    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.LIST);
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
