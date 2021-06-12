package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.regions.RegionType;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * Abstract all goal with location involved like capture the flag, block destroy, sabotage etc.
 */
public abstract class AbstractBlockLocationGoal extends ArenaGoal {

    // use to setup a block via a command in game
    protected String setupBlockName;
    protected String setupBlockTeamName = null;

    protected Map<PABlockLocation, ArenaTeam> teamsBlockLocations = new HashMap<>();

    /**
     * create an arena type instance
     *
     * @param goalName the arena type name
     */
    protected AbstractBlockLocationGoal(String goalName) {
        super(goalName);
    }

    /**
     * Return the location block name.
     * <p>
     * Examples: tnt, flag, block ...
     *
     * @return string location name
     */
    protected abstract String getLocationBlockName();

    // some goal need a BATTLE region setup to handle blocks destroy/explosion
    protected boolean isRegionBattleNeeded() {
        return false;
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(Config.CFG.PERMS_JOIN_IN_BATTLE);
    }

    @Override
    public void commitStart() {
        // empty to kill the error ;)
    }

    @Override
    public void parseStart() {

        debug(this.arena, "[{}] parsing start", this.name);

        debug(this.arena, "Teams: {}", String.join(",",
                this.arena.getTeams().stream().map(ArenaTeam::getName).collect(Collectors.toSet())));

        debug(this.arena, "Flags: {}", String.join(",",
                this.arena.getBlocks().stream().map(PABlock::getName).collect(Collectors.toSet())));

        this.teamsBlockLocations = this.arena.getBlocks().stream()
                .filter(paBlock -> paBlock.getName().startsWith(getLocationBlockName()))
                // check if team exist (prevent NPE on collector)
                .filter(paBlock -> this.arena.getTeam(paBlock.getTeamName()) != null)
                // save block data (orientation, face, etc. to restore them later)
                .peek(paBlock -> paBlock.getLocation().setBlockData(paBlock.getLocation().toLocation().getBlock().getBlockData()))
                .collect(Collectors.toMap(PABlock::getLocation,
                        paBlock -> this.arena.getTeam(paBlock.getTeamName())));
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
    public boolean checkCommand(final String command) {
        return command.startsWith(getLocationBlockName());
    }

    @Override
    public List<String> getGoalCommands() {
        return Collections.singletonList(getLocationBlockName());
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(this.arena.getTeams()
                .stream().map(ArenaTeam::getName).toArray(String[]::new));
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            this.arena.msg(
                    sender,
                    Language.MSG.ERROR_INVALID_ARGUMENT_COUNT,
                    String.valueOf(args.length), "2");
            return;
        }

        ArenaTeam arenaTeam = this.arena.getTeam(args[1]);
        if (arenaTeam == null) {
            this.arena.msg(
                    sender,
                    Language.MSG.ERROR_TEAM_NOT_FOUND,
                    args[1]);
            return;
        }

        this.setupBlockName = args[0];
        this.setupBlockTeamName = args[1];
        PAA_Region.activeSelections.put(sender.getName(), this.arena);

        this.arena.msg(sender,
                Language.MSG.GOAL_FLAGS_TOSET, this.setupBlockName);
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        if (this.setupBlockName == null || this.setupBlockTeamName == null) {
            return false;
        }

        debug(this.arena, player, "trying to set a block " + this.setupBlockName + " " + this.setupBlockTeamName);

        if(isRegionBattleNeeded()) {
            // region BATTLE required to handle TNT explosions
            new PABlockLocation(block.getLocation());
            if (this.arena.getRegionsByType(RegionType.BATTLE).stream()
                    .noneMatch(arenaRegion -> arenaRegion.containsLocation(new PALocation(block.getLocation())))) {
                this.arena.msg(player, Language.MSG.ERROR_GOAL_REQUIRE_REGION_BATTLE);
                return true;
            }
        }

        final ArenaTeam arenaTeam = this.arena.getTeam(this.setupBlockTeamName);
        if (arenaTeam == null) {
            this.arena.msg(player, Language.MSG.ERROR_TEAM_NOT_FOUND, this.setupBlockTeamName);
            return true;
        }

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), this.setupBlockName, this.setupBlockTeamName);
        this.arena.msg(player, Language.MSG.GOAL_FLAGS_SET, this.setupBlockName + " " + arenaTeam.getColoredName());

        PAA_Region.activeSelections.remove(player.getName());
        this.setupBlockName = null;
        this.setupBlockTeamName = null;

        return true;
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String spawnTeamName) {
        final boolean hasSpawn = super.hasSpawn(spawnName, spawnTeamName);
        if (hasSpawn) {
            return true;
        }

        return this.arena.getTeamNames().stream()
                .anyMatch(teamName -> spawnName.startsWith(getLocationBlockName())
                        && spawnTeamName.equalsIgnoreCase(teamName));
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(final Set<PASpawn> spawnsNames) {
        return SpawnManager.getMissingTeamSpawn(this.arena, spawnsNames);
    }

    @Override
    public Set<PABlock> checkForMissingBlocks(final Set<PABlock> blocks) {
        return SpawnManager.getMissingBlocksTeamCustom(this.arena, blocks, getLocationBlockName());
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (!PAA_Region.activeSelections.containsKey(player.getName())) {
            debug(this.arena, player, "player not in active selection");
            return false;
        }

        return PVPArena.hasAdminPerms(player) || PVPArena.hasCreatePerms(player, this.arena);
    }

    protected Set<PABlockLocation> getTeamBlockLocations(ArenaTeam arenaTeam) {
        return this.teamsBlockLocations.entrySet().stream()
                .filter(teamEntry -> arenaTeam.equals(teamEntry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    protected void computeScore(final Arena arena, final ArenaTeam arenaTeam, final ArenaTeam arenaTeamBlock) {
        debug(arena, "reducing lives of team " + arenaTeam);
        if (this.getTeamLifeMap().get(arenaTeam) != null) {
            final int iLives = this.getTeamLifeMap().get(arenaTeam) - 1;
            if (iLives > 0) {
                this.getTeamLifeMap().put(arenaTeam, iLives);
            } else {
                this.getTeamLifeMap().remove(arenaTeam);
                this.commit(arena, arenaTeam, false);
            }
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

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaTeam team : this.arena.getTeams()) {
            double score = this.getTeamLifeMap().getOrDefault(team, 0);
            if (scores.containsKey(team.getName())) {
                scores.put(team.getName(), scores.get(team.getName()) + score);
            } else {
                scores.put(team.getName(), score);
            }
        }
        return scores;
    }

    protected void commit(final Arena arena, final ArenaTeam arenaTeam, final boolean win) {
        if (arena.realEndRunner != null) {
            debug(arena, "already ending");
        }

        debug(arena, "committing end: " + arenaTeam);
        debug(arena, "win: " + win);

        ArenaTeam winner = null;
        if (win) {
            // force winner
            winner = arenaTeam;
        } else {
            // Update losing team status
            arenaTeam.getTeamMembers().forEach(arenaPlayer -> {
                arenaPlayer.addLosses();
                arenaPlayer.setStatus(PlayerStatus.LOST);
            });

            // only one team remaining -> win !
            final List<ArenaTeam> withFighters = TeamManager.getArenaTeamsWithFighters(arena);
            if (withFighters.size() == 1) {
                winner = withFighters.get(0);
            }
        }

        if (winner != null) {
            ArenaModuleManager
                    .announce(
                            arena,
                            Language.parse(Language.MSG.TEAM_HAS_WON,
                                    winner.getColor()
                                            + winner.getName() + ChatColor.YELLOW),
                            "WINNER");
            arena.broadcast(Language.parse(Language.MSG.TEAM_HAS_WON,
                    winner.getColor() + winner.getName()
                            + ChatColor.YELLOW));

            this.getTeamLifeMap().clear();
            new EndRunnable(arena, arena.getConfig().getInt(Config.CFG.TIME_ENDCOUNTDOWN));
        }
    }

    protected void applyToAllBlocksOfTeam(ArenaTeam arenaTeam, Material material, BlockData blockData) {
        this.arena.getBlocks().stream()
                .filter(paBlock -> paBlock.getTeamName().equalsIgnoreCase(arenaTeam.getName())
                        && paBlock.getName().startsWith(getLocationBlockName()))
                .forEach(paBlock -> {
                    paBlock.getLocation().toLocation().getBlock().setType(material);
                    if (blockData != null) {
                        paBlock.getLocation().toLocation().getBlock().setBlockData(blockData);
                    }
                });
    }

    protected void applyToAllBlocksOfTeam(ArenaTeam arenaTeam, ChatColor color) {
        this.arena.getBlocks().stream()
                .filter(paBlock -> paBlock.getTeamName().equalsIgnoreCase(arenaTeam.getName())
                        && paBlock.getName().startsWith(getLocationBlockName()))
                .forEach(paBlock -> ColorUtils.setNewBlockColor(paBlock.getLocation().toLocation().getBlock(), color));
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }
}
