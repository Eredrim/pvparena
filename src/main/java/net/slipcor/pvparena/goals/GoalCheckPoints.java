package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "Domination"
 * </pre>
 *
 * @author slipcor
 */

public class GoalCheckPoints extends ArenaGoal {

    private static final String CHECKPOINT = "checkpoint";

    public GoalCheckPoints() {
        super("CheckPoints");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean isFreeForAll() {
        return true;
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public boolean checkCommand(final String string) {
        return CHECKPOINT.equalsIgnoreCase(string);
    }

    @Override
    public List<String> getGoalCommands() {
        return Collections.singletonList(CHECKPOINT);
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        Set<PASpawn> missing = new HashSet<>(SpawnManager.getMissingSpawns(spawns, SPAWN));
        // check if the first checkpoint is set
        missing.addAll(SpawnManager.getMissingSpawns(spawns, CHECKPOINT + "1"));
        return missing;
    }

    /**
     * return a hashset of players names being near a specified location
     *
     * @param loc      the location to check
     * @param distance the distance in blocks
     * @return a set of player names
     */
    private Set<ArenaPlayer> checkLocationPresentPlayers(final Location loc, final int distance) {
        final Set<ArenaPlayer> result = new HashSet<>();

        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            if (arenaPlayer.getPlayer().getLocation().getWorld().getName().equals(loc.getWorld().getName())) {
                if (arenaPlayer.getPlayer().getLocation().distance(loc) > distance) {
                    continue;
                }

                result.add(arenaPlayer);
            }
        }

        return result;
    }

    private void checkMove() {

        debug(this.arena, "------------------");
        debug(this.arena, "  GCP checkMove();");
        debug(this.arena, "------------------");

        final int checkDistance = this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_CLAIMRANGE);

        for (PASpawn spawn : SpawnManager.getPASpawnsStartingWith(this.arena, CHECKPOINT)) {
            final PALocation paLoc = spawn.getPALocation();
            final Set<ArenaPlayer> arenaPlayers = this.checkLocationPresentPlayers(paLoc.toLocation(),
                    checkDistance);

            debug(this.arena, "players: " + Arrays.toString(arenaPlayers.toArray()));

            // players now contains all players near the checkpoint

            if (arenaPlayers.isEmpty()) {
                continue;
            }
            int value = Integer.parseInt(spawn.getName().substring(10));
            for (ArenaPlayer arenaPlayer : arenaPlayers) {
                this.maybeAddScoreAndBroadCast(arenaPlayer, value);
            }

        }
    }

    private void maybeAddScoreAndBroadCast(final ArenaPlayer arenaPlayer, int checkpoint) {

        if (!this.getPlayerLifeMap().containsKey(arenaPlayer.getPlayer())) {
            return;
        }


        final int max = this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_LIVES);

        final int position = max - this.getPlayerLifeMap().get(arenaPlayer.getPlayer()) + 1;

        if (checkpoint == position) {
            this.arena.broadcast(Language.parse(MSG.GOAL_CHECKPOINTS_SCORE,
                    arenaPlayer.getName(), position + "/" + max));
            this.reduceLivesCheckEndAndCommit(this.arena, arenaPlayer);
        } else if (checkpoint > position) {
            this.arena.broadcast(Language.parse(MSG.GOAL_CHECKPOINTS_YOUMISSED,
                    String.valueOf(position), String.valueOf(checkpoint)));
        }

    }

    private void commitWin(final Arena arena, final ArenaPlayer arenaPlayer) {
        if (arena.realEndRunner != null) {
            debug(arena, "[CP] already ending");
            return;
        }
        debug(arena, "[CP] committing end: " + arenaPlayer);
        ArenaPlayer winner = null;
        for (ArenaPlayer fighter : arena.getFighters()) {
            if (fighter.equals(arenaPlayer)) {
                winner = fighter;
            } else {
                fighter.addLosses();
                fighter.setStatus(PlayerStatus.LOST);
            }
        }

        if (winner != null) {

            ArenaModuleManager
                    .announce(
                            arena,
                            Language.parse(MSG.PLAYER_HAS_WON,
                                    winner.getName()),
                            "WINNER");
            arena.broadcast(Language.parse(MSG.PLAYER_HAS_WON,
                    winner.getName()));
        }

        this.getPlayerLifeMap().clear();
        new EndRunnable(arena, arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // 0 = checkpoint , [1 = number]

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        ArenaPlayer ap = ArenaPlayer.fromPlayer((Player) sender);
        int cpLives = this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_LIVES);

        if (args.length < 2 && this.arena.getFighters().contains(ap)) {
            ap.setTelePass(true);
            int value = cpLives - this.getPlayerLifeMap().get(ap.getPlayer());
            if (value == 0) {
                ap.getPlayer().teleport(SpawnManager.getSpawnByExactName(this.arena, SPAWN).toLocation());
            } else {
                ap.getPlayer().teleport(SpawnManager.getSpawnByExactName(this.arena, CHECKPOINT + value).toLocation());
            }
            ap.setTelePass(false);
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
            return;
        }
        int value;
        try {
            value = Integer.parseInt(args[1]);
        } catch (Exception e) {
            this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[1]);
            return;
        }
        Player player = (Player) sender;
        String spawnName = CHECKPOINT + value;
        if (value > 0 && value <= cpLives) {
            this.arena.setSpawn(spawnName, new PALocation(player.getLocation()), null, null);
            this.arena.msg(sender, MSG.SPAWN_SET, spawnName);
        } else {
            this.arena.msg(sender, MSG.SPAWN_UNKNOWN, spawnName);
        }
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[CP] already ending");
            return;
        }
        debug(this.arena, "[CP]");

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);

        ArenaPlayer arenaPlayer = null;

        for (ArenaPlayer aPlayer : this.arena.getFighters()) {
            if (aPlayer.getStatus() == PlayerStatus.FIGHT) {
                arenaPlayer = aPlayer;
                break;
            }
        }

        if (arenaPlayer != null && !force) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()), "END");

            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()), "WINNER");
            this.arena.broadcast(Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()));
        }

        if (arenaPlayer != null && ArenaModuleManager.commitEnd(this.arena, arenaPlayer.getArenaTeam())) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("needed points: " +
                this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_LIVES));
        sender.sendMessage("claim range: " +
                this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_CLAIMRANGE));
        sender.sendMessage("tick interval (ticks): " +
                this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_TICKINTERVAL));
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String spawnTeamName) {
        boolean hasSpawn = super.hasSpawn(spawnName, spawnTeamName);

        return hasSpawn || spawnName.startsWith(CHECKPOINT);
    }

    @Override
    public void initiate(final Player player) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        if (!this.getPlayerLifeMap().containsKey(arenaPlayer.getPlayer())) {
            this.getPlayerLifeMap().put(arenaPlayer.getPlayer(), this.arena.getConfig()
                    .getInt(CFG.GOAL_CHECKPOINTS_LIVES));
        }
    }

    @Override
    public void lateJoin(final Player player) {
        this.initiate(player);
    }

    @Override
    public void parseStart() {
        this.getPlayerLifeMap().clear();
        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            debug(this.arena, "adding player " + arenaPlayer.getName());
            this.getPlayerLifeMap().put(arenaPlayer.getPlayer(),
                    this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_LIVES, 3));
        }

        final CheckPointsMainRunnable cpMainRunner = new CheckPointsMainRunnable(this.arena, this);
        final int tickInterval = this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_TICKINTERVAL);
        cpMainRunner.runTaskTimer(PVPArena.getInstance(), tickInterval, tickInterval);
    }

    private void reduceLivesCheckEndAndCommit(final Arena arena, final ArenaPlayer arenaPlayer) {

        debug(arena, "reducing lives of player " + arenaPlayer);
        if (this.getPlayerLifeMap().get(arenaPlayer.getPlayer()) != null) {
            final int iLives = this.getPlayerLifeMap().get(arenaPlayer.getPlayer()) - 1;
            if (iLives > 0) {
                this.getPlayerLifeMap().put(arenaPlayer.getPlayer(), iLives);
            } else {
                this.getPlayerLifeMap().remove(arenaPlayer.getPlayer());
                this.commitWin(arena, arenaPlayer);
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getPlayerLifeMap().clear();
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.arena.getConfig().getInt(CFG.GOAL_CHECKPOINTS_LIVES)
                    - (this.getPlayerLifeMap()
                    .getOrDefault(arenaPlayer.getPlayer(), 0));
            if (scores.containsKey(arenaPlayer.getName())) {
                scores.put(arenaPlayer.getName(), scores.get(arenaPlayer.getName()) + score);
            } else {
                scores.put(arenaPlayer.getName(), score);
            }
        }
        return scores;
    }

    private static class CheckPointsMainRunnable extends BukkitRunnable {
        private final Arena arena;
        private final GoalCheckPoints goal;

        public CheckPointsMainRunnable(final Arena arena, final GoalCheckPoints goal) {
            this.arena = arena;
            this.goal = goal;
            debug(arena, "CheckPointsMainRunnable constructor");
        }

        /**
         * the run method, commit arena end
         */
        @Override
        public void run() {
            if (!this.arena.isFightInProgress() || this.arena.realEndRunner != null) {
                this.cancel();
            }
            this.goal.checkMove();
        }
    }
}
