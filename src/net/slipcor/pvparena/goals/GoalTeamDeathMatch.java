package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * Arena Goal class "TeamDeathMatch"
 * </pre>
 * <p/>
 * The second Arena Goal. Arena Teams have lives. When every life is lost, the
 * team is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalTeamDeathMatch extends ArenaGoal {
    public GoalTeamDeathMatch() {
        super("TeamDeathMatch");
        debug = new Debug(104);
    }

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    private static final int PRIORITY = 5;

    @Override
    public boolean allowsJoinInBattle() {
        return arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public PACheck checkEnd(final PACheck res) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        final int count = TeamManager.countActiveTeams(arena);

        if (count == 1) {
            res.setPriority(this, PRIORITY); // yep. only one team left. go!
        } else if (count == 0) {
            res.setError(this, MSG.ERROR_NOTEAMFOUND.toString());
        }

        return res;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        return checkForMissingTeamSpawn(list);
    }

    @Override
    public PACheck checkJoin(final CommandSender sender, final PACheck res, final String[] args) {
        if (res.getPriority() >= PRIORITY) {
            return res;
        }

        final int maxPlayers = arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS);
        final int maxTeamPlayers = arena.getArenaConfig().getInt(
                CFG.READY_MAXTEAMPLAYERS);

        if (maxPlayers > 0 && arena.getFighters().size() >= maxPlayers) {
            res.setError(this, Language.parse(arena, MSG.ERROR_JOIN_ARENA_FULL));
            return res;
        }

        if (args == null || args.length < 1) {
            return res;
        }

        if (!arena.isFreeForAll()) {
            final ArenaTeam team = arena.getTeam(args[0]);

            if (team != null && maxTeamPlayers > 0
                    && team.getTeamMembers().size() >= maxTeamPlayers) {
                res.setError(this, Language.parse(arena, MSG.ERROR_JOIN_TEAM_FULL, team.getName()));
                return res;
            }
        }

        res.setPriority(this, PRIORITY);
        return res;
    }

    @Override
    public PACheck checkPlayerDeath(final PACheck res, final Player player) {
        if (res.getPriority() <= PRIORITY && player.getKiller() != null
                && arena.hasPlayer(player.getKiller())) {
            res.setPriority(this, PRIORITY);
        }
        return res;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (arena.realEndRunner != null) {
            arena.getDebugger().i("[TDM] already ending");
            return;
        }
        arena.getDebugger().i("[TDM]");
        final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);

        ArenaTeam aTeam = null;

        for (final ArenaTeam team : arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == Status.FIGHT) {
                    aTeam = team;
                    break;
                }
            }
        }

        if (aTeam != null && !force) {
            ArenaModuleManager.announce(
                    arena,
                    Language.parse(arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "END");
            ArenaModuleManager.announce(
                    arena,
                    Language.parse(arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                            + aTeam.getName() + ChatColor.YELLOW), "WINNER");
            arena.broadcast(Language.parse(arena, MSG.TEAM_HAS_WON, aTeam.getColor()
                    + aTeam.getName() + ChatColor.YELLOW));
        }

        if (ArenaModuleManager.commitEnd(arena, aTeam)) {
            return;
        }
        new EndRunnable(arena, arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player respawnPlayer, final boolean doesRespawn,
                                  final String error, final PlayerDeathEvent event) {

        Player killer = respawnPlayer.getKiller();

        if (respawnPlayer.getKiller() == null ||
                respawnPlayer.getPlayer().equals(respawnPlayer.getPlayer().getKiller())) {
            if (!arena.getArenaConfig().getBoolean(CFG.GOAL_TDM_SUICIDESCORE)) {
                final List<ItemStack> returned;

                if (arena.getArenaConfig().getBoolean(
                        CFG.PLAYER_DROPSINVENTORY)) {
                    returned = InventoryManager.drop(respawnPlayer);
                    event.getDrops().clear();
                } else {
                    returned = event.getDrops();
                }

                PACheck.handleRespawn(arena,
                        ArenaPlayer.parsePlayer(respawnPlayer.getName()), returned);
                if (doesRespawn) {
                    final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "doesRespawn", "playerDeath:" + respawnPlayer.getName());
                    Bukkit.getPluginManager().callEvent(gEvent);
                } else {
                    final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "playerDeath:" + respawnPlayer.getName());
                    Bukkit.getPluginManager().callEvent(gEvent);
                }


                return;
            }

            if (killer == null) {
                killer = respawnPlayer;
            }
        }

        final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "playerDeath:" + respawnPlayer.getName(),
                "playerKill:" + respawnPlayer.getName() + ':' + killer.getName());
        Bukkit.getPluginManager().callEvent(gEvent);

        final ArenaTeam respawnTeam = ArenaPlayer
                .parsePlayer(respawnPlayer.getName()).getArenaTeam();
        final ArenaTeam killerTeam = ArenaPlayer.parsePlayer(
                killer.getName()).getArenaTeam();

        if (killerTeam.equals(respawnTeam)) {
            for (ArenaTeam newKillerTeam : arena.getTeams()) {
                if (!newKillerTeam.equals(respawnTeam) && reduceLives(arena, newKillerTeam, respawnPlayer, event)) {
                    if (arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {

                        final List<ItemStack> returned;

                        if (arena.getArenaConfig().getBoolean(
                                CFG.PLAYER_DROPSINVENTORY)) {
                            returned = InventoryManager.drop(respawnPlayer);
                            event.getDrops().clear();
                        } else {
                            returned = event.getDrops();
                        }

                        PACheck.handleRespawn(arena,
                                ArenaPlayer.parsePlayer(respawnPlayer.getName()), returned);
                        ArenaPlayer.parsePlayer(respawnPlayer.getName()).setStatus(Status.LOST);

                    }
                    return;

                }
            }
        } else if (!killerTeam.equals(respawnTeam) && reduceLives(arena, killerTeam, respawnPlayer, event)) {
            if (arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {

                final List<ItemStack> returned;

                if (arena.getArenaConfig().getBoolean(
                        CFG.PLAYER_DROPSINVENTORY)) {
                    returned = InventoryManager.drop(respawnPlayer);
                    event.getDrops().clear();
                } else {
                    returned = event.getDrops();
                }

                PACheck.handleRespawn(arena,
                        ArenaPlayer.parsePlayer(respawnPlayer.getName()), returned);
                ArenaPlayer.parsePlayer(respawnPlayer.getName()).setStatus(Status.LOST);
            }
            return;


        }

        if (this.getLifeMap().get(killerTeam.getName()) != null) {
            if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (killerTeam.equals(respawnTeam) || !arena.getArenaConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastSimpleDeathMessage(respawnPlayer, event);
                } else {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_TEAM_FRAGS, respawnPlayer, event, this.getLifeMap().get(killerTeam.getName()));
                }
            }
            final List<ItemStack> returned;

            if (this.arena.getArenaConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
                returned = InventoryManager.drop(respawnPlayer);
                event.getDrops().clear();
            } else {
                returned = new ArrayList<>(event.getDrops());
            }

            PACheck.handleRespawn(this.arena, ArenaPlayer.parsePlayer(respawnPlayer.getName()), returned);
        }

    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (config.get("flagColors") == null) {
            arena.getDebugger().i("no flagheads defined, adding white and black!");
            config.addDefault("flagColors.red", "WHITE");
            config.addDefault("flagColors.blue", "BLACK");
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + arena.getArenaConfig().getInt(CFG.GOAL_TDM_LIVES));
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(arena.getArenaConfig()
                            .getInt(CFG.GOAL_TDM_LIVES) - (getLifeMap()
                            .getOrDefault(aPlayer.getArenaTeam().getName(), 0))));
        }
        return res;
    }

    @Override
    public boolean hasSpawn(final String string) {
        for (final String teamName : arena.getTeamNames()) {
            if (string.toLowerCase().startsWith(
                    teamName.toLowerCase() + "spawn")) {
                return true;
            }
            if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                for (final ArenaClass aClass : arena.getClasses()) {
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
        updateLives(aPlayer.getArenaTeam(), arena.getArenaConfig()
                .getInt(CFG.GOAL_TDM_LIVES));
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    /**
     * @param arena the arena this is happening in
     * @param team  the killing team
     * @return true if the player should not respawn but be removed
     */
    private boolean reduceLives(final Arena arena, final ArenaTeam team, final Player respawnPlayer, final EntityDeathEvent event) {
        final int iLives = getLifeMap().get(team.getName());

        if (iLives <= 1) {
            for (final ArenaTeam otherTeam : arena.getTeams()) {
                if (otherTeam.equals(team)) {
                    continue;
                }
                getLifeMap().remove(otherTeam.getName());
                for (final ArenaPlayer ap : otherTeam.getTeamMembers()) {
                    if (ap.getStatus() == Status.FIGHT) {
                        ap.setStatus(Status.LOST);
                        /*
						arena.removePlayer(ap.get(), CFG.TP_LOSE.toString(),
								true, false);*/
                    }
                }
            }
            arena.broadcast(Language.parse(arena,
                    MSG.FIGHT_KILLED_BY,
                    team.colorizePlayer(respawnPlayer)
                            + ChatColor.YELLOW, arena.parseDeathCause(
                            respawnPlayer, event.getEntity()
                                    .getLastDamageCause().getCause(), event
                                    .getEntity().getKiller())));
            PACheck.handleEnd(arena, false);
            return true;
        }

        getLifeMap().put(team.getName(), iLives - 1);
        return false;
    }

    @Override
    public void reset(final boolean force) {
        getLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (arena.isFreeForAll()) {
            return;
        }

        if (config.get("teams.free") != null) {
            config.set("teams", null);
        }
        if (config.get("teams") == null) {
            arena.getDebugger().i("no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
        if (arena.getArenaConfig().getBoolean(CFG.GOAL_FLAGS_WOOLFLAGHEAD)
                && config.get("flagColors") == null) {
            arena.getDebugger().i("no flagheads defined, adding white and black!");
            config.addDefault("flagColors.red", "WHITE");
            config.addDefault("flagColors.blue", "BLACK");
        }
    }

    @Override
    public void parseStart() {
        for (final ArenaTeam team : arena.getTeams()) {
            updateLives(team, arena.getArenaConfig().getInt(CFG.GOAL_TDM_LIVES));
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaTeam team : arena.getTeams()) {
            double score = arena.getArenaConfig().getInt(CFG.GOAL_TDM_LIVES)
                    - (getLifeMap().containsKey(team.getName()) ? getLifeMap().get(team
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
        if (allowsJoinInBattle()) {
            arena.hasNotPlayed(ArenaPlayer.parsePlayer(player.getName()));
        }
    }
}
