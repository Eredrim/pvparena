package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.StatisticsManager.Type;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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

public class GoalSabotage extends AbstractBlockLocationGoal {

    public static final String TNT = "tnt";

    private Material sabotageItem = Material.FLINT_AND_STEEL;


    public GoalSabotage() {
        super("Sabotage");
    }

    /**
     * Players holding a team sabotage item
     */
    private final Map<ArenaTeam, Player> sabotageItemHolders = new HashMap<>();
    /**
     * Primed TNT
     */
    private final Map<ArenaTeam, TNTPrimed> primedTnts = new HashMap<>();

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    protected String getLocationBlockName() {
        return TNT;
    }

    protected Material getLocationBlockType() {
        return Material.TNT;
    }

    protected boolean isRegionBattleNeeded() {
        return true;
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
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam playerTeam = arenaPlayer.getArenaTeam();
        final Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return false;
        }

        if (!this.getLocationBlockType().equals(clickedBlock.getType())) {
            return false;
        }

        final PABlockLocation blockLocation = new PABlockLocation(clickedBlock.getLocation());
        final ArenaTeam arenaTeamFlag = this.teamsBlockLocations.get(blockLocation);

        if (arenaTeamFlag == null) {
            return false;
        }

        debug(arenaPlayer, "block of team {} found.", arenaTeamFlag);

        if (arenaTeamFlag.equals(playerTeam)) {
            debug("block, but self destroy");
            this.arena.msg(player, MSG.GOAL_SABOTAGE_NO_SELF_DESTROY);
            return true;
        }

        if (player.getEquipment() != null &&
                (player.getEquipment().getItemInMainHand().getType() == Material.AIR
                        || player.getEquipment().getItemInMainHand().getType() != this.sabotageItem)) {

            debug(this.arena, "block, but no sabotage items");
            this.arena.msg(player, MSG.GOAL_SABOTAGE_NOT_GOOD_ITEM);
            return true;
        }

        this.arena.broadcast(Language.parse(MSG.GOAL_SABOTAGE_IGNITED,
                playerTeam.colorizePlayer(player) + ChatColor.YELLOW,
                arenaTeamFlag.getColoredName() + ChatColor.YELLOW));

        final PAGoalEvent goalEvent = new PAGoalEvent(this.arena, this, "trigger:" + player.getName());
        Bukkit.getPluginManager().callEvent(goalEvent);
        setFlag(arenaTeamFlag.getName(), true, blockLocation);

        return true;
    }

    private void commit(final Arena arena, final ArenaTeam arenaTeam) {
        if (arena.realEndRunner != null) {
            debug(arena, "[SABOTAGE] already ending");
            return;
        }
        debug(arena, "[SABOTAGE] committing end: " + arenaTeam);
        debug(arena, "win: " + false);

        final PAGoalEvent goalEvent = new PAGoalEvent(arena, this, "");
        Bukkit.getPluginManager().callEvent(goalEvent);
        ArenaTeam winteam = null;

        arenaTeam.getTeamMembers().forEach(arenaPlayer -> {
            arenaPlayer.addStatistic(arena.getName(), Type.LOSSES, 1);
            arenaPlayer.setStatus(PlayerStatus.LOST);
        });

        final List<ArenaTeam> teamsWithFighters = TeamManager.getArenaTeamsWithFighters(arena);
        // only one team left: winner !
        if (teamsWithFighters.size() == 1) {
            winteam = teamsWithFighters.get(0);

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
        }

        new EndRunnable(arena, arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }


    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[SABOTAGE] already ending");
            return;
        }
        debug(this.arena, "[SABOTAGE]");

        ArenaTeam arenaTeam = null;

        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == PlayerStatus.FIGHT) {
                    arenaTeam = team;
                    break;
                }
            }
        }

        if (arenaTeam != null && !force) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, arenaTeam.getColor()
                            + arenaTeam.getName() + ChatColor.YELLOW), "END");
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, arenaTeam.getColor()
                            + arenaTeam.getName() + ChatColor.YELLOW), "WINNER");
            this.arena.broadcast(Language.parse(MSG.TEAM_HAS_WON, arenaTeam.getColor()
                    + arenaTeam.getName() + ChatColor.YELLOW));
        }

        if (ArenaModuleManager.commitEnd(this.arena, arenaTeam)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void disconnect(final ArenaPlayer arenaPlayer) {

        // if player have sabotage item -> send it to someone else
        final ArenaTeam arenaTeam = this.getHeldFlagTeam(arenaPlayer.getPlayer());
        if (arenaTeam != null) {
            this.getSabotageItemHolders().remove(arenaTeam);
            this.distributeSabotageItems(arenaTeam);
        }
    }

    private void distributeSabotageItems(final ArenaTeam arenaTeam) {
        final Set<ArenaPlayer> arenaTeamTeamMembers = arenaTeam.getTeamMembers();
        final ArenaPlayer randomPlayer = RandomUtils.getRandom(arenaTeamTeamMembers, new Random());

        debug(randomPlayer, "distributing sabotage: {}", randomPlayer.getName());

        this.getSabotageItemHolders().put(arenaTeam, randomPlayer.getPlayer());
        randomPlayer.getPlayer().getInventory().addItem(new ItemStack(this.sabotageItem, 1));
        this.arena.msg(randomPlayer.getPlayer(), MSG.GOAL_SABOTAGE_YOU_CARRY_TNT);
    }

    private ArenaTeam getHeldFlagTeam(final Player player) {
        if (this.getSabotageItemHolders().isEmpty()) {
            return null;
        }

        debug(player, "getting held sabotage item of player {}", player);
        return this.getSabotageItemHolders().keySet().stream().filter(arenaTeam ->
                player.equals(this.getSabotageItemHolders().get(arenaTeam)))
                .findFirst().orElse(null);
    }

    private Map<ArenaTeam, Player> getSabotageItemHolders() {
        return this.sabotageItemHolders;
    }

    private Map<ArenaTeam, TNTPrimed> getTNTmap() {
        return this.primedTnts;
    }

    @Override
    public void initiate(final Player player) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = arenaPlayer.getArenaTeam();

        if (!this.getSabotageItemHolders().containsKey(team)) {
            debug(this.arena, player, "give sabotage item to " + team.getName());
            this.distributeSabotageItems(team);
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final PADeathInfo event) {
        final ArenaTeam arenaTeam = this.getHeldFlagTeam(player);
        if (arenaTeam != null) {
            this.getSabotageItemHolders().remove(arenaTeam);
            this.distributeSabotageItems(arenaTeam);
        }
    }

    @Override
    public void parseStart() {
        super.parseStart();
        debug(this.arena, "initiating arena");
        this.sabotageItem = this.arena.getConfig().getMaterial(CFG.GOAL_SABOTAGE_ITEM);
        this.getSabotageItemHolders().clear();
        this.arena.getTeams().forEach(arenaTeam -> {
            this.getTeamBlockLocations(arenaTeam).forEach(
                    blockLocation -> this.setFlag(arenaTeam.getName(), false,blockLocation)
            );

            if (!this.getSabotageItemHolders().containsKey(arenaTeam)) {
                debug(this.arena, "adding team " + arenaTeam.getName());
                this.distributeSabotageItems(arenaTeam);
            }
        });
    }

    @Override
    public void reset(final boolean force) {
        this.getSabotageItemHolders().clear();
        this.getTNTmap().values().forEach(TNTPrimed::remove);
        this.getTNTmap().clear();
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
     * @param teamName        the teamcolor to reset
     * @param take            true if take, else reset
     * @param paBlockLocation the location to take/reset
     */
    void setFlag(final String teamName, final boolean take, final PABlockLocation paBlockLocation) {
        debug(String.format("Take flag for team %s, take: %s, location: %s", teamName, take, paBlockLocation.toString()));
        paBlockLocation.toLocation().getBlock().setType(take ? Material.AIR : Material.TNT);
        if (take) {
            final TNTPrimed tnt = (TNTPrimed) Bukkit.getWorld(
                    paBlockLocation.getWorldName())
                    .spawnEntity(paBlockLocation.toLocation(), EntityType.PRIMED_TNT);

            getTNTmap().put(this.arena.getTeam(teamName), tnt);
        }
    }

    @Override
    public void unload(final Player player) {
        this.disconnect(ArenaPlayer.fromPlayer(player));
    }

    @Override
    public void checkExplode(final EntityExplodeEvent event) {
        debug(this.arena, "sabotage: checkExplosion");
        if (event.getEntityType() != EntityType.PRIMED_TNT) {
            return;
        }

        final TNTPrimed tnt = (TNTPrimed) event.getEntity();

        ArenaTeam tntArenaTeam = getTNTmap().entrySet().stream()
                .filter(teamEntry -> tnt.getUniqueId().equals(teamEntry.getValue().getUniqueId()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (tntArenaTeam == null) {
            debug(this.arena, "Tnt is not a sabotage target.");
            return;
        }

        debug("Checking tnt {} for team {}", tnt.getUniqueId(), tntArenaTeam.getName());
        event.setCancelled(true);

        commit(this.arena, tntArenaTeam);
        World world = event.getEntity().getLocation().getWorld();
        Location location = event.getEntity().getLocation();
        tnt.remove();
        if (world != null) {
            world.spawnParticle(Particle.EXPLOSION_LARGE, location.getX(), location.getY() + 1, location.getZ(), 25);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 20, 2);
        }
    }
}
