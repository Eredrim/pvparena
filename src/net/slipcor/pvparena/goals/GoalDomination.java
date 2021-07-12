package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.*;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.*;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.CircleParticleRunnable;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * <pre>
 * Arena Goal class "Domination"
 * </pre>
 *
 * @author slipcor
 */

public class GoalDomination extends ArenaGoal {

    private static final int PRIORITY = 8;
    private static final int INTERVAL = 200;

    private BukkitTask circleTask = null;

    public GoalDomination() {
        super("Domination");
        this.debug = new Debug(99);
    }

    private Map<Location, String> flagMap = new HashMap<>();
    private Map<Location, DominationRunnable> runnerMap = new HashMap<>();
    private Map<Location, PAClaimBar> flagBars = new HashMap<>();

    private int announceOffset;

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    private void barStart(Location location, String title, ChatColor color, int range) {
        if (!this.arena.getArenaConfig().getBoolean(CFG.GOAL_DOM_BOSSBAR)) {
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
    public PACheck checkCommand(final PACheck res, final String string) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        if ("flag".equals(string)) {
            res.setPriority(this, PRIORITY);
        }

        return res;
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("flag");
    }

    @Override
    public PACheck checkEnd(final PACheck res) {

        if (res.getPriority() > PRIORITY) {
            return res;
        }

        final int count = TeamManager.countActiveTeams(this.arena);

        if (count == 1) {
            res.setPriority(this, PRIORITY); // yep. only one team left. go!
        } else if (count == 0) {
            this.arena.getDebugger().i("No teams playing!");
        }

        return res;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {

        final String team = this.checkForMissingTeamSpawn(list);
        if (team != null) {
            return team;
        }
        int count = 0;
        for (final String s : list) {
            if (s.startsWith("flag")) {
                count++;
            }
        }
        if (count < 1) {
            return "flags: " + count + " / 1";
        }
        return null;
    }

    @Override
    public PACheck checkJoin(final CommandSender sender, final PACheck res, final String[] args) {
        if (res.getPriority() >= PRIORITY) {
            return res;
        }

        final int maxPlayers = this.arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS);
        final int maxTeamPlayers = this.arena.getArenaConfig().getInt(
                CFG.READY_MAXTEAMPLAYERS);

        if (maxPlayers > 0 && this.arena.getFighters().size() >= maxPlayers) {
            res.setError(this, Language.parse(this.arena, MSG.ERROR_JOIN_ARENA_FULL));
            return res;
        }

        if (args == null || args.length < 1) {
            return res;
        }

        if (!this.arena.isFreeForAll()) {
            final ArenaTeam team = this.arena.getTeam(args[0]);

            if (team != null && maxTeamPlayers > 0
                    && team.getTeamMembers().size() >= maxTeamPlayers) {
                res.setError(this, Language.parse(this.arena, MSG.ERROR_JOIN_TEAM_FULL, team.getName()));
                return res;
            }
        }

        res.setPriority(this, PRIORITY);
        return res;
    }

    /**
     * return a hashset of players names being near a specified location, except
     * one player
     *
     * @param loc      the location to check
     * @param distance the distance in blocks
     * @return a set of player names
     */
    private Set<String> checkLocationPresentTeams(final Location loc, final int distance) {
        final Set<String> result = new HashSet<>();
        final Location flagCenter = Utils.getCenteredLocation(loc);

        for (final ArenaPlayer p : this.arena.getFighters()) {

            if (p.get().getLocation().distance(flagCenter) > distance) {
                continue;
            }

            result.add(p.getArenaTeam().getName());
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

        this.arena.getDebugger().i("------------------");
        this.arena.getDebugger().i("   checkMove();");
        this.arena.getDebugger().i("------------------");

        final int checkDistance = this.arena.getArenaConfig().getInt(CFG.GOAL_DOM_CLAIMRANGE);

        for (final PABlockLocation paLoc : SpawnManager.getBlocksStartingWith(this.arena, "flag")) {
            // arena.getDebugger().info("checking location: " + loc.toString());

            final Location loc = paLoc.toLocation();

            final Set<String> teams = this.checkLocationPresentTeams(loc, checkDistance);

            this.arena.getDebugger().i("teams: " + StringParser.joinSet(teams, ", "));

            // teams now contains all teams near the flag

            if (teams.size() < 1) {
                // arena.getDebugger().info("=> noone there!");
                // no one there
                if (this.getRunnerMap().containsKey(loc)) {
                    this.arena.getDebugger().i("flag is being (un)claimed! Cancelling!");
                    // cancel unclaiming/claiming if noone's near
                    this.getRunnerMap().get(loc).cancel();
                    this.getRunnerMap().remove(loc);
                    this.barStop(loc);
                }
                if (this.getFlagMap().containsKey(loc)) {
                    final String team = this.getFlagMap().get(loc);

                    if (!this.getLifeMap().containsKey(team)) {
                        continue;
                    }

                    // flag claimed! add score!
                    this.maybeAddScoreAndBroadCast(team);
                }
                continue;
            }

            // there are actually teams at the flag
            this.arena.getDebugger().i("=> at least one team is at the flag!");

            if (this.getFlagMap().containsKey(loc)) {
                // flag is taken. by whom?
                if (teams.contains(this.getFlagMap().get(loc))) {
                    // owning team is there
                    this.arena.getDebugger().i("  - owning team is there");
                    if (teams.size() > 1) {
                        // another team is there
                        this.arena.getDebugger().i("    - and another one");
                        if (this.getRunnerMap().containsKey(loc)) {
                            // it is being unclaimed
                            this.arena.getDebugger().i("      - being unclaimed. continue!");
                        } else {
                            // unclaim
                            this.arena.getDebugger().i("      - not being unclaimed. do it!");
                            ArenaTeam team = this.arena.getTeam(this.getFlagMap().get(loc));
                            String contestingMsg = Language.parse(this.arena, MSG.GOAL_DOMINATION_CONTESTING, team.getColoredName() + ChatColor.YELLOW);
                            this.arena.broadcast(contestingMsg);
                            final DominationRunnable domRunner = new DominationRunnable(
                                    this.arena, false, loc,
                                    this.getFlagMap().get(loc), this);

                            domRunner.runTaskTimer(PVPArena.instance, INTERVAL, INTERVAL);

                            this.getRunnerMap().put(loc, domRunner);
                            this.barStart(loc, contestingMsg, ChatColor.WHITE, checkDistance);
                        }
                    } else {
                        // just the owning team is there
                        this.arena.getDebugger().i("    - noone else");
                        if (this.getRunnerMap().containsKey(loc)) {
                            this.arena.getDebugger().i("      - being unclaimed. cancel!");
                            // it is being unclaimed
                            // cancel task!
                            this.getRunnerMap().get(loc).cancel();
                            this.getRunnerMap().remove(loc);
                            this.barStop(loc);
                        } else {

                            final String team = this.getFlagMap().get(loc);

                            if (!this.getLifeMap().containsKey(team)) {
                                continue;
                            }

                            this.maybeAddScoreAndBroadCast(team);
                        }
                    }
                    continue;
                }

                this.arena.getDebugger().i("  - owning team is not there!");
                // owning team is NOT there ==> unclaim!

                if (this.getRunnerMap().containsKey(loc)) {
                    if (this.getRunnerMap().get(loc).isTaken()) {
                        this.arena.getDebugger().i("    - runnable is trying to score, abort");

                        this.getRunnerMap().get(loc).cancel();
                        this.getRunnerMap().remove(loc);
                    } else {
                        this.arena.getDebugger().i("    - being unclaimed. continue.");
                    }
                    continue;
                }
                this.arena.getDebugger().i("    - not yet being unclaimed, do it!");
                // create an unclaim runnable
                ArenaTeam team = this.arena.getTeam(this.getFlagMap().get(loc));
                String unclaimingMsg = Language.parse(this.arena, MSG.GOAL_DOMINATION_UNCLAIMING, team.getColoredName() + ChatColor.YELLOW);
                this.arena.broadcast(unclaimingMsg);
                final DominationRunnable running = new DominationRunnable(this.arena,
                        false, loc, this.getFlagMap().get(loc), this);

                running.runTaskTimer(PVPArena.instance, INTERVAL, INTERVAL);
                this.getRunnerMap().put(loc, running);
                this.barStart(loc, unclaimingMsg, ChatColor.WHITE, checkDistance);
            } else {
                // flag not taken
                this.arena.getDebugger().i("- flag not taken");

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
                    this.arena.getDebugger().i("  - being claimed");
                    if (teams.size() < 2) {
                        this.arena.getDebugger().i("  - only one team present");
                        if (teams.contains(this.getRunnerMap().get(loc).team)) {
                            // just THE team that is claiming => NEXT
                            this.arena.getDebugger().i("  - claiming team present. next!");
                            continue;
                        }
                    }
                    this.arena.getDebugger().i("  - more than one team or another team. cancel claim!");
                    // more than THE team that is claiming => cancel!
                    this.getRunnerMap().get(loc).cancel();
                    this.getRunnerMap().remove(loc);
                    this.barStop(loc);
                } else {
                    this.arena.getDebugger().i("  - not being claimed");
                    // not being claimed
                    if (teams.size() < 2) {
                        this.arena.getDebugger().i("  - just one team present");
                        for (final String sName : teams) {
                            this.arena.getDebugger().i("TEAM " + sName + " IS CLAIMING "
                                    + loc);
                            final ArenaTeam team = this.arena.getTeam(sName);
                            String claimingMsg = Language.parse(this.arena, MSG.GOAL_DOMINATION_CLAIMING,
                                    team.getColoredName() + ChatColor.YELLOW);
                            this.arena.broadcast(claimingMsg);

                            final DominationRunnable running = new DominationRunnable(
                                    this.arena, true, loc, sName, this);

                            running.runTaskTimer(PVPArena.instance, INTERVAL, INTERVAL);
                            this.getRunnerMap().put(loc, running);
                            this.barStart(loc, claimingMsg, team.getColor(), checkDistance);
                        }
                    } else {
                        this.arena.getDebugger().i("  - more than one team present. continue!");
                    }
                }
            }
        }
    }

    private void maybeAddScoreAndBroadCast(final String team) {
        if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_DOM_ONLYWHENMORE)) {
            final Map<String, Integer> claimed = new HashMap<>();
            for (final String s : this.getFlagMap().values()) {
                final int toAdd;
                if (claimed.containsKey(s)) {
                    toAdd = claimed.get(s) + 1;
                } else {
                    toAdd = 1;
                }
                claimed.put(s, toAdd);
            }
            for (final Map.Entry<String, Integer> stringIntegerEntry : claimed.entrySet()) {
                if (stringIntegerEntry.getKey().equals(team)) {
                    continue;
                }
                if (stringIntegerEntry.getValue() >= claimed.get(team)) {
                    return;
                }
            }
        }

        this.reduceLivesCheckEndAndCommit(this.arena, team);

        final int max = this.arena.getArenaConfig().getInt(CFG.GOAL_DOM_LIVES);
        if (!this.getLifeMap().containsKey(team)) {
            return;
        }

        final int lives = this.getLifeMap().get(team);

        if ((max - lives) % this.announceOffset != 0) {
            return;
        }

        this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_DOMINATION_SCORE,
                this.arena.getTeam(team).getColoredName()
                        + ChatColor.YELLOW, (max - lives) + "/" + max));
    }

    @Override
    public PACheck checkSetBlock(final PACheck res, final Player player, final Block block) {

        if (res.getPriority() > PRIORITY || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return res;
        }
        if (block == null || !ColorUtils.isColorableMaterial(block.getType())) {
            return res;
        }
        res.setPriority(this, PRIORITY); // success :)

        return res;
    }

    private void commit(final Arena arena, final String sTeam) {
        if (arena.realEndRunner != null) {
            arena.getDebugger().i("[DOM] already ending");
            return;
        }
        arena.getDebugger().i("[DOM] committing end: " + sTeam);
        arena.getDebugger().i("win: " + true);

        String winteam = sTeam;

        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getName().equals(sTeam)) {
                continue;
            }
            for (final ArenaPlayer ap : team.getTeamMembers()) {

                ap.addLosses();
				/*
				arena.tpPlayerToCoordName(ap.get(), "spectator");
				ap.setTelePass(false);
				*/

                ap.setStatus(Status.LOST);
            }
        }
        for (final ArenaTeam team : arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != Status.FIGHT) {
                    continue;
                }
                winteam = team.getName();
                break;
            }
        }

        if (arena.getTeam(winteam) != null) {

            ArenaModuleManager
                    .announce(
                            arena,
                            Language.parse(arena, MSG.TEAM_HAS_WON,
                                    arena.getTeam(winteam).getColor()
                                            + winteam + ChatColor.YELLOW),
                            "WINNER");
            arena.broadcast(Language.parse(arena, MSG.TEAM_HAS_WON,
                    arena.getTeam(winteam).getColor() + winteam
                            + ChatColor.YELLOW));
        }

        this.getLifeMap().clear();
        new EndRunnable(arena, arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (PAA_Region.activeSelections.containsKey(sender.getName())) {
            PAA_Region.activeSelections.remove(sender.getName());
            this.arena.msg(sender, Language.parse(this.arena, MSG.GOAL_FLAGS_SET, "flags"));
        } else {

            PAA_Region.activeSelections.put(sender.getName(), this.arena);
            this.arena.msg(sender, Language.parse(this.arena, MSG.GOAL_FLAGS_TOSET, "flags"));
        }
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            this.arena.getDebugger().i("[DOMINATION] already ending");
            return;
        }
        this.arena.getDebugger().i("[DOMINATION]");

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        ArenaTeam aTeam = null;

        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == Status.FIGHT) {
                    aTeam = team;
                    break;
                }
            }
        }

        if (aTeam != null && !force) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(this.arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "END");

            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(this.arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "WINNER");
            this.arena.broadcast(Language.parse(this.arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                    + aTeam.getName() + ChatColor.YELLOW));
        }

        if (ArenaModuleManager.commitEnd(this.arena, aTeam)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public boolean commitSetFlag(final Player player, final Block block) {

        if (PermissionManager.hasAdminPerm(player)
                || PermissionManager.hasBuilderPerm(player, this.arena)
                && player.getInventory().getItemInMainHand().getType().toString().equals(this.arena
                .getArenaConfig().getString(CFG.GENERAL_WAND))) {

            final Set<PABlockLocation> flags = SpawnManager.getBlocksStartingWith(this.arena,"flag");

            if (flags.contains(new PABlockLocation(block.getLocation()))) {
                return false;
            }

            final String flagName = "flag" + flags.size();

            SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), flagName);

            this.arena.msg(player, Language.parse(this.arena, MSG.GOAL_FLAGS_SET, flagName));
            return true;
        }
        return false;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("needed points: " + this.arena.getArenaConfig().getInt(CFG.GOAL_DOM_LIVES));
        sender.sendMessage("claim range: " + this.arena.getArenaConfig().getInt(CFG.GOAL_DOM_CLAIMRANGE));
    }

    private Map<Location, String> getFlagMap() {
        if (this.flagMap == null) {
            this.flagMap = new HashMap<>();
        }
        return this.flagMap;
    }

    private Map<Location, PAClaimBar> getBarMap() {
        if (this.flagBars == null) {
            this.flagBars = new HashMap<>();
        }
        return this.flagBars;
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(this.getLifeMap().getOrDefault(aPlayer.getArenaTeam().getName(), 0))
            );
        }
        return res;
    }

    private Map<Location, DominationRunnable> getRunnerMap() {
        if (this.runnerMap == null) {
            this.runnerMap = new HashMap<>();
        }
        return this.runnerMap;
    }

    @Override
    public boolean hasSpawn(final String string) {
        for (final String teamName : this.arena.getTeamNames()) {
            if (string.toLowerCase().startsWith(
                    teamName.toLowerCase() + "spawn")) {
                return true;
            }

            if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                for (final ArenaClass aClass : this.arena.getClasses()) {
                    if (string.toLowerCase().startsWith(teamName.toLowerCase() +
                            aClass.getName().toLowerCase() + "spawn")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void initate(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (!this.getLifeMap().containsKey(team.getName())) {
            this.getLifeMap().put(aPlayer.getArenaTeam().getName(), this.arena.getArenaConfig()
                    .getInt(CFG.GOAL_DOM_LIVES));

            final Set<PABlockLocation> spawns = SpawnManager.getBlocksStartingWith(this.arena, "flag");
            for (final PABlockLocation spawn : spawns) {
                this.takeFlag(spawn);
            }
        }
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public void parseStart() {
        this.getLifeMap().clear();
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (!team.getTeamMembers().isEmpty()) {
                this.arena.getDebugger().i("adding team " + team.getName());
                // team is active
                this.getLifeMap().put(team.getName(),
                        this.arena.getArenaConfig().getInt(CFG.GOAL_DOM_LIVES, 3));
            }
        }
        final Set<PABlockLocation> spawns = SpawnManager.getBlocksStartingWith(this.arena, "flag");
        for (final PABlockLocation spawn : spawns) {
            this.takeFlag(spawn);
        }

        final DominationMainRunnable domMainRunner = new DominationMainRunnable(this.arena, this);
        final int tickInterval = this.arena.getArenaConfig().getInt(CFG.GOAL_DOM_TICKINTERVAL);
        domMainRunner.runTaskTimer(PVPArena.instance, tickInterval, tickInterval);

        this.announceOffset = this.arena.getArenaConfig().getInt(CFG.GOAL_DOM_ANNOUNCEOFFSET);

        if(this.arena.getArenaConfig().getBoolean(CFG.GOAL_DOM_PARTICLECIRCLE)) {
            this.circleTask = Bukkit.getScheduler().runTaskTimer(PVPArena.instance, new CircleParticleRunnable(this.arena, CFG.GOAL_DOM_CLAIMRANGE, this.getFlagMap()), 1L, 1L);
        }
    }

    private void reduceLivesCheckEndAndCommit(final Arena arena, final String team) {

        arena.getDebugger().i("reducing lives of team " + team);
        if (this.getLifeMap().get(team) != null) {
            final int score = arena.getArenaConfig().getInt(CFG.GOAL_DOM_TICKREWARD);
            final int iLives = this.getLifeMap().get(team) - score;

            final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "score:null:"+team+":"+score);
            Bukkit.getPluginManager().callEvent(gEvent);

            if (iLives > 0) {
                this.getLifeMap().put(team, iLives);
            } else {
                this.getLifeMap().remove(team);
                this.commit(arena, team);
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getBarMap().clear();
        this.getLifeMap().clear();
        this.getRunnerMap().clear();
        this.getFlagMap().clear();
        if (this.circleTask != null) {
            this.circleTask.cancel();
            this.circleTask = null;
        }
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (this.arena.isFreeForAll()) {
            return;
        }

        if (config.get("teams.free") != null) {
            config.set("teams", null);
        }
        if (config.get("teams") == null) {
            this.arena.getDebugger().i("no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
    }

    /**
     * take/reset an arena flag
     *
     * @param paBlockLocation the location to take/reset*/
    private void takeFlag(final PABlockLocation paBlockLocation) {
        Block flagBlock = paBlockLocation.toLocation().getBlock();
        ColorUtils.setNewFlagColor(flagBlock, ChatColor.WHITE);
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaTeam team : this.arena.getTeams()) {
            double score = this.getLifeMap().getOrDefault(team.getName(), 0);
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
        public final String team;
        private final GoalDomination domination;

        /**
         * create a domination runnable
         *
         * @param arena the arena we are running in
         */
        public DominationRunnable(final Arena arena, final boolean taken, final Location loc2, final String teamName,
                                  final GoalDomination goal) {
            this.arena = arena;
            this.taken = taken;
            this.team = teamName;
            this.loc = loc2;
            this.domination = goal;
            arena.getDebugger().i("Domination constructor");
        }

        /**
         * the run method
         */
        @Override
        public void run() {
            this.arena.getDebugger().i("DominationRunnable commiting");
            this.arena.getDebugger().i("team " + this.team + ", take: " + this.taken);
            if (this.taken) {
                // claim a flag for the team
                if (!this.domination.getFlagMap().containsKey(this.loc)) {
                    // flag unclaimed! claim!
                    this.arena.getDebugger().i("clag unclaimed. claim!");
                    this.domination.getFlagMap().put(this.loc, this.team);
                    // long interval = 20L * 5;

                    this.arena.broadcast(Language.parse(this.arena,
                            MSG.GOAL_DOMINATION_CLAIMED, this.arena.getTeam(this.team)
                                    .getColoredName() + ChatColor.YELLOW));
                    this.takeFlag(this.arena, this.loc, this.team);
                    this.domination.getFlagMap().put(this.loc, this.team);

                    // claim done. end timer
                    this.cancel();
                    this.domination.getRunnerMap().remove(this.loc);
                }
            } else {
                // unclaim
                this.arena.getDebugger().i("unclaimed");
                this.takeFlag(this.arena, this.loc, "");
                this.cancel();
                this.domination.getRunnerMap().remove(this.loc);
                this.domination.getFlagMap().remove(this.loc);
            }
        }


        private void takeFlag(final Arena arena, final Location lBlock, final String name) {
            ArenaTeam team = null;
            Block flagBlock = lBlock.getBlock();
            for (final ArenaTeam t : arena.getTeams()) {
                if (t.getName().equals(name)) {
                    team = t;
                }
            }
            if (team == null) {
                ColorUtils.setNewFlagColor(flagBlock, ChatColor.WHITE);
            } else {
                ColorUtils.setNewFlagColor(flagBlock, team.getColor());
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
            arena.getDebugger().i("DominationMainRunnable constructor");
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
