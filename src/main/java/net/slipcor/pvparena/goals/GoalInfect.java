package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.*;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.events.PATeamChangeEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.WorkflowManager;
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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

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

    private static final String INFECTED = "infected";
    private static final String SPAWN = "spawn";
    private static final String GETPROTECT = "getprotect";
    private static final String SETPROTECT = "setprotect";

    private ArenaTeam infectedTeam;

    public GoalInfect() {
        super("Infect");
    }

    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean isFreeForAll() {
        return true;
    }

    @Override
    public boolean checkEnd() {
        final int count = this.getPlayerLifeMap().size();

        return count <= 1 || this.anyTeamEmpty(); // yep. only one player left. go!
    }

    private boolean anyTeamEmpty() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            boolean bbreak = false;
            for (final ArenaPlayer player : team.getTeamMembers()) {
                if (player.getStatus() == PlayerStatus.FIGHT) {
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
    public Set<String> checkForMissingSpawns(final Set<String> spawnNames) {

        Set<String> errors = this.checkForMissingFFACustom(spawnNames, INFECTED);
        errors.addAll(this.checkForMissingFFASpawn(spawnNames));

        return errors;
    }

    @Override
    public boolean checkCommand(final String string) {
        return GETPROTECT.equalsIgnoreCase(string) || SETPROTECT.equalsIgnoreCase(string);
    }

    @Override
    public void checkBreak(BlockBreakEvent event) throws GameplayException {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(event.getPlayer());
        if (this.arena.equals(arenaPlayer.getArena()) && arenaPlayer.getStatus() == PlayerStatus.FIGHT
                && this.infectedTeam.equals(arenaPlayer.getArenaTeam())) {
            if (PlayerPrevention.has(
                    this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.BREAK
            )) {
                event.setCancelled(true);
                this.arena.msg(event.getPlayer(), MSG.PLAYER_PREVENTED_BREAK);
                throw new GameplayException("BREAK not allowed");
            } else if (event.getBlock().getType() == Material.TNT &&
                    PlayerPrevention.has(
                            this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.TNTBREAK
                    )) {
                event.setCancelled(true);
                this.arena.msg(event.getPlayer(), MSG.PLAYER_PREVENTED_TNTBREAK);
                throw new GameplayException("TNTBREAK not allowed");
            }
        }
    }

    @Override
    public void checkCraft(CraftItemEvent event) throws GameplayException {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(((Player) event.getInventory().getHolder()).getName());
        if (this.arena.equals(arenaPlayer.getArena())
                && arenaPlayer.getStatus() == PlayerStatus.FIGHT
                && this.infectedTeam.equals(arenaPlayer.getArenaTeam())
                && PlayerPrevention.has(this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.CRAFT)
        ) {
            event.setCancelled(true);
            this.arena.msg(event.getWhoClicked(), MSG.PLAYER_PREVENTED_CRAFT);
            throw new GameplayException("CRAFT not allowed");
        }
    }

    @Override
    public void checkDrop(PlayerDropItemEvent event) throws GameplayException {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(event.getPlayer().getName());
        if (this.arena.equals(arenaPlayer.getArena())
                && arenaPlayer.getStatus() == PlayerStatus.FIGHT
                && this.infectedTeam.equals(arenaPlayer.getArenaTeam())
                && PlayerPrevention.has(this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.DROP)
        ) {
            event.setCancelled(true);
            this.arena.msg(event.getPlayer(), MSG.PLAYER_PREVENTED_DROP);
            throw new GameplayException("DROP not allowed");
        }
    }

    @Override
    public void checkInventory(InventoryClickEvent event) throws GameplayException {
        ArenaPlayer ap = ArenaPlayer.fromPlayer(event.getWhoClicked().getName());
        if (this.arena.equals(ap.getArena())
                && ap.getStatus() == PlayerStatus.FIGHT
                && INFECTED.equals(ap.getArenaTeam().getName())
                && PlayerPrevention.has(this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.INVENTORY)
        ) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            this.arena.msg(event.getWhoClicked(), MSG.PLAYER_PREVENTED_INVENTORY);
            throw new GameplayException("INVENTORY not allowed");
        }
    }

    @Override
    public void checkPickup(EntityPickupItemEvent event) throws GameplayException {
        ArenaPlayer ap = ArenaPlayer.fromPlayer(event.getEntity().getName());
        if (this.arena.equals(ap.getArena())
                && ap.getStatus() == PlayerStatus.FIGHT
                && INFECTED.equals(ap.getArenaTeam().getName())
                && PlayerPrevention.has(this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.PICKUP)
        ) {
            event.setCancelled(true);
            throw new GameplayException("PICKUP not allowed");
        }
    }

    @Override
    public void checkPlace(BlockPlaceEvent event) throws GameplayException {
        ArenaPlayer ap = ArenaPlayer.fromPlayer(event.getPlayer().getName());
        if (this.arena.equals(ap.getArena())
                && ap.getStatus() == PlayerStatus.FIGHT
                && INFECTED.equals(ap.getArenaTeam().getName())) {
            if (PlayerPrevention.has(
                    this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.PLACE
            )) {
                event.setCancelled(true);
                this.arena.msg(event.getPlayer(), MSG.PLAYER_PREVENTED_PLACE);
                throw new GameplayException("PLACE not allowed");
            } else if (event.getBlock().getType() == Material.TNT &&
                    PlayerPrevention.has(
                            this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS), PlayerPrevention.TNT
                    )) {
                event.setCancelled(true);
                this.arena.msg(event.getPlayer(), MSG.PLAYER_PREVENTED_TNT);
                throw new GameplayException("TNT not allowed");
            }
        }
    }

    @Override
    public Boolean shouldRespawnPlayer(final Player player, PADeathInfo deathInfo) {
        if (this.getPlayerLifeMap().containsKey(player)) {
            final int iLives = this.getPlayerLifeMap().get(player);
            debug(this.arena, player, "lives before death: " + iLives);
            return iLives > 1 || !INFECTED.equals(ArenaPlayer.fromPlayer(player).getArenaTeam().getName());
        }
        return true;
    }

    @Override
    public boolean overridesStart() {
        return true;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {

        int value = this.arena.getConfig().getInt(CFG.GOAL_INFECTED_PPROTECTS);

        if (GETPROTECT.equalsIgnoreCase(args[0])) {
            List<String> values = new ArrayList<>();


            for (PlayerPrevention pp : PlayerPrevention.values()) {
                if (pp == null) {
                    continue;
                }
                values.add((PlayerPrevention.has(value, pp) ?
                        ChatColor.GREEN.toString() : ChatColor.RED.toString()) + pp.name());
            }
            this.arena.msg(sender, MSG.GOAL_INFECTED_IPROTECT, StringParser.joinList(values, (ChatColor.WHITE + ", ")));

        } else if (SETPROTECT.equalsIgnoreCase(args[0])) {
            // setprotect [value] {true|false}
            if (args.length < 2) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "2|3");
                return;
            }

            try {
                final PlayerPrevention pp = PlayerPrevention.valueOf(args[1].toUpperCase());
                final boolean has = PlayerPrevention.has(value, pp);

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
                            Language.parse(MSG.GOAL_INFECTED_IPROTECT_SET,
                                    pp.name(), ChatColor.GREEN + "true") + ChatColor.YELLOW);
                } else {
                    value = value ^ (int) Math.pow(2, pp.ordinal());
                    this.arena.msg(
                            sender,
                            Language.parse(MSG.GOAL_INFECTED_IPROTECT_SET,
                                    pp.name(), ChatColor.RED + "false") + ChatColor.YELLOW);
                }
                this.arena.getConfig().set(CFG.GOAL_INFECTED_PPROTECTS, value);
            } catch (final Exception e) {
                List<String> values = new ArrayList<>();


                for (PlayerPrevention pp : PlayerPrevention.values()) {
                    values.add(pp.name());
                }
                this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], StringParser.joinList(values, ", "));
                return;
            }
            this.arena.getConfig().save();

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
            for (final ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                if (arenaPlayer.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                if (INFECTED.equals(arenaPlayer.getArenaTeam().getName())) {
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(MSG.GOAL_INFECTED_WON), "END");

                    ArenaModuleManager.announce(this.arena,
                            Language.parse(MSG.GOAL_INFECTED_WON), "WINNER");

                    this.arena.broadcast(Language.parse(MSG.GOAL_INFECTED_WON));
                    break;
                } else {

                    ArenaModuleManager.announce(this.arena,
                            Language.parse(MSG.GOAL_INFECTED_LOST), "END");
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(MSG.GOAL_INFECTED_LOST), "LOSER");

                    this.arena.broadcast(Language.parse(MSG.GOAL_INFECTED_LOST));
                    break;
                }
            }

            if (ArenaModuleManager.commitEnd(this.arena, team)) {
                return;
            }
        }

        this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn, PADeathInfo deathInfo) {
        if (!this.getPlayerLifeMap().containsKey(player)) {
            return;
        }
        int iLives = this.getPlayerLifeMap().get(player);
        debug(this.arena, player, "lives before death: " + iLives);
        ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        if (iLives <= 1 || INFECTED.equals(aPlayer.getArenaTeam().getName())) {
            if (iLives <= 1 && INFECTED.equals(aPlayer.getArenaTeam().getName())) {

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, INFECTED, "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
                // kill, remove!
                this.getPlayerLifeMap().remove(player);

                debug(aPlayer, "no remaining lives -> LOST");
                aPlayer.handleDeathAndLose(deathInfo);

                return;
            }
            if (iLives <= 1) {
                PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
                // dying player -> infected
                this.getPlayerLifeMap().put(player.getPlayer(), this.arena.getConfig().getInt(CFG.GOAL_INFECTED_ILIVES));
                this.arena.msg(player, MSG.GOAL_INFECTED_YOU);
                this.arena.broadcast(Language.parse(MSG.GOAL_INFECTED_PLAYER, player.getName()));

                final ArenaTeam oldTeam = aPlayer.getArenaTeam();
                final ArenaTeam respawnTeam = this.arena.getTeam(INFECTED);

                PATeamChangeEvent tcEvent = new PATeamChangeEvent(this.arena, player, oldTeam, respawnTeam);
                Bukkit.getPluginManager().callEvent(tcEvent);
                this.arena.getScoreboard().switchPlayerTeam(player, oldTeam, respawnTeam);

                oldTeam.remove(aPlayer);

                respawnTeam.add(aPlayer);

                final ArenaClass infectedClass = this.arena.getClass("%infected%");
                if (infectedClass != null) {
                    aPlayer.setArenaClass(infectedClass);
                }

                if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                    this.broadcastSimpleDeathMessage(player, deathInfo);
                }

                ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
                arenaPlayer.setMayDropInventory(true);
                arenaPlayer.setMayRespawn(true);

                if (this.anyTeamEmpty()) {
                    WorkflowManager.handleEnd(this.arena, false);
                }
                return;
            }
            // dying infected player, has lives remaining
            PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, INFECTED, "doesRespawn", "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
            iLives--;
            this.getPlayerLifeMap().put(player, iLives);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, player, deathInfo, iLives);
            }

            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);


            // player died => commit death!
            WorkflowManager.handleEnd(this.arena, false);
        } else {
            iLives--;
            this.getPlayerLifeMap().put(player.getPlayer(), iLives);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, player, deathInfo, iLives);
            }

            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);
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
                + this.arena.getConfig().getInt(CFG.GOAL_INFECTED_NLIVES) + " || " +
                "infected lives: "
                + this.arena.getConfig().getInt(CFG.GOAL_INFECTED_ILIVES));
    }

    @Override
    public List<String> getGoalCommands() {
        return Arrays.asList(GETPROTECT, SETPROTECT);
    }

    @Override
    public boolean hasSpawn(final String string) {

        if (this.arena.getConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            for (final ArenaClass aClass : this.arena.getClasses()) {
                if (string.toLowerCase().startsWith(
                        aClass.getName().toLowerCase() + SPAWN)) {
                    return true;
                }
            }
        }

        return string.toLowerCase().startsWith(SPAWN) || string.toLowerCase().startsWith(INFECTED);
    }

    @Override
    public void initiate(final Player player) {
        this.updateLives(player, this.arena.getConfig().getInt(CFG.GOAL_INFECTED_NLIVES));
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        this.getPlayerLifeMap().remove(player);
    }

    @Override
    public void parseStart() {
        // we already build the infected team
        if (this.arena.getTeam(INFECTED) != null) {
            this.infectedTeam = this.arena.getTeam(INFECTED);
            return;
        }
        // create the team infected
        this.infectedTeam = new ArenaTeam(INFECTED, "PINK");

        // select starting infected players
        ArenaPlayer infected = null;
        final Random random = new Random();
        for (final ArenaTeam team : this.arena.getNotEmptyTeams()) {
            int pos = random.nextInt(team.getTeamMembers().size());
            debug(this.arena, "team " + team.getName() + " random " + pos);
            for (final ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                debug(this.arena, arenaPlayer.getPlayer(), "#" + pos + ": " + arenaPlayer);
                this.getPlayerLifeMap().put(arenaPlayer.getPlayer(),
                        this.arena.getConfig().getInt(CFG.GOAL_INFECTED_NLIVES));
                if (pos-- == 0) {
                    infected = arenaPlayer;
                    this.getPlayerLifeMap().put(arenaPlayer.getPlayer(),
                            this.arena.getConfig().getInt(CFG.GOAL_INFECTED_ILIVES));
                }
            }
        }

        assert infected != null;
        for (final ArenaTeam arenaTeam : this.arena.getNotEmptyTeams()) {
            if (arenaTeam.getTeamMembers().contains(infected)) {
                final PATeamChangeEvent tcEvent = new PATeamChangeEvent(this.arena, infected.getPlayer(), arenaTeam, this.infectedTeam);
                Bukkit.getPluginManager().callEvent(tcEvent);
                this.arena.getScoreboard().switchPlayerTeam(infected.getPlayer(), arenaTeam, this.infectedTeam);
                arenaTeam.remove(infected);
            }
        }
        this.infectedTeam.add(infected);

        final ArenaClass infectedClass = this.arena.getClass("%infected%");
        if (infectedClass != null) {
            infected.setArenaClass(infectedClass);
            InventoryManager.clearInventory(infected.getPlayer());
            infectedClass.equip(infected.getPlayer());
            for (final ArenaModule mod : this.arena.getMods()) {
                mod.parseRespawn(infected.getPlayer(), this.infectedTeam, DamageCause.CUSTOM,
                        infected.getPlayer());
            }
        }

        this.arena.msg(infected.getPlayer(), MSG.GOAL_INFECTED_YOU, infected.getName());
        this.arena.broadcast(Language.parse(MSG.GOAL_INFECTED_PLAYER, infected.getName()));

        final Set<PASpawn> spawns = new HashSet<>(SpawnManager.getPASpawnsStartingWith(this.arena, INFECTED));

        int pos = spawns.size();

        for (final PASpawn spawn : spawns) {
            if (pos-- < 0) {
                this.arena.tpPlayerToCoordName(infected, spawn.getName());
                break;
            }
        }
        this.arena.getTeams().add(this.infectedTeam);
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
        this.arena.getTeams().remove(this.arena.getTeam(INFECTED));
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
            if (arenaPlayer.getArenaTeam() != null && INFECTED.equals(arenaPlayer.getArenaTeam().getName())) {
                score *= this.arena.getFighters().size();
            }
            if (scores.containsKey(arenaPlayer.getName())) {
                scores.put(arenaPlayer.getName(), scores.get(arenaPlayer.getName()) + score);
            } else {
                scores.put(arenaPlayer.getName(), score);
            }
        }

        return scores;
    }
}
