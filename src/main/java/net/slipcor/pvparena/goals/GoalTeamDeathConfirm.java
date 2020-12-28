package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "TeamDeathConfirm"
 * </pre>
 * <p/>
 * Arena Teams need to achieve kills. When a player dies, they drop an item that needs to be
 * collected. First team to collect the needed amount of those items wins!
 *
 * @author slipcor
 */

public class GoalTeamDeathConfirm extends ArenaGoal {
    public GoalTeamDeathConfirm() {
        super("TeamDeathConfirm");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    private static final int PRIORITY = 8;

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public PACheck checkEnd(final PACheck res) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        final int count = TeamManager.countActiveTeams(this.arena);

        if (count == 1) {
            res.setPriority(this, PRIORITY); // yep. only one team left. go!
        } else if (count == 0) {
            res.setError(this, MSG.ERROR_NOTEAMFOUND.toString());
        }

        return res;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        return this.checkForMissingTeamSpawn(list);
    }

    @Override
    public PACheck checkJoin(final CommandSender sender, final PACheck res, final String[] args) {
        if (res.getPriority() >= PRIORITY) {
            return res;
        }

        final int maxPlayers = this.arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS);
        final int maxTeamPlayers = this.arena.getArenaConfig().getInt(
                CFG.READY_MAXTEAMPLAYERS);

        if (maxPlayers > 0 && this.arena.getFighters().size() >= maxPlayers) {
            res.setError(this, Language.parse(this.arena, MSG.ERROR_JOIN_ARENA_FULL));
            return res;
        }

        if (args == null || args.length < 1) {
            return res;
        }

        if (!this.arena.isFreeForAll()) {
            final ArenaTeam team = this.arena.getTeam(args[0]);

            if (team != null && maxTeamPlayers > 0
                    && team.getTeamMembers().size() >= maxTeamPlayers) {
                res.setError(this, Language.parse(this.arena, MSG.ERROR_JOIN_TEAM_FULL, team.getName()));
                return res;
            }
        }

        res.setPriority(this, PRIORITY);
        return res;
    }

    @Override
    public PACheck checkPlayerDeath(final PACheck res, final Player player) {
        if (res.getPriority() <= PRIORITY && player.getKiller() != null
                && this.arena.hasPlayer(player.getKiller())) {
            res.setPriority(this, PRIORITY);
        }
        return res;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[TDC] already ending");
            return;
        }
        debug(this.arena, "[TDC]");
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);

        ArenaTeam aTeam = null;

        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == Status.FIGHT) {
                    aTeam = team;
                    break;
                }
            }
        }

        if (aTeam != null && !force) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(this.arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "END");
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(this.arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "WINNER");
            this.arena.broadcast(Language.parse(this.arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                    + aTeam.getName() + ChatColor.YELLOW));
        }

        if (ArenaModuleManager.commitEnd(this.arena, aTeam)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player respawnPlayer, final boolean doesRespawn,
                                  final String error, final PlayerDeathEvent event) {

        if (respawnPlayer.getKiller() == null) {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + respawnPlayer.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        } else {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + respawnPlayer.getName(),
                    "playerKill:" + respawnPlayer.getName() + ':' + respawnPlayer.getKiller().getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        }


        final ArenaTeam respawnTeam = ArenaPlayer.parsePlayer(respawnPlayer.getName()).getArenaTeam();

        this.drop(respawnPlayer, respawnTeam);

        if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            this.broadcastSimpleDeathMessage(respawnPlayer, event);
        }

        final List<ItemStack> returned;
        if (this.arena.getArenaConfig().getBoolean(
                CFG.PLAYER_DROPSINVENTORY)) {
            returned = InventoryManager.drop(respawnPlayer);
            event.getDrops().clear();
        } else {
            returned = new ArrayList<>(event.getDrops());
        }

        PACheck.handleRespawn(this.arena, ArenaPlayer.parsePlayer(respawnPlayer.getName()), returned);
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + this.arena.getArenaConfig().getInt(CFG.GOAL_TDC_LIVES));
    }

    private void drop(final Player player, final ArenaTeam team) {
        final ItemStack item = this.arena.getArenaConfig().getItems(CFG.GOAL_TDC_ITEM)[0];

        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(team.getColoredName());
        item.setItemMeta(meta);

        player.getWorld().dropItem(player.getLocation(), item);
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(this.arena.getArenaConfig()
                            .getInt(CFG.GOAL_TDC_LIVES) - (this.getLifeMap()
                            .getOrDefault(aPlayer.getArenaTeam().getName(), 0))));
        }
        return res;
    }

    @Override
    public boolean hasSpawn(final String string) {
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
        }
        return false;
    }

    @Override
    public void initate(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        this.updateLives(aPlayer.getArenaTeam(), this.arena.getArenaConfig()
                .getInt(CFG.GOAL_TDC_LIVES));
    }

    @Override
    public void onPlayerPickUp(final EntityPickupItemEvent event) {
        final ItemStack item = event.getItem().getItemStack();

        final ItemStack check = this.arena.getArenaConfig().getItems(CFG.GOAL_TDC_ITEM)[0];

        final ArenaPlayer player = ArenaPlayer.parsePlayer(event.getEntity().getName());

        if (item.getType() == check.getType() && item.hasItemMeta()) {
            for (final ArenaTeam team : this.arena.getTeams()) {
                if (item.getItemMeta().getDisplayName().equals(team.getColoredName())) {
                    // it IS an item !!!!

                    event.setCancelled(true);
                    event.getItem().remove();

                    if (team.equals(player.getArenaTeam())) {
                        // denied a kill
                        this.arena.broadcastExcept(event.getEntity(), Language.parse(this.arena, MSG.GOAL_TEAMDEATHCONFIRM_DENIED, player.toString()));
                        this.arena.msg(event.getEntity(), Language.parse(this.arena, MSG.GOAL_TEAMDEATHCONFIRM_YOUDENIED, player.toString()));
                    } else {
                        // scored a kill
                        this.arena.broadcastExcept(event.getEntity(), Language.parse(this.arena, MSG.GOAL_TEAMDEATHCONFIRM_SCORED, player.toString()));
                        this.arena.msg(event.getEntity(), Language.parse(this.arena, MSG.GOAL_TEAMDEATHCONFIRM_YOUSCORED, player.toString()));
                        this.reduceLives(this.arena, team);
                    }
                    return;
                }
            }
        }
    }

    /**
     * @param arena the arena this is happening in
     * @param team  the killing team
     * @return true if the player should not respawn but be removed
     */
    private boolean reduceLives(final Arena arena, final ArenaTeam team) {
        final int iLives = this.getLifeMap().get(team.getName());

        if (iLives <= 1) {
            for (final ArenaTeam otherTeam : arena.getTeams()) {
                if (otherTeam.equals(team)) {
                    continue;
                }
                this.getLifeMap().remove(otherTeam.getName());
                for (final ArenaPlayer ap : otherTeam.getTeamMembers()) {
                    if (ap.getStatus() == Status.FIGHT) {
                        ap.setStatus(Status.LOST);
                        /*
						arena.removePlayer(ap.get(), CFG.TP_LOSE.toString(),
								true, false);*/
                    }
                }
            }
            PACheck.handleEnd(arena, false);
            return true;
        }
        arena.broadcast(Language.parse(arena, MSG.GOAL_TEAMDEATHCONFIRM_REMAINING, String.valueOf(iLives - 1), team.getColoredName()));

        this.getLifeMap().put(team.getName(), iLives - 1);
        return false;
    }

    @Override
    public void reset(final boolean force) {
        this.getLifeMap().clear();
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
    public void parseStart() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            this.updateLives(team, this.arena.getArenaConfig().getInt(CFG.GOAL_TDC_LIVES));
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaTeam team : this.arena.getTeams()) {
            double score = this.arena.getArenaConfig().getInt(CFG.GOAL_TDC_LIVES)
                    - (this.getLifeMap().containsKey(team.getName()) ? this.getLifeMap().get(team
                    .getName()) : 0);
            if (scores.containsKey(team.getName())) {
                scores.put(team.getName(), scores.get(team.getName()) + score);
            } else {
                scores.put(team.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final Player player) {
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.parsePlayer(player.getName()));
        }
    }
}