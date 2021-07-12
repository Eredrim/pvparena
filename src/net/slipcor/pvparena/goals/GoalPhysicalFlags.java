package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.*;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * <pre>
 * Arena Goal class "PhysicalFlags"
 * </pre>
 * <p/>
 * Capture flags by breaking them, bring them home, get points, win.
 *
 * @author slipcor
 */

public class GoalPhysicalFlags extends ArenaGoal implements Listener {

    private static final int PRIORITY = 7;
    private static final String TOUCHDOWN = "touchdown";
    private Map<String, String> flagMap;
    private Map<String, BlockData> flagDataMap;
    private Map<String, ItemStack> headGearMap;

    private String flagName = "";

    public GoalPhysicalFlags() {
        super("PhysicalFlags");
        this.debug = new Debug(100);
    }

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public PACheck checkCommand(final PACheck res, final String string) {
        if (res.getPriority() > PRIORITY) {
            return res;
        }

        if ("flagtype".equalsIgnoreCase(string) || "flageffect".equalsIgnoreCase(string) || TOUCHDOWN.equalsIgnoreCase(string)) {
            res.setPriority(this, PRIORITY);
        }

        for (final ArenaTeam team : this.arena.getTeams()) {
            final String sTeam = team.getName();
            if (string.contains(sTeam + "flag")) {
                res.setPriority(this, PRIORITY);
            }
        }

        return res;
    }

    @Override
    public List<String> getMain() {
        final List<String> result = Stream.of("flagtype", "flageffect", TOUCHDOWN).collect(Collectors.toList());
        if (this.arena != null) {
            for (final ArenaTeam team : this.arena.getTeams()) {
                final String sTeam = team.getName();
                result.add(sTeam + "flag");
            }
        }
        return result;
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Material}"});
        return result;
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
            this.arena.getDebugger().i("No teams playing!");
        }

        return res;
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        final String team = this.checkForMissingTeamSpawn(list);
        if (team != null) {
            return team;
        }
        return this.checkForMissingTeamCustom(list, "flag");
    }

    /**
     * hook into an interacting player
     *
     * @param res    the PACheck instance
     * @param player the interacting player
     * @param block  the block being clicked
     * @return the PACheck instance
     */
    @Override
    public PACheck checkInteract(final PACheck res, final Player player, final Block block) {
        if (block == null || res.getPriority() > PRIORITY) {
            return res;
        }
        this.arena.getDebugger().i("checking interact", player);

        Material flagType = this.arena.getArenaConfig().getMaterial(CFG.GOAL_PFLAGS_FLAGTYPE);
        if (!ColorUtils.isSubType(block.getType(), flagType)) {
            this.arena.getDebugger().i("block, but not flag", player);
            return res;
        }
        this.arena.getDebugger().i("flag click!", player);

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        if (this.getFlagMap().containsValue(player.getName())) {
            this.arena.getDebugger().i("player " + player.getName() + " has got a flag", player);

            final Vector vLoc = block.getLocation().toVector();
            final String sTeam = aPlayer.getArenaTeam().getName();
            this.arena.getDebugger().i("block: " + vLoc, player);
            Vector vFlag = null;
            if (this.getTeamFlagLoc(sTeam) != null) {
                vFlag = this.getTeamFlagLoc(sTeam).toLocation().toVector();
            } else {
                this.arena.getDebugger().i(sTeam + "flag = null", player);
            }

            this.arena.getDebugger().i("player is in the team " + sTeam, player);
            if (vFlag != null && vLoc.distance(vFlag) < 2) {

                this.arena.getDebugger().i("player is at his flag", player);

                if (this.getFlagMap().containsKey(sTeam) || this.getFlagMap().containsKey(TOUCHDOWN)) {
                    this.arena.getDebugger().i("the flag of the own team is taken!", player);

                    if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_PFLAGS_MUSTBESAFE)
                            && !this.getFlagMap().containsKey(TOUCHDOWN)) {
                        this.arena.getDebugger().i("cancelling", player);

                        this.arena.msg(player, Language.parse(this.arena, MSG.GOAL_FLAGS_NOTSAFE));
                        return res;
                    }
                }

                String flagTeam = this.getHeldFlagTeam(player.getName());

                this.arena.getDebugger().i("the flag belongs to team " + flagTeam, player);

                ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                if (!ColorUtils.isDroppedItemSubType(mainHandItem, flagType)) {
                    this.arena.getDebugger().i("player " + player.getName() + " is not holding the flag", player);
                    this.arena.msg(player, Language.parse(this.arena, MSG.GOAL_PHYSICALFLAGS_HOLDFLAG));
                    return res;
                }

                player.getInventory().remove(mainHandItem);
                player.updateInventory();

                try {
                    if (TOUCHDOWN.equals(flagTeam)) {
                        this.arena.broadcast(Language.parse(this.arena,
                                MSG.GOAL_FLAGS_TOUCHHOME, this.arena.getTeam(sTeam)
                                        .colorizePlayer(player)
                                        + ChatColor.YELLOW, String
                                        .valueOf(this.getLifeMap().get(aPlayer
                                                .getArenaTeam().getName()) - 1)));
                    } else {
                        this.arena.broadcast(Language.parse(this.arena,
                                MSG.GOAL_FLAGS_BROUGHTHOME, this.arena
                                        .getTeam(sTeam).colorizePlayer(player)
                                        + ChatColor.YELLOW,
                                this.arena.getTeam(flagTeam).getColoredName()
                                        + ChatColor.YELLOW, String
                                        .valueOf(this.getLifeMap().get(flagTeam) - 1)));
                    }
                    this.getFlagMap().remove(flagTeam);
                } catch (final Exception e) {
                    Bukkit.getLogger().severe(
                            "[PVP Arena] team unknown/no lives: " + flagTeam);
                    e.printStackTrace();
                }
                if (TOUCHDOWN.equals(flagTeam)) {
                    this.releaseFlag(TOUCHDOWN);
                } else {
                    this.releaseFlag(flagTeam);
                }
                this.removeEffects(player);
                if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_PFLAGS_WOOLFLAGHEAD)) {
                    player.getInventory().setHelmet(new ItemStack(Material.AIR, 1));
                } else {
                    if (this.getHeadGearMap().get(player.getName()) == null) {
                        player.getInventory().setHelmet(this.getHeadGearMap().get(player.getName()).clone());
                        this.getHeadGearMap().remove(player.getName());
                    }
                }

                flagTeam = TOUCHDOWN.equals(flagTeam) ? flagTeam + ':' + aPlayer.getArenaTeam().getName() : flagTeam;

                this.reduceLivesCheckEndAndCommit(this.arena, flagTeam);

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);

                // used to cancel block put event
                res.setPriority(this, PRIORITY);
            }
        }

        return res;
    }

    @Override
    public void commitInteract(final Player player, final Block clickedBlock) {}

    private PABlockLocation getTeamFlagLoc(String teamName) {
        return SpawnManager.getBlockByExactName(this.arena, teamName + "flag");
    }

    private void applyEffects(final Player player) {
        final String value = this.arena.getArenaConfig().getString(CFG.GOAL_PFLAGS_FLAGEFFECT);

        if ("none".equalsIgnoreCase(value)) {
            return;
        }

        final String[] split = value.split("x");

        int amp = 1;

        if (split.length > 1) {
            try {
                amp = Integer.parseInt(split[1]);
            } catch (final Exception ignored) {

            }
        }

        PotionEffectType pet = null;
        for (final PotionEffectType x : PotionEffectType.values()) {
            if (x == null) {
                continue;
            }
            if (x.getName().equalsIgnoreCase(split[0])) {
                pet = x;
                break;
            }
        }

        if (pet == null) {
            PVPArena.instance.getLogger().warning(
                    "Invalid Potion Effect Definition: " + value);
            return;
        }

        player.addPotionEffect(new PotionEffect(pet, amp, 2147000));
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
    public PACheck checkSetBlock(final PACheck res, final Player player, final Block block) {

        if (res.getPriority() > PRIORITY || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return res;
        }

        Material flagType = this.arena.getArenaConfig().getMaterial(CFG.GOAL_PFLAGS_FLAGTYPE);
        if (block == null || !ColorUtils.isSubType(block.getType(), flagType)) {
            return res;
        }

        if (!PermissionManager.hasAdminPerm(player) && !PermissionManager.hasBuilderPerm(player, this.arena)) {
            return res;
        }
        res.setPriority(this, PRIORITY); // success :)

        return res;
    }

    private void commit(final Arena arena, final String sTeam, final boolean win) {
        if (arena.realEndRunner != null) {
            arena.getDebugger().i("[CTF] already ending");
            return;
        }
        arena.getDebugger().i("[CTF] committing end: " + sTeam);
        arena.getDebugger().i("win: " + win);

        String winteam = sTeam;

        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getName().equals(sTeam) == win) {
                continue;
            }
            for (final ArenaPlayer ap : team.getTeamMembers()) {

                ap.addLosses();
            /*
				arena.tpPlayerToCoordName(ap.get(), "spectator");
				ap.setTelePass(false);*/

                ap.setStatus(Status.LOST);
            }
        }
        for (final ArenaTeam team : arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != Status.FIGHT) {
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
                            Language.parse(arena, MSG.TEAM_HAS_WON,
                                    arena.getTeam(winteam).getColor()
                                            + winteam + ChatColor.YELLOW),
                            "WINNER");
            arena.broadcast(Language.parse(arena, MSG.TEAM_HAS_WON,
                    arena.getTeam(winteam).getColor() + winteam
                            + ChatColor.YELLOW));
        }

        this.getLifeMap().clear();
        this.getFlagDataMap().clear();
        new EndRunnable(arena, arena.getArenaConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if ("flagtype".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                this.arena.msg(
                        sender,
                        Language.parse(this.arena, MSG.ERROR_INVALID_ARGUMENT_COUNT,
                                String.valueOf(args.length), "2"));
                return;
            }

            final Material mat = Material.getMaterial(args[1].toUpperCase());

            if (mat == null) {
                this.arena.msg(sender,
                        Language.parse(this.arena, MSG.ERROR_MAT_NOT_FOUND, args[1]));
                return;
            }

            this.arena.getArenaConfig().set(CFG.GOAL_PFLAGS_FLAGTYPE, mat.name());

            this.arena.getArenaConfig().save();
            this.arena.msg(sender, Language.parse(this.arena, MSG.GOAL_FLAGS_TYPESET, mat.name()));

        } else if ("flageffect".equalsIgnoreCase(args[0])) {

            // /pa [arena] flageffect SLOW 2
            if (args.length < 2) {
                this.arena.msg(
                        sender,
                        Language.parse(this.arena, MSG.ERROR_INVALID_ARGUMENT_COUNT,
                                String.valueOf(args.length), "2"));
                return;
            }

            if ("none".equalsIgnoreCase(args[1])) {
                this.arena.getArenaConfig().set(CFG.GOAL_PFLAGS_FLAGEFFECT, args[1]);

                this.arena.getArenaConfig().save();
                this.arena.msg(
                        sender,
                        Language.parse(this.arena, MSG.SET_DONE,
                                CFG.GOAL_PFLAGS_FLAGEFFECT.getNode(), args[1]));
                return;
            }

            PotionEffectType pet = null;

            for (final PotionEffectType x : PotionEffectType.values()) {
                if (x == null) {
                    continue;
                }
                if (x.getName().equalsIgnoreCase(args[1])) {
                    pet = x;
                    break;
                }
            }

            if (pet == null) {
                this.arena.msg(sender, Language.parse(this.arena,
                        MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[1]));
                return;
            }

            int amp = 1;

            if (args.length == 5) {
                try {
                    amp = Integer.parseInt(args[2]);
                } catch (final Exception e) {
                    this.arena.msg(sender,
                            Language.parse(this.arena, MSG.ERROR_NOT_NUMERIC, args[2]));
                    return;
                }
            }
            final String value = args[1] + 'x' + amp;
            this.arena.getArenaConfig().set(CFG.GOAL_PFLAGS_FLAGEFFECT, value);

            this.arena.getArenaConfig().save();
            this.arena.msg(
                    sender,
                    Language.parse(this.arena, MSG.SET_DONE,
                            CFG.GOAL_PFLAGS_FLAGEFFECT.getNode(), value));

        } else if (args[0].contains("flag")) {
            for (final ArenaTeam team : this.arena.getTeams()) {
                final String sTeam = team.getName();
                if (args[0].contains(sTeam + "flag")) {
                    this.flagName = args[0];
                    PAA_Region.activeSelections.put(sender.getName(), this.arena);

                    this.arena.msg(sender,
                            Language.parse(this.arena, MSG.GOAL_FLAGS_TOSET, this.flagName));
                }
            }
        } else if (TOUCHDOWN.equalsIgnoreCase(args[0])) {
            this.flagName = args[0] + "flag";
            PAA_Region.activeSelections.put(sender.getName(), this.arena);

            this.arena.msg(sender, Language.parse(this.arena, MSG.GOAL_FLAGS_TOSET, this.flagName));
        }
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            this.arena.getDebugger().i("[FLAGS] already ending");
            return;
        }
        this.arena.getDebugger().i("[FLAGS]");

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
        new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public boolean commitSetFlag(final Player player, final Block block) {

        this.arena.getDebugger().i("trying to set a flag", player);

        // command : /pa redflag1
        // location: red1flag:

        SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), this.flagName);

        this.arena.msg(player, Language.parse(this.arena, MSG.GOAL_FLAGS_SET, this.flagName));

        PAA_Region.activeSelections.remove(player.getName());
        this.flagName = "";

        return true;
    }

    @Override
    public void commitStart() {
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        Bukkit.getPluginManager().registerEvents(this, PVPArena.instance);
    }

    @Override
    public void disconnect(final ArenaPlayer aPlayer) {
        if (this.getFlagMap().isEmpty()) {
            return;
        }
        final String sTeam = this.getHeldFlagTeam(aPlayer.getName());
        final ArenaTeam flagTeam = this.arena.getTeam(sTeam);

        if (flagTeam == null) {
            if (sTeam != null) {
                this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_FLAGS_DROPPEDTOUCH, aPlayer
                        .getArenaTeam().getColorCodeString()
                        + aPlayer.getName()
                        + ChatColor.YELLOW));

                this.getFlagMap().remove(TOUCHDOWN);
                if (this.getHeadGearMap() != null && this.getHeadGearMap().get(aPlayer.getName()) != null) {
                    if (aPlayer.get() != null) {
                        aPlayer.get().getInventory()
                                .setHelmet(this.getHeadGearMap().get(aPlayer.getName()).clone());
                    }
                    this.getHeadGearMap().remove(aPlayer.getName());
                }

                this.releaseFlag(TOUCHDOWN);
            }
        } else {
            this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_FLAGS_DROPPED, aPlayer
                    .getArenaTeam().getColorCodeString()
                    + aPlayer.getName()
                    + ChatColor.YELLOW, flagTeam.getName() + ChatColor.YELLOW));
            this.getFlagMap().remove(flagTeam.getName());
            if (this.getHeadGearMap() != null && this.getHeadGearMap().get(aPlayer.getName()) != null) {
                if (aPlayer.get() != null) {
                    aPlayer.get().getInventory()
                            .setHelmet(this.getHeadGearMap().get(aPlayer.getName()).clone());
                }
                this.getHeadGearMap().remove(aPlayer.getName());
            }

            this.releaseFlag(flagTeam.getName());
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        Config cfg = this.arena.getArenaConfig();
        sender.sendMessage("flageffect: " + cfg.getString(CFG.GOAL_PFLAGS_FLAGEFFECT));
        sender.sendMessage("flagtype: " + cfg.getString(CFG.GOAL_PFLAGS_FLAGTYPE));
        sender.sendMessage("lives: " + cfg.getInt(CFG.GOAL_PFLAGS_LIVES));
        sender.sendMessage(StringParser.colorVar("mustbesafe", cfg.getBoolean(CFG.GOAL_PFLAGS_MUSTBESAFE))
                + " | " + StringParser.colorVar("flaghead", cfg.getBoolean(CFG.GOAL_PFLAGS_WOOLFLAGHEAD)));
    }

    private Map<String, String> getFlagMap() {
        if (this.flagMap == null) {
            this.flagMap = new HashMap<>();
        }
        return this.flagMap;
    }

    private Map<String, BlockData> getFlagDataMap() {
        if (this.flagDataMap == null) {
            this.flagDataMap = new HashMap<>();
        }
        return this.flagDataMap;
    }

    private Material getFlagOverrideTeamMaterial(final Arena arena, final String team) {
        if (arena.getArenaConfig().getUnsafe("flagColors." + team) == null) {
            if (TOUCHDOWN.equals(team)) {
                return ColorUtils.getWoolMaterialFromChatColor(ChatColor.BLACK);
            }
            return ColorUtils.getWoolMaterialFromChatColor(arena.getTeam(team).getColor());
        }
        return ColorUtils.getWoolMaterialFromDyeColor(
                (String) arena.getArenaConfig().getUnsafe("flagColors." + team));
    }

    @Override
    public PACheck getLives(final PACheck res, final ArenaPlayer aPlayer) {
        if (res.getPriority() <= PRIORITY + 1000) {
            res.setError(
                    this,
                    String.valueOf(this.getLifeMap().getOrDefault(aPlayer.getArenaTeam().getName(), 0))
            );
        }
        return res;
    }

    private Map<String, ItemStack> getHeadGearMap() {
        if (this.headGearMap == null) {
            this.headGearMap = new HashMap<>();
        }
        return this.headGearMap;
    }

    /**
     * get the team name of the flag a player holds
     *
     * @param player the player to check
     * @return a team name
     */
    private String getHeldFlagTeam(final String player) {
        if (this.getFlagMap().isEmpty()) {
            return null;
        }

        this.arena.getDebugger().i("getting held FLAG of player " + player, player);
        for (final String sTeam : this.getFlagMap().keySet()) {
            this.arena.getDebugger().i("team " + sTeam + " is in " + this.getFlagMap().get(sTeam) + "s hands", player);
            if (player.equals(this.getFlagMap().get(sTeam))) {
                return sTeam;
            }
        }
        return null;
    }

    @Override
    public boolean hasSpawn(final String string) {
        for (final String teamName : this.arena.getTeamNames()) {
            if (string.toLowerCase().equals(teamName.toLowerCase() + "flag")) {
                return true;
            }
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
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (!this.getLifeMap().containsKey(team.getName())) {
            this.getLifeMap().put(aPlayer.getArenaTeam().getName(), this.arena.getArenaConfig().getInt(CFG.GOAL_PFLAGS_LIVES));
        }
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public void parsePlayerDeath(final Player player,
                                 final EntityDamageEvent lastDamageCause) {

        if (this.getFlagMap().isEmpty()) {
            this.arena.getDebugger().i("no flags set!!", player);
            return;
        }
        final String sTeam = this.getHeldFlagTeam(player.getName());
        final ArenaTeam flagTeam = this.arena.getTeam(sTeam);
        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        if (flagTeam == null) {
            if (sTeam != null) {
                this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_FLAGS_DROPPEDTOUCH, aPlayer
                        .getArenaTeam().getColorCodeString()
                        + aPlayer.getName()
                        + ChatColor.YELLOW));

                this.getFlagMap().remove(TOUCHDOWN);
                if (this.getHeadGearMap() != null && this.getHeadGearMap().get(aPlayer.getName()) != null) {
                    if (aPlayer.get() != null) {
                        aPlayer.get().getInventory().setHelmet(this.getHeadGearMap().get(aPlayer.getName()).clone());
                    }
                    this.getHeadGearMap().remove(aPlayer.getName());
                }

                this.releaseFlag(TOUCHDOWN);
            }
        } else {
            this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_FLAGS_DROPPED, aPlayer
                            .getArenaTeam().colorizePlayer(player) + ChatColor.YELLOW,
                    flagTeam.getColoredName() + ChatColor.YELLOW));
            this.getFlagMap().remove(flagTeam.getName());
            if (this.getHeadGearMap() != null && this.getHeadGearMap().get(player.getName()) != null) {
                player.getInventory().setHelmet(this.getHeadGearMap().get(player.getName()).clone());
                this.getHeadGearMap().remove(player.getName());
            }

            this.releaseFlag(flagTeam.getName());
        }
    }

    @Override
    public void parseStart() {
        this.getLifeMap().clear();
        this.getFlagDataMap().clear();
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (!team.getTeamMembers().isEmpty()) {
                this.arena.getDebugger().i("adding team " + team.getName());
                // team is active
                this.getLifeMap().put(team.getName(), this.arena.getArenaConfig().getInt(CFG.GOAL_PFLAGS_LIVES, 3));
                Block flagBlock = this.getTeamFlagLoc(team.getName()).toLocation().getBlock();
                this.getFlagDataMap().put(team.getName(), flagBlock.getBlockData().clone());
            }
        }
        ofNullable(this.getTeamFlagLoc(TOUCHDOWN)).ifPresent(paBlockLocation -> {
            Block touchdownFlagBlock = paBlockLocation.toLocation().getBlock();
            this.getFlagDataMap().put(TOUCHDOWN, touchdownFlagBlock.getBlockData().clone());
        });
    }

    private void reduceLivesCheckEndAndCommit(final Arena arena, final String team) {

        arena.getDebugger().i("reducing lives of team " + team);
        if (this.getLifeMap().get(team) == null) {
            if (team.contains(":")) {
                final String realTeam = team.split(":")[1];
                final int iLives = this.getLifeMap().get(realTeam) - 1;
                if (iLives > 0) {
                    this.getLifeMap().put(realTeam, iLives);
                } else {
                    this.getLifeMap().remove(realTeam);
                    this.commit(arena, realTeam, true);
                }
            }
        } else {
            if (this.getLifeMap().get(team) != null) {
                final int iLives = this.getLifeMap().get(team) - 1;
                if (iLives > 0) {
                    this.getLifeMap().put(team, iLives);
                } else {
                    this.getLifeMap().remove(team);
                    this.commit(arena, team, false);
                }
            }
        }
    }

    private void removeEffects(final Player player) {
        final String value = this.arena.getArenaConfig().getString(
                CFG.GOAL_PFLAGS_FLAGEFFECT);

        if ("none".equalsIgnoreCase(value)) {
            return;
        }

        PotionEffectType pet = null;

        final String[] split = value.split("x");

        for (final PotionEffectType x : PotionEffectType.values()) {
            if (x == null) {
                continue;
            }
            if (x.getName().equalsIgnoreCase(split[0])) {
                pet = x;
                break;
            }
        }

        if (pet == null) {
            PVPArena.instance.getLogger().warning("Invalid Potion Effect Definition: " + value);
            return;
        }

        player.removePotionEffect(pet);
        player.addPotionEffect(new PotionEffect(pet, 0, 1));
    }

    @Override
    public void reset(final boolean force) {
        this.getHeadGearMap().clear();
        this.getLifeMap().clear();
        this.getFlagMap().clear();
        if(!this.getFlagDataMap().isEmpty()) {
            for (final ArenaTeam team : this.arena.getTeams()) {
                this.releaseFlag(team.getName());
            }
            this.releaseFlag(TOUCHDOWN);
        }
        this.getFlagDataMap().clear();
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
            this.arena.getDebugger().i("no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
        if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_PFLAGS_WOOLFLAGHEAD)
                && config.get("flagColors") == null) {
            this.arena.getDebugger().i("no flagheads defined, adding white and black!");
            config.addDefault("flagColors.red", "WHITE");
            config.addDefault("flagColors.blue", "BLACK");
        }
    }

    /**
     * reset an arena flag
     *
     * @param teamName  team whose flag needs to be reset
     */
    private void releaseFlag(final String teamName) {
        PABlockLocation paBlockLocation = this.getTeamFlagLoc(teamName);
        if (paBlockLocation == null) {
            return;
        }

        Block flagBlock = paBlockLocation.toLocation().getBlock();
        try {
            flagBlock.setBlockData(this.getFlagDataMap().get(teamName));
        } catch (Exception e) {
            PVPArena.instance.getLogger().warning("Impossible to reset flag data ! You may recreate arena flags.");
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaTeam team : this.arena.getTeams()) {
            double score = this.getLifeMap().getOrDefault(team.getName(), 0);
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
        this.disconnect(ArenaPlayer.parsePlayer(player.getName()));
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.parsePlayer(player.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onFlagClaim(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        Material brokenMaterial = event.getBlock().getType();
        if (!this.arena.hasPlayer(event.getPlayer()) ||
                !ColorUtils.isSubType(brokenMaterial, this.arena.getArenaConfig().getMaterial(CFG.GOAL_PFLAGS_FLAGTYPE))) {

            this.arena.getDebugger().i("block destroy, ignoring", player);
            this.arena.getDebugger().i(String.valueOf(this.arena.hasPlayer(event.getPlayer())), player);
            this.arena.getDebugger().i(event.getBlock().getType().name(), player);
            return;
        }

        final Block block = event.getBlock();

        this.arena.getDebugger().i("flag destroy!", player);

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(player.getName());

        if (this.getFlagMap().containsValue(player.getName())) {
            this.arena.getDebugger().i("already carries a flag!", player);
            return;
        }
        final ArenaTeam pTeam = aPlayer.getArenaTeam();
        if (pTeam == null) {
            return;
        }

        final Set<ArenaTeam> setTeam = new HashSet<>(this.arena.getTeams());

        setTeam.add(new ArenaTeam(TOUCHDOWN, "BLACK"));
        Vector vFlag = null;
        for (final ArenaTeam team : setTeam) {
            final String teamName = team.getName();
            final PABlockLocation teamFlagLoc = this.getTeamFlagLoc(teamName);

            if (teamName.equals(pTeam.getName())) {
                this.arena.getDebugger().i("equals!OUT! ", player);
                continue;
            }
            if (team.getTeamMembers().size() < 1 && !TOUCHDOWN.equals(team.getName())) {
                this.arena.getDebugger().i("size!OUT! ", player);
                continue; // dont check for inactive teams
            }
            if (this.getFlagMap().containsKey(teamName)) {
                this.arena.getDebugger().i("taken!OUT! ", player);
                continue; // already taken
            }
            this.arena.getDebugger().i("checking for flag of team " + teamName, player);
            Vector vLoc = block.getLocation().toVector();
            this.arena.getDebugger().i("block: " + vLoc, player);

            if(teamFlagLoc != null && vLoc.equals(teamFlagLoc.toLocation().toVector())) {
                this.arena.getDebugger().i("flag found!", player);
                this.arena.getDebugger().i("vFlag: " + vFlag, player);

                if (TOUCHDOWN.equals(team.getName())) {

                    this.arena.broadcast(Language.parse(this.arena,
                            MSG.GOAL_FLAGS_GRABBEDTOUCH,
                            pTeam.colorizePlayer(player) + ChatColor.YELLOW));
                } else {

                    this.arena.broadcast(Language
                            .parse(this.arena, MSG.GOAL_FLAGS_GRABBED,
                                    pTeam.colorizePlayer(player)
                                            + ChatColor.YELLOW,
                                    team.getColoredName()
                                            + ChatColor.YELLOW));
                }
                try {
                    this.getHeadGearMap().put(player.getName(), player.getInventory().getHelmet().clone());
                } catch (final Exception ignored) {

                }

                if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_PFLAGS_WOOLFLAGHEAD)) {
                    final ItemStack itemStack = new ItemStack(this.getFlagOverrideTeamMaterial(this.arena, teamName));
                    player.getInventory().setHelmet(itemStack);
                }
                this.applyEffects(player);
                this.getFlagMap().put(teamName, player.getName());
                player.getInventory().addItem(block.getDrops().toArray(new ItemStack[0]));
                block.setType(Material.AIR);;
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();

        final Arena arena = ArenaPlayer.parsePlayer(player.getName()).getArena();

        if (arena == null || !arena.getName().equals(this.arena.getName())) {
            return;
        }

        if (event.isCancelled() || this.getHeldFlagTeam(player.getName()) == null) {
            return;
        }

        if (event.getInventory().getType() == InventoryType.CRAFTING && event.getRawSlot() != 5) {
            return;
        }

        event.setCancelled(true);
    }
}
