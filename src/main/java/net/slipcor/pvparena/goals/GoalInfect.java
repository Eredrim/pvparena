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
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
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
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "Infect"
 * </pre>
 * <p/>
 * Infected players kill ppl to enhance their team. Configurable lives
 *
 * @author slipcor
 */

public class GoalInfect extends ArenaGoal {
    public GoalInfect() {
        super("Infect");
    }
// BREAK, PLACE, TNT, TNTBREAK, DROP, INVENTORY, PICKUP, CRAFT;
    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    private static final int PRIORITY = 9;

    @Override
    public PACheck checkEnd(final PACheck res) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        final int count = this.getLifeMap().size();

        if (count <= 1
                || this.anyTeamEmpty()) {
            res.setPriority(this, PRIORITY); // yep. only one player left. go!
        }
        if (count == 0) {
            res.setError(this, "");
        }

        return res;
    }

    private boolean anyTeamEmpty() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            boolean bbreak = false;
            for (final ArenaPlayer player : team.getTeamMembers()) {
                if (player.getStatus() == Status.FIGHT) {
                    bbreak = true;
                    break;
                }
            }
            if (bbreak) {
                continue;
            }
            debug(this.arena, "team empty: " + team.getName());
            return true;
        }
        return false;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        if (!this.arena.isFreeForAll()) {
            return null; // teams are handled somewhere else
        }

        boolean infected = false;

        int count = 0;
        for (final String s : list) {
            if (s.startsWith("infected")) {
                infected = true;
            }
            if (s.startsWith("spawn")) {
                count++;
            }
        }
        if (!infected) {
            return "infected";
        }
        return count > 3 ? null : "need more spawns! (" + count + "/4)";
    }
    @Override
    public PACheck checkCommand(final PACheck res, final String string) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        if ("getprotect".equalsIgnoreCase(string)
                || "setprotect".equalsIgnoreCase(string)) {
            res.setPriority(this, PRIORITY);
        }

        return res;
    }

    @Override
    public PACheck checkBreak(PACheck result, Arena arena, BlockBreakEvent event) {
        ArenaPlayer ap = ArenaPlayer.parsePlayer(event.getPlayer().getName());
        if (arena.equals(ap.getArena()) && ap.getStatus() == Status.FIGHT) {
            if ("infected".equals(ap.getArenaTeam().getName())) {
                if (ArenaPlayer.PlayerPrevention.has(
                        arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.BREAK
                )) {
                    event.setCancelled(true);
                    arena.msg(event.getPlayer(), Language.parse(arena, MSG.PLAYER_PREVENTED_BREAK));
                    result.setError(this, "BREAK not allowed");
                } else if (event.getBlock().getType() == Material.TNT &&
                        ArenaPlayer.PlayerPrevention.has(
                                arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.TNTBREAK
                        )) {
                    event.setCancelled(true);
                    arena.msg(event.getPlayer(), Language.parse(arena, MSG.PLAYER_PREVENTED_TNTBREAK));
                    result.setError(this, "TNTBREAK not allowed");
                }
            }
        }
        return result;
    }

    @Override
    public PACheck checkCraft(PACheck result, Arena arena, CraftItemEvent event) {
        ArenaPlayer ap = ArenaPlayer.parsePlayer(((Player) event.getInventory().getHolder()).getName());
        if (arena.equals(ap.getArena()) && ap.getStatus() == Status.FIGHT) {
            if ("infected".equals(ap.getArenaTeam().getName())) {
                if (ArenaPlayer.PlayerPrevention.has(
                        arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.CRAFT
                )) {
                    event.setCancelled(true);
                    arena.msg(event.getWhoClicked(), Language.parse(arena, MSG.PLAYER_PREVENTED_CRAFT));
                    result.setError(this, "CRAFT not allowed");
                }
            }
        }
        return result;
    }

    @Override
    public PACheck checkDrop(PACheck result, Arena arena, PlayerDropItemEvent event) {
        ArenaPlayer ap = ArenaPlayer.parsePlayer(event.getPlayer().getName());
        if (arena.equals(ap.getArena()) && ap.getStatus() == Status.FIGHT) {
            if ("infected".equals(ap.getArenaTeam().getName())) {
                if (ArenaPlayer.PlayerPrevention.has(
                        arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.DROP
                )) {
                    event.setCancelled(true);
                    arena.msg(event.getPlayer(), Language.parse(arena, MSG.PLAYER_PREVENTED_DROP));
                    result.setError(this, "DROP not allowed");
                }
            }
        }
        return result;
    }

    @Override
    public PACheck checkInventory(PACheck result, Arena arena, InventoryClickEvent event) {
        ArenaPlayer ap = ArenaPlayer.parsePlayer(event.getWhoClicked().getName());
        if (arena.equals(ap.getArena()) && ap.getStatus() == Status.FIGHT) {
            if ("infected".equals(ap.getArenaTeam().getName())) {
                if (ArenaPlayer.PlayerPrevention.has(
                        arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.INVENTORY
                )) {
                    event.setCancelled(true);
                    event.getWhoClicked().closeInventory();
                    arena.msg(event.getWhoClicked(), Language.parse(arena, MSG.PLAYER_PREVENTED_INVENTORY));
                    result.setError(this, "INVENTORY not allowed");
                }
            }
        }
        return result;
    }

    @Override
    public PACheck checkPickup(PACheck result, Arena arena, EntityPickupItemEvent event) {
        ArenaPlayer ap = ArenaPlayer.parsePlayer(event.getEntity().getName());
        if (arena.equals(ap.getArena()) && ap.getStatus() == Status.FIGHT) {
            if ("infected".equals(ap.getArenaTeam().getName())) {
                if (ArenaPlayer.PlayerPrevention.has(
                        arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.PICKUP
                )) {
                    event.setCancelled(true);
                    result.setError(this, "PICKUP not allowed");
                }
            }
        }
        return result;
    }

    @Override
    public PACheck checkPlace(PACheck result, Arena arena, BlockPlaceEvent event) {
        ArenaPlayer ap = ArenaPlayer.parsePlayer(event.getPlayer().getName());
        if (arena.equals(ap.getArena()) && ap.getStatus() == Status.FIGHT) {
            if ("infected".equals(ap.getArenaTeam().getName())) {
                if (ArenaPlayer.PlayerPrevention.has(
                        arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.PLACE
                )) {
                    event.setCancelled(true);
                    arena.msg(event.getPlayer(), Language.parse(arena, MSG.PLAYER_PREVENTED_PLACE));
                    result.setError(this, "PLACE not allowed");
                } else if (event.getBlock().getType() == Material.TNT &&
                        ArenaPlayer.PlayerPrevention.has(
                                arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), ArenaPlayer.PlayerPrevention.TNT
                        )) {
                    event.setCancelled(true);
                    arena.msg(event.getPlayer(), Language.parse(arena, MSG.PLAYER_PREVENTED_TNT));
                    result.setError(this, "TNT not allowed");
                }
            }
        }
        return result;
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
        if (res.getPriority() <= PRIORITY) {
            res.setPriority(this, PRIORITY);

            if (!this.getLifeMap().containsKey(player.getName())) {
                return res;
            }
            final int iLives = this.getLifeMap().get(player.getName());
            debug(this.arena, player, "lives before death: " + iLives);
            if (iLives <= 1 && "infected".equals(ArenaPlayer.parsePlayer(player.getName()).getArenaTeam().getName())) {
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
    public void commitCommand(final CommandSender sender, final String[] args) {

        int value = this.arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS);

        if ("getprotect".equalsIgnoreCase(args[0])) {
            List<String> values = new ArrayList<>();


            for (ArenaPlayer.PlayerPrevention pp : ArenaPlayer.PlayerPrevention.values()) {
                if (pp == null) {
                    continue;
                }
                values.add((ArenaPlayer.PlayerPrevention.has(value, pp) ?
                        ChatColor.GREEN.toString() : ChatColor.RED.toString()) + pp.name());
            }
            this.arena.msg(sender, Language.parse(this.arena, MSG.GOAL_INFECTED_IPROTECT, StringParser.joinList(values, (ChatColor.WHITE + ", "))));

        } else if ("setprotect".equalsIgnoreCase(args[0])) {
            // setprotect [value] {true|false}
            if (args.length < 2) {
                this.arena.msg(
                        sender,
                        Language.parse(this.arena, MSG.ERROR_INVALID_ARGUMENT_COUNT,
                                String.valueOf(args.length), "2|3"));
                return;
            }

            try {
                final ArenaPlayer.PlayerPrevention pp = ArenaPlayer.PlayerPrevention.valueOf(args[1].toUpperCase());
                final boolean has = ArenaPlayer.PlayerPrevention.has(value, pp);

                debug(this.arena, "plain value: " + value);
                debug(this.arena, "checked: " + pp.name());
                debug(this.arena, "has: " + has);

                boolean future = !has;

                if (args.length > 2) {
                    if (StringParser.isNegativeValue(args[2])) {
                        future = false;
                    } else if (StringParser.isPositiveValue(args[2])) {
                        future = true;
                    }
                }

                if (future) {
                    value = value | (int) Math.pow(2, pp.ordinal());
                    this.arena.msg(
                            sender,
                            Language.parse(this.arena, MSG.GOAL_INFECTED_IPROTECT_SET,
                                    pp.name(), ChatColor.GREEN + "true") + ChatColor.YELLOW);
                } else {
                    value = value ^ (int) Math.pow(2, pp.ordinal());
                    this.arena.msg(
                            sender,
                            Language.parse(this.arena, MSG.GOAL_INFECTED_IPROTECT_SET,
                                    pp.name(), ChatColor.RED + "false") + ChatColor.YELLOW);
                }
                this.arena.getArenaConfig().set(CFG.GOAL_INFECTED_PPROTECTS, value);
            } catch (final Exception e) {
                List<String> values = new ArrayList<>();


                for (ArenaPlayer.PlayerPrevention pp : ArenaPlayer.PlayerPrevention.values()) {
                    values.add(pp.name());
                }
                this.arena.msg(sender,
                        Language.parse(this.arena, MSG.ERROR_ARGUMENT, args[1], StringParser.joinList(values, ", ")));
                return;
            }
            this.arena.getArenaConfig().save();

        }
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[INFECT] already ending");
            return;
        }
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);

        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != Status.FIGHT) {
                    continue;
                }
                if ("infected".equals(ap.getArenaTeam().getName())) {
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_INFECTED_WON), "END");

                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_INFECTED_WON), "WINNER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_INFECTED_WON));
                    break;
                } else {

                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_INFECTED_LOST), "END");
                    // String tank = tanks.get(arena);
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_INFECTED_LOST), "LOSER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_INFECTED_LOST));
                    break;
                }
            }

            if (ArenaModuleManager.commitEnd(this.arena, team)) {
                return;
            }
        }

        this.endRunner = new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn,
                                  final String error, final PlayerDeathEvent event) {
        if (!this.getLifeMap().containsKey(player.getName())) {
            return;
        }
        int iLives = this.getLifeMap().get(player.getName());
        debug(this.arena, player, "lives before death: " + iLives);
        ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());
        if (iLives <= 1 || "infected".equals(aPlayer.getArenaTeam().getName())) {
            if (iLives <= 1 && "infected".equals(aPlayer.getArenaTeam().getName())) {

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "infected", "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
                aPlayer.setStatus(Status.LOST);
                // kill, remove!
                this.getLifeMap().remove(player.getName());
                if (this.arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                    debug(this.arena, player, "faking player death");
                    PlayerListener.finallyKillPlayer(this.arena, player, event);
                }
                return;
            }
            if (iLives <= 1) {
                PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
                // dying player -> infected
                this.getLifeMap().put(player.getName(), this.arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_ILIVES));
                this.arena.msg(player, Language.parse(this.arena, MSG.GOAL_INFECTED_YOU));
                this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_INFECTED_PLAYER, player.getName()));

                final ArenaTeam oldTeam = aPlayer.getArenaTeam();
                final ArenaTeam respawnTeam = this.arena.getTeam("infected");

                PATeamChangeEvent tcEvent = new PATeamChangeEvent(this.arena, player, oldTeam, respawnTeam);
                Bukkit.getPluginManager().callEvent(tcEvent);
                this.arena.updateScoreboardTeam(player, oldTeam, respawnTeam);

                oldTeam.remove(aPlayer);

                respawnTeam.add(aPlayer);

                final ArenaClass infectedClass = this.arena.getClass("%infected%");
                if (infectedClass != null) {
                    aPlayer.setArenaClass(infectedClass);
                }

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

                PACheck.handleRespawn(this.arena,
                        aPlayer, returned);

                if (this.anyTeamEmpty()) {
                    PACheck.handleEnd(this.arena, false);
                }
                return;
            }
            // dying infected player, has lives remaining
            PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "infected", "doesRespawn", "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
            iLives--;
            this.getLifeMap().put(player.getName(), iLives);

            final ArenaTeam respawnTeam = aPlayer
                    .getArenaTeam();
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

            PACheck.handleRespawn(this.arena,
                    aPlayer, returned);


            // player died => commit death!
            PACheck.handleEnd(this.arena, false);
        } else {
            iLives--;
            this.getLifeMap().put(player.getName(), iLives);

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

            PACheck.handleRespawn(this.arena,
                    aPlayer, returned);
        }
    }

    @Override
    public void commitStart() {
        this.parseStart(); // hack the team in before spawning, derp!
        for (final ArenaTeam team : this.arena.getTeams()) {
            SpawnManager.distribute(this.arena, team);
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("normal lives: "
                + this.arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_NLIVES) + " || " +
                "infected lives: "
                + this.arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_ILIVES));
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("getprotect", "setprotect");
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(this.getLifeMap().getOrDefault(aPlayer.getName(), 0))
            );
        }
        return res;
    }

    @Override
    public boolean hasSpawn(final String string) {


        if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            for (final ArenaClass aClass : this.arena.getClasses()) {
                if (string.toLowerCase().startsWith(
                        aClass.getName().toLowerCase() + "spawn")) {
                    return true;
                }
            }
        }

        return this.arena.isFreeForAll() && string.toLowerCase()
                .startsWith("spawn") || string.toLowerCase().startsWith("infected");
    }

    @Override
    public void initate(final Player player) {
        this.updateLives(player, this.arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_NLIVES));
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        this.getLifeMap().remove(player.getName());
    }

    @Override
    public void parseStart() {
        if (this.arena.getTeam("infected") != null) {
            return;
        }
        ArenaPlayer infected = null;
        final Random random = new Random();
        for (final ArenaTeam team : this.arena.getTeams()) {
            int pos = random.nextInt(team.getTeamMembers().size());
            debug(this.arena, "team " + team.getName() + " random " + pos);
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                debug(this.arena, ap.get(), "#" + pos + ": " + ap);
                this.getLifeMap().put(ap.getName(),
                        this.arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_NLIVES));
                if (pos-- == 0) {
                    infected = ap;
                    this.getLifeMap().put(ap.getName(),
                            this.arena.getArenaConfig().getInt(CFG.GOAL_INFECTED_ILIVES));
                }
                //break;
            }
        }
        final ArenaTeam infectedTeam = new ArenaTeam("infected", "PINK");
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (team.getTeamMembers().contains(infected)) {
                final PATeamChangeEvent tcEvent = new PATeamChangeEvent(this.arena, infected.get(), team, infectedTeam);
                Bukkit.getPluginManager().callEvent(tcEvent);
                this.arena.updateScoreboardTeam(infected.get(), team, infectedTeam);
                team.remove(infected);
            }
        }
        infectedTeam.add(infected);

        final ArenaClass infectedClass = this.arena.getClass("%infected%");
        if (infectedClass != null) {
            infected.setArenaClass(infectedClass);
            InventoryManager.clearInventory(infected.get());
            infectedClass.equip(infected.get());
            for (final ArenaModule mod : this.arena.getMods()) {
                mod.parseRespawn(infected.get(), infectedTeam, DamageCause.CUSTOM,
                        infected.get());
            }
        }

        this.arena.msg(infected.get(), Language.parse(this.arena, MSG.GOAL_INFECTED_YOU, infected.getName()));
        this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_INFECTED_PLAYER, infected.getName()));

        final Set<PASpawn> spawns = new HashSet<>(SpawnManager.getPASpawnsStartingWith(this.arena, "infected"));

        int pos = spawns.size();

        for (final PASpawn spawn : spawns) {
            if (pos-- < 0) {
                this.arena.tpPlayerToCoordName(infected, spawn.getName());
                break;
            }
        }
        this.arena.getTeams().add(infectedTeam);
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getLifeMap().clear();
        this.arena.getTeams().remove(this.arena.getTeam("infected"));
    }

    @Override
    public void setPlayerLives(final int value) {
        final Set<String> plrs = new HashSet<>(this.getLifeMap().keySet());

        for (final String s : plrs) {
            this.getLifeMap().put(s, value);
        }
    }

    @Override
    public void setPlayerLives(final ArenaPlayer aPlayer, final int value) {
        this.getLifeMap().put(aPlayer.getName(), value);
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaPlayer ap : this.arena.getFighters()) {
            double score = this.getLifeMap().getOrDefault(ap.getName(), 0);
            if (ap.getArenaTeam() != null && "infected".equals(ap.getArenaTeam().getName())) {
                score *= this.arena.getFighters().size();
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
        this.getLifeMap().remove(player.getName());
    }
}
