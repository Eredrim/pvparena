package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "BlockDestroy"
 * </pre>
 * <p/>
 * Win by breaking the other team's block(s).
 *
 * @author slipcor
 */

public class GoalBlockDestroy extends AbstractBlockLocationGoal {

    private static final String BLOCK = "block";

    public GoalBlockDestroy() {
        super("BlockDestroy");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    protected String getLocationBlockName() {
        return BLOCK;
    }

    @Override
    protected boolean isRegionBattleNeeded() {
        return true;
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

    private void commit(final Arena arena, final ArenaTeam arenaTeam) {
        debug(arena, "[BD] checking end: " + arenaTeam);
        debug(arena, "win: " + false);

        for (ArenaTeam currentArenaTeam : arena.getTeams()) {
            if (!currentArenaTeam.equals(arenaTeam)) {
                /*
				team is sTeam and win
				team is not sTeam and not win
				*/
                continue;
            }
            for (ArenaPlayer ap : currentArenaTeam.getTeamMembers()) {
                if (ap.getStatus() == PlayerStatus.FIGHT || ap.getStatus() == PlayerStatus.DEAD) {
                    ap.addLosses();
                    ap.setStatus(PlayerStatus.LOST);
                }
            }
        }
        WorkflowManager.handleEnd(arena, false);
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[BD] already ending");
            return;
        }
        debug(this.arena, "[BD]");

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
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
        }

        if (ArenaModuleManager.commitEnd(this.arena, aTeam)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitStart() {
        // implement to not throw exception
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: " +
                this.arena.getConfig().getInt(CFG.GOAL_BLOCKDESTROY_LIVES));
    }

    @Override
    public void initiate(final Player player) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
            this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(), this.arena.getConfig()
                    .getInt(CFG.GOAL_BLOCKDESTROY_LIVES));

            final Set<PABlockLocation> blocks = SpawnManager.getPABlockLocationsStartingWith(this.arena, BLOCK);

            for (PABlockLocation block : blocks) {
                this.resetBlock(block, arenaTeam);
            }
        }
    }

    @Override
    public void parseStart() {
        super.parseStart();
        this.getTeamLifeMap().clear();
        for (ArenaTeam arenaTeam : this.arena.getNotEmptyTeams()) {
            debug(this.arena, "adding team " + arenaTeam.getName());
            this.getTeamLifeMap().put(
                    arenaTeam,
                    this.arena.getConfig().getInt(
                            CFG.GOAL_BLOCKDESTROY_LIVES, 1));
            SpawnManager.getBlocksStartingWith(this.arena, BLOCK, arenaTeam.getName())
                    .forEach(block -> this.resetBlock(block, arenaTeam));

        }
    }

    private void reduceLivesCheckEndAndCommit(final Arena arena, final ArenaTeam team) {

        debug(arena, "reducing lives of team " + team);
        if (!this.getTeamLifeMap().containsKey(team)) {
            return;
        }
        final int count = this.getTeamLifeMap().get(team) - 1;
        if (count > 0) {
            this.getTeamLifeMap().put(team, count);
        } else {
            this.getTeamLifeMap().remove(team);
            this.commit(arena, team);
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getTeamLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (config.get("teams") == null) {
            debug(this.arena, "no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            double score = this.getTeamLifeMap().getOrDefault(arenaTeam, 0);
            if (scores.containsKey(arenaTeam.getName())) {
                scores.put(arenaTeam.getName(), scores.get(arenaTeam.getName()) + score);
            } else {
                scores.put(arenaTeam.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final Player player) {
        this.disconnect(ArenaPlayer.fromPlayer(player));
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.fromPlayer(player));
        }
    }

    @Override
    public void checkBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (!this.arena.hasPlayer(event.getPlayer())) {
            debug(this.arena, player, "block destroy, ignoring");
            debug(this.arena, player, String.valueOf(this.arena.hasPlayer(event.getPlayer())));
            debug(this.arena, player, event.getBlock().getType().name());
            return;
        }
        if (!this.arena.isFightInProgress()) {
            event.setCancelled(true);
            return;
        }
        if (breakBlock(event.getBlock(), player)) {
            event.setCancelled(true);
        }

    }

    /**
     * return true if block has been handled
     */
    private boolean breakBlock(Block block, Player player) {

        debug(this.arena, "block destroy!");

        ArenaPlayer arenaPlayer = null;
        ArenaTeam arenaTeam = null;
        if (player != null) {
            arenaPlayer = ArenaPlayer.fromPlayer(player);
            arenaTeam = arenaPlayer.getArenaTeam();
        }

        if (this.teamsBlockLocations.keySet().stream()
                .noneMatch(blockLocation -> block.getLocation().equals(blockLocation.toLocation()))
        ) {
            debug(this.arena, "block, but not flag location");
            return false;
        }

        final PABlockLocation blockLocation = new PABlockLocation(block.getLocation());
        final ArenaTeam arenaTeamBlock = this.teamsBlockLocations.get(blockLocation);

        if (arenaTeamBlock == null) {
            debug(this.arena, "It's not a team block. Valid are: {}",
                    this.teamsBlockLocations.entrySet().toString());
            return false;
        }

        if (arenaTeam != null && arenaTeam.equals(arenaTeamBlock)) {
            debug(this.arena, "can't break his own block");
            return true;
        }

        // dont check for inactive teams
        if (arenaTeam != null && arenaTeamBlock.isEmpty()) {
            debug(this.arena, player, "can't break empty team's block");
            return true;
        }

        // destroy all team's block
        applyToAllBlocksOfTeam(arenaTeamBlock, Material.AIR, null);

        if (arenaTeam != null) {

            PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);

            gEvent = new PAGoalEvent(this.arena, this,
                    "score:" + player.getName() + ':' + arenaPlayer.getArenaTeam().getName() + ":1");
            Bukkit.getPluginManager().callEvent(gEvent);

            this.arena.broadcast(Language.parse(MSG.GOAL_BLOCKDESTROY_SCORE,
                    arenaTeam.colorizePlayer(player)
                            + ChatColor.YELLOW, arenaTeamBlock.getColoredName()
                            + ChatColor.YELLOW, String
                            .valueOf(this.getTeamLifeMap().get(arenaTeamBlock) - 1)));
        } else {
            this.arena.broadcast(Language.parse(MSG.GOAL_BLOCKDESTROY_SCORE,
                    Language.parse(MSG.DEATHCAUSE_BLOCK_EXPLOSION)
                            + ChatColor.YELLOW, arenaTeamBlock.getColoredName()
                            + ChatColor.YELLOW, String
                            .valueOf(this.getTeamLifeMap().get(arenaTeamBlock) - 1)));
        }

        class RunLater implements Runnable {

            private final ArenaTeam arenaTeamBlock;
            private final PABlockLocation blockLocation;

            public RunLater(ArenaTeam arenaTeamBlock, PABlockLocation blockLocation) {
                this.arenaTeamBlock = arenaTeamBlock;
                this.blockLocation = blockLocation;
            }

            @Override
            public void run() {
                GoalBlockDestroy.this.resetBlock(this.blockLocation, this.arenaTeamBlock);
            }
        }

        if (this.getTeamLifeMap().containsKey(arenaTeamBlock)
                && this.getTeamLifeMap().get(arenaTeamBlock) >
                SpawnManager.getBlocksStartingWith(this.arena, BLOCK, arenaTeamBlock.getName()).size()) {

            Bukkit.getScheduler().runTaskLater(
                    PVPArena.getInstance(),
                    new RunLater(arenaTeamBlock, blockLocation), 5L);
        }
        this.reduceLivesCheckEndAndCommit(this.arena, arenaTeamBlock);
        return true;
    }

    private void resetBlock(PABlockLocation paBlockLocation, ArenaTeam arenaTeam) {
        if (paBlockLocation == null) {
            return;
        }

        Block block = paBlockLocation.toLocation().getBlock();
        Material blockType = paBlockLocation.getBlockData().getMaterial();

        debug("Restore block with {}", paBlockLocation.getBlockData().getAsString());
        applyToAllBlocksOfTeam(arenaTeam, blockType, paBlockLocation.getBlockData());

        if (ColorUtils.isColorableMaterial(blockType)) {
            debug("color actual block {} in {}", block, arenaTeam.getColor());
            applyToAllBlocksOfTeam(arenaTeam, arenaTeam.getColor());
        }
    }

    @Override
    public void checkExplode(EntityExplodeEvent event) {
        if (this.arena == null) {
            return;
        }

        boolean contains = false;

        for (ArenaRegion region : this.arena.getRegionsByType(RegionType.BATTLE)) {
            if (region.getShape().contains(new PABlockLocation(event.getLocation()))) {
                contains = true;
                break;
            }
        }

        if (!contains) {
            return;
        }

        // explosion can destroy one or more team's block !
        event.blockList().stream()
                .map(block -> new PABlockLocation(block.getLocation()))
                .filter(block -> SpawnManager.getPABlockLocationsStartingWith(this.arena, BLOCK).contains(block))
                .forEach(blockLocation -> breakBlock(blockLocation.toLocation().getBlock(), null));
    }
}
