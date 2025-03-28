package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;

import net.slipcor.pvparena.events.goal.PAGoalPlayerDeathEvent;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

public class GoalTeamDeathConfirm extends AbstractTeamKillGoal {
    public GoalTeamDeathConfirm() {
        super("TeamDeathConfirm");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH);
    }

    @Override
    protected int getScore(ArenaTeam team) {
        return this.getTeamLivesCfg() - (this.getTeamLifeMap().getOrDefault(team, 0));
    }

    @Override
    protected int getTeamLivesCfg() {
        return this.arena.getConfig().getInt(CFG.GOAL_TDC_LIVES);
    }

    @Override
    public Boolean shouldRespawnPlayer(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        Player killer = deathInfo.getKiller();
        if (killer != null && this.arena.hasPlayer(killer)) {
            return true;
        }
        return null;
    }

    @Override
    public void commitPlayerDeath(final ArenaPlayer respawnPlayer, final boolean doesRespawn, PADeathInfo deathInfo) {
        final PAGoalPlayerDeathEvent gEvent = new PAGoalPlayerDeathEvent(this.arena, this, respawnPlayer, deathInfo, false);
        Bukkit.getPluginManager().callEvent(gEvent);

        final ArenaTeam respawnTeam = respawnPlayer.getArenaTeam();

        this.drop(respawnPlayer.getPlayer(), respawnTeam);

        if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            this.broadcastSimpleDeathMessage(respawnPlayer, deathInfo);
        }

        respawnPlayer.setMayDropInventory(true);
        respawnPlayer.setMayRespawn(true);
    }

    private void drop(final Player player, final ArenaTeam team) {
        Material material = this.arena.getConfig().getMaterial(CFG.GOAL_TDC_ITEM);
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(team.getColoredName());
        item.setItemMeta(meta);

        player.getWorld().dropItem(player.getLocation(), item);
    }

    @Override
    public void onPlayerPickUp(final EntityPickupItemEvent event) {
        final ItemStack item = event.getItem().getItemStack();

        final Material check = this.arena.getConfig().getMaterial(CFG.GOAL_TDC_ITEM);

        final ArenaPlayer player = ArenaPlayer.fromPlayer(event.getEntity().getName());

        if (item.getType().equals(check) && item.hasItemMeta()) {
            for (ArenaTeam team : this.arena.getTeams()) {
                if (item.getItemMeta().getDisplayName().equals(team.getColoredName())) {
                    // it IS an item !!!!

                    event.setCancelled(true);
                    event.getItem().remove();

                    if (team.equals(player.getArenaTeam())) {
                        // denied a kill
                        this.arena.broadcastExcept(event.getEntity(), Language.parse(MSG.GOAL_TEAMDEATHCONFIRM_DENIED, player.toString()));
                        this.arena.msg(event.getEntity(), MSG.GOAL_TEAMDEATHCONFIRM_YOUDENIED, player.toString());
                    } else {
                        // scored a kill
                        this.arena.broadcastExcept(event.getEntity(), Language.parse(MSG.GOAL_TEAMDEATHCONFIRM_SCORED, player.toString()));
                        this.arena.msg(event.getEntity(), MSG.GOAL_TEAMDEATHCONFIRM_YOUSCORED, player.toString());
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
        final int iLives = this.getTeamLifeMap().get(team);

        if (iLives <= 1) {
            for (ArenaTeam otherTeam : arena.getTeams()) {
                if (otherTeam.equals(team)) {
                    continue;
                }
                this.getTeamLifeMap().remove(otherTeam);
                for (ArenaPlayer ap : otherTeam.getTeamMembers()) {
                    if (ap.getStatus() == PlayerStatus.FIGHT) {
                        ap.setStatus(PlayerStatus.LOST);
                    }
                }
            }
            WorkflowManager.handleEnd(arena, false);
            return true;
        }
        arena.broadcast(Language.parse(MSG.GOAL_TEAMDEATHCONFIRM_REMAINING, String.valueOf(iLives - 1), team.getColoredName()));

        this.getTeamLifeMap().put(team, iLives - 1);
        arena.getScoreboard().refresh();
        return false;
    }

    @Override
    public void unload(final ArenaPlayer arenaPlayer) {
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(arenaPlayer);
        }
    }
}
