package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.goal.PAGoalEndEvent;
import net.slipcor.pvparena.events.goal.PAGoalSabotageIgniteEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "Sabotage"
 * </pre>
 * <p/>
 * The first advanced Arena Goal. Sneak into an other team's base and ignite
 * their TNT.
 *
 * @author slipcor
 */

public class GoalSabotage extends ArenaGoal implements Listener {

    private static final String TNT = "tnt";

    public GoalSabotage() {
        super("Sabotage");
    }

    private String blockTeamName;
    private ArenaTeam winningTeam;
    private Map<ArenaTeam, ArenaPlayer> teamLighterOwners;
    private TNTPrimed explodingTNT;

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
        return TNT.equalsIgnoreCase(string);
    }

    @Override
    public List<String> getGoalCommands() {
        return Collections.singletonList(TNT);
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        arena.getTeamNames().forEach(teamName -> result.define(new String[]{"set", teamName}));
        arena.getBlocks().stream().filter(b -> TNT.equalsIgnoreCase(b.getName()))
                .forEach(b -> result.define(new String[]{"remove", b.getTeamName()}));

        return result;
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingTeamSpawn(this.arena, spawns);
    }

    @Override
    public Set<PABlock> checkForMissingBlocks(Set<PABlock> blocks) {
        return SpawnManager.getMissingBlocksTeamCustom(this.arena, blocks, TNT);
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

        if (block.getType() != Material.TNT) {
            debug(this.arena, player, "block, but not TNT");
            return false;
        }
        debug(this.arena, player, "tnt click!");

        if (player.getEquipment() == null || player.getEquipment().getItemInMainHand().getType() != Material.FLINT_AND_STEEL) {
            debug(this.arena, player, "block, but no sabotage items");
            this.arena.msg(player, MSG.GOAL_SABOTAGE_NOTGOODITEM);
            return false;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        final ArenaTeam pTeam = aPlayer.getArenaTeam();
        if (pTeam == null) {
            return false;
        }

        for (ArenaTeam team : this.arena.getTeams()) {
            final String teamName = team.getName();
            if (team.getTeamMembers().isEmpty()) {
                continue; // dont check for inactive teams
            }
            debug(this.arena, player, "checking for tnt of team " + teamName);

            PABlockLocation teamTNTLoc = SpawnManager.getBlockByExactName(this.arena, TNT, teamName);
            PABlockLocation blockLocation = new PABlockLocation(block.getLocation());
            debug(this.arena, player, "block: " + blockLocation);

            if (blockLocation.equals(teamTNTLoc)) {
                debug(this.arena, player, "TNT found!");

                if(team.equals(pTeam)) {
                    debug(this.arena, player, "Player is trying to self-destroy their own TNT");
                    this.arena.msg(player, MSG.GOAL_SABOTAGE_NOSELFDESTROY);
                    event.setCancelled(true);

                } else if(this.winningTeam == null) {
                    this.arena.broadcast(Language.parse(MSG.GOAL_SABOTAGE_IGNITED,
                            pTeam.colorizePlayer(aPlayer) + ChatColor.YELLOW,
                            team.getColoredName() + ChatColor.YELLOW));

                    final PAGoalSabotageIgniteEvent gEvent = new PAGoalSabotageIgniteEvent(this.arena, this, ArenaPlayer.fromPlayer(player), team);
                    Bukkit.getPluginManager().callEvent(gEvent);
                    this.primeTNT(team, new PABlockLocation(block.getLocation()));

                } else {
                    debug(this.arena, player, "Trying to prime TNT after another team has won");
                    event.setCancelled(true);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (StringUtils.isBlank(this.blockTeamName) || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }
        if (block == null || block.getType() != Material.TNT) {
            return false;
        }

        return PermissionManager.hasAdminPerm(player) || PermissionManager.hasBuilderPerm(player, this.arena);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].equals(TNT)) {
            if (args.length != 3) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3");
            } else {
                String teamName = args[2];
                if (this.arena.getTeam(teamName) == null) {
                    this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, args[1]);
                    return;
                }
                this.blockTeamName = teamName;

                if("set".equalsIgnoreCase(args[1])) {
                    PAA_Region.activeSelections.put(sender.getName(), this.arena);
                    this.arena.msg(sender, MSG.GOAL_SABOTAGE_TOSET, teamName);
                } else if ("remove".equalsIgnoreCase(args[1])) {
                    Optional<PABlock> paBlock = this.arena.getBlocks().stream()
                            .filter(block -> teamName.equalsIgnoreCase(block.getTeamName()) && block.getName().equalsIgnoreCase(TNT))
                            .findAny();

                    if (!paBlock.isPresent()) {
                        this.arena.msg(sender, MSG.GOAL_SABOTAGE_TNT_NOTFOUND, teamName);
                        return;
                    }
                    SpawnManager.removeBlock(this.arena, paBlock.get());
                    this.arena.msg(sender, MSG.GOAL_SABOTAGE_TNT_REMOVED, teamName);
                } else {
                    this.blockTeamName = null;
                }
            }
        }
    }

    public boolean checkEnd() {
        Set<ArenaTeam> activeTeams = TeamManager.getActiveTeams(this.arena);

        if (activeTeams.size() == 1) {
            this.winningTeam = activeTeams.stream().findAny().get();
            return true; // yep. only one team left. go!
        } else if (activeTeams.isEmpty()) {
            debug(this.arena, "No teams playing!");
        }
        return false;
    }


    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[SABOTAGE] already ending");
            return;
        }
        debug(this.arena, "[SABOTAGE] committing end. Force: {}, Winner: {}", force, this.winningTeam);

        if(!force) {
            final PAGoalEndEvent gEvent = new PAGoalEndEvent(arena, this);
            Bukkit.getPluginManager().callEvent(gEvent);

            this.arena.getTeams().stream()
                    .filter(team -> !team.equals(this.winningTeam))
                    .flatMap(team -> team.getTeamMembers().stream())
                    .forEach(arenaPlayer -> arenaPlayer.setStatus(PlayerStatus.LOST));

            String winMsg = Language.parse(MSG.TEAM_HAS_WON, this.winningTeam.getColoredName() + ChatColor.YELLOW);
            ArenaModuleManager.announce(this.arena, winMsg, "WINNER");
            this.arena.broadcast(winMsg);
        }

        if (ArenaModuleManager.commitEnd(this.arena, this.winningTeam)) {
            return;
        }

        new EndRunnable(this.arena, this.arena.getConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        debug(this.arena, player, "trying to set a tnt");

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), TNT, this.blockTeamName);
        this.arena.msg(player, MSG.GOAL_SABOTAGE_SETTNT, this.blockTeamName);

        PAA_Region.activeSelections.remove(player.getName());
        this.blockTeamName = null;
        return true;
    }

    @Override
    public void disconnect(final ArenaPlayer aPlayer) {
        final boolean holdingLighter = this.isPlayerHoldingLighter(aPlayer);
        if (holdingLighter) {
            ArenaTeam holderTeam = aPlayer.getArenaTeam();
            this.getLighterMap().remove(holderTeam);

            if(holderTeam.getTeamMembers().size() > 1) {
                this.distributeLighter(holderTeam, aPlayer);
            }
        }
    }

    private void distributeLighter(ArenaTeam team, ArenaPlayer leavingPlayer) {
        Random random = new Random();

        // If one player leaves, it means match started. So limit redistribution to fighting players
        Set<ArenaPlayer> activeTeamPlayers = team.getTeamMembers().stream()
                .filter(teamPlayer -> leavingPlayer == null || teamPlayer.getStatus() == PlayerStatus.FIGHT)
                .collect(Collectors.toSet());

        ArenaPlayer randomTeamPlayer = RandomUtils.getRandom(activeTeamPlayers, random);
        while(activeTeamPlayers.size() > 1 && Objects.equals(randomTeamPlayer, leavingPlayer)) {
            randomTeamPlayer = RandomUtils.getRandom(activeTeamPlayers, random);
        }

        this.getLighterMap().put(team, randomTeamPlayer);
        randomTeamPlayer.getPlayer().getInventory().addItem(new ItemStack(Material.FLINT_AND_STEEL, 1));
        this.arena.msg(randomTeamPlayer.getPlayer(), MSG.GOAL_SABOTAGE_YOUTNT);
    }

    private boolean isPlayerHoldingLighter(ArenaPlayer player) {
        debug(player, "getting held TNT of player {}", player);
        boolean holding = this.getLighterMap().values().stream()
                .anyMatch(player::equals);

        if(holding) {
            debug(player, "team {}'s sabotage is carried by {}s hands", player.getArenaTeam().getName(), player.getName());
        }

        return holding;
    }

    private Map<ArenaTeam, ArenaPlayer> getLighterMap() {
        if (this.teamLighterOwners == null) {
            this.teamLighterOwners = new HashMap<>();
        }
        return this.teamLighterOwners;
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String spawnTeamName) {
        boolean hasSpawn = super.hasSpawn(spawnName, spawnTeamName);
        if (hasSpawn) {
            return true;
        }

        for (String teamName : this.arena.getTeamNames()) {
            if (spawnName.equalsIgnoreCase(TNT) && spawnTeamName.equalsIgnoreCase(teamName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initiate(final ArenaPlayer arenaPlayer) {
        final ArenaTeam team = arenaPlayer.getArenaTeam();
        if (!this.getLighterMap().containsKey(team)) {
            debug(arenaPlayer, "adding team " + team.getName());
            SpawnManager.getBlockByExactName(this.arena, TNT, team.getName()).toLocation().getBlock().setType(Material.TNT);
            this.distributeLighter(team, null);
        }
    }

    @Override
    public void parsePlayerDeath(final ArenaPlayer aPlayer, final PADeathInfo event) {
        ArenaTeam team = aPlayer.getArenaTeam();
        boolean holdingLighter = this.isPlayerHoldingLighter(aPlayer);

        if (holdingLighter && team != null) {
            this.getLighterMap().remove(team);
            this.distributeLighter(team, aPlayer);
        }
    }

    @Override
    public void parseStart() {
        debug(this.arena, "initiating arena");
        this.getLighterMap().clear();
        for (ArenaTeam team : this.arena.getTeams()) {
            if (!this.getLighterMap().containsKey(team)) {
                debug(this.arena, "adding team " + team.getName());
                SpawnManager.getBlockByExactName(this.arena, TNT, team.getName()).toLocation().getBlock().setType(Material.TNT);
                this.distributeLighter(team, null);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
    }

    @Override
    public void reset(final boolean force) {
        HandlerList.unregisterAll(this);
        this.getLighterMap().clear();
        this.winningTeam = null;
        this.explodingTNT = null;
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
     * Fake prime a TNT (to bypass TNT protection of the arena)
     * @param team Team owning the primed TNT
     * @param paBlockLocation Location of the TNT
     */
    private void primeTNT(ArenaTeam team, PABlockLocation paBlockLocation) {
        this.winningTeam = team;
        paBlockLocation.toLocation().getBlock().setType(Material.AIR);
        World world = Bukkit.getWorld(paBlockLocation.getWorldName());
        this.explodingTNT = (TNTPrimed) world.spawnEntity(paBlockLocation.toLocation(), EntityType.PRIMED_TNT);
    }

    @Override
    public void unload(final ArenaPlayer arenaPlayer) {
        this.disconnect(arenaPlayer);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTNTExplode(final EntityExplodeEvent event) {
        if (!this.arena.isFightInProgress() && event.getEntityType() != EntityType.PRIMED_TNT) {
            return;
        }

        debug(this.arena, "Sabotage goal is handling EntityExplodeEvent");

        final TNTPrimed tnt = (TNTPrimed) event.getEntity();
        if(this.explodingTNT.getUniqueId().equals(tnt.getUniqueId())) {
            debug(this.arena, "Cancel and simulate TNT explosion to avoid damage. TNT UUID: {}", tnt.getUniqueId());

            event.setCancelled(true);
            this.commitEnd(false);
            World world = event.getEntity().getLocation().getWorld();
            Location location = event.getEntity().getLocation();
            tnt.remove();
            world.spawnParticle(Particle.EXPLOSION_LARGE, location.getX(), location.getY() + 1, location.getZ(), 25);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 20, 2);
            this.explodingTNT = null;
        }
    }
}
