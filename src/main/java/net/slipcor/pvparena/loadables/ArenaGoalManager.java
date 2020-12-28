package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.config.Debugger;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.goals.*;
import net.slipcor.pvparena.ncloader.NCBLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.io.File;
import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal Manager class
 * </pre>
 * <p/>
 * Loads and manages arena goals
 *
 * @author slipcor
 * @version v0.10.2
 */

public class ArenaGoalManager {
    private List<ArenaGoal> types;
    private final NCBLoader<ArenaGoal> loader;

    /**
     * create an arena type instance
     *
     * @param plugin the plugin instance
     */
    public ArenaGoalManager(final PVPArena plugin) {
        final File path = new File(plugin.getDataFolder() + "/goals");
        if (!path.exists()) {
            path.mkdir();
        }
        this.loader = new NCBLoader<>(plugin, path);
        this.types = this.loader.load(ArenaGoal.class);
        this.fill();
    }

    private void fill() {
        this.types.add(new GoalBlockDestroy());
        this.types.add(new GoalCheckPoints());
        this.types.add(new GoalDomination());
        this.types.add(new GoalFlags());
        this.types.add(new GoalFood());
        this.types.add(new GoalInfect());
        this.types.add(new GoalLiberation());
        this.types.add(new GoalPhysicalFlags());
        this.types.add(new GoalPlayerDeathMatch());
        this.types.add(new GoalPlayerKillReward());
        this.types.add(new GoalPlayerLives());
        this.types.add(new GoalSabotage());
        this.types.add(new GoalTank());
        this.types.add(new GoalTeamDeathConfirm());
        this.types.add(new GoalTeamDeathMatch());
        this.types.add(new GoalTeamLives());

        for (final ArenaGoal goal : this.types) {
            goal.onThisLoad();
            debug("ArenaGoal loaded: {} (version {})", goal.getName(), goal.version());
        }
    }

    public boolean allowsJoinInBattle(final Arena arena) {
        for (final ArenaGoal type : arena.getGoals()) {
            if (!type.allowsJoinInBattle()) {
                return false;
            }
        }
        return true;
    }

    public String checkForMissingSpawns(final Arena arena,
                                        final Set<String> list) {
        for (final ArenaGoal type : arena.getGoals()) {
            final String error = type.checkForMissingSpawns(list);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    public static PACheck checkBreak(Arena arena, BlockBreakEvent event) {
        PACheck result = new PACheck();
        for (final ArenaGoal type : arena.getGoals()) {
            result = type.checkBreak(result, arena, event);
        }
        return result;
    }

    public static PACheck checkCraft(Arena arena, CraftItemEvent event) {
        PACheck result = new PACheck();
        for (final ArenaGoal type : arena.getGoals()) {
            result = type.checkCraft(result, arena, event);
        }
        return result;
    }

    public static PACheck checkDrop(Arena arena, PlayerDropItemEvent event) {
        PACheck result = new PACheck();
        for (final ArenaGoal type : arena.getGoals()) {
            result = type.checkDrop(result, arena, event);
        }
        return result;
    }

    public static PACheck checkInventory(Arena arena, InventoryClickEvent event) {
        PACheck result = new PACheck();
        for (final ArenaGoal type : arena.getGoals()) {
            result = type.checkInventory(result, arena, event);
        }
        return result;
    }

    public static PACheck checkPickup(Arena arena, EntityPickupItemEvent event) {
        PACheck result = new PACheck();
        if (event.getEntity() instanceof Player) {
            for (final ArenaGoal type : arena.getGoals()) {
                result = type.checkPickup(result, arena, event);
            }
        }
        return result;
    }

    public static PACheck checkPlace(Arena arena, BlockPlaceEvent event) {
        PACheck result = new PACheck();
        for (final ArenaGoal type : arena.getGoals()) {
            result = type.checkPlace(result, arena, event);
        }
        return result;
    }

    public void configParse(final Arena arena, final YamlConfiguration config) {
        for (final ArenaGoal type : arena.getGoals()) {
            type.configParse(config);
        }
    }

    public Set<String> getAllGoalNames() {
        final Set<String> result = new HashSet<>();

        for (final ArenaGoal goal : this.types) {
            result.add(goal.getName());
        }

        return result;
    }

    public List<ArenaGoal> getAllGoals() {
        return this.types;
    }

    /**
     * find an arena type by arena type name
     *
     * @param tName the type name to find
     * @return the arena type if found, null otherwise
     */
    public ArenaGoal getGoalByName(final String tName) {
        for (final ArenaGoal type : this.types) {
            if (type.getName().equalsIgnoreCase(tName)) {
                return type;
            }
        }
        return null;
    }

    public void initiate(final Arena arena, final Player player) {
        debug(arena, player, "initiating " + player.getName());
        for (final ArenaGoal type : arena.getGoals()) {
            type.initate(player);
        }
    }

    public String ready(final Arena arena) {
        debug(arena, "AGM ready!?!");
        for (final ArenaGoal type : arena.getGoals()) {
            String error = type.ready();
            if (error != null) {

                debug(arena, "type error:" + type.getName());
                return error;
            }
        }
        return null;
    }

    public void refillInventory(final Arena arena, final Player player) {
        if (player == null) {
            return;
        }
        for (final ArenaGoal type : arena.getGoals()) {
            type.refillInventory(player);
        }
    }

    public void reload() {
        this.types = this.loader.reload(ArenaGoal.class);
        this.fill();
    }

    public void reset(final Arena arena, final boolean force) {
        for (final ArenaGoal type : arena.getGoals()) {
            type.reset(force);
        }
    }

    public void setDefaults(final Arena arena, final YamlConfiguration config) {
        for (final ArenaGoal type : arena.getGoals()) {
            type.setDefaults(config);
        }
    }

    public void setPlayerLives(final Arena arena, final int value) {
        for (final ArenaGoal type : arena.getGoals()) {
            type.setPlayerLives(value);
        }
    }

    public void setPlayerLives(final Arena arena, final ArenaPlayer player,
                               final int value) {
        for (final ArenaGoal type : arena.getGoals()) {
            type.setPlayerLives(player, value);
        }
    }

    public void timedEnd(final Arena arena) {

        /*
          name/team => score points

          handed over to each module
         */

        debug(arena, "timed end!");

        Map<String, Double> scores = new HashMap<>();

        for (final ArenaGoal type : arena.getGoals()) {
            debug(arena, "scores: " + type.getName());
            scores = type.timedEnd(scores);
        }

        final Set<String> winners = new HashSet<>();

        if (arena.isFreeForAll() && arena.getTeams().size() <= 1) {
            winners.add("free");
            debug(arena, "adding FREE");
        } else if ("none".equals(arena.getArenaConfig().getString(CFG.GENERAL_TIME_WINNER))) {
            // check all teams
            double maxScore = 0;

            int neededTeams = arena.getTeams().size();

            for (final String team : arena.getTeamNames()) {
                if (scores.containsKey(team)) {
                    final double teamScore = scores.get(team);

                    if (teamScore > maxScore) {
                        maxScore = teamScore;
                        winners.clear();
                        winners.add(team);
                        debug(arena, "clear and add team " + team);
                    } else if (teamScore == maxScore) {
                        winners.add(team);
                        debug(arena, "add team " + team);
                    }
                } else {
                    neededTeams -= 1;
                }
            }

            // neededTeams should be the number of active teams

            if (neededTeams <= 2) {
                debug(arena, "fixing neededTeams to be of size 2!");
                neededTeams = 2;
            }

            if (winners.size() >= neededTeams) {
                debug(arena, "team of winners is too big: "+winners.size()+"!");
                for (String s : winners) {
                    debug(arena, "- "+s);
                }
                debug(arena, "clearing winners!");
                winners.clear(); // noone wins.
            }
        } else {
            winners.add(arena.getArenaConfig().getString(CFG.GENERAL_TIME_WINNER));
            debug(arena, "added winner!");
        }

        if (winners.size() > 1) {
            debug(arena, "more than 1");
            final Set<String> preciseWinners = new HashSet<>();

            // several teams have max score!!
            double maxSum = 0;
            for (final ArenaTeam team : arena.getTeams()) {
                if (!winners.contains(team.getName())) {
                    continue;
                }

                double sum = 0;

                for (final ArenaPlayer ap : team.getTeamMembers()) {
                    if (scores.containsKey(ap.getName())) {
                        sum += scores.get(ap.getName());
                    }
                }

                if (sum == maxSum) {
                    preciseWinners.add(team.getName());
                    debug(arena, "adding " + team.getName());
                } else if (sum > maxSum) {
                    maxSum = sum;
                    preciseWinners.clear();
                    preciseWinners.add(team.getName());
                    debug(arena, "clearing and adding + " + team.getName());
                }
            }

            if (!preciseWinners.isEmpty()) {
                winners.clear();
                winners.addAll(preciseWinners);
            }
        }

        if (arena.isFreeForAll() && arena.getTeams().size() <= 1) {
            debug(arena, "FFA");
            final Set<String> preciseWinners = new HashSet<>();

            for (final ArenaTeam team : arena.getTeams()) {
                if (!winners.contains(team.getName())) {
                    continue;
                }

                double maxSum = 0;

                for (final ArenaPlayer ap : team.getTeamMembers()) {
                    double sum = 0;
                    if (scores.containsKey(ap.getName())) {
                        sum = scores.get(ap.getName());
                    }
                    if (sum == maxSum) {
                        preciseWinners.add(ap.getName());
                        debug(arena, "ffa adding " + ap.getName());
                    } else if (sum > maxSum) {
                        maxSum = sum;
                        preciseWinners.clear();
                        preciseWinners.add(ap.getName());
                        debug(arena, "ffa clr & adding " + ap.getName());
                    }
                }
            }
            winners.clear();

            if (preciseWinners.size() != arena.getPlayedPlayers().size()) {
                winners.addAll(preciseWinners);
            }
        }

        ArenaModuleManager.timedEnd(arena, winners);

        if (arena.isFreeForAll() && arena.getTeams().size() <= 1) {
            debug(arena, "FFA and <= 1!");
            for (final ArenaTeam team : arena.getTeams()) {
                final Set<ArenaPlayer> apSet = new HashSet<>(team.getTeamMembers());

                for (final ArenaPlayer p : apSet) {
                    if (winners.isEmpty()) {
                        arena.removePlayer(p.get(), arena.getArenaConfig()
                                .getString(CFG.TP_LOSE), true, false);
                    } else {
                        if (winners.contains(p.getName())) {

                            ArenaModuleManager.announce(
                                    arena,
                                    Language.parse(arena, MSG.PLAYER_HAS_WON,
                                            p.getName()), "WINNER");
                            arena.broadcast(Language.parse(arena, MSG.PLAYER_HAS_WON,
                                    p.getName()));
                        } else {
                            if (p.getStatus() != Status.FIGHT) {
                                continue;
                            }
                            p.addLosses();
                            p.setStatus(Status.LOST);
                        }
                    }
                }
            }
            if (winners.isEmpty()) {
                ArenaModuleManager.announce(arena,
                        Language.parse(arena, MSG.FIGHT_DRAW), "WINNER");
                arena.broadcast(Language.parse(arena, MSG.FIGHT_DRAW));
            }
        } else if (!winners.isEmpty()) {

            boolean hasBroadcasted = false;
            for (final ArenaTeam team : arena.getTeams()) {
                if (winners.contains(team.getName())) {
                    if (!hasBroadcasted) {
                        ArenaModuleManager.announce(
                                arena,
                                Language.parse(arena, MSG.TEAM_HAS_WON,
                                        team.getName()), "WINNER");
                        arena.broadcast(Language.parse(arena, MSG.TEAM_HAS_WON,
                                team.getColor() + team.getName()));
                        hasBroadcasted = true;
                    }
                } else {

                    final Set<ArenaPlayer> apSet = new HashSet<>(team.getTeamMembers());
                    for (final ArenaPlayer p : apSet) {
                        if (p.getStatus() != Status.FIGHT) {
                            continue;
                        }
                        p.addLosses();
                        if (!hasBroadcasted) {
                            for (final String winTeam : winners) {
                                ArenaModuleManager.announce(arena, Language
                                        .parse(arena, MSG.TEAM_HAS_WON, winTeam), "WINNER");

                                final ArenaTeam winningTeam = arena.getTeam(winTeam);

                                if (winningTeam != null) {
                                    arena.broadcast(Language.parse(arena, MSG.TEAM_HAS_WON,
                                            winningTeam.getColor() + winTeam));
                                } else {
                                    PVPArena.getInstance().getLogger().severe("Winning team is NULL: " + winTeam);
                                }
                            }
                            hasBroadcasted = true;
                        }

                        p.setStatus(Status.LOST);
                    }
                }
            }
        } else {
            ArenaModuleManager.announce(arena, Language.parse(arena, MSG.FIGHT_DRAW),
                    "WINNER");
            arena.broadcast(Language.parse(arena, MSG.FIGHT_DRAW));
            arena.reset(true);
            return;
        }
        /*
		 * for (ArenaPlayer player : arena.getEveryone()) { if
		 * (player.getStatus() == Status.FIGHT) { player.setStatus(Status.LOST);
		 * } }
		 */
        debug(arena, "resetting arena!");

        arena.reset(false); // TODO: try to establish round compatibility with
        // new EndRunnable();
    }

    public void unload(final Arena arena, final Player player) {
        for (final ArenaGoal type : arena.getGoals()) {
            type.unload(player);
        }
    }

    public void disconnect(final Arena arena, final ArenaPlayer player) {
        if (arena == null) {
            return;
        }
        for (final ArenaGoal type : arena.getGoals()) {
            type.disconnect(player);
        }
    }

    public static void lateJoin(final Arena arena, final Player player) {
        for (final ArenaGoal goal : arena.getGoals()) {
            goal.lateJoin(player);
        }
    }

    public static void onPlayerPickUp(final Arena arena, final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            for (final ArenaGoal goal : arena.getGoals()) {
                goal.onPlayerPickUp(event);
            }
        }
    }
}
