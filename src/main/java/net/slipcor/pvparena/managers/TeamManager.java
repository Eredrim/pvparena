package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.RandomUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Team Manager class
 * </pre>
 * <p/>
 * Provides static methods to manage Arena Teams
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class TeamManager {

    public static final String FREE = "free";

    private TeamManager() {
    }

    /**
     * check if arena full
     *
     * @param arena arena
     *
     * @return true if full
     */
    public static boolean isArenaFull(final Arena arena) {
        Integer totalPlayers = arena.getNotEmptyTeams().stream().map(ArenaTeam::getTeamMembers)
                .map(Set::size).reduce(Integer::sum).orElse(0);

        final int maxPlayers = arena.getConfig().getInt(CFG.READY_MAXPLAYERS) == 0 ? 500 :
                arena.getConfig().getInt(CFG.READY_MAXPLAYERS);

        if(totalPlayers >= maxPlayers){
            // arena is full.
            debug(arena, String.format("Arena is full. (%s/%s)", totalPlayers, maxPlayers));
            return true;
        }
        return false;
    }

    /**
     * calculate the team that needs players the most
     *
     * @return the team name
     */
    public static ArenaTeam getRandomTeam(final Arena arena) {
        debug(arena, "calculating free team");

        int maxTeamPlayersCfg = arena.getConfig().getInt(CFG.READY_MAXTEAMPLAYERS);
        final int maxPlayerPerTeam = (maxTeamPlayersCfg == 0) ? 100 : maxTeamPlayersCfg;

        // collect the available teams into a map and get count of "players missing" or "space available"
        Map<ArenaTeam, Integer> availableTeamsWithMemberCount = arena.getTeams().stream()
                // don't map full teams
                .filter(arenaTeam -> arenaTeam.getTeamMembers().size() < maxPlayerPerTeam)
                .collect(Collectors.toMap(
                        Function.identity(),
                        arenaTeam -> maxPlayerPerTeam - arenaTeam.getTeamMembers().size()));

        if(availableTeamsWithMemberCount.isEmpty()) {
                // no team available (full or no team defined ?)
                return null;
        }

        // pick only teams with the most "players missing" or "space available"
        // ex: team blue have 3 spaces, blue have 2 spaces, orange have 1 space and red have 3 spaces
        // this return a set with blue and red.
        int max = availableTeamsWithMemberCount.values().stream().max(Comparator.naturalOrder()).orElse(0);
        final Set<ArenaTeam> teamsWithLessMembers = availableTeamsWithMemberCount.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // pick a random
        return RandomUtils.getRandom(teamsWithLessMembers, new Random());
        // return RandomUtils.getWeightedRandom(availableTeamsWithMemberCount, new Random());
    }

    /**
     * check if the teams are even
     *
     * @return true if teams have the same amount of players, false otherwise
     */
    public static boolean checkEven(final Arena arena) {
        debug(arena, "checking if teams are even");
        final Map<String, Integer> counts = new HashMap<>();

        // count each team members

        for (ArenaTeam team : arena.getTeams()) {
            debug(arena, team.getName() + ": " + team.getTeamMembers().size());
            counts.put(team.getName(), team.getTeamMembers().size());
        }

        if (counts.size() < 1) {
            debug(arena, "noone in there");
            return false; // noone there => not even
        }

        int temp = -1;
        for (int i : counts.values()) {
            if (temp == -1) {
                temp = i;
                continue;
            }
            if (temp != i) {
                debug(arena, "NOT EVEN");
                return false; // different count => not even
            }
        }
        debug(arena, "EVEN");
        return true; // every team has the same player count!
    }

    /**
     * count all teams that have active players
     *
     * @return the number of teams that have active players
     */
    public static int countActiveTeams(final Arena arena) {
        debug(arena, "counting active teams");

        final Set<String> activeteams = new HashSet<>();
        for (ArenaTeam team : arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == PlayerStatus.FIGHT) {
                    activeteams.add(team.getName());
                    break;
                }
            }
        }
        debug(arena, "result: " + activeteams.size());
        return activeteams.size();
    }

    /**
     * count all players that have a team
     *
     * @return the team player count
     */
    public static int countPlayersInTeams(final Arena arena) {
        int result = 0;
        for (ArenaTeam team : arena.getTeams()) {
            result += team.getTeamMembers().size();
        }
        debug(arena, "players having a team: " + result);
        return result;
    }
}
