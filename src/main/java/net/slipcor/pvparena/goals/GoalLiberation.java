package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import net.slipcor.pvparena.runnables.InventoryRefillRunnable;
import net.slipcor.pvparena.runnables.RespawnRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

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

    public GoalLiberation() {
        super("Liberation");
    }

    private EndRunnable endRunner;
    private String flagName;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean checkCommand(final String string) {
        return this.arena.getTeams().stream().anyMatch(team -> string.contains(team.getName() + BUTTON));
    }

    @Override
    public List<String> getGoalCommands() {
        final List<String> result = new ArrayList<>();
        if (this.arena != null) {
            for (final ArenaTeam team : this.arena.getTeams()) {
                final String sTeam = team.getName();
                result.add(sTeam + BUTTON);
            }
        }
        return result;
    }

    @Override
    public boolean checkEnd() {
        debug(this.arena, "checkEnd - " + this.arena.getName());

        if (!this.arena.isFreeForAll()) {
            debug(this.arena, "TEAMS!");
            final int count = TeamManager.countActiveTeams(this.arena);
            debug(this.arena, "count: " + count);

            return count <= 1; // yep. only one team left. go!
        }

        PVPArena.getInstance().getLogger().warning("Liberation goal running in FFA mode: " + this.arena.getName());

        return false;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        if (!this.arena.isFreeForAll()) {
            final String team = this.checkForMissingTeamSpawn(list);
            if (team != null) {
                return team;
            }

            return this.checkForMissingTeamCustom(list, "jail");
        }
        PVPArena.getInstance().getLogger().warning("Liberation goal running in FFA mode: " + this.arena.getName());
        return null;
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
        debug(this.arena, player, "checking interact");

        if (block.getType() != Material.STONE_BUTTON) {
            debug(this.arena, player, "block, but not button");
            return false;
        }
        debug(this.arena, player, "button click!");

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        final ArenaTeam playerArenaTeam = arenaPlayer.getArenaTeam();
        if (playerArenaTeam == null) {
            return false;
        }

        Vector vFlag = null;
        for (final ArenaTeam arenaTeam : this.arena.getNotEmptyTeams()) {

            if (arenaTeam.equals(playerArenaTeam)) {
                debug(this.arena, player, "equals!OUT! ");
                continue;
            }
            debug(this.arena, player, "checking for flag of team " + arenaTeam);
            Vector vLoc = block.getLocation().toVector();
            debug(this.arena, player, "block: " + vLoc);
            if (!SpawnManager.getBlocksStartingWith(this.arena, arenaTeam.getName() + BUTTON).isEmpty()) {
                vFlag = SpawnManager
                        .getBlockNearest(
                                SpawnManager.getBlocksStartingWith(this.arena, arenaTeam.getName()
                                        + BUTTON),
                                new PABlockLocation(player.getLocation()))
                        .toLocation().toVector();
            }
            if (vFlag != null && vLoc.distance(vFlag) < 2) {
                debug(this.arena, player, "button found!");
                debug(this.arena, player, "vFlag: " + vFlag);

                boolean success = false;

                for (final ArenaPlayer jailedPlayer : playerArenaTeam.getTeamMembers()) {
                    if (jailedPlayer.getStatus() == Status.DEAD) {
                        SpawnManager.respawn(this.arena, jailedPlayer, null);
                        final List<ItemStack> iList = new ArrayList<>();

                        for (final ItemStack item : jailedPlayer.getArenaClass().getItems()) {
                            if (item == null) {
                                continue;
                            }
                            iList.add(item.clone());
                        }
                        new InventoryRefillRunnable(this.arena, jailedPlayer.getPlayer(), iList);
                        if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_LIBERATION_JAILEDSCOREBOARD)) {
                            player.getScoreboard().getObjective("lives").getScore(player.getName()).setScore(0);
                        }
                        success = true;
                    }
                }

                if (success) {

                    this.arena.broadcast(ChatColor.YELLOW + Language
                            .parse(this.arena, MSG.GOAL_LIBERATION_LIBERATED,
                                    playerArenaTeam.getColoredName()
                                            + ChatColor.YELLOW));

                    final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + player.getName());
                    Bukkit.getPluginManager().callEvent(gEvent);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (StringUtils.isBlank(this.flagName) || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }

        if (block == null || block.getType() != Material.STONE_BUTTON) {
            return false;
        }

        return PVPArena.hasAdminPerms(player) || PVPArena.hasCreatePerms(player, this.arena);
    }

    @Override
    public Boolean checkPlayerDeath(Player player) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        final int pos = this.getTeamLifeMap().get(arenaTeam);
        debug(this.arena, player, "lives before death: " + pos);
        if (pos <= 1) {
            this.getTeamLifeMap().put(arenaTeam, 1);

            final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
            final ArenaTeam team = aPlayer.getArenaTeam();
            boolean someoneAlive = false;

            for (final ArenaPlayer temp : team.getTeamMembers()) {
                if (temp.getStatus() == Status.FIGHT) {
                    someoneAlive = true;
                    break;
                }
            }

            return someoneAlive;
        }
        return true;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].contains(BUTTON)) {
            for (final ArenaTeam team : this.arena.getTeams()) {
                final String sTeam = team.getName();
                if (args[0].contains(sTeam + BUTTON)) {
                    this.flagName = args[0];
                    PAA_Region.activeSelections.put(sender.getName(), this.arena);

                    this.arena.msg(sender, Language.parse(this.arena, MSG.GOAL_LIBERATION_TOSET, this.flagName));
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

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        for (final ArenaTeam arenaTeam : this.arena.getTeams()) {
            for (final ArenaPlayer arenaPlayer : arenaTeam.getTeamMembers()) {
                if (arenaPlayer.getStatus() != Status.FIGHT) {
                    continue;
                }
                if (this.arena.isFreeForAll()) {
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                            "END");

                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                            "WINNER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.PLAYER_HAS_WON,
                            arenaPlayer.getName()));
                } else {

                    ArenaModuleManager.announce(
                            this.arena,
                            Language.parse(this.arena, MSG.TEAM_HAS_WON,
                                    arenaTeam.getColoredName()), "WINNER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.TEAM_HAS_WON,
                            arenaTeam.getColoredName()));
                    break;
                }
            }

            if (ArenaModuleManager.commitEnd(this.arena, arenaTeam)) {
                return;
            }
        }

        this.endRunner = new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn,
                                  final PlayerDeathEvent event) {

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();

        if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
            debug(this.arena, player, "cmd: not in life map!");
            return;
        }
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + player.getName());
        Bukkit.getPluginManager().callEvent(gEvent);
        int lives = this.getTeamLifeMap().get(arenaTeam);
        debug(this.arena, player, "lives before death: " + lives);
        if (lives <= 1) {
            this.getTeamLifeMap().put(arenaTeam, 1);

            final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

            aPlayer.setStatus(Status.DEAD);

            final ArenaTeam team = aPlayer.getArenaTeam();

            boolean someoneAlive = false;

            for (final ArenaPlayer temp : team.getTeamMembers()) {
                if (temp.getStatus() == Status.FIGHT) {
                    someoneAlive = true;
                    break;
                }
            }

            if (someoneAlive) {

                if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                    this.broadcastSimpleDeathMessage(player, event);
                }
                final List<ItemStack> returned;

                if (this.arena.getArenaConfig().getBoolean(
                        CFG.PLAYER_DROPSINVENTORY)) {
                    returned = InventoryManager.drop(player);
                    event.getDrops().clear();
                } else {
                    returned = new ArrayList<>(event.getDrops());
                }
                new InventoryRefillRunnable(this.arena, aPlayer.getPlayer(), returned);

                String teamName = aPlayer.getArenaTeam().getName();

                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RespawnRunnable(this.arena, aPlayer, teamName + "jail"), 1L);

                this.arena.unKillPlayer(aPlayer.getPlayer(), ofNullable(aPlayer.getPlayer().getLastDamageCause()).map(EntityDamageEvent::getCause).orElse(null), aPlayer.getPlayer().getKiller());

                if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_LIBERATION_JAILEDSCOREBOARD)) {
                    aPlayer.getPlayer().getScoreboard().getObjective("lives").getScore(aPlayer.getName()).setScore(101);
                }
            } else {
                this.getTeamLifeMap().remove(arenaTeam);
                final List<ItemStack> returned;

                if (this.arena.getArenaConfig().getBoolean(
                        CFG.PLAYER_DROPSINVENTORY)) {
                    returned = InventoryManager.drop(player);
                    event.getDrops().clear();
                } else {
                    returned = new ArrayList<>(event.getDrops());
                }

                WorkflowManager.handleRespawn(this.arena,
                        ArenaPlayer.fromPlayer(player), returned);

                ArenaPlayer.fromPlayer(player).setStatus(Status.LOST);

                if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                    this.broadcastSimpleDeathMessage(player, event);
                }

                WorkflowManager.handleEnd(this.arena, false);
            }

        } else {
            lives--;
            this.getTeamLifeMap().put(arenaTeam, lives);

            if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, player, event, lives);
            }

            final List<ItemStack> returned;

            if (this.arena.getArenaConfig().getBoolean(
                    CFG.PLAYER_DROPSINVENTORY)) {
                returned = InventoryManager.drop(player);
                event.getDrops().clear();
            } else {
                returned = new ArrayList<>(event.getDrops());
            }

            WorkflowManager.handleRespawn(this.arena,
                    ArenaPlayer.fromPlayer(player), returned);

        }
    }

    @Override
    public boolean commitSetFlag(final Player player, final Block block) {

        debug(this.arena, player, "trying to set a button");

        // command : /pa redbutton1
        // location: redbutton1:

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), this.flagName);
        this.arena.msg(player, Language.parse(this.arena, MSG.GOAL_LIBERATION_SET, this.flagName));

        PAA_Region.activeSelections.remove(player.getName());
        this.flagName = null;

        return true;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + this.arena.getArenaConfig().getInt(CFG.GOAL_LLIVES_LIVES));
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
    }

    @Override
    public boolean hasSpawn(final String string) {
        if (this.arena.isFreeForAll()) {
            PVPArena.getInstance().getLogger().warning("Liberation goal running in FFA mode! /pa " + this.arena.getName() + " !gm team");
            return false;
        }
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
            if (string.toLowerCase().startsWith(
                    teamName.toLowerCase() + "jail")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initiate(final Player player) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        this.getTeamLifeMap().put(arenaTeam, this.arena.getArenaConfig().getInt(CFG.GOAL_LLIVES_LIVES));
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        this.getTeamLifeMap().remove(arenaTeam);
    }

    @Override
    public void parseStart() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(),
                        this.arena.getArenaConfig().getInt(CFG.GOAL_LLIVES_LIVES));
            }
        }
        if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_LIBERATION_JAILEDSCOREBOARD)) {
            this.arena.addCustomScoreBoardEntry(null, Language.parse(this.arena, MSG.GOAL_LIBERATION_SCOREBOARD_HEADING), 102);
            this.arena.addCustomScoreBoardEntry(null, Language.parse(this.arena, MSG.GOAL_LIBERATION_SCOREBOARD_SEPARATOR), 100);
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getTeamLifeMap().clear();
        if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_LIBERATION_JAILEDSCOREBOARD)) {
            this.arena.removeCustomScoreBoardEntry(null, 102);
            this.arena.removeCustomScoreBoardEntry(null, 100);
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
            debug(this.arena, "no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
            if (this.arena.isFreeForAll()) {

                if (scores.containsKey(arenaPlayer.getName())) {
                    scores.put(arenaPlayer.getName(), scores.get(arenaPlayer.getName()) + score);
                } else {
                    scores.put(arenaPlayer.getName(), score);
                }
            } else {
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
        }

        return scores;
    }
}
