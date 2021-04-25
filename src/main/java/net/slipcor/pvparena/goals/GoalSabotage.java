package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.StatisticsManager.Type;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

import static net.slipcor.pvparena.classes.PABlock.PA_BLOCK_TEAM_FORMAT;
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

public class GoalSabotage extends ArenaGoal {


    public static final String TNT = "tnt";

    public GoalSabotage() {
        super("Sabotage");
    }

    private String blockName;
    private String blockTeamName;
    private Map<String, String> teamFlags;
    private Map<ArenaTeam, TNTPrimed> teamTNTs;

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
        return this.arena.getTeams().stream().anyMatch(team -> string.contains(team.getName() + TNT));
    }

    @Override
    public List<String> getGoalCommands() {
        List<String> result = new ArrayList<>();
        if (this.arena != null) {
            this.arena.getTeams()
                    .forEach(team -> result.add(String.format(PA_BLOCK_TEAM_FORMAT, team.getName(), TNT)));
        }
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
            debug(this.arena, player, "block, but not flag");
            return false;
        }
        debug(this.arena, player, "flag click!");

        if (player.getEquipment() == null
                || player.getEquipment().getItemInMainHand().getType() != Material.FLINT_AND_STEEL) {
            debug(this.arena, player, "block, but no sabotage items");
            return false;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        final ArenaTeam pTeam = aPlayer.getArenaTeam();
        if (pTeam == null) {
            return false;
        }
        Vector vFlag = null;
        for (ArenaTeam team : this.arena.getTeams()) {
            final String aTeam = team.getName();
            if (team.getTeamMembers().size() < 1) {
                continue; // dont check for inactive teams
            }
            debug(this.arena, player, "checking for tnt of team " + aTeam);
            Vector vLoc = block.getLocation().toVector();
            debug(this.arena, player, "block: " + vLoc);
            if (!SpawnManager.getBlocksStartingWith(this.arena, TNT, aTeam).isEmpty()) {
                vFlag = SpawnManager
                        .getBlockNearest(
                                SpawnManager.getBlocksStartingWith(this.arena, TNT, aTeam),
                                new PABlockLocation(player.getLocation()))
                        .toLocation().toVector();
            }

            if (vFlag != null && vLoc.distance(vFlag) < 2) {
                debug(this.arena, player, "flag found!");
                debug(this.arena, player, "vFlag: " + vFlag);

                if (aTeam.equals(pTeam.getName())) {
                    this.arena.msg(aPlayer.getPlayer(), MSG.ERROR_ERROR);
                    continue;
                }

                this.arena.broadcast(Language.parse(MSG.GOAL_SABOTAGE_IGNITED,
                        pTeam.colorizePlayer(player) + ChatColor.YELLOW,
                        team.getColoredName() + ChatColor.YELLOW));

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
                this.takeFlag(team.getName(), true, new PABlockLocation(block.getLocation()));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (StringUtils.isBlank(this.blockName) || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }
        if (block == null || block.getType() != Material.TNT) {
            return false;
        }

        return PVPArena.hasAdminPerms(player) || PVPArena.hasCreatePerms(player, this.arena);
    }

    private void commit(final Arena arena, final String sTeam) {
        if (arena.realEndRunner != null) {
            debug(arena, "[SABOTAGE] already ending");
            return;
        }
        debug(arena, "[SABOTAGE] committing end: " + sTeam);
        debug(arena, "win: " + false);

        final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        String winteam = sTeam;

        for (ArenaTeam team : arena.getTeams()) {
            if (!team.getName().equals(sTeam)) {
                continue;
            }
            for (ArenaPlayer ap : team.getTeamMembers()) {

                ap.addStatistic(arena.getName(), Type.LOSSES, 1);
                /*
				arena.tpPlayerToCoordName(ap.getPlayer(), "spectator");
				ap.setTelePass(false);*/

                ap.setStatus(PlayerStatus.LOST);
            }
        }
        for (ArenaTeam team : arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != PlayerStatus.FIGHT) {
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
                            Language.parse(MSG.TEAM_HAS_WON,
                                    arena.getTeam(winteam).getColor()
                                            + winteam + ChatColor.YELLOW),
                            "WINNER");
            arena.broadcast(Language.parse(MSG.TEAM_HAS_WON,
                    arena.getTeam(winteam).getColor() + winteam
                            + ChatColor.YELLOW));
        }

        new EndRunnable(arena, arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].equals(TNT)) {
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

        this.arena.msg(sender, MSG.GOAL_SABOTAGE_TOSETTNT, this.blockName, arenaTeam.getColoredName());
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[SABOTAGE] already ending");
            return;
        }
        debug(this.arena, "[SABOTAGE]");

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

        debug(this.arena, player, "trying to set a tnt");

        // command : /pa red_tnt1
        // location: red_tnt1:

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), this.blockName, this.blockTeamName);
        this.arena.msg(player, MSG.GOAL_SABOTAGE_SETTNT, this.blockName);

        PAA_Region.activeSelections.remove(player.getName());
        this.blockName = null;
        return true;
    }

    @Override
    public void disconnect(final ArenaPlayer aPlayer) {

        final String flag = this.getHeldFlagTeam(aPlayer.getPlayer());
        if (flag != null) {
            final ArenaTeam flagTeam = this.arena.getTeam(flag);
            this.getFlagMap().remove(flag);
            this.distributeFlag(aPlayer, flagTeam);
        }
    }

    private void distributeFlag(final ArenaPlayer player, final ArenaTeam team) {
        final Set<ArenaPlayer> players = team.getTeamMembers();

        int pos = new Random().nextInt(players.size());

        for (ArenaPlayer ap : players) {
            debug(this.arena, ap.getPlayer(), "distributing sabotage: " + ap.getName());
            if (ap.equals(player)) {
                continue;
            }
            if (--pos <= 1) {
                this.getFlagMap().put(team.getName(), ap.getName());
                ap.getPlayer().getInventory()
                        .addItem(new ItemStack(Material.FLINT_AND_STEEL, 1));
                this.arena.msg(ap.getPlayer(), MSG.GOAL_SABOTAGE_YOUTNT);
                return;
            }
        }
    }

    private String getHeldFlagTeam(final Player player) {
        if (this.getFlagMap().size() < 1) {
            return null;
        }

        debug(player, "getting held TNT of player {}", player);
        for (String sTeam : this.getFlagMap().keySet()) {
            debug(player, "team {}'s sabotage is carried by {}s hands", sTeam, this.getFlagMap().get(sTeam));
            if (player.getName().equals(this.getFlagMap().get(sTeam))) {
                return sTeam;
            }
        }
        return null;
    }

    private Map<String, String> getFlagMap() {
        if (this.teamFlags == null) {
            this.teamFlags = new HashMap<>();
        }
        return this.teamFlags;
    }

    private Map<ArenaTeam, TNTPrimed> getTNTmap() {
        if (this.teamTNTs == null) {
            this.teamTNTs = new HashMap<>();
        }
        return this.teamTNTs;
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
    public void initiate(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();
        this.takeFlag(team.getName(), false,
                SpawnManager.getBlockByExactName(this.arena, team.getName() + TNT));
        //TODO: allow multiple TNTs?
        if (!this.getFlagMap().containsKey(team.getName())) {
            debug(this.arena, player, "adding team " + team.getName());
            this.distributeFlag(null, team);
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final PADeathInfo event) {
        final String teamName = this.getHeldFlagTeam(player);
        final ArenaTeam team = this.arena.getTeam(teamName);
        if (teamName != null && team != null) {
            final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
            this.getFlagMap().remove(teamName);
            this.distributeFlag(aPlayer, team);
        }
    }

    @Override
    public void parseStart() {
        debug(this.arena, "initiating arena");
        this.getFlagMap().clear();
        for (ArenaTeam team : this.arena.getTeams()) {

            SpawnManager.getBlocksStartingWith(this.arena, TNT, team.getName()).forEach(blockLocation ->
                    this.takeFlag(team.getName(), false,
                            blockLocation));

            if (!this.getFlagMap().containsKey(team.getName())) {
                debug(this.arena, "adding team " + team.getName());
                this.distributeFlag(null, team);
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getFlagMap().clear();
        for (TNTPrimed t : this.getTNTmap().values()) {
            t.remove();
        }
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
    void takeFlag(final String teamName, final boolean take, final PABlockLocation paBlockLocation) {
        paBlockLocation.toLocation().getBlock()
                .setType(take ? Material.AIR : Material.TNT);
        if (take) {
            final TNTPrimed tnt = (TNTPrimed) Bukkit.getWorld(
                    paBlockLocation.getWorldName())
                    .spawnEntity(paBlockLocation.toLocation(), EntityType.PRIMED_TNT);
            this.getTNTmap().put(this.arena.getTeam(teamName), tnt);
        }
    }

    @Override
    public void unload(final Player player) {
        this.disconnect(ArenaPlayer.fromPlayer(player));
    }

    @Override
    public void checkExplode(final EntityExplodeEvent event) {
        if (event.getEntityType() != EntityType.PRIMED_TNT) {
            return;
        }

        final TNTPrimed tnt = (TNTPrimed) event.getEntity();

        for (ArenaTeam team : this.getTNTmap().keySet()) {
            if (tnt.getUniqueId().equals(this.getTNTmap().get(team).getUniqueId())) {
                event.setCancelled(true);
                tnt.remove();
                this.commit(this.arena, team.getName());
            }
        }

        final PABlockLocation tLoc = new PABlockLocation(event.getEntity().getLocation());

        for (String sTeam : this.arena.getTeamNames()) {
            final Set<PABlockLocation> locs = SpawnManager.getBlocksStartingWith(this.arena, TNT, sTeam);

            final PABlockLocation nearest = SpawnManager.getBlockNearest(locs, tLoc);

            if (nearest.getDistanceSquared(tLoc) < 4) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
