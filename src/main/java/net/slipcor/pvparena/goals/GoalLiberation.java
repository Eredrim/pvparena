package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerState;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.goal.PAGoalEndEvent;
import net.slipcor.pvparena.events.goal.PAGoalJailReleaseEvent;
import net.slipcor.pvparena.events.goal.PAGoalPlayerDeathEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.loadables.ModuleType;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.managers.TeleportManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import net.slipcor.pvparena.runnables.InventoryRefillRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.managers.SpawnManager.getPASpawnsStartingWith;

/**
 * <pre>
 * Arena Goal class "Liberation"
 * </pre>
 * <p/>
 * Players have lives. When every life is lost, the player is teleported
 * to the killer's team's jail. Once every player of a team is jailed, the
 * team is out.
 *
 * @author slipcor
 */

public class GoalLiberation extends ArenaGoal {

    private static final String BUTTON = "button";
    private static final String JAIL = "jail";
    private static final String BUTTON_SUFFIX = "_BUTTON";

    public GoalLiberation() {
        super("Liberation");
    }

    private EndRunnable endRunner;
    private String blockTeamName;
    private List<ArenaPlayer> jailedPlayers = new ArrayList<>();

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean checkCommand(final String command) {
        return command.equalsIgnoreCase(BUTTON);
    }

    @Override
    public List<String> getGoalCommands() {
        return Collections.singletonList(BUTTON);
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        arena.getTeamNames().forEach(teamName -> result.define(new String[]{"set", teamName}));
        arena.getBlocks().stream().filter(b -> BUTTON.equalsIgnoreCase(b.getName()))
                        .forEach(b -> result.define(new String[]{"remove", b.getTeamName()}));

        return result;
    }

    @Override
    public boolean checkEnd() {
        debug(this.arena, "checkEnd - " + this.arena.getName());
        final int count = TeamManager.countActiveTeams(this.arena);
        debug(this.arena, "count: " + count);

        return count <= 1; // yep. only one team left. go!
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        final Set<PASpawn> missing = SpawnManager.getMissingTeamSpawn(this.arena, spawns);
        missing.addAll(SpawnManager.getMissingTeamCustom(this.arena, spawns, JAIL));
        return missing;
    }

    @Override
    public Set<PABlock> checkForMissingBlocks(Set<PABlock> blocks) {
        return SpawnManager.getMissingBlocksTeamCustom(this.arena, blocks, BUTTON);
    }

    /**
     * hook into an interacting player
     *
     * @param player the interacting player
     * @param event  the interact event
     * @return true if event has been handled
     */
    @Override
    public boolean checkInteract(final Player player, final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return false;
        }
        debug(this.arena, player, "checking interact (Liberation)");

        if (!block.getType().name().endsWith(BUTTON_SUFFIX)) {
            debug(this.arena, player, "block, but not button");
            return false;
        }

        debug(this.arena, player, "button click!");

        PABlockLocation clickedLoc  = new PABlockLocation(block.getLocation());
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();

        if (arenaTeam == null) {
            debug(arenaPlayer, "ArenaTeam is null");
            return false;
        }

        boolean isTeamButton = SpawnManager.getBlocksStartingWith(this.arena, BUTTON, arenaTeam.getName())
                .stream()
                .anyMatch(btnLoc -> btnLoc.equals(clickedLoc));

        if (isTeamButton) {

            debug(arenaPlayer, "Button of team {} matched!", arenaTeam.getName());

            boolean success = false;
            for (ArenaPlayer jailedPlayer : arenaTeam.getTeamMembers()) {
                if (jailedPlayer.getStatus() == PlayerStatus.DEAD) {
                    this.jailedPlayers.remove(jailedPlayer);
                    SpawnManager.respawn(jailedPlayer, null);
                    new InventoryRefillRunnable(this.arena, jailedPlayer.getPlayer(), emptyList());
                    if (this.arena.getConfig().getBoolean(CFG.GOAL_LIBERATION_JAILED_SCOREBOARD)) {
                        player.getScoreboard().getObjective("lives").getScore(player.getName()).setScore(0);
                    }

                    success = true;
                }
            }

            if (success) {
                this.arena.broadcast(Language.parse(MSG.GOAL_LIBERATION_LIBERATED, arenaTeam.getColoredName()));

                PAGoalJailReleaseEvent gEvent = new PAGoalJailReleaseEvent(this.arena, this, arenaPlayer, arenaTeam);
                Bukkit.getPluginManager().callEvent(gEvent);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (StringUtils.isBlank(this.blockTeamName) || !SpawnManager.activeSelections.containsKey(player.getName())) {
            return false;
        }

        if (block == null || !block.getType().name().endsWith(BUTTON_SUFFIX)) {
            debug(player, "Block {} is not a button", block);
            return false;
        }

        return PermissionManager.hasAdminPerm(player) || PermissionManager.hasBuilderPerm(player, this.arena);
    }

    @Override
    public Boolean shouldRespawnPlayer(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        int teamLeaves = this.getTeamLifeMap().get(arenaTeam);
        debug(arenaPlayer, "lives before death: {}", teamLeaves);
        if (teamLeaves <= 1) {
            return arenaPlayer.getArenaTeam().getTeamMembers()
                    .stream()
                    .anyMatch(ap -> !this.jailedPlayers.contains(ap));
        }
        return true;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].equals(BUTTON)) {
            if (args.length != 3) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3");
            } else {
                String teamName = args[2];
                ArenaTeam relatedTeam = this.arena.getTeam(teamName);
                if (relatedTeam == null) {
                    this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, args[1]);
                    return;
                }
                this.blockTeamName = relatedTeam.getName();

                if("set".equalsIgnoreCase(args[1])) {
                    SpawnManager.activeSelections.put(sender.getName(), this.arena);
                    this.arena.msg(sender, MSG.GOAL_LIBERATION_TOSET, teamName);
                } else if ("remove".equalsIgnoreCase(args[1])) {
                    Optional<PABlock> paBlock = this.arena.getBlocks().stream()
                            .filter(block -> teamName.equalsIgnoreCase(block.getTeamName()) && block.getName().equalsIgnoreCase(BUTTON))
                            .findAny();

                    if (paBlock.isEmpty()) {
                        this.arena.msg(sender, MSG.GOAL_LIBERATION_BTN_NOTFOUND, teamName);
                        return;
                    }
                    SpawnManager.removeBlock(this.arena, paBlock.get());
                    this.arena.msg(sender, MSG.GOAL_LIBERATION_BTN_REMOVED, teamName);
                } else {
                    this.blockTeamName = null;
                }
            }
        }
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[LIBERATION] already ending");
            return;
        }

        final PAGoalEndEvent gEvent = new PAGoalEndEvent(this.arena, this);
        Bukkit.getPluginManager().callEvent(gEvent);
        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            for (ArenaPlayer arenaPlayer : arenaTeam.getTeamMembers()) {
                if (arenaPlayer.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                ArenaModuleManager.announce(
                        this.arena,
                        Language.parse(MSG.TEAM_HAS_WON, arenaTeam.getColoredName()),
                        "WINNER"
                );

                this.arena.broadcast(Language.parse(MSG.TEAM_HAS_WON, arenaTeam.getColoredName()));
                this.arena.addWinner(arenaTeam.getName());
                break;
            }

            if (ArenaModuleManager.commitEnd(this.arena, arenaTeam, null)) {
                return;
            }
        }

        this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final ArenaPlayer arenaPlayer, final boolean doesRespawn, PADeathInfo deathInfo) {
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();

        if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
            debug(arenaPlayer, "cmd: not in life map!");
            return;
        }

        final PAGoalPlayerDeathEvent gEvent = new PAGoalPlayerDeathEvent(this.arena, this, arenaPlayer, deathInfo, false);
        Bukkit.getPluginManager().callEvent(gEvent);
        int teamLives = this.getTeamLifeMap().get(arenaTeam);
        debug(arenaPlayer, "teamLives before death: {}", teamLives);

        if (teamLives <= 1) {
            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(false);

            this.getTeamLifeMap().put(arenaTeam, 1);

            arenaPlayer.setStatus(PlayerStatus.DEAD);
            this.jailedPlayers.add(arenaPlayer);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastSimpleDeathMessage(arenaPlayer, deathInfo);
            }

        } else {
            int remainingLives = teamLives - 1;
            this.getTeamLifeMap().put(arenaTeam, remainingLives);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, arenaPlayer, deathInfo, remainingLives);
            }

            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);
        }
    }

    // Called after commitPlayerDeath() and inventory management in WorkflowManager.handlePlayerDeath()
    @Override
    public void parsePlayerDeath(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        Player player = arenaPlayer.getPlayer();
        ArenaTeam team = arenaPlayer.getArenaTeam();

        if (this.jailedPlayers.contains(arenaPlayer)) {
            boolean someoneAlive = team.getTeamMembers().stream()
                    .anyMatch(pl -> !this.jailedPlayers.contains(pl));

            if (someoneAlive) {
                // Player can still be freed => respawn them to jail
                debug(arenaPlayer, "player is DEAD - teleporting them to jail");
                InventoryManager.clearInventory(player);

                arenaPlayer.revive(deathInfo);
                Set<PASpawn> jailSpawns = getPASpawnsStartingWith(this.arena, JAIL, arenaPlayer.getArenaTeam().getName());
                TeleportManager.teleportPlayerToRandomSpawn(this.arena, arenaPlayer, jailSpawns);

                if (this.arena.getConfig().getBoolean(CFG.GOAL_LIBERATION_JAILED_SCOREBOARD)) {
                    player.getScoreboard().getObjective("lives").getScore(arenaPlayer.getName()).setScore(101);
                }
            } else {
                // Last player killed => all their team should be excluded
                ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
                debug(arenaPlayer, "player LOST - all {} team should be excluded", arenaTeam.getName());
                this.getTeamLifeMap().remove(arenaTeam);

                InventoryManager.clearInventory(player);

                Optional<ArenaModule> spectateMod = this.arena.getMods().stream()
                        .filter(mod -> mod.getType() == ModuleType.SPECTATE)
                        .min(Comparator.comparingInt(ArenaModule::getPriority));

                Consumer<Player> spectateConsumer = (p) -> spectateMod.ifPresentOrElse(
                        arenaModule -> arenaModule.switchToSpectate(p),
                        () -> Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> new PAG_Leave().commit(this.arena, p, new String[0]), 5L)
                );

                arenaTeam.getTeamMembers().forEach(member -> {
                    if (member.getStatus() == PlayerStatus.DEAD) {
                        member.setStatus(PlayerStatus.LOST);

                        this.arena.removePlayer(member, this.arena.getConfig().getString(CFG.TP_DEATH), true, false);
                        spectateConsumer.accept(member.getPlayer());

                        PlayerState.fullReset(this.arena, member.getPlayer());
                    } else {
                        member.setStatus(PlayerStatus.LOST);
                    }
                });

                WorkflowManager.handleEnd(this.arena, false);
            }
        }
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        debug(this.arena, player, "trying to set a button");

        // command : /pa button1 red
        // location: red.button1:

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), BUTTON, this.blockTeamName);
        this.arena.msg(player, MSG.GOAL_LIBERATION_SET, this.blockTeamName);

        SpawnManager.activeSelections.remove(player.getName());
        this.blockTeamName = null;

        return true;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(String.format("lives: %d", this.arena.getConfig().getInt(CFG.GOAL_LLIVES_LIVES)));
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String spawnTeamName) {
        boolean hasSpawn = super.hasSpawn(spawnName, spawnTeamName);
        if (hasSpawn) {
            return true;
        }

        for (String teamName : this.arena.getTeamNames()) {
            if (spawnName.equalsIgnoreCase(JAIL) && spawnTeamName.equalsIgnoreCase(teamName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initiate(final ArenaPlayer arenaPlayer) {
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        this.getTeamLifeMap().put(arenaTeam, this.arena.getConfig().getInt(CFG.GOAL_LLIVES_LIVES));
    }

    @Override
    public void parseLeave(final ArenaPlayer arenaPlayer) {
        if (arenaPlayer == null) {
            PVPArena.getInstance().getLogger().warning(this.getName() + ": player NULL");
            return;
        }
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        this.getTeamLifeMap().remove(arenaTeam);
    }

    @Override
    public void parseStart() {
        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(),
                        this.arena.getConfig().getInt(CFG.GOAL_LLIVES_LIVES));
            }
        }
        if (this.arena.getConfig().getBoolean(CFG.GOAL_LIBERATION_JAILED_SCOREBOARD)) {
            this.arena.getScoreboard().addCustomEntry(null, Language.parse(MSG.GOAL_LIBERATION_SCOREBOARD_HEADING), 102);
            this.arena.getScoreboard().addCustomEntry(null, Language.parse(MSG.GOAL_LIBERATION_SCOREBOARD_SEPARATOR), 100);
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getTeamLifeMap().clear();
        this.jailedPlayers.clear();
        if (this.arena.getConfig().getBoolean(CFG.GOAL_LIBERATION_JAILED_SCOREBOARD)) {
            this.arena.getScoreboard().removeCustomEntry(null, 102);
            this.arena.getScoreboard().removeCustomEntry(null, 100);
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

        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
            if (arenaPlayer.getArenaTeam() == null) {
                continue;
            }
            if (scores.containsKey(arenaPlayer.getArenaTeam().getName())) {
                scores.put(arenaPlayer.getArenaTeam().getName(),
                        scores.get(arenaPlayer.getName()) + score);
            } else {
                scores.put(arenaPlayer.getArenaTeam().getName(), score);
            }
        }

        return scores;
    }
}
