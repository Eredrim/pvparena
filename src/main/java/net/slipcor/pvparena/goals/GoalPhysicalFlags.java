package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.goal.PAGoalFlagBringEvent;
import net.slipcor.pvparena.events.goal.PAGoalFlagTakeEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

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
    public GoalPhysicalFlags() {
        super("PhysicalFlags");
    }

    @Override
    protected CFG getFlagEffectCfg() {
        return CFG.GOAL_PFLAGS_FLAGEFFECT;
    }

    @Override
    protected CFG getFlagLivesCfg() {
        return CFG.GOAL_PFLAGS_LIVES;
    }

    @Override
    protected boolean doesAutoColorBlocks() {
        return this.arena.getConfig().getBoolean(CFG.GOAL_PFLAGS_AUTOCOLOR);
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

        if (!this.isSameTypeThanFlags(block.getType())) {
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

                ArenaTeam flagTeam = this.getHeldFlagTeam(arenaPlayer);

                debug(this.arena, player, "the flag belongs to team " + flagTeam);

                ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                if (!this.isSameTypeThanFlags(mainHandItem.getType())) {
                    debug(this.arena, player, "player " + player.getName() + " is not holding the flag");
                    this.arena.msg(player, MSG.GOAL_PHYSICALFLAGS_HOLDFLAG);
                    return false;
                }

                player.getInventory().remove(mainHandItem);
                player.updateInventory();

                try {
                    if (TOUCHDOWN.equals(flagTeam.getName())) {
                        this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_TOUCHHOME,
                                arenaTeam.colorizePlayer(arenaPlayer) + ChatColor.YELLOW));
                    } else {
                        this.arena.broadcast(Language.parse(
                                MSG.GOAL_FLAGS_BROUGHTHOME, arenaTeam.colorizePlayer(arenaPlayer)
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
                    this.checkAndCommitTouchdown(this.arena, arenaPlayer.getArenaTeam());
                } else {
                    this.reduceLivesCheckEndAndCommit(this.arena, flagTeam);
                }

                final PAGoalFlagBringEvent gEvent = new PAGoalFlagBringEvent(this.arena, this, arenaPlayer, flagTeam);
                Bukkit.getPluginManager().callEvent(gEvent);

                return true;
            }
        }

        return false;
    }

    @Override
    protected void commit(final Arena arena, final ArenaTeam arenaTeam) {
        super.commit(arena, arenaTeam);
        this.getFlagDataMap().clear();
    }

    @Override
    public void disconnect(final ArenaPlayer arenaPlayer) {
        if (this.getFlagMap().isEmpty()) {
            return;
        }
        final ArenaTeam flagTeam = this.getHeldFlagTeam(arenaPlayer);

        if(flagTeam == null) {
            return;
        }

        if (this.touchdownTeam.equals(flagTeam)) {
            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPEDTOUCH, arenaPlayer
                    .getArenaTeam().getColorCodeString()
                    + arenaPlayer.getName()
                    + ChatColor.YELLOW));
        } else {
            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPED, arenaPlayer
                    .getArenaTeam().getColorCodeString()
                    + arenaPlayer.getName()
                    + ChatColor.YELLOW, flagTeam.getName() + ChatColor.YELLOW));

        }

        this.getFlagMap().remove(flagTeam);
        if (this.getHeadGearMap().get(arenaPlayer) != null) {
            arenaPlayer.getPlayer().getInventory().setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
            this.getHeadGearMap().remove(arenaPlayer);
        }

        this.releaseFlag(flagTeam);
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        Config cfg = this.arena.getConfig();
        sender.sendMessage("flag effect: " + cfg.getString(CFG.GOAL_PFLAGS_FLAGEFFECT));
        sender.sendMessage("lives: " + cfg.getInt(CFG.GOAL_PFLAGS_LIVES));
        sender.sendMessage("auto color: " + StringParser.colorVar(cfg.getBoolean(CFG.GOAL_FLAGS_AUTOCOLOR)));
        sender.sendMessage(StringParser.colorVar("mustbesafe", cfg.getBoolean(CFG.GOAL_PFLAGS_MUSTBESAFE))
                + " | " + StringParser.colorVar("flaghead", this.hasWoolHead()));
    }

    @Override
    public void parsePlayerDeath(final ArenaPlayer arenaPlayer,
                                 final PADeathInfo deathInfo) {

        if (this.getFlagMap().isEmpty()) {
            debug(arenaPlayer, "no flags set!!");
            return;
        }
        final ArenaTeam flagTeam = this.getHeldFlagTeam(arenaPlayer);

        if(flagTeam == null) {
            return;
        }

        if (this.touchdownTeam.equals(flagTeam)) {
            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPEDTOUCH, arenaPlayer
                    .getArenaTeam().getColorCodeString()
                    + arenaPlayer.getName()
                    + ChatColor.YELLOW));
        } else {
            this.arena.broadcast(Language.parse(MSG.GOAL_FLAGS_DROPPED, arenaPlayer
                            .getArenaTeam().colorizePlayer(arenaPlayer) + ChatColor.YELLOW,
                    flagTeam.getColoredName() + ChatColor.YELLOW));
        }

        this.getFlagMap().remove(flagTeam);
        if (this.getHeadGearMap().get(arenaPlayer) != null) {
            arenaPlayer.getPlayer().getInventory().setHelmet(this.getHeadGearMap().get(arenaPlayer).clone());
            this.getHeadGearMap().remove(arenaPlayer);
        }

        this.releaseFlag(flagTeam);
    }

    @Override
    public void reset(final boolean force) {
        super.reset(force);
        if(!this.getFlagDataMap().isEmpty()) {
            for (ArenaTeam arenaTeam : this.arena.getTeams()) {
                this.releaseFlag(arenaTeam);
            }
            this.releaseFlag(this.touchdownTeam);
        }
        this.getFlagDataMap().clear();
    }

    public void checkBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        Material brokenMaterial = event.getBlock().getType();
        if (!this.arena.hasPlayer(event.getPlayer()) || !this.isSameTypeThanFlags(brokenMaterial)) {

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
                            pTeam.colorizePlayer(aPlayer) + ChatColor.YELLOW));
                } else {

                    this.arena.broadcast(Language
                            .parse(MSG.GOAL_FLAGS_GRABBED,
                                    pTeam.colorizePlayer(aPlayer)
                                            + ChatColor.YELLOW,
                                    arenaTeam.getColoredName()
                                            + ChatColor.YELLOW));
                }
                try {
                    this.getHeadGearMap().put(aPlayer, player.getInventory().getHelmet().clone());
                } catch (final Exception ignored) {

                }

                if (this.hasWoolHead()) {
                    final ItemStack itemStack = new ItemStack(this.getFlagOverrideTeamMaterial(this.arena, arenaTeam));
                    player.getInventory().setHelmet(itemStack);
                }
                this.applyEffects(player);
                this.getFlagMap().put(arenaTeam, player.getName());
                ItemStack flagItemStack = block.getDrops().stream().findAny().get();
                flagItemStack.getItemMeta().addEnchant(Enchantment.VANISHING_CURSE, ENCHANT_LVL_KEY, true);
                flagItemStack.getItemMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);
                player.getInventory().addItem(flagItemStack);
                block.setType(Material.AIR);
                event.setCancelled(true);

                final PAGoalFlagTakeEvent gEvent = new PAGoalFlagTakeEvent(this.arena, this, aPlayer, arenaTeam);
                Bukkit.getPluginManager().callEvent(gEvent);
                return;
            }
        }
    }

    @Override
    public void checkInventory(InventoryClickEvent event) throws GameplayException {
        if (!this.isIrrelevantInventoryClickEvent(event)) {
            if(event.getCurrentItem().getEnchantmentLevel(Enchantment.VANISHING_CURSE) == ENCHANT_LVL_KEY) {
                event.setCancelled(true);
                throw new GameplayException("INVENTORY not allowed");
            }
        }
    }
}
