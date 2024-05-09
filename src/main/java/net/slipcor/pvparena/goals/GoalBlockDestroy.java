package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.goal.PAGoalEndEvent;
import net.slipcor.pvparena.events.goal.PAGoalScoreEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.PermissionManager;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

public class GoalBlockDestroy extends ArenaGoal {
    private static final String BLOCK = "block";

    public GoalBlockDestroy() {
        super("BlockDestroy");
    }

    private String blockTeamName;
    private Map<PABlockLocation, BlockData> blockDataMap;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH);
    }

    @Override
    public boolean checkCommand(final String string) {
        return BLOCK.equalsIgnoreCase(string);
    }

    @Override
    public List<String> getGoalCommands() {
        List<String> result = new ArrayList<>();
        if (this.arena != null) {
            result.add(BLOCK);
        }
        return result;
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        arena.getTeamNames().forEach(teamName -> result.define(new String[]{"set", teamName}));
        arena.getBlocks().forEach(paBlock -> result.define(new String[]{"remove", paBlock.getTeamName()}));
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
        return SpawnManager.getMissingBlocksTeamCustom(this.arena, blocks, BLOCK);
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (StringUtils.isBlank(this.blockTeamName) || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }

        if (block == null) {
            return false;
        }

        return PermissionManager.hasAdminPerm(player) || PermissionManager.hasBuilderPerm(player, this.arena);
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
                    ap.getStats().incLosses();
                    ap.setStatus(PlayerStatus.LOST);
                }
            }
        }
        WorkflowManager.handleEnd(arena, false);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].equalsIgnoreCase(BLOCK)) {
            if(args.length != 3) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3");
            } else {
                if("set".equalsIgnoreCase(args[1])) {
                    this.commitSetBlockCommand(sender, args[2]);
                } else if ("remove".equalsIgnoreCase(args[1])) {
                    this.commitRemoveBlockCommand(sender, args[2]);
                }
            }
        }
    }

    private void commitSetBlockCommand(CommandSender sender, String teamName) {
        final ArenaTeam arenaTeam = this.arena.getTeam(teamName);
        if (arenaTeam == null) {
            this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, teamName);
            return;
        }
        this.blockTeamName = teamName;

        PAA_Region.activeSelections.put(sender.getName(), this.arena);

        this.arena.msg(sender, MSG.GOAL_BLOCKDESTROY_TOSET, arenaTeam.getColoredName());
    }

    private void commitRemoveBlockCommand(CommandSender sender, String teamName) {
        final ArenaTeam arenaTeam = this.arena.getTeam(teamName);
        if (arenaTeam == null) {
            this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, teamName);
            return;
        }

        String blockName = String.join("_", teamName, BLOCK);
        Optional<PABlock> paBlock = this.arena.getBlocks().stream()
                .filter(block -> block.getFullName().equalsIgnoreCase(blockName))
                .findAny();

        if(!paBlock.isPresent()) {
            this.arena.msg(sender, MSG.GOAL_BLOCKDESTROY_NOTFOUND, teamName);
            return;
        }
        SpawnManager.removeBlock(this.arena, paBlock.get());
        this.arena.msg(sender, MSG.GOAL_BLOCKDESTROY_REMOVED, teamName, arenaTeam.getColoredName());
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[BD] already ending");
            return;
        }
        debug(this.arena, "[BD]");

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

        debug(this.arena, player, "trying to set a block");

        // command : /pa red_block1
        // location: red block1:

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), BLOCK, this.blockTeamName);
        this.arena.msg(player, MSG.GOAL_BLOCKDESTROY_SET, this.arena.getTeam(this.blockTeamName).getColoredName());

        PAA_Region.activeSelections.remove(player.getName());
        this.blockTeamName = null;

        return true;
    }

    @Override
    public void commitStart() {
        // implement to not throw exception
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: " +
                this.arena.getConfig().getInt(CFG.GOAL_BLOCKDESTROY_LIVES));
        sender.sendMessage("auto color: " +
                this.arena.getConfig().getBoolean(CFG.GOAL_BLOCKDESTROY_AUTOCOLOR));
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String spawnTeamName) {
        boolean hasSpawn = super.hasSpawn(spawnName, spawnTeamName);
        if (hasSpawn) {
            return true;
        }

        for (String teamName : this.arena.getTeamNames()) {
            if (spawnName.equalsIgnoreCase(BLOCK) && spawnTeamName.equalsIgnoreCase(teamName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void parseStart() {
        this.getTeamLifeMap().clear();
        this.blockDataMap = new HashMap<>();

        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            if (!arenaTeam.getTeamMembers().isEmpty()) {
                debug(this.arena, "adding team " + arenaTeam.getName());
                // team is active
                this.getTeamLifeMap().put(arenaTeam, this.arena.getConfig().getInt(CFG.GOAL_BLOCKDESTROY_LIVES, 1));
            }

            final Set<PABlockLocation> blocks = SpawnManager.getBlocksStartingWith(this.arena, BLOCK, arenaTeam.getName());

            for (PABlockLocation block : blocks) {
                this.blockDataMap.put(block, block.toLocation().getBlock().getBlockData().clone());
                this.takeBlock(arenaTeam.getColor(), block);
            }
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

        if(this.blockDataMap != null) {
            this.blockDataMap.forEach((paBlockLoc, blockData) ->
                    paBlockLoc.toLocation().getBlock().setBlockData(blockData)
            );
            this.blockDataMap.clear();
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
     * take/reset an arena block
     *
     * @param blockColor      the teamcolor to reset
     * @param paBlockLocation the location to take/reset
     */
    void takeBlock(final ChatColor blockColor, final PABlockLocation paBlockLocation) {
        if (paBlockLocation == null) {
            return;
        }

        Material blockDestroyType = this.blockDataMap.get(paBlockLocation).getMaterial();
        boolean shouldSetTeamColor = this.arena.getConfig().getBoolean(CFG.GOAL_BLOCKDESTROY_AUTOCOLOR);
        if (shouldSetTeamColor && ColorUtils.isColorableMaterial(blockDestroyType)) {
            paBlockLocation.toLocation()
                    .getBlock()
                    .setType(ColorUtils.getColoredMaterialFromChatColor(blockColor, blockDestroyType));
        } else {
            paBlockLocation.toLocation()
                    .getBlock()
                    .setBlockData(this.blockDataMap.get(paBlockLocation));
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
    public void unload(final ArenaPlayer arenaPlayer) {
        this.disconnect(arenaPlayer);
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(arenaPlayer);
        }
    }

    @Override
    public void checkBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final PABlockLocation blockLoc = new PABlockLocation(event.getBlock().getLocation());
        if (!this.arena.hasPlayer(event.getPlayer()) || !this.blockDataMap.containsKey(blockLoc)) {
            debug(this.arena, player, "block destroy, ignoring");
            debug(this.arena, player, String.valueOf(this.arena.hasPlayer(event.getPlayer())));
            debug(this.arena, player, event.getBlock().getType().name());
            return;
        }

        if (!this.arena.isFightInProgress()) {
            event.setCancelled(true);
            return;
        }

        final Block block = event.getBlock();

        debug(this.arena, player, "block destroy!");

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        final ArenaTeam pTeam = arenaPlayer.getArenaTeam();
        if (pTeam == null) {
            return;
        }

        Vector vBlock = null;
        for (ArenaTeam arenaTeam : this.arena.getTeams()) {

            if (arenaTeam.isEmpty() && !"touchdown".equals(arenaTeam.getName())) {
                debug(this.arena, player, "size!OUT! ");
                continue; // dont check for inactive teams
            }

            debug(this.arena, player, "checking for block of team " + arenaTeam);
            Vector vLoc = block.getLocation().toVector();
            debug(this.arena, player, "block: " + vLoc);
            Set<PABlockLocation> teamBlockSet = SpawnManager.getBlocksStartingWith(this.arena, BLOCK, arenaTeam.getName());

            if (!teamBlockSet.isEmpty()) {
                vBlock = SpawnManager.getBlockNearest(teamBlockSet, blockLoc).toLocation().toVector();
            }

            if (vBlock != null && vLoc.distance(vBlock) < 1) {
                if (arenaTeam.equals(pTeam)) {
                    debug(this.arena, player, "is own team! cancel and OUT! ");
                    event.setCancelled(true);
                    break;
                }
                final String sTeam = pTeam.getName();
                try {
                    this.arena.broadcast(Language.parse(MSG.GOAL_BLOCKDESTROY_SCORE,
                            this.arena.getTeam(sTeam).colorizePlayer(arenaPlayer)
                                    + ChatColor.YELLOW, arenaTeam.getColoredName()
                                    + ChatColor.YELLOW, String
                                    .valueOf(this.getTeamLifeMap().get(arenaTeam) - 1)));
                } catch (final Exception e) {
                    Bukkit.getLogger().severe(
                            "[PVP Arena] team unknown/no lives: " + arenaTeam);
                    e.printStackTrace();
                }


                PAGoalScoreEvent goalScoreEvent = new PAGoalScoreEvent(this.arena, this,
                        arenaPlayer, arenaPlayer.getArenaTeam(), 1L);
                Bukkit.getPluginManager().callEvent(goalScoreEvent);
                class RunLater implements Runnable {
                    final ChatColor localColor;
                    final PABlockLocation localLoc;

                    RunLater(final ChatColor color, final PABlockLocation loc) {
                        this.localColor = color;
                        this.localLoc = loc;
                    }

                    @Override
                    public void run() {
                        GoalBlockDestroy.this.takeBlock(this.localColor, this.localLoc);
                    }
                }

                if (this.getTeamLifeMap().containsKey(arenaTeam)
                        && this.getTeamLifeMap().get(arenaTeam) > teamBlockSet.size()) {

                    Bukkit.getScheduler().runTaskLater(
                            PVPArena.getInstance(),
                            new RunLater(
                                    arenaTeam.getColor(),
                                    new PABlockLocation(event.getBlock().getLocation())), 5L);
                }
                this.reduceLivesCheckEndAndCommit(this.arena, arenaTeam);

                return;
            }
        }
    }

    @Override
    public void checkExplode(EntityExplodeEvent event) {
        if (this.arena == null || event.getEntityType() != EntityType.PRIMED_TNT) {
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

        final Set<PABlock> blocks = SpawnManager.getPABlocksContaining(this.arena, BLOCK);

        //final Set<PABlockLocation>

        for (Block b : event.blockList()) {
            final PABlockLocation loc = new PABlockLocation(b.getLocation());
            for (PABlock paBlock : blocks) {
                if (paBlock.getLocation().getDistanceSquared(loc) < 1) {
                    final ArenaTeam blockTeam = this.arena.getTeam(paBlock.getTeamName());

                    try {
                        this.arena.broadcast(Language.parse(MSG.GOAL_BLOCKDESTROY_SCORE,
                                Language.parse(MSG.DEATHCAUSE_BLOCK_EXPLOSION)
                                        + ChatColor.YELLOW, blockTeam.getColoredName()
                                        + ChatColor.YELLOW, String
                                        .valueOf(this.getTeamLifeMap().get(blockTeam) - 1)));
                    } catch (final Exception e) {
                        Bukkit.getLogger().severe(
                                "[PVP Arena] team unknown/no lives: " + blockTeam);
                        e.printStackTrace();
                    }
                    this.takeBlock(blockTeam.getColor(), paBlock.getLocation());

                    this.reduceLivesCheckEndAndCommit(this.arena, blockTeam);
                    break;
                }
            }
        }
    }
}
