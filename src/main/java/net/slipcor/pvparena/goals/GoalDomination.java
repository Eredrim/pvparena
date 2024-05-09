package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PAClaimBar;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.events.goal.PAGoalEndEvent;
import net.slipcor.pvparena.events.goal.PAGoalScoreEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.CircleParticleRunnable;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "Domination"
 * </pre>
 *
 * @author slipcor
 */

public class GoalDomination extends ArenaGoal {

    private static final int INTERVAL = 200;
    private static final String FLAG = "flag";

    private BukkitTask circleTask = null;

    public GoalDomination() {
        super("Domination");
    }

    private final Map<Location, ArenaTeam> flagMap = new HashMap<>();
    private final Map<Location, DominationRunnable> runnerMap = new HashMap<>();
    private final Map<Location, PAClaimBar> flagBars = new HashMap<>();

    private int announceOffset;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH);
    }

    private void barStart(Location location, String title, ChatColor color, int range) {
        if (!this.arena.getConfig().getBoolean(CFG.GOAL_DOM_BOSSBAR)) {
            return;
        }
        if (this.getBarMap().containsKey(location)) {
            PAClaimBar claimBar = this.getBarMap().get(location);
            claimBar.restart(title, color, location, range, INTERVAL);
        } else {
            PAClaimBar claimBar = new PAClaimBar(this.arena, title, color, location, range, INTERVAL);
            this.getBarMap().put(location, claimBar);
        }
    }

    private void barStop(Location location) {
        if (this.getBarMap().containsKey(location)) {
            this.getBarMap().get(location).stop();
        }
    }

    @Override
    public boolean checkCommand(final String string) {
        return FLAG.equalsIgnoreCase(string);
    }

    @Override
    public List<String> getGoalCommands() {
        return Collections.singletonList(FLAG);
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"add"});
        arena.getBlocks().forEach(paBlock -> result.define(new String[]{"remove", paBlock.getName()}));
        return result;
    }

    @Override
    public boolean checkEnd() {
        final int count = TeamManager.countActiveTeams(this.arena);

        if (count == 1) {
            return true; // yep. only one team left. go!
        } else if (count == 0) {
            debug(this.arena, "No teams playing!");
        }
        return false;
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingTeamSpawn(this.arena, spawns);
    }

    @Override
    public Set<PABlock> checkForMissingBlocks(Set<PABlock> blocks) {
        // at least one flag must be set
        return SpawnManager.getMissingBlocksCustom(this.arena, blocks, FLAG + "1");
    }

    /**
     * return a hashset of players names being near a specified location, except
     * one player
     *
     * @param loc      the location to check
     * @param distance the distance in blocks
     * @return a set of player names
     */
    private Set<ArenaTeam> checkLocationPresentTeams(final Location loc, final int distance) {
        final Set<ArenaTeam> result = new HashSet<>();
        final Location flagCenter = Utils.getCenteredLocation(loc);

        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {

            if (arenaPlayer.getPlayer().getLocation().distance(flagCenter) > distance) {
                continue;
            }
            result.add(arenaPlayer.getArenaTeam());
        }

        return result;
    }

    void checkMove() {

        /*
          possible Situations

          >>- flag is unclaimed and no one is there
          >>- flag is unclaimed and team a is there
          >>- flag is unclaimed and multiple teams are there

          >>- flag is being claimed by team a, no one is present
          >>- flag is being claimed by team a, team a is present
          >>- flag is being claimed by team a, multiple teams are present
          >>- flag is being claimed by team a, team b is present

          >>- flag is claimed by team a, no one is present
          >>- flag is claimed by team a, team a is present
          >>- flag is claimed by team a, multiple teams are present
          >>- flag is claimed by team a, team b is present

          >>- flag is claimed by team a and being unclaimed, no one is present
          >>- flag is claimed by team a and being unclaimed, team a is present
          >>- flag is claimed by team a and being unclaimed, multiple teams are present
          >>- flag is claimed by team a and being unclaimed, team b is present

         */

        debug(this.arena, "------------------");
        debug(this.arena, "   checkMove();");
        debug(this.arena, "------------------");

        final int checkDistance = this.arena.getConfig().getInt(CFG.GOAL_DOM_CLAIMRANGE);

        for (PABlockLocation paLoc : SpawnManager.getBlocksStartingWith(this.arena, FLAG, null)) {

            final Location loc = paLoc.toLocation();

            final Set<ArenaTeam> teams = this.checkLocationPresentTeams(loc, checkDistance);

            debug(this.arena, "teams: " + StringParser.joinSet(teams, ", "));

            // teams now contains all teams near the flag

            if (teams.isEmpty()) {
                // no one there
                if (this.getRunnerMap().containsKey(loc)) {
                    debug(this.arena, "flag is being (un)claimed! Cancelling!");
                    // cancel unclaiming/claiming if noone's near
                    this.getRunnerMap().get(loc).cancel();
                    this.getRunnerMap().remove(loc);
                    this.barStop(loc);
                }
                if (this.getFlagMap().containsKey(loc)) {
                    final ArenaTeam arenaTeam = this.getFlagMap().get(loc);

                    if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
                        continue;
                    }

                    // flag claimed! add score!
                    this.maybeAddScoreAndBroadCast(arenaTeam);
                }
                continue;
            }

            // there are actually teams at the flag
            debug(this.arena, "=> at least one team is at the flag!");

            if (this.getFlagMap().containsKey(loc)) {
                // flag is taken. by whom?
                if (teams.contains(this.getFlagMap().get(loc))) {
                    // owning team is there
                    debug(this.arena, "  - owning team is there");
                    if (teams.size() > 1) {
                        // another team is there
                        debug(this.arena, "    - and another one");
                        if (this.getRunnerMap().containsKey(loc)) {
                            // it is being unclaimed
                            debug(this.arena, "      - being unclaimed. continue!");
                        } else {
                            // unclaim
                            debug(this.arena, "      - not being unclaimed. do it!");
                            ArenaTeam team = this.getFlagMap().get(loc);
                            String contestingMsg = Language.parse(MSG.GOAL_DOMINATION_CONTESTING, team.getColoredName() + ChatColor.YELLOW);
                            this.arena.broadcast(contestingMsg);
                            final DominationRunnable domRunner = new DominationRunnable(
                                    this.arena, false, loc,
                                    this.getFlagMap().get(loc), this);

                            domRunner.runTaskTimer(PVPArena.getInstance(), INTERVAL, INTERVAL);

                            this.getRunnerMap().put(loc, domRunner);
                            this.barStart(loc, contestingMsg, ChatColor.WHITE, checkDistance);
                        }
                    } else {
                        // just the owning team is there
                        debug(this.arena, "    - noone else");
                        if (this.getRunnerMap().containsKey(loc)) {
                            debug(this.arena, "      - being unclaimed. cancel!");
                            // it is being unclaimed
                            // cancel task!
                            this.getRunnerMap().get(loc).cancel();
                            this.getRunnerMap().remove(loc);
                            this.barStop(loc);
                        } else {

                            final ArenaTeam arenaTeam = this.getFlagMap().get(loc);

                            if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
                                continue;
                            }

                            this.maybeAddScoreAndBroadCast(arenaTeam);
                        }
                    }
                    continue;
                }

                debug(this.arena, "  - owning team is not there!");
                // owning team is NOT there ==> unclaim!

                if (this.getRunnerMap().containsKey(loc)) {
                    if (this.getRunnerMap().get(loc).isTaken()) {
                        debug(this.arena, "    - runnable is trying to score, abort");

                        this.getRunnerMap().get(loc).cancel();
                        this.getRunnerMap().remove(loc);
                    } else {
                        debug(this.arena, "    - being unclaimed. continue.");
                    }
                    continue;
                }
                debug(this.arena, "    - not yet being unclaimed, do it!");
                // create an unclaim runnable
                ArenaTeam arenaTeam = this.getFlagMap().get(loc);
                String unclaimingMsg = Language.parse(MSG.GOAL_DOMINATION_UNCLAIMING, arenaTeam.getColoredName() + ChatColor.YELLOW);
                this.arena.broadcast(unclaimingMsg);
                final DominationRunnable running = new DominationRunnable(this.arena,
                        false, loc, this.getFlagMap().get(loc), this);

                running.runTaskTimer(PVPArena.getInstance(), INTERVAL, INTERVAL);
                this.getRunnerMap().put(loc, running);
                this.barStart(loc, unclaimingMsg, ChatColor.WHITE, checkDistance);
            } else {
                // flag not taken
                debug(this.arena, "- flag not taken");

                /*
                 * check if a runnable
                 * 	yes
                 * 		check if only that team
                 * 			yes => continue;
                 * 			no => cancel
                 * 	no
                 * 		check if only that team
                 * 			yes => create runnable;
                 * 			no => continue
                 */
                if (this.getRunnerMap().containsKey(loc)) {
                    debug(this.arena, "  - being claimed");
                    if (teams.size() < 2) {
                        debug(this.arena, "  - only one team present");
                        if (teams.contains(this.getRunnerMap().get(loc).arenaTeam)) {
                            // just THE team that is claiming => NEXT
                            debug(this.arena, "  - claiming team present. next!");
                            continue;
                        }
                    }
                    debug(this.arena, "  - more than one team or another team. cancel claim!");
                    // more than THE team that is claiming => cancel!
                    this.getRunnerMap().get(loc).cancel();
                    this.getRunnerMap().remove(loc);
                    this.barStop(loc);
                } else {
                    debug(this.arena, "  - not being claimed");
                    // not being claimed
                    if (teams.size() < 2) {
                        debug(this.arena, "  - just one team present");
                        for (ArenaTeam arenaTeam : teams) {
                            debug(this.arena, "TEAM " + arenaTeam.getName() + " IS CLAIMING "
                                    + loc);
                            String claimingMsg = Language.parse(MSG.GOAL_DOMINATION_CLAIMING,
                                    arenaTeam.getColoredName() + ChatColor.YELLOW);
                            this.arena.broadcast(claimingMsg);

                            final DominationRunnable running = new DominationRunnable(
                                    this.arena, true, loc, arenaTeam, this);

                            running.runTaskTimer(PVPArena.getInstance(), INTERVAL, INTERVAL);
                            this.getRunnerMap().put(loc, running);
                            this.barStart(loc, claimingMsg, arenaTeam.getColor(), checkDistance);
                        }
                    } else {
                        debug(this.arena, "  - more than one team present. continue!");
                    }
                }
            }
        }
    }

    private void maybeAddScoreAndBroadCast(final ArenaTeam arenaTeam) {
        if (this.arena.getConfig().getBoolean(CFG.GOAL_DOM_ONLYWHENMORE)) {
            final Map<ArenaTeam, Integer> claimed = new HashMap<>();
            for (ArenaTeam currentArenaTeam : this.getFlagMap().values()) {
                final int toAdd;
                if (claimed.containsKey(currentArenaTeam)) {
                    toAdd = claimed.get(currentArenaTeam) + 1;
                } else {
                    toAdd = 1;
                }
                claimed.put(currentArenaTeam, toAdd);
            }
            for (Map.Entry<ArenaTeam, Integer> stringIntegerEntry : claimed.entrySet()) {
                if (stringIntegerEntry.getKey().equals(arenaTeam)) {
                    continue;
                }
                if (stringIntegerEntry.getValue() >= claimed.get(arenaTeam)) {
                    return;
                }
            }
        }

        this.reduceLivesCheckEndAndCommit(this.arena, arenaTeam);

        final int max = this.arena.getConfig().getInt(CFG.GOAL_DOM_LIVES);
        if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
            return;
        }

        final int lives = this.getTeamLifeMap().get(arenaTeam);

        if ((max - lives) % this.announceOffset != 0) {
            return;
        }

        this.arena.broadcast(Language.parse(MSG.GOAL_DOMINATION_SCORE,
                arenaTeam.getColoredName()
                        + ChatColor.YELLOW, (max - lives) + "/" + max));
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (!PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }

        return block != null && ColorUtils.isColorableMaterial(block.getType());
    }

    private void commit(final Arena arena, final ArenaTeam arenaTeam) {
        if (arena.realEndRunner != null) {
            debug(arena, "[DOM] already ending");
            return;
        }
        debug(arena, "[DOM] committing end: " + arenaTeam);
        debug(arena, "win: " + true);

        ArenaTeam winteam = arenaTeam;

        for (ArenaTeam team : arena.getTeams()) {
            if (team.equals(arenaTeam)) {
                continue;
            }
            for (ArenaPlayer ap : team.getTeamMembers()) {

                ap.getStats().incLosses();
                ap.setStatus(PlayerStatus.LOST);
            }
        }
        for (ArenaTeam currentArenaTeam : arena.getNotEmptyTeams()) {
            for (ArenaPlayer ap : currentArenaTeam.getTeamMembers()) {
                if (ap.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                winteam = currentArenaTeam;
                break;
            }
        }

        if (winteam != null) {

            ArenaModuleManager
                    .announce(
                            arena,
                            Language.parse(MSG.TEAM_HAS_WON,
                                    winteam.getColor()
                                            + winteam.getName() + ChatColor.YELLOW),
                            "WINNER");
            arena.broadcast(Language.parse(MSG.TEAM_HAS_WON,
                    winteam.getColor() + winteam.getName()
                            + ChatColor.YELLOW));
            this.arena.addWinner(winteam.getName());
        }

        this.getTeamLifeMap().clear();
        new EndRunnable(arena, arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if(args.length != 2 && args.length != 3) {
            this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "2, 3");
        } else {
            if("add".equalsIgnoreCase(args[1])) {
                if (PAA_Region.activeSelections.containsKey(sender.getName())) {
                    PAA_Region.activeSelections.remove(sender.getName());
                    this.arena.msg(sender, MSG.GOAL_CLOSED_SELECTION);
                } else {
                    PAA_Region.activeSelections.put(sender.getName(), this.arena);
                    this.arena.msg(sender, MSG.GOAL_DOMINATION_SET_FLAG);
                }
            } else if("remove".equalsIgnoreCase(args[1])) {
                if(args.length != 3) {
                    this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3");
                } else {
                    String flagName = args[2];
                    Optional<PABlock> paBlock = this.arena.getBlocks().stream()
                            .filter(block -> block.getName().equalsIgnoreCase(flagName))
                            .findAny();

                    if(!paBlock.isPresent()) {
                        this.arena.msg(sender, MSG.GOAL_FLAGS_NOTFOUND, flagName);
                        return;
                    }
                    SpawnManager.removeBlock(this.arena, paBlock.get());
                    this.arena.msg(sender, MSG.GOAL_FLAGS_REMOVED, flagName);
                }
            } else {
                this.arena.msg(sender, MSG.ERROR_COMMAND_INVALID, String.join(" ", FLAG, args[0]));
            }
        }
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[DOMINATION] already ending");
            return;
        }
        debug(this.arena, "[DOMINATION]");

        final PAGoalEndEvent gEvent = new PAGoalEndEvent(this.arena, this);
        Bukkit.getPluginManager().callEvent(gEvent);
        ArenaTeam aTeam = null;

        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == PlayerStatus.FIGHT) {
                    aTeam = team;
                    break;
                }
            }
        }

        if (aTeam != null && !force) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "END");

            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "WINNER");
            this.arena.broadcast(Language.parse(MSG.TEAM_HAS_WON, aTeam.getColor()
                    + aTeam.getName() + ChatColor.YELLOW));
            this.arena.addWinner(aTeam.getName());
        }

        if (ArenaModuleManager.commitEnd(this.arena, aTeam, null)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        if (PermissionManager.hasAdminPerm(player) || PermissionManager.hasBuilderPerm(player, this.arena)) {

            final Set<PABlockLocation> flags = SpawnManager.getBlocksStartingWith(this.arena, FLAG, null);

            if (flags.contains(new PABlockLocation(block.getLocation()))) {
                this.arena.msg(player, MSG.GOAL_DOMINATION_EXISTING_FLAG);
                return true;
            }

            int newIndex = this.arena.getBlocks().stream()
                    .map(paBlock -> Integer.valueOf(paBlock.getName().replace(FLAG, "")))
                    .max(Integer::compareTo)
                    .map(index -> index + 1)
                    .orElse(0);

            final String flagName = FLAG + newIndex;

            SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), flagName, null);

            this.arena.msg(player, MSG.GOAL_FLAGS_SET, flagName);
            return true;
        }
        return false;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("needed points: " + this.arena.getConfig().getInt(CFG.GOAL_DOM_LIVES));
        sender.sendMessage("claim range: " + this.arena.getConfig().getInt(CFG.GOAL_DOM_CLAIMRANGE));
    }

    @NotNull
    private Map<Location, ArenaTeam> getFlagMap() {
        return this.flagMap;
    }

    @NotNull
    private Map<Location, PAClaimBar> getBarMap() {
        return this.flagBars;
    }

    @NotNull
    private Map<Location, DominationRunnable> getRunnerMap() {
        return this.runnerMap;
    }

    @Override
    public void initiate(final ArenaPlayer arenaPlayer) {
        final ArenaTeam team = arenaPlayer.getArenaTeam();
        if (!this.getTeamLifeMap().containsKey(team)) {
            this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(), this.arena.getConfig()
                    .getInt(CFG.GOAL_DOM_LIVES));

            final Set<PABlockLocation> spawns = SpawnManager.getBlocksStartingWith(this.arena, FLAG, null);
            for (PABlockLocation spawn : spawns) {
                this.takeFlag(spawn);
            }
        }
    }

    @Override
    public void parseStart() {
        this.getTeamLifeMap().clear();
        for (ArenaTeam team : this.arena.getTeams()) {
            if (!team.getTeamMembers().isEmpty()) {
                debug(this.arena, "adding team " + team);
                // team is active
                this.getTeamLifeMap().put(team,
                        this.arena.getConfig().getInt(CFG.GOAL_DOM_LIVES, 3));
            }
        }
        final Set<PABlockLocation> spawns = SpawnManager.getBlocksStartingWith(this.arena, FLAG, null);
        for (PABlockLocation spawn : spawns) {
            this.takeFlag(spawn);
        }

        final DominationMainRunnable domMainRunner = new DominationMainRunnable(this.arena, this);
        final int tickInterval = this.arena.getConfig().getInt(CFG.GOAL_DOM_TICKINTERVAL);
        domMainRunner.runTaskTimer(PVPArena.getInstance(), tickInterval, tickInterval);

        this.announceOffset = this.arena.getConfig().getInt(CFG.GOAL_DOM_ANNOUNCEOFFSET);

        if (this.arena.getConfig().getBoolean(CFG.GOAL_DOM_PARTICLECIRCLE)) {
            this.circleTask = Bukkit.getScheduler().runTaskTimer(PVPArena.getInstance(),
                    new CircleParticleRunnable(this.arena, CFG.GOAL_DOM_CLAIMRANGE, this.getFlagMap()), 1L, 1L);
        }
    }

    private void reduceLivesCheckEndAndCommit(final Arena arena, final ArenaTeam arenaTeam) {

        debug(arena, "reducing lives of team " + arenaTeam);
        if (this.getTeamLifeMap().get(arenaTeam) != null) {
            final int score = arena.getConfig().getInt(CFG.GOAL_DOM_TICKREWARD);
            final int iLives = this.getTeamLifeMap().get(arenaTeam) - score;

            final PAGoalScoreEvent gEvent = new PAGoalScoreEvent(arena, this, null, arenaTeam, (long) score);
            Bukkit.getPluginManager().callEvent(gEvent);

            if (iLives > 0) {
                this.getTeamLifeMap().put(arenaTeam, iLives);
            } else {
                this.getTeamLifeMap().remove(arenaTeam);
                this.commit(arena, arenaTeam);
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getBarMap().clear();
        this.getTeamLifeMap().clear();
        this.getRunnerMap().clear();
        this.getFlagMap().clear();
        if (this.circleTask != null) {
            this.circleTask.cancel();
            this.circleTask = null;
        }
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (config.get("teams") == null) {
            debug(this.arena, "no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
    }

    /**
     * take/reset an arena flag
     *
     * @param paBlockLocation the location to take/reset
     */
    private void takeFlag(final PABlockLocation paBlockLocation) {
        Block flagBlock = paBlockLocation.toLocation().getBlock();
        ColorUtils.setNewFlagColor(flagBlock, ChatColor.WHITE);
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaTeam team : this.arena.getNotEmptyTeams()) {
            double score = this.getTeamLifeMap().getOrDefault(team, 0);
            if (scores.containsKey(team.getName())) {
                scores.put(team.getName(), scores.get(team.getName()) + score);
            } else {
                scores.put(team.getName(), score);
            }
        }

        return scores;
    }

    private static class DominationRunnable extends BukkitRunnable {
        private final boolean taken;
        private final Location loc;
        private final Arena arena;
        public final ArenaTeam arenaTeam;
        private final GoalDomination domination;

        /**
         * create a domination runnable
         *
         * @param arena the arena we are running in
         */
        public DominationRunnable(final Arena arena, final boolean taken, final Location loc2, final ArenaTeam arenaTeam,
                                  final GoalDomination goal) {
            this.arena = arena;
            this.taken = taken;
            this.arenaTeam = arenaTeam;
            this.loc = loc2;
            this.domination = goal;
            debug(arena, "Domination constructor");
        }

        /**
         * the run method
         */
        @Override
        public void run() {
            debug(this.arena, "DominationRunnable commiting");
            debug(this.arena, "team " + this.arenaTeam + ", take: " + this.taken);
            if (this.taken) {
                // claim a flag for the team
                if (!this.domination.getFlagMap().containsKey(this.loc)) {
                    // flag unclaimed! claim!
                    debug(this.arena, "clag unclaimed. claim!");
                    this.domination.getFlagMap().put(this.loc, this.arenaTeam);
                    // long interval = 20L * 5;

                    this.arena.broadcast(Language.parse(
                            MSG.GOAL_DOMINATION_CLAIMED, this.arenaTeam
                                    .getColoredName() + ChatColor.YELLOW));
                    this.takeFlag(this.arena, this.loc, this.arenaTeam);
                    this.domination.getFlagMap().put(this.loc, this.arenaTeam);

                    // claim done. end timer
                    this.cancel();
                    this.domination.getRunnerMap().remove(this.loc);
                }
            } else {
                // unclaim
                debug(this.arena, "unclaimed");
                this.takeFlag(this.arena, this.loc, null);
                this.cancel();
                this.domination.getRunnerMap().remove(this.loc);
                this.domination.getFlagMap().remove(this.loc);
            }
        }


        private void takeFlag(final Arena arena, final Location lBlock, final ArenaTeam arenaTeam) {
            Block flagBlock = lBlock.getBlock();
            // unclaim
            if (arenaTeam == null) {
                ColorUtils.setNewFlagColor(flagBlock, ChatColor.WHITE);
            } else {
                ColorUtils.setNewFlagColor(flagBlock, arenaTeam.getColor());
            }
        }

        private boolean isTaken() {
            return this.taken;
        }
    }

    private static class DominationMainRunnable extends BukkitRunnable {
        private final Arena arena;
        private final GoalDomination domination;

        public DominationMainRunnable(final Arena arena, final GoalDomination goal) {
            this.arena = arena;
            this.domination = goal;
            debug(arena, "DominationMainRunnable constructor");
        }

        /**
         * the run method, commit arena end
         */
        @Override
        public void run() {
            if (!this.arena.isFightInProgress() || this.arena.realEndRunner != null) {
                this.cancel();
            }
            this.domination.checkMove();
        }
    }
}
