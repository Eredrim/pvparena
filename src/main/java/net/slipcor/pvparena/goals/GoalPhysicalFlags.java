package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "PhysicalFlags"
 * </pre>
 * <p/>
 * Capture flags by breaking them, bring them home, get points, win.
 *
 * @author slipcor
 */

public class GoalPhysicalFlags extends AbstractFlagGoal {
    private Map<ArenaTeam, BlockData> flagDataMap;

    public GoalPhysicalFlags() {
        super("PhysicalFlags");
    }

    @Override
    protected CFG getFlagTypeCfg() {
        return CFG.GOAL_PFLAGS_FLAGTYPE;
    }

    @Override
    protected CFG getFlagEffectCfg() {
        return CFG.GOAL_PFLAGS_FLAGEFFECT;
    }

    @Override
    protected boolean hasWoolHead() {
        return this.arena.getConfig().getBoolean(CFG.GOAL_PFLAGS_WOOLFLAGHEAD);
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
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

        Material flagType = this.arena.getConfig().getMaterial(CFG.GOAL_PFLAGS_FLAGTYPE);
        if (!ColorUtils.isSubType(block.getType(), flagType)) {
            debug(this.arena, player, "block, but not flag");
            return false;
        }
        debug(this.arena, player, "flag click!");

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if (this.getFlagMap().containsValue(player.getName())) {
            debug(this.arena, player, "player " + player.getName() + " has got a flag");

            final Vector vLoc = block.getLocation().toVector();
            final ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
            debug(this.arena, player, "block: " + vLoc);
            Vector vFlag = null;
            if (this.getTeamFlagLoc(arenaTeam) != null) {
                vFlag = this.getTeamFlagLoc(arenaTeam).toLocation().toVector();
            } else {
                debug(this.arena, player, arenaTeam + "flag = null");
            }

            debug(this.arena, player, "player is in the team " + arenaTeam);
            if (vFlag != null && vLoc.distance(vFlag) < 2) {

                debug(this.arena, player, "player is at his flag");

                if (this.getFlagMap().containsKey(arenaTeam) || this.getFlagMap().keySet().stream()
                        .anyMatch(team -> team.getName().equals(TOUCHDOWN))) {
                    debug(this.arena, player, "the flag of the own team is taken!");

                    if (this.arena.getConfig().getBoolean(CFG.GOAL_PFLAGS_MUSTBESAFE)
                            && this.getFlagMap().keySet().stream()
                            .noneMatch(team -> team.getName().equals(TOUCHDOWN))) {
                        debug(this.arena, player, "cancelling");

                        this.arena.msg(player, MSG.GOAL_FLAGS_NOTSAFE);
                        return false;
                    }
                }

                ArenaTeam flagTeam = this.getHeldFlagTeam(player);

                debug(this.arena, player, "the flag belongs to team " + flagTeam);

                ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                if (!ColorUtils.isSubType(mainHandItem.getType(), flagType)) {
                    debug(this.arena, player, "player " + player.getName() + " is not holding the flag");
                    this.arena.msg(player, MSG.GOAL_PHYSICALFLAGS_HOLDFLAG);
                    return false;
                }

                player.getInventory().remove(mainHandItem);
                player.updateInventory();

                try {
                    if (TOUCHDOWN.equals(flagTeam.getName())) {
                        this.arena.broadcast(Language.parse(
                                MSG.GOAL_FLAGS_TOUCHHOME, arenaTeam
                                        .colorizePlayer(player)
                                        + ChatColor.YELLOW, String
                                        .valueOf(this.getTeamLifeMap().get(arenaPlayer
                                                .getArenaTeam()) - 1)));
                    } else {
                        this.arena.broadcast(Language.parse(
                                MSG.GOAL_FLAGS_BROUGHTHOME, arenaTeam.colorizePlayer(player)
                                        + ChatColor.YELLOW,
                                flagTeam.getColoredName()
                                        + ChatColor.YELLOW, String
                                        .valueOf(this.getTeamLifeMap().get(flagTeam) - 1)));
                    }
                    this.getFlagMap().remove(flagTeam);
                } catch (final Exception e) {
                    Bukkit.getLogger().severe(
                            "[PVP Arena] team unknown/no lives: " + flagTeam);
                    e.printStackTrace();
                }
                if (TOUCHDOWN.equals(flagTeam.getName())) {
                    this.releaseFlag(this.touchdownTeam);
                } else {
                    this.releaseFlag(flagTeam);
                }
                this.removeEffects(player);
                if (this.hasWoolHead()) {
                    player.getInventory().setHelmet(new ItemStack(Material.AIR, 1));
                } else {
                    if (this.getHeadGearMap().get(arenaPlayer) == null) {
                        player.getInventory().setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
                        this.getHeadGearMap().remove(arenaPlayer);
                    }
                }

                if (this.touchdownTeam.equals(flagTeam)) {
                    checkAndCommitTouchdown(this.arena, arenaPlayer.getArenaTeam());
                } else {
                    this.reduceLivesCheckEndAndCommit(this.arena, flagTeam);
                }

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);

                return true;
            }
        }

        return false;
    }

    @Override
    protected void commit(final Arena arena, final ArenaTeam arenaTeam, final boolean win) {
        super.commit(arena, arenaTeam, win);
        this.getFlagDataMap().clear();
    }

    @Override
    public void disconnect(final ArenaPlayer arenaPlayer) {
        if (this.getFlagMap().isEmpty()) {
            return;
        }
        final ArenaTeam flagTeam = this.getHeldFlagTeam(arenaPlayer.getPlayer());

        if (flagTeam == null) {

            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPEDTOUCH, arenaPlayer
                    .getArenaTeam().getColorCodeString()
                    + arenaPlayer.getName()
                    + ChatColor.YELLOW));

            this.getFlagMap().remove(this.touchdownTeam);
            if (this.getHeadGearMap().get(arenaPlayer) != null) {
                arenaPlayer.getPlayer().getInventory()
                        .setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
                this.getHeadGearMap().remove(arenaPlayer);
            }

            this.releaseFlag(this.touchdownTeam);
        } else {
            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPED, arenaPlayer
                    .getArenaTeam().getColorCodeString()
                    + arenaPlayer.getName()
                    + ChatColor.YELLOW, flagTeam.getName() + ChatColor.YELLOW));
            this.getFlagMap().remove(flagTeam);
            if (this.getHeadGearMap().get(arenaPlayer) != null) {
                arenaPlayer.getPlayer().getInventory()
                        .setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
                this.getHeadGearMap().remove(arenaPlayer);
            }

            this.releaseFlag(flagTeam);
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        Config cfg = this.arena.getConfig();
        sender.sendMessage("flageffect: " + cfg.getString(CFG.GOAL_PFLAGS_FLAGEFFECT));
        sender.sendMessage("flagtype: " + cfg.getString(CFG.GOAL_PFLAGS_FLAGTYPE));
        sender.sendMessage("lives: " + cfg.getInt(CFG.GOAL_PFLAGS_LIVES));
        sender.sendMessage(StringParser.colorVar("mustbesafe", cfg.getBoolean(CFG.GOAL_PFLAGS_MUSTBESAFE))
                + " | " + StringParser.colorVar("flaghead", this.hasWoolHead()));
    }

    private Map<ArenaTeam, BlockData> getFlagDataMap() {
        if (this.flagDataMap == null) {
            this.flagDataMap = new HashMap<>();
        }
        return this.flagDataMap;
    }

    @Override
    public void initiate(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();
        if (!this.getTeamLifeMap().containsKey(team)) {
            this.getTeamLifeMap().put(aPlayer.getArenaTeam(), this.arena.getConfig().getInt(CFG.GOAL_PFLAGS_LIVES));
        }
    }

    @Override
    public void parsePlayerDeath(final Player player,
                                 final PADeathInfo deathInfo) {

        if (this.getFlagMap().isEmpty()) {
            debug(this.arena, player, "no flags set!!");
            return;
        }
        final ArenaTeam flagTeam = this.getHeldFlagTeam(player);
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if (flagTeam == null) {
            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPEDTOUCH, arenaPlayer
                    .getArenaTeam().getColorCodeString()
                    + arenaPlayer.getName()
                    + ChatColor.YELLOW));

            this.getFlagMap().remove(this.touchdownTeam);
            if (this.getHeadGearMap().get(arenaPlayer) != null) {
                arenaPlayer.getPlayer().getInventory().setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
                this.getHeadGearMap().remove(arenaPlayer);
            }

            this.releaseFlag(this.touchdownTeam);
        } else {
            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPED, arenaPlayer
                            .getArenaTeam().colorizePlayer(player) + ChatColor.YELLOW,
                    flagTeam.getColoredName() + ChatColor.YELLOW));
            this.getFlagMap().remove(flagTeam);
            if (this.getHeadGearMap().get(arenaPlayer) != null) {
                player.getInventory().setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
                this.getHeadGearMap().remove(arenaPlayer);
            }

            this.releaseFlag(flagTeam);
        }
    }

    @Override
    public void parseStart() {
        this.getTeamLifeMap().clear();
        this.getFlagDataMap().clear();
        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            if (!arenaTeam.getTeamMembers().isEmpty()) {
                debug(this.arena, "adding team " + arenaTeam.getName());
                // team is active
                this.getTeamLifeMap().put(arenaTeam, this.arena.getConfig().getInt(CFG.GOAL_PFLAGS_LIVES, 3));
                Block flagBlock = this.getTeamFlagLoc(arenaTeam).toLocation().getBlock();
                this.getFlagDataMap().put(arenaTeam, flagBlock.getBlockData().clone());
            }
        }
        this.touchdownTeam = new ArenaTeam(TOUCHDOWN, "BLACK");
        ofNullable(this.getTeamFlagLoc(this.touchdownTeam)).ifPresent(paBlockLocation -> {
            Block touchdownFlagBlock = paBlockLocation.toLocation().getBlock();
            this.getFlagDataMap().put(this.touchdownTeam, touchdownFlagBlock.getBlockData().clone());
        });
    }

    @Override
    public void reset(final boolean force) {
        this.getHeadGearMap().clear();
        this.getTeamLifeMap().clear();
        this.getFlagMap().clear();
        if(!this.getFlagDataMap().isEmpty()) {
            for (ArenaTeam arenaTeam : this.arena.getTeams()) {
                this.releaseFlag(arenaTeam);
            }
            this.releaseFlag(this.touchdownTeam);
        }
        this.getFlagDataMap().clear();
    }

    /**
     * reset an arena flag
     *
     * @param arenaTeam  team whose flag needs to be reset
     */
    private void releaseFlag(final ArenaTeam arenaTeam) {
        PABlockLocation paBlockLocation = this.getTeamFlagLoc(arenaTeam);
        if (paBlockLocation == null) {
            return;
        }

        Block flagBlock = paBlockLocation.toLocation().getBlock();
        try {
            flagBlock.setBlockData(this.getFlagDataMap().get(arenaTeam));
        } catch (Exception e) {
            PVPArena.getInstance().getLogger().warning("Impossible to reset flag data ! You may recreate arena flags.");
        }
    }

    public void checkBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        Material brokenMaterial = event.getBlock().getType();
        if (!this.arena.hasPlayer(event.getPlayer()) ||
                !ColorUtils.isSubType(brokenMaterial, this.arena.getConfig().getMaterial(CFG.GOAL_PFLAGS_FLAGTYPE))) {

            debug(this.arena, player, "block destroy, ignoring");
            debug(this.arena, player, String.valueOf(this.arena.hasPlayer(event.getPlayer())));
            debug(this.arena, player, event.getBlock().getType().name());
            return;
        }

        final Block block = event.getBlock();

        debug(this.arena, player, "flag destroy!");

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (this.getFlagMap().containsValue(player.getName())) {
            debug(this.arena, player, "already carries a flag!");
            return;
        }
        final ArenaTeam pTeam = aPlayer.getArenaTeam();
        if (pTeam == null) {
            return;
        }

        final Set<ArenaTeam> setTeam = new HashSet<>(this.arena.getTeams());

        setTeam.add(new ArenaTeam(TOUCHDOWN, "BLACK"));
        for (ArenaTeam arenaTeam : setTeam) {
            final PABlockLocation teamFlagLoc = this.getTeamFlagLoc(arenaTeam);

            if (arenaTeam.equals(pTeam)) {
                debug(this.arena, player, "equals!OUT! ");
                continue;
            }
            if (arenaTeam.isEmpty() && !this.touchdownTeam.equals(arenaTeam)) {
                debug(this.arena, player, "size!OUT! ");
                continue; // dont check for inactive teams
            }
            if (this.getFlagMap().containsKey(arenaTeam)) {
                debug(this.arena, player, "taken!OUT! ");
                continue; // already taken
            }
            debug(this.arena, player, "checking for flag of team " + arenaTeam);
            Vector vLoc = block.getLocation().toVector();
            debug(this.arena, player, "block: " + vLoc);

            if(teamFlagLoc != null && vLoc.equals(teamFlagLoc.toLocation().toVector())) {
                debug(this.arena, player, "flag found!");

                if (this.touchdownTeam.equals(arenaTeam)) {

                    this.arena.broadcast(Language.parse(
                            MSG.GOAL_FLAGS_GRABBEDTOUCH,
                            pTeam.colorizePlayer(player) + ChatColor.YELLOW));
                } else {

                    this.arena.broadcast(Language
                            .parse(MSG.GOAL_FLAGS_GRABBED,
                                    pTeam.colorizePlayer(player)
                                            + ChatColor.YELLOW,
                                    arenaTeam.getColoredName()
                                            + ChatColor.YELLOW));
                }
                try {
                    this.getHeadGearMap().put(ArenaPlayer.fromPlayer(player), player.getInventory().getHelmet().clone());
                } catch (final Exception ignored) {

                }

                if (this.hasWoolHead()) {
                    final ItemStack itemStack = new ItemStack(this.getFlagOverrideTeamMaterial(this.arena, arenaTeam));
                    player.getInventory().setHelmet(itemStack);
                }
                this.applyEffects(player);
                this.getFlagMap().put(arenaTeam, player.getName());
                player.getInventory().addItem(new ItemStack(block.getType()));
                block.setType(Material.AIR);
                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void checkInventory(InventoryClickEvent event) throws GameplayException {
        if (!this.isIrrelevantInventoryClickEvent(event) && this.getFlagType().equals(event.getCurrentItem().getType())) {
            event.setCancelled(true);
            throw new GameplayException("INVENTORY not allowed");
        }
    }
}
