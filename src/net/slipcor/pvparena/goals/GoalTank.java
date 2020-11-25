package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.events.PATeamChangeEvent;
import net.slipcor.pvparena.listeners.PlayerListener;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * <pre>
 * Arena Goal class "PlayerLives"
 * </pre>
 * <p/>
 * The first Arena Goal. Players have lives. When every life is lost, the player
 * is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalTank extends ArenaGoal {
    public GoalTank() {
        super("Tank");
        debug = new Debug(108);
    }

    private static final Map<Arena, String> tanks = new HashMap<>();

    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    private static final int PRIORITY = 8;

    @Override
    public PACheck checkEnd(final PACheck res) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        final int count = getLifeMap().size();

        if (count <= 1
                || ArenaPlayer.parsePlayer(tanks.get(arena)).getStatus() != Status.FIGHT) {
            res.setPriority(this, PRIORITY); // yep. only one player left. go!
        }
        if (count == 0) {
            res.setError(this, "");
        }

        return res;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        if (!arena.isFreeForAll()) {
            return null; // teams are handled somewhere else
        }

        if (!list.contains("tank")) {
            return "tank";
        }

        return checkForMissingSpawn(list);
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

            if (!getLifeMap().containsKey(player.getName())) {
                return res;
            }
            final int iLives = getLifeMap().get(player.getName());
            arena.getDebugger().i("lives before death: " + iLives, player);
            if (iLives <= 1 || tanks.get(arena).equals(player.getName())) {
                res.setError(this, "0");
            }
        }
        return res;
    }

    @Override
    public PACheck checkStart(final PACheck res) {
        if (res.getPriority() < PRIORITY) {
            res.setPriority(this, PRIORITY);
        }
        return res;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (endRunner != null) {
            return;
        }
        if (arena.realEndRunner != null) {
            arena.getDebugger().i("[TANK] already ending");
            return;
        }
        final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        for (final ArenaTeam team : arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != Status.FIGHT) {
                    continue;
                }
                if (tanks.containsValue(ap.getName())) {
                    ArenaModuleManager.announce(arena,
                            Language.parse(arena, MSG.GOAL_TANK_TANKWON, ap.getName()), "END");
                    ArenaModuleManager.announce(arena,
                            Language.parse(arena, MSG.GOAL_TANK_TANKWON, ap.getName()), "WINNER");

                    arena.broadcast(Language.parse(arena, MSG.GOAL_TANK_TANKWON, ap.getName()));
                } else {

                    ArenaModuleManager.announce(arena,
                            Language.parse(arena, MSG.GOAL_TANK_TANKDOWN), "END");
                    ArenaModuleManager.announce(arena,
                            Language.parse(arena, MSG.GOAL_TANK_TANKDOWN), "LOSER");

                    arena.broadcast(Language.parse(arena, MSG.GOAL_TANK_TANKDOWN));
                }
            }

            if (ArenaModuleManager.commitEnd(arena, team)) {
                return;
            }
        }

        endRunner = new EndRunnable(arena, arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn,
                                  final String error, final PlayerDeathEvent event) {
        if (!getLifeMap().containsKey(player.getName())) {
            return;
        }
        int iLives = getLifeMap().get(player.getName());
        arena.getDebugger().i("lives before death: " + iLives, player);
        if (iLives <= 1 || tanks.get(arena).equals(player.getName())) {

            if (tanks.get(arena).equals(player.getName())) {

                final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "tank", "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
            } else if (doesRespawn) {

                final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "doesRespawn", "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
            } else {

                final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
            }


            getLifeMap().remove(player.getName());
            if (arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                arena.getDebugger().i("faking player death", player);
                PlayerListener.finallyKillPlayer(arena, player, event);
            }

            ArenaPlayer.parsePlayer(player.getName()).setStatus(Status.LOST);
            // player died => commit death!
            PACheck.handleEnd(arena, false);
        } else {
            final PAGoalEvent gEvent = new PAGoalEvent(arena, this, "doesRespawn", "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
            iLives--;
            getLifeMap().put(player.getName(), iLives);

            if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, player, event, iLives);
            }
            final List<ItemStack> returned;

            if (this.arena.getArenaConfig().getBoolean(
                    CFG.PLAYER_DROPSINVENTORY)) {
                returned = InventoryManager.drop(player);
                event.getDrops().clear();
            } else {
                returned = new ArrayList<>(event.getDrops());
            }

            PACheck.handleRespawn(this.arena, ArenaPlayer.parsePlayer(player.getName()), returned);
        }
    }

    @Override
    public void commitStart() {
        parseStart(); // hack the team in before spawning, derp!
        for (final ArenaTeam team : arena.getTeams()) {
            SpawnManager.distribute(arena, team);
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + arena.getArenaConfig().getInt(CFG.GOAL_TANK_LIVES));
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(getLifeMap().getOrDefault(aPlayer.getName(), 0))
            );
        }
        return res;
    }

    @Override
    public boolean hasSpawn(final String string) {


        if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            for (final ArenaClass aClass : arena.getClasses()) {
                if (string.toLowerCase().startsWith(
                        aClass.getName().toLowerCase() + "spawn")) {
                    return true;
                }
            }
        }

        return arena.isFreeForAll() && string.toLowerCase()
                .startsWith("spawn") || "tank".equals(string);
    }

    @Override
    public void initate(final Player player) {
        getLifeMap().put(player.getName(),
                arena.getArenaConfig().getInt(CFG.GOAL_TANK_LIVES));
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.instance.getLogger().warning(
                    getName() + ": player NULL");
            return;
        }
        if (getLifeMap().containsKey(player.getName())) {
            getLifeMap().remove(player.getName());
        }
    }

    @Override
    public void parseStart() {
        if (arena.getTeam("tank") != null) {
            return;
        }
        ArenaPlayer tank = null;
        final Random random = new Random();
        for (final ArenaTeam team : arena.getTeams()) {
            int pos = random.nextInt(team.getTeamMembers().size());
            arena.getDebugger().i("team " + team.getName() + " random " + pos);
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                arena.getDebugger().i("#" + pos + ": " + ap, ap.getName());
                if (pos-- == 0) {
                    tank = ap;
                }
                getLifeMap().put(ap.getName(),
                        arena.getArenaConfig().getInt(CFG.GOAL_TANK_LIVES));
            }
        }
        final ArenaTeam tankTeam = new ArenaTeam("tank", "PINK");


        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getTeamMembers().contains(tank)) {
                final PATeamChangeEvent tcEvent = new PATeamChangeEvent(arena, tank.get(), team, tankTeam);
                Bukkit.getPluginManager().callEvent(tcEvent);
                arena.updateScoreboardTeam(tank.get(), team, tankTeam);
                team.remove(tank);
            }
        }
        tankTeam.add(tank);
        tanks.put(arena, tank.getName());

        final ArenaClass tankClass = arena.getClass("%tank%");
        if (tankClass != null) {
            tank.setArenaClass(tankClass);
            InventoryManager.clearInventory(tank.get());
            tankClass.equip(tank.get());
            for (final ArenaModule mod : arena.getMods()) {
                mod.parseRespawn(tank.get(), tankTeam, DamageCause.CUSTOM,
                        tank.get());
            }
        }

        arena.broadcast(Language.parse(arena, MSG.GOAL_TANK_TANKMODE, tank.getName()));

        final Set<PASpawn> spawns = new HashSet<>();
        spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, "tank"));

        int pos = spawns.size();

        for (final PASpawn spawn : spawns) {
            if (--pos < 0) {
                this.arena.tpPlayerToCoordName(tank, spawn.getName());
                break;
            }
        }

        arena.getTeams().add(tankTeam);
    }

    @Override
    public void reset(final boolean force) {
        endRunner = null;
        getLifeMap().clear();
        tanks.remove(arena);
        arena.getTeams().remove(arena.getTeam("tank"));
    }

    @Override
    public void setPlayerLives(final int value) {
        final Set<String> plrs = new HashSet<>();

        for (final String name : getLifeMap().keySet()) {
            plrs.add(name);
        }

        for (final String s : plrs) {
            getLifeMap().put(s, value);
        }
    }

    @Override
    public void setPlayerLives(final ArenaPlayer aPlayer, final int value) {
        getLifeMap().put(aPlayer.getName(), value);
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaPlayer ap : arena.getFighters()) {
            double score = getLifeMap().containsKey(ap.getName()) ? getLifeMap().get(ap.getName())
                    : 0;
            if (tanks.containsValue(ap.getName())) {
                score *= arena.getFighters().size();
            }
            if (scores.containsKey(ap.getName())) {
                scores.put(ap.getName(), scores.get(ap.getName()) + score);
            } else {
                scores.put(ap.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final Player player) {
        getLifeMap().remove(player.getName());
    }
}
