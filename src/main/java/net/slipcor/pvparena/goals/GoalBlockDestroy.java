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
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.classes.PABlock.PA_BLOCK_TEAM_FORMAT;
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

    private static final String BLOCK_TYPE = "blocktype";
    private static final String BLOCK = "block";

    public GoalBlockDestroy() {
        super("BlockDestroy");
    }

    private String blockName;
    private String blockTeamName;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public boolean checkCommand(final String string) {
        if (BLOCK_TYPE.equalsIgnoreCase(string) || BLOCK.equalsIgnoreCase(string)) {
            return true;
        }

        return this.arena.getTeams().stream()
                .anyMatch(team -> string.contains(String.format(PA_BLOCK_TEAM_FORMAT, team.getName(), BLOCK)));
    }

    @Override
    public List<String> getGoalCommands() {
        List<String> result = new ArrayList<>();
        if (this.arena != null) {
            result.add(BLOCK_TYPE);
            this.arena.getTeams()
                    .forEach(team -> result.add(String.format(PA_BLOCK_TEAM_FORMAT, team.getName(), BLOCK)));
        }
        return result;
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Material}"});
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

        if (StringUtils.isBlank(this.blockName) || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }

        if (block == null || block.getType() != this.arena.getConfig().getMaterial(CFG.GOAL_BLOCKDESTROY_BLOCKTYPE)) {
            return false;
        }

        return PVPArena.hasAdminPerms(player) || PVPArena.hasCreatePerms(player, this.arena);
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
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (BLOCK_TYPE.equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "2");
                return;
            }

            final Material mat = Material.getMaterial(args[1].toUpperCase());

            if (mat == null) {
                this.arena.msg(sender, MSG.ERROR_MAT_NOT_FOUND, args[1]);
                return;
            }

            this.arena.getConfig().set(CFG.GOAL_BLOCKDESTROY_BLOCKTYPE,
                    mat.name());
            this.arena.getConfig().save();
            this.arena.msg(sender, MSG.GOAL_BLOCKDESTROY_TYPESET, mat.name());

        } else if (args[0].contains(BLOCK)) {
            commitSetBlockCommand(sender, args);
        }
    }

    private void commitSetBlockCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "1");
            return;
        }

        String[] spawnArgs = args[0].split("_");
        if (spawnArgs.length < 2) {
            this.arena.msg(sender, MSG.ERROR_SPAWN_UNKNOWN, args[0]);
            return;
        }
        this.blockName = spawnArgs[1];
        String teamName = spawnArgs[0];
        final ArenaTeam arenaTeam = this.arena.getTeam(teamName);
        if (arenaTeam == null) {
            this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, this.blockTeamName);
            return;
        }
        this.blockTeamName = teamName;

        PAA_Region.activeSelections.put(sender.getName(), this.arena);

        this.arena.msg(sender, MSG.GOAL_BLOCKDESTROY_TOSET, this.blockName, arenaTeam.getColoredName());
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
    public boolean commitSetBlock(final Player player, final Block block) {

        debug(this.arena, player, "trying to set a block");

        // command : /pa red_block1
        // location: red block1:

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), this.blockName, this.blockTeamName);
        this.arena.msg(player, MSG.GOAL_BLOCKDESTROY_SET, this.blockName, this.arena.getTeam(this.blockTeamName).getColoredName());

        PAA_Region.activeSelections.remove(player.getName());
        this.blockName = null;

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
        sender.sendMessage("block type: " +
                this.arena.getConfig().getString(CFG.GOAL_BLOCKDESTROY_BLOCKTYPE));
        sender.sendMessage("lives: " +
                this.arena.getConfig().getInt(CFG.GOAL_BLOCKDESTROY_LIVES));
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
    public void initiate(final Player player) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
            this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(), this.arena.getConfig()
                    .getInt(CFG.GOAL_BLOCKDESTROY_LIVES));

            final Set<PABlockLocation> blocks = SpawnManager.getBlocksContaining(this.arena, BLOCK);

            for (PABlockLocation block : blocks) {
                this.takeBlock(arenaTeam.getColor(), block);
            }
        }
    }

    @Override
    public void parseStart() {
        this.getTeamLifeMap().clear();
        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            if (!arenaTeam.getTeamMembers().isEmpty()) {
                debug(this.arena, "adding team " + arenaTeam.getName());
                // team is active
                this.getTeamLifeMap().put(
                        arenaTeam,
                        this.arena.getConfig().getInt(
                                CFG.GOAL_BLOCKDESTROY_LIVES, 1));
            }
            final Set<PABlockLocation> blocks = SpawnManager.getBlocksContaining(this.arena, BLOCK);

            for (PABlockLocation block : blocks) {
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
        Material blockDestroyType = Material.valueOf(this.arena.getConfig().getString(CFG.GOAL_BLOCKDESTROY_BLOCKTYPE));
        if (ColorUtils.isColorableMaterial(blockDestroyType)) {
            paBlockLocation.toLocation()
                    .getBlock()
                    .setType(ColorUtils.getColoredMaterialFromChatColor(blockColor, blockDestroyType));
        } else {
            paBlockLocation.toLocation()
                    .getBlock()
                    .setType(
                            Material.valueOf(
                                    this.arena.getConfig().getString(
                                            CFG.GOAL_BLOCKDESTROY_BLOCKTYPE)));
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
        final Material blockToBreak = this.arena.getConfig().getMaterial(CFG.GOAL_BLOCKDESTROY_BLOCKTYPE);
        final Material brokenBlock = event.getBlock().getType();
        if (!this.arena.hasPlayer(event.getPlayer()) || !ColorUtils.isSubType(brokenBlock, blockToBreak)) {
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
            if (!SpawnManager.getBlocksStartingWith(this.arena, BLOCK, arenaTeam.getName()).isEmpty()) {
                vBlock = SpawnManager
                        .getBlockNearest(
                                SpawnManager.getBlocksStartingWith(this.arena, BLOCK, arenaTeam.getName()),
                                new PABlockLocation(player.getLocation()))
                        .toLocation().toVector();
            }
            if (vBlock != null && vLoc.distance(vBlock) < 2) {

                // ///////

                if (arenaTeam.equals(pTeam)) {
                    debug(this.arena, player, "is own team! cancel and OUT! ");
                    event.setCancelled(true);
                    break;
                }
                PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
                final String sTeam = pTeam.getName();

                try {
                    this.arena.broadcast(Language.parse(MSG.GOAL_BLOCKDESTROY_SCORE,
                            this.arena.getTeam(sTeam).colorizePlayer(player)
                                    + ChatColor.YELLOW, arenaTeam.getColoredName()
                                    + ChatColor.YELLOW, String
                                    .valueOf(this.getTeamLifeMap().get(arenaTeam) - 1)));
                } catch (final Exception e) {
                    Bukkit.getLogger().severe(
                            "[PVP Arena] team unknown/no lives: " + arenaTeam);
                    e.printStackTrace();
                }


                gEvent = new PAGoalEvent(this.arena, this,
                        "score:" + player.getName() + ':' + arenaPlayer.getArenaTeam().getName() + ":1");
                Bukkit.getPluginManager().callEvent(gEvent);
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
                        && this.getTeamLifeMap().get(arenaTeam) > SpawnManager.getBlocksStartingWith(this.arena, BLOCK, arenaTeam.getName()).size()) {

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

        final Set<PABlock> blocks = SpawnManager.getPABlocksContaining(this.arena, BLOCK);

        //final Set<PABlockLocation>

        for (Block b : event.blockList()) {
            final PABlockLocation loc = new PABlockLocation(b.getLocation());
            for (PABlock paBlock : blocks) {
                if (paBlock.getLocation().getDistanceSquared(loc) < 1) {
                    final ArenaTeam blockTeam = this.arena
                            .getTeam(paBlock.getName().split(BLOCK)[0]);

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

                    reduceLivesCheckEndAndCommit(this.arena, blockTeam);
                    break;
                }
            }
        }
    }
}
