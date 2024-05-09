package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModuleManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Runnable class "TimedEnd"</pre>
 * <p/>
 * An arena timer to end the arena match after a certain amount of time
 */

public class TimedEndRunnable extends ArenaRunnable {

    public static final String WINNER = "WINNER";

    /**
     * create a timed arena runnable
     *
     * @param arena    the arena we are running in
     */
    public TimedEndRunnable(final Arena arena, final int seconds) {
        super(MSG.TIMER_ENDING_IN.getNode(), seconds, null, arena, false);
        debug(arena, "TimedEndRunnable constructor arena");
        arena.endRunner = this;
    }

    @Override
    public void commit() {
        debug(this.arena, "TimedEndRunnable committing");
        if (this.arena.isFightInProgress()) {
           this.endTimer();
        }
        this.arena.endRunner = null;
        if (this.arena.realEndRunner != null) {
            this.arena.realEndRunner.cancel();
            this.arena.realEndRunner = null;
        }
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("TimedEndRunnable not scheduled yet!");
    }

    private void endTimer() {

        /*
          name/team => score points

          handed over to each module
         */

        debug(this.arena, "timed end!");

        Map<String, Double> scores = this.arena.getGoal().timedEnd(new HashMap<>());
        debug(this.arena, "scores: " + this.arena.getGoal().getName());

        Set<String> computedWinners = new HashSet<>();
        Set<String> finalWinners = new HashSet<>();

        if (this.arena.isFreeForAll() && this.arena.getTeams().size() <= 1) {
            computedWinners.add("free");
            debug(this.arena, "adding FREE");
        } else if (this.arena.getConfig().getDefinedString(Config.CFG.GENERAL_TIMER_WINNER) == null) {
            // check all teams
            double maxScore = 0;

            int neededTeams = this.arena.getTeams().size();

            for (String team : this.arena.getTeamNames()) {
                if (scores.containsKey(team)) {
                    final double teamScore = scores.get(team);

                    if (teamScore > maxScore) {
                        maxScore = teamScore;
                        computedWinners.clear();
                        computedWinners.add(team);
                        debug(this.arena, "clear and add team " + team);
                    } else if (teamScore == maxScore) {
                        computedWinners.add(team);
                        debug(this.arena, "add team " + team);
                    }
                } else {
                    neededTeams -= 1;
                }
            }

            // neededTeams should be the number of active teams

            if (neededTeams <= 2) {
                debug(this.arena, "fixing neededTeams to be of size 2!");
                neededTeams = 2;
            }

            if (computedWinners.size() >= neededTeams) {
                debug(this.arena, "team of computedWinners is too big: "+computedWinners.size()+"!");
                for (String s : computedWinners) {
                    debug(this.arena, "- "+s);
                }
                debug(this.arena, "clearing computedWinners!");
                computedWinners.clear(); // noone wins.
            }
        } else {
            computedWinners.add(this.arena.getConfig().getString(Config.CFG.GENERAL_TIMER_WINNER));
            debug(this.arena, "added winner!");
        }

        if (computedWinners.size() > 1) {
            debug(this.arena, "more than 1");
            final Set<String> preciseWinners = new HashSet<>();

            // several teams have max score!!
            double maxSum = 0;
            for (ArenaTeam team : this.arena.getTeams()) {
                if (!computedWinners.contains(team.getName())) {
                    continue;
                }

                double sum = 0;

                for (ArenaPlayer ap : team.getTeamMembers()) {
                    if (scores.containsKey(ap.getName())) {
                        sum += scores.get(ap.getName());
                    }
                }

                if (sum == maxSum) {
                    preciseWinners.add(team.getName());
                    debug(this.arena, "adding " + team.getName());
                } else if (sum > maxSum) {
                    maxSum = sum;
                    preciseWinners.clear();
                    preciseWinners.add(team.getName());
                    debug(this.arena, "clearing and adding + " + team.getName());
                }
            }

            if (!preciseWinners.isEmpty()) {
                computedWinners.clear();
                computedWinners.addAll(preciseWinners);
            }
        }

        if (this.arena.isFreeForAll() && this.arena.getTeams().size() <= 1) {
            debug(this.arena, "FFA");
            final Set<String> preciseWinners = new HashSet<>();

            for (ArenaTeam team : this.arena.getTeams()) {
                if (!computedWinners.contains(team.getName())) {
                    continue;
                }

                double maxSum = 0;

                for (ArenaPlayer ap : team.getTeamMembers()) {
                    double sum = 0;
                    if (scores.containsKey(ap.getName())) {
                        sum = scores.get(ap.getName());
                    }
                    if (sum == maxSum) {
                        preciseWinners.add(ap.getName());
                        debug(this.arena, "ffa adding " + ap.getName());
                    } else if (sum > maxSum) {
                        maxSum = sum;
                        preciseWinners.clear();
                        preciseWinners.add(ap.getName());
                        debug(this.arena, "ffa clr & adding " + ap.getName());
                    }
                }
            }
            computedWinners.clear();

            if (preciseWinners.size() != this.arena.getPlayedPlayers().size()) {
                computedWinners.addAll(preciseWinners);
            }
        }

        if (this.arena.isFreeForAll() && this.arena.getTeams().size() <= 1) {
            debug(this.arena, "FFA and <= 1!");
            for (ArenaTeam team : this.arena.getTeams()) {
                final Set<ArenaPlayer> arenaPlayers = new HashSet<>(team.getTeamMembers());

                for (ArenaPlayer arenaPlayer : arenaPlayers) {
                    if (computedWinners.isEmpty()) {
                        this.arena.removePlayer(arenaPlayer, this.arena.getConfig().getString(Config.CFG.TP_LOSE), true, false);
                    } else {
                        if (computedWinners.contains(arenaPlayer.getName())) {
                            ArenaModuleManager.announce(this.arena, Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()), WINNER);
                            this.arena.broadcast(Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()));
                            finalWinners.add(arenaPlayer.getName());
                        } else {
                            if (arenaPlayer.getStatus() == PlayerStatus.FIGHT || arenaPlayer.getStatus() == PlayerStatus.DEAD) {
                                arenaPlayer.setStatus(PlayerStatus.LOST);
                            }
                        }
                    }
                }
            }
            if (computedWinners.isEmpty()) {
                ArenaModuleManager.announce(this.arena, Language.parse(MSG.FIGHT_DRAW), WINNER);
                this.arena.broadcast(Language.parse(MSG.FIGHT_DRAW));
            }
        } else if (!computedWinners.isEmpty()) {

            boolean hasBroadcasted = false;
            for (ArenaTeam team : this.arena.getTeams()) {
                if (computedWinners.contains(team.getName())) {
                    if (!hasBroadcasted) {
                        ArenaModuleManager.announce(this.arena, Language.parse(MSG.TEAM_HAS_WON, team.getName()), WINNER);
                        this.arena.broadcast(Language.parse(MSG.TEAM_HAS_WON, team.getColor() + team.getName()));
                        hasBroadcasted = true;
                        finalWinners.add(team.getName());
                    }
                } else {

                    final Set<ArenaPlayer> apSet = new HashSet<>(team.getTeamMembers());
                    for (ArenaPlayer p : apSet) {
                        if (p.getStatus() != PlayerStatus.FIGHT || p.getStatus() != PlayerStatus.DEAD) {
                            continue;
                        }
                        p.setStatus(PlayerStatus.LOST);
                    }
                }
            }
        } else {
            ArenaModuleManager.announce(this.arena, Language.parse(MSG.FIGHT_DRAW), WINNER);
            this.arena.broadcast(Language.parse(MSG.FIGHT_DRAW));
        }

        this.arena.setWinners(finalWinners);
        ArenaModuleManager.timedEnd(this.arena, finalWinners);

        debug(this.arena, "resetting arena!");

        this.arena.reset(false);
    }
}
