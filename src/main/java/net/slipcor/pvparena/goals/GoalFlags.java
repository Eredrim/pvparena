package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

/**
 * <pre>
 * Arena Goal class "Flags"
 * </pre>
 * <p/>
 * Well, should be clear. Capture flags, bring them home, get points, win.
 *
 * @author slipcor
 */

public class GoalFlags extends AbstractBlockLocationGoal {

    protected static final String FLAG = "flag";
    protected static final String TOUCHDOWN = "touchdown";
    protected static final String FLAGEFFECT = "flageffect";

    // players currently holding a team flag
    protected Map<ArenaTeam, Player> flagHolders = new HashMap<>();
    // store player helmet to restore it (woolHead option)
    protected Map<ArenaPlayer, ItemStack> headGearMap = new HashMap<>();

    protected ArenaTeam touchdownTeam;

    public GoalFlags() {
        super("Flags");
    }

    @Override
    protected String getLocationBlockName() {
        return FLAG;
    }

    protected Config.CFG getFlagEffectCfg() {
        return Config.CFG.GOAL_FLAGS_FLAG_EFFECT;
    }

    protected boolean enableWoolHead() {
        return this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_WOOL_FLAG_HEAD);
    }

    @Override
    protected boolean isRegionBattleNeeded() {
        return this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE);
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        Config cfg = this.arena.getConfig();
        sender.sendMessage(StringParser.colorVar("breakToCapture", cfg.getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE)));
        sender.sendMessage("flageffect: " + cfg.getString(Config.CFG.GOAL_FLAGS_FLAG_EFFECT));
        sender.sendMessage("lives: " + cfg.getInt(Config.CFG.GOAL_FLAGS_LIVES));
        sender.sendMessage(StringParser.colorVar("mustbesafe", cfg.getBoolean(Config.CFG.GOAL_FLAGS_MUST_BE_SAFE))
                + " | " + StringParser.colorVar("flaghead", this.enableWoolHead())
                + " | " + StringParser.colorVar("whiteIfCaptured", cfg.getBoolean(Config.CFG.GOAL_FLAGS_WHITE_IF_CAPTURED))
                + " | " + StringParser.colorVar("noneIfCaptured", cfg.getBoolean(Config.CFG.GOAL_FLAGS_NONE_IF_CAPTURED)));
    }

    @Override
    public void initiate(final Player player) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
            this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(), this.arena.getConfig().getInt(Config.CFG.GOAL_FLAGS_LIVES));
        }
    }

    @Override
    public boolean checkCommand(final String command) {
        return FLAGEFFECT.equalsIgnoreCase(command)
                || super.checkCommand(command);
    }

    @Override
    public List<String> getGoalCommands() {
        return Stream.of(FLAGEFFECT, TOUCHDOWN, FLAG).collect(Collectors.toList());
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Material}"});
        return result;
    }

    protected void applyEffects(final Player player) {
        final String value = this.arena.getConfig().getDefinedString(this.getFlagEffectCfg());

        if (value == null) {
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

        PotionEffectType pet = PotionEffectType.getByName(split[0]);
        if (pet == null) {
            PVPArena.getInstance().getLogger().warning(
                    "Invalid Potion Effect Definition: " + split[0]);
            return;
        }

        player.addPotionEffect(new PotionEffect(pet, 2147000, amp));
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (FLAGEFFECT.equalsIgnoreCase(args[0])) {

            // /pa [arena] flageffect SLOW 2
            if (args.length < 2) {
                this.arena.msg(
                        sender,
                        Language.MSG.ERROR_INVALID_ARGUMENT_COUNT,
                        String.valueOf(args.length), "2");
                return;
            }

            if ("none".equalsIgnoreCase(args[1])) {
                this.arena.getConfig().set(this.getFlagEffectCfg(), args[1]);

                this.arena.getConfig().save();
                this.arena.msg(
                        sender,
                        Language.MSG.SET_DONE,
                        this.getFlagEffectCfg().getNode(), args[1]);
                return;
            }

            PotionEffectType pet = PotionEffectType.getByName(args[1]);
            if (pet == null) {
                this.arena.msg(sender, Language.MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[1]);
                return;
            }

            int amp = 1;

            if (args.length == 5) {
                try {
                    amp = Integer.parseInt(args[2]);
                } catch (final Exception e) {
                    this.arena.msg(sender, Language.MSG.ERROR_NOT_NUMERIC, args[2]);
                    return;
                }
            }
            final String value = args[1] + 'x' + amp;
            this.arena.getConfig().set(this.getFlagEffectCfg(), value);

            this.arena.getConfig().save();
            this.arena.msg(
                    sender,
                    Language.MSG.SET_DONE,
                    this.getFlagEffectCfg().getNode(), value);

        } else if (args[0].contains(getLocationBlockName())) {

            if (args.length < 2) {
                this.arena.msg(
                        sender,
                        Language.MSG.ERROR_INVALID_ARGUMENT_COUNT,
                        String.valueOf(args.length), "2");
                return;
            }

            if (!TOUCHDOWN.equalsIgnoreCase(args[1])) {
                ArenaTeam arenaTeam = this.arena.getTeam(args[1]);
                if (arenaTeam == null) {
                    this.arena.msg(
                            sender,
                            Language.MSG.ERROR_TEAM_NOT_FOUND,
                            args[1]);
                    return;
                }
            }

            this.setupBlockName = args[0];
            this.setupBlockTeamName = args[1];
            PAA_Region.activeSelections.put(sender.getName(), this.arena);

            this.arena.msg(sender,
                    Language.MSG.GOAL_FLAGS_TOSET, this.setupBlockName);

        } else if (TOUCHDOWN.equalsIgnoreCase(args[0])) {
            this.setupBlockName = args[0] + getLocationBlockName();
            PAA_Region.activeSelections.put(sender.getName(), this.arena);

            this.arena.msg(sender, Language.MSG.GOAL_FLAGS_TOSET, this.setupBlockName);
        }
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        if (this.setupBlockName == null || this.setupBlockTeamName == null) {
            return false;
        }

        if (!ColorUtils.isColorableMaterial(block.getType())) {
            // Block type is not colorable, warn user
            this.arena.msg(player, Language.MSG.WARN_MAT_NOT_COLORABLE, block.getType().name());
        }
        return super.commitSetBlock(player, block);
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[FLAGS] already ending");
            return;
        }
        debug(this.arena, "[FLAGS]");

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        ArenaTeam arenaTeam = null;

        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() == PlayerStatus.FIGHT) {
                    arenaTeam = team;
                    break;
                }
            }
        }

        if (arenaTeam != null && !force) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(Language.MSG.TEAM_HAS_WON, arenaTeam.getColor()
                            + arenaTeam.getName() + ChatColor.YELLOW), "END");

            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(Language.MSG.TEAM_HAS_WON, arenaTeam.getColor()
                            + arenaTeam.getName() + ChatColor.YELLOW), "WINNER");
            this.arena.broadcast(Language.parse(Language.MSG.TEAM_HAS_WON, arenaTeam.getColor()
                    + arenaTeam.getName() + ChatColor.YELLOW));
        }

        if (ArenaModuleManager.commitEnd(this.arena, arenaTeam)) {
            return;
        }
        new EndRunnable(this.arena, this.arena.getConfig().getInt(Config.CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(final Set<PASpawn> spawnsNames) {
        return super.checkForMissingSpawns(spawnsNames);
    }

    @NotNull
    protected Map<ArenaTeam, Player> getFlagHolders() {
        return this.flagHolders;
    }

    protected Material getFlagOverrideTeamMaterial(final Arena arena, final ArenaTeam team) {
        if (arena.getConfig().getUnsafe("flagColors." + team.getName()) == null) {
            if (this.touchdownTeam.equals(team)) {
                return ColorUtils.getWoolMaterialFromChatColor(ChatColor.BLACK);
            }
            return ColorUtils.getWoolMaterialFromChatColor(team.getColor());
        }
        return ColorUtils.getWoolMaterialFromDyeColor(
                (String) arena.getConfig().getUnsafe("flagColors." + team));
    }

    protected Map<ArenaPlayer, ItemStack> getHeadGearMap() {
        return this.headGearMap;
    }

    /**
     * get the team name of the flag a player holds
     *
     * @param player the player to check
     * @return a team name
     */
    protected ArenaTeam getHeldFlagTeam(final Player player) {
        if (this.getFlagHolders().isEmpty()) {
            return null;
        }

        debug(player, "getting held FLAG of player");
        for (final ArenaTeam arenaTeam : this.getFlagHolders().keySet()) {
            debug(player, "team {} is in {}'s hands", arenaTeam, this.getFlagHolders().get(arenaTeam));
            if (player.equals(this.getFlagHolders().get(arenaTeam))) {
                return arenaTeam;
            }
        }
        return null;
    }

    @Override
    public void commitArenaLoaded() {
        debug(this.arena, "[{}] commit Arena Loaded", this.name);
        this.touchdownTeam = new ArenaTeam(TOUCHDOWN, "BLACK", true);
        this.arena.getTeams().add(this.touchdownTeam);
    }

    @Override
    public void parseStart() {
        super.parseStart();
        this.getTeamLifeMap().clear();
        for (final ArenaTeam arenaTeam : this.arena.getTeams()) {
            if (!arenaTeam.getTeamMembers().isEmpty()) {
                debug(this.arena, "adding team {}", arenaTeam);
                // team is active
                this.getTeamLifeMap().put(arenaTeam,
                        this.arena.getConfig().getInt(Config.CFG.GOAL_FLAGS_LIVES, 3));
            }
            this.getTeamBlockLocations(arenaTeam).forEach(
                    blockLocation1 -> this.releaseFlag(arenaTeam, blockLocation1)
            );
        }

        final Optional<PABlock> touchdownFlagLocation = this.arena.getBlocks().stream()
                .filter(paBlock -> paBlock.getTeamName().equalsIgnoreCase(TOUCHDOWN)
                        && paBlock.getName().startsWith(this.getLocationBlockName()))
                .findFirst();
        if (touchdownFlagLocation.isPresent()) {
            this.teamsBlockLocations.put(touchdownFlagLocation.get().getLocation(), this.touchdownTeam);
        } else {
            debug(this.arena, "touchdown has no flag set !");
        }
        this.getTeamBlockLocations(this.touchdownTeam).forEach(
                blockLocation -> this.releaseFlag(this.touchdownTeam, blockLocation)
        );

    }

    /**
     * hook into an interacting player
     *
     * @param player the interacting player
     * @param event  the interact event
     * @return true if event has been handled
     */
    @Override
    public boolean checkInteract(Player player, PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if (block == null) {
            trace(arenaPlayer, "[{}] block null", this.name);
            return false;
        }
        trace(arenaPlayer, "[{}] checking interact", this.name);
        if (this.arena.realEndRunner != null) {
            debug(arenaPlayer, "[{}] already ending", this.name);
            return false;
        }

        if (this.teamsBlockLocations.keySet().stream()
                .noneMatch(blockLocation -> block.getLocation().equals(blockLocation.toLocation()))
        ) {
            debug(arenaPlayer, "block, but not flag location");
            return false;
        }

        final PABlockLocation blockLocation = new PABlockLocation(block.getLocation());
        final ArenaTeam arenaTeamFlag = this.teamsBlockLocations.get(blockLocation);

        if (arenaTeamFlag == null) {
            trace(arenaPlayer, "It's not a team flag. Valid are: {}", this.teamsBlockLocations.entrySet().toString());
            return false;
        }

        debug(arenaPlayer, "block of team {} found.", arenaTeamFlag);
        if (this.getFlagHolders().containsValue(player)) {
            bringFlag(arenaTeamFlag, arenaPlayer, blockLocation);
            return true;
        } else if (!this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE)) {
            // grab flag
            grabFlag(arenaTeamFlag, arenaPlayer, blockLocation);
            return true;
        }
        return false;
    }

    @Override
    public void checkBreak(final BlockBreakEvent event) {

        Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        Block block = event.getBlock();

        if (this.teamsBlockLocations.keySet().stream()
                .noneMatch(blockLocation -> block.getLocation().equals(blockLocation.toLocation()))
        ) {
            debug(arenaPlayer, "block, but not flag location");
            return;
        }

        trace(arenaPlayer, "Checking flag break");

        PABlockLocation blockLocation = new PABlockLocation(block.getLocation());
        ArenaTeam arenaTeamFlag = this.teamsBlockLocations.get(blockLocation);

        if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE)) {
            grabFlag(arenaTeamFlag, arenaPlayer, blockLocation);
        }
        event.setCancelled(true);
    }

    /**
     * When a player catch a enemy flag
     *
     * @param arenaTeamFlag arenaTeamFlag
     * @param arenaPlayer   arenaPlayer
     * @param blockLocation blockLocation
     */
    protected void grabFlag(ArenaTeam arenaTeamFlag, ArenaPlayer arenaPlayer, PABlockLocation blockLocation) {

        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        Player player = arenaPlayer.getPlayer();

        debug(arenaPlayer, "Try to grab the flag of team {}.", arenaTeamFlag);
        if (arenaTeam == null) {
            return;
        }

        if (this.getFlagHolders().containsValue(player)) {
            debug(arenaPlayer, "already carries a flag!");
            return;
        }

        if (arenaTeam.equals(arenaTeamFlag)) {
            debug(arenaPlayer, "can't capture his own flag");
            return;
        }

        // dont check for inactive teams
        if (arenaTeam.isEmpty() && !TOUCHDOWN.equals(arenaTeam.getName())) {
            debug(arenaPlayer, "can't capture empty team's flag");
            return;
        }

        // already taken
        if (this.getFlagHolders().containsKey(arenaTeamFlag)) {
            debug(arenaPlayer, "already taken");
            return;
        }

        if (this.touchdownTeam.equals(arenaTeam)) {

            this.arena.broadcast(
                    Language.parse(Language.MSG.GOAL_FLAGS_GRABBEDTOUCH,
                            arenaTeam.colorizePlayer(player) + ChatColor.YELLOW));
        } else {

            this.arena.broadcast(Language.parse(Language.MSG.GOAL_FLAGS_GRABBED,
                    arenaTeam.colorizePlayer(player)
                            + ChatColor.YELLOW,
                    arenaTeamFlag.getColoredName()
                            + ChatColor.YELLOW));
        }

        if (this.enableWoolHead()) {
            // save current helmet
            if (player.getInventory().getHelmet() != null) {
                this.getHeadGearMap().put(arenaPlayer, player.getInventory().getHelmet().clone());
            }

            // set wool head with picked flag color
            final ItemStack itemStack = new ItemStack(this.getFlagOverrideTeamMaterial(this.arena, arenaTeamFlag));
            player.getInventory().setHelmet(itemStack);
        }
        this.applyEffects(player);
        this.getFlagHolders().put(arenaTeamFlag, player);

        this.takeFlag(player, blockLocation, arenaTeamFlag);
    }

    /**
     * When a player bring back a enemy flag to home
     *
     * @param arenaTeamFlag the enemy team
     * @param arenaPlayer   arenaPlayer
     */
    protected void bringFlag(ArenaTeam arenaTeamFlag, ArenaPlayer arenaPlayer, PABlockLocation blockLocation) {

        Player player = arenaPlayer.getPlayer();
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE)
                && !ColorUtils.isSubType(mainHandItem.getType(), blockLocation.getBlockData().getMaterial())) {
            debug(arenaPlayer, "player is not holding the physical flag");
            this.arena.msg(player, Language.MSG.GOAL_PHYSICALFLAGS_HOLDFLAG);
            return;
        }

        // collect flag
        debug(arenaPlayer, "player " + player.getName() + " has got a flag");

        if (!arenaTeamFlag.equals(arenaTeam)) {
            debug(arenaPlayer, "player tried to collect flag but it is own team flag!");
            return;
        }

        ArenaTeam flagTeam = this.getHeldFlagTeam(player);
        debug(arenaPlayer, "the flag belongs to team ", flagTeam);

        if (this.getFlagHolders().containsKey(arenaTeam)) {
            debug(arenaPlayer, "the flag of the own team is taken!");

            if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_MUST_BE_SAFE)) {
                debug(arenaPlayer, "cancelling");

                this.arena.msg(player, Language.MSG.GOAL_FLAGS_NOTSAFE);
                return;
            }
        }

        if (this.touchdownTeam.equals(flagTeam)) {
            this.arena.broadcast(Language.parse(
                    Language.MSG.GOAL_FLAGS_TOUCHHOME, arenaTeam
                            .colorizePlayer(player)
                            + ChatColor.YELLOW, String
                            .valueOf(this.getTeamLifeMap().get(arenaPlayer
                                    .getArenaTeam()) - 1)));
        } else {
            this.arena.broadcast(Language.parse(
                    Language.MSG.GOAL_FLAGS_BROUGHTHOME, arenaTeam.colorizePlayer(player)
                            + ChatColor.YELLOW,
                    flagTeam.getColoredName()
                            + ChatColor.YELLOW, String
                            .valueOf(this.getTeamLifeMap().get(flagTeam) - 1)));
        }
        this.getFlagHolders().remove(flagTeam);
        this.getTeamBlockLocations(flagTeam).forEach(
                blockLocation1 -> this.releaseFlag(flagTeam, blockLocation1)
        );

        this.removeEffects(player);
        if (this.enableWoolHead()) {
            if (this.getHeadGearMap().get(ArenaPlayer.fromPlayer(player)) == null) {
                player.getInventory().setHelmet(
                        new ItemStack(Material.AIR, 1));
            } else {
                player.getInventory().setHelmet(
                        this.getHeadGearMap().get(ArenaPlayer.fromPlayer(player)).clone());
                this.getHeadGearMap().remove(ArenaPlayer.fromPlayer(player));
            }
        }
        this.computeScore(this.arena, arenaTeam, flagTeam);

        if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE)) {
            player.getInventory().remove(mainHandItem);
            player.updateInventory();
            return;
        }

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + arenaPlayer.getName());
        Bukkit.getPluginManager().callEvent(gEvent);
    }

    /**
     * Flags:
     * Team lose if score reach 0
     * Decrease score for each own flag capture
     * <p>
     * Special touchdown flag:
     * Team lose 1 point if capture the black flag
     * If team have only 1 point left, win !
     *
     * @param arena     arena
     * @param arenaTeam arenaTeam who capture the flag
     * @param flagTeam  arenaTeam of the flag captured
     */
    @Override
    protected void computeScore(final Arena arena, final ArenaTeam arenaTeam, final ArenaTeam flagTeam) {
        if (this.getTeamLifeMap().get(arenaTeam) == null) {
            debug(arena, "team {} has no lives remaining.", arenaTeam);
            return;
        }

        if (flagTeam.equals(this.touchdownTeam)) {
            debug(arena, "touchdown for team {}", arenaTeam);
            final int iLives = this.getTeamLifeMap().get(arenaTeam) - 1;
            if (iLives > 0) {
                this.getTeamLifeMap().put(arenaTeam, iLives);
            } else {
                this.getTeamLifeMap().remove(arenaTeam);
                this.commit(arena, arenaTeam, true);
            }
        } else {
            debug(arena, "reducing lives of team {}", flagTeam);
            final int iLives = this.getTeamLifeMap().get(flagTeam) - 1;
            if (iLives > 0) {
                this.getTeamLifeMap().put(flagTeam, iLives);
            } else {
                this.getTeamLifeMap().remove(flagTeam);
                this.commit(arena, flagTeam, false);
            }
        }
    }

    /**
     * take an arena flag
     *
     * @param paBlockLocation the location to take/reset
     */
    protected void takeFlag(Player player, final PABlockLocation paBlockLocation, final ArenaTeam arenaTeamFlag) {
        if (paBlockLocation == null) {
            return;
        }
        final Block block = paBlockLocation.toLocation().getBlock();
        if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE)) {
            player.getInventory().addItem(new ItemStack(block.getType()));
            applyToAllBlocksOfTeam(arenaTeamFlag, Material.AIR, null);

        } else {
            // set air only if flag must be safe otherwise player can't bring the enemy flag if his flag is missing...
            if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_NONE_IF_CAPTURED)
                    && this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_MUST_BE_SAFE)) {
                applyToAllBlocksOfTeam(arenaTeamFlag, Material.AIR, null);
                return;
            }

            Set<ArenaTeam> setTeam = new HashSet<>(this.arena.getTeams());
            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

            setTeam.add(new ArenaTeam(TOUCHDOWN, "BLACK"));
            for (ArenaTeam arenaTeam : setTeam) {

                if (arenaTeam.equals(ArenaPlayer.fromPlayer(player).getArenaTeam())) {
                    debug(arenaPlayer, "equals!OUT! ");
                    continue;
                }

                if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_WHITE_IF_CAPTURED)) {
                    if (ColorUtils.isColorableMaterial(block.getType())) {
                        this.applyToAllBlocksOfTeam(arenaTeamFlag, ChatColor.WHITE);
                    } else {
                        applyToAllBlocksOfTeam(arenaTeamFlag, Material.BEDROCK, null);
                    }
                }
            }
        }
    }

    /**
     * reset an arena flag
     *
     * @param arenaTeam       the team to reset
     * @param paBlockLocation the location to take/reset
     */
    protected void releaseFlag(ArenaTeam arenaTeam, PABlockLocation paBlockLocation) {
        if (paBlockLocation == null) {
            return;
        }

        Block flagBlock = paBlockLocation.toLocation().getBlock();
        Material blockType = paBlockLocation.getBlockData().getMaterial();

        if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_BREAK_TO_CAPTURE)) {
            debug("Restore flag with {}", paBlockLocation.getBlockData().getAsString());
            applyToAllBlocksOfTeam(arenaTeam, blockType, paBlockLocation.getBlockData());
            if (ColorUtils.isColorableMaterial(blockType)) {
                debug("color actual block {} in {}", flagBlock, arenaTeam.getColor());
                applyToAllBlocksOfTeam(arenaTeam, arenaTeam.getColor());
            }
        }

        if (this.arena.getConfig().getBoolean(Config.CFG.GOAL_FLAGS_WHITE_IF_CAPTURED)
                && ColorUtils.isColorableMaterial(blockType)) {
            debug(this.arena, "set color block");
            if (ColorUtils.isSubType(blockType, flagBlock.getType())) {
                debug("color actual block {} in {}", flagBlock, arenaTeam.getColor());
                applyToAllBlocksOfTeam(arenaTeam, arenaTeam.getColor());
            } else {
                debug("Block {} is not right type. Set it to {}", flagBlock.getType(), blockType);
                applyToAllBlocksOfTeam(arenaTeam, ColorUtils.getColoredMaterialFromChatColor(arenaTeam.getColor(), blockType),
                        paBlockLocation.getBlockData());
            }
        } else {
            debug("No color, set block {}", blockType);
            applyToAllBlocksOfTeam(arenaTeam, blockType, paBlockLocation.getBlockData());
        }
    }

    protected void removeEffects(final Player player) {
        final String value = this.arena.getConfig().getDefinedString(this.getFlagEffectCfg());

        if (value == null) {
            return;
        }

        final String[] split = value.split("x");
        PotionEffectType pet = PotionEffectType.getByName(split[0]);
        if (pet == null) {
            PVPArena.getInstance().getLogger().warning(String.format("Invalid Potion Effect Definition: %s", value));
            return;
        }

        player.removePotionEffect(pet);
        player.addPotionEffect(new PotionEffect(pet, 0, 1));
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        super.setDefaults(config);
        if (this.enableWoolHead() || config.get("flagColors") == null) {
            debug(this.arena, "no flag colors defined, adding red and blue!");
            config.addDefault("flagColors.red", DyeColor.RED.name());
            config.addDefault("flagColors.blue", DyeColor.BLUE.name());
        }
    }

    @Override
    public void unload(final Player player) {
        this.disconnect(ArenaPlayer.fromPlayer(player));
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.fromPlayer(player));
        }
    }

    @Override
    public void checkInventory(InventoryClickEvent event) throws GameplayException {
        if (this.isIrrelevantInventoryClickEvent(event)) {
            return;
        }

        if (this.enableWoolHead() && event.getSlotType() == InventoryType.SlotType.ARMOR
                && event.getCurrentItem() != null
                && ColorUtils.isSubType(event.getCurrentItem().getType(), Material.WHITE_WOOL)) {
            event.setCancelled(true);
            throw new GameplayException("INVENTORY not allowed");
        }
    }

    protected boolean isIrrelevantInventoryClickEvent(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final Arena arena = ArenaPlayer.fromPlayer(player).getArena();

        if (arena == null || !arena.getName().equals(this.arena.getName())) {
            return true;
        }

        if (event.isCancelled() || this.getHeldFlagTeam(player) == null) {
            return true;
        }

        if (event.getInventory().getType() == InventoryType.CRAFTING && event.getRawSlot() != 5) {
            return true;
        }

        return event.getCurrentItem() == null || !InventoryType.PLAYER.equals(event.getInventory().getType());
    }

    @Override
    public void disconnect(final ArenaPlayer arenaPlayer) {
        resetPlayer(arenaPlayer.getPlayer());
    }

    @Override
    public void parsePlayerDeath(final Player player,
                                 final PADeathInfo deathInfo) {
        resetPlayer(player);
    }

    protected void resetPlayer(Player player) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        if (this.getFlagHolders().isEmpty()) {
            debug(arenaPlayer, "no flags set!!");
            return;
        }

        ArenaTeam flagTeam = this.getHeldFlagTeam(player);

        if (flagTeam == null) {
            this.arena.broadcast(Language.parse(Language.MSG.GOAL_FLAGS_DROPPEDTOUCH, arenaPlayer
                    .getArenaTeam().getColorCodeString()
                    + arenaPlayer.getName()
                    + ChatColor.YELLOW));

            this.getFlagHolders().remove(this.touchdownTeam);
            if (this.getHeadGearMap().get(arenaPlayer) != null) {
                arenaPlayer.getPlayer().getInventory().setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
                this.getHeadGearMap().remove(arenaPlayer);
            }
            this.getTeamBlockLocations(this.touchdownTeam).forEach(
                    blockLocation1 -> this.releaseFlag(this.touchdownTeam, blockLocation1)
            );
        } else {
            this.arena.broadcast(Language.parse(Language.MSG.GOAL_FLAGS_DROPPED, arenaPlayer
                            .getArenaTeam().colorizePlayer(player) + ChatColor.YELLOW,
                    flagTeam.getColoredName() + ChatColor.YELLOW));
            this.getFlagHolders().remove(flagTeam);
            if (this.getHeadGearMap().get(arenaPlayer) != null) {
                player.getInventory().setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
                this.getHeadGearMap().remove(arenaPlayer);
            }
            this.getTeamBlockLocations(flagTeam).forEach(
                    blockLocation1 -> this.releaseFlag(flagTeam, blockLocation1)
            );
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getHeadGearMap().clear();
        this.getTeamLifeMap().clear();
        this.getFlagHolders().clear();

        this.arena.getTeams()
                .forEach(arenaTeam -> this.getTeamBlockLocations(arenaTeam).forEach(
                        blockLocation -> this.releaseFlag(arenaTeam, blockLocation)
                ));

        this.getTeamBlockLocations(this.touchdownTeam).forEach(
                blockLocation1 -> this.releaseFlag(this.touchdownTeam, blockLocation1)
        );
    }
}
