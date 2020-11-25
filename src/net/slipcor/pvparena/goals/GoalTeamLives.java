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
import net.slipcor.pvparena.listeners.PlayerListener;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * Arena Goal class "TeamLives"
 * </pre>
 * <p/>
 * The second Arena Goal. Arena Teams have lives. When every life is lost, the
 * team is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalTeamLives extends ArenaGoal {
    public GoalTeamLives() {
        super("TeamLives");
        debug = new Debug(105);
    }

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    private static final int PRIORITY = 4;

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
        if (res.getPriority() <= PRIORITY) {
            res.setPriority(this, PRIORITY);

            final ArenaTeam respawnTeam = ArenaPlayer
                    .parsePlayer(player.getName()).getArenaTeam();

            if (this.getTeamLives(respawnTeam) != null) {
                return res;
            }
            if (this.getTeamLives(respawnTeam) <= 1) {
                res.setError(this, "0");
            }

        }
        return res;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (arena.realEndRunner != null) {
            arena.getDebugger().i("[TEAMS] already ending");
            return;
        }
        arena.getDebugger().i("[TEAMS]");
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
        if (doesRespawn) {
            final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "doesRespawn", "playerDeath:" + respawnPlayer.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        } else {
            final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "playerDeath:" + respawnPlayer.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        }

        final ArenaTeam respawnTeam = ArenaPlayer
                .parsePlayer(respawnPlayer.getName()).getArenaTeam();
        reduceLives(arena, respawnTeam);

        if (this.getTeamLives(respawnTeam) != null) {
            if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_TEAM, respawnPlayer, event,
                            this.getTeamLives(respawnTeam));
                } else {
                    this.broadcastSimpleDeathMessage(respawnPlayer, event);
                }
            }

            final List<ItemStack> returned;

            if (arena.getArenaConfig().getBoolean(
                    CFG.PLAYER_DROPSINVENTORY)) {
                returned = InventoryManager.drop(respawnPlayer);
                event.getDrops().clear();
            } else {
                returned = new ArrayList<>(event.getDrops());
            }

            PACheck.handleRespawn(arena,
                    ArenaPlayer.parsePlayer(respawnPlayer.getName()), returned);

        } else if (arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
            arena.getDebugger().i("faking player death", respawnPlayer);
            ArenaPlayer.parsePlayer(respawnPlayer.getName()).setStatus(Status.LOST);
            PlayerListener.finallyKillPlayer(arena, respawnPlayer, event);
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
                + arena.getArenaConfig().getInt(CFG.GOAL_TLIVES_LIVES));
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(getLifeMap().getOrDefault(aPlayer.getArenaTeam().getName(), 0))
            );
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
                .getInt(CFG.GOAL_TLIVES_LIVES));
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    private void reduceLives(final Arena arena, final ArenaTeam team) {
        final int iLives = this.getTeamLives(team);

        if (iLives <= 1) {
            getLifeMap().remove(team.getName());
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == Status.FIGHT) {
                    ap.setStatus(Status.LOST);
                    /*
					arena.removePlayer(ap.get(), CFG.TP_LOSE.toString(), true,
							false);*/
                }
            }
            PACheck.handleEnd(arena, false);
            return;
        }

        getLifeMap().put(team.getName(), iLives - 1);
    }

    @Override
    public void reset(final boolean force) {
        getLifeMap().clear();
    }

    @Override
    public void parseStart() {
        for (final ArenaTeam team : arena.getTeams()) {
            updateLives(team, arena.getArenaConfig().getInt(CFG.GOAL_TLIVES_LIVES));
        }
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
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaTeam team : arena.getTeams()) {
            double score = this.getLifeMap().containsKey(team.getName()) ? this.getTeamLives(team) : 0;
            if (scores.containsKey(team.getName())) {
                scores.put(team.getName(), scores.get(team.getName()) + score);
            } else {
                scores.put(team.getName(), score);
            }
        }

        return scores;
    }

    private Integer getTeamLives(ArenaTeam respawnTeam) {
        return this.getLifeMap().get(respawnTeam.getName());
    }
}
