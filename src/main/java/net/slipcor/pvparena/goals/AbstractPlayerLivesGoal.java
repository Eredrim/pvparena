package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.goal.PAGoalEndEvent;
import net.slipcor.pvparena.events.goal.PAGoalPlayerDeathEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import static net.slipcor.pvparena.config.Debugger.debug;

public abstract class AbstractPlayerLivesGoal extends ArenaGoal {

    protected EndRunnable endRunner;

    public AbstractPlayerLivesGoal(String goalName) {
        super(goalName);
    }

    @Override
    public Boolean shouldRespawnPlayer(ArenaPlayer arenaPlayer, PADeathInfo deathInfo) {
        int pos = this.getPlayerLifeMap().get(arenaPlayer);
        debug(arenaPlayer, "lives before death: " + pos);
        return pos > 1;
    }

    protected abstract void setWinnerAndBroadcastEndMessages(ArenaTeam teamToCheck);
    protected abstract ArenaPlayer getWinningPlayerIfNeeded(ArenaTeam teamToCheck);
    protected abstract int getLivesConfigValue();

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[LIVES] already ending");
            return;
        }
        final PAGoalEndEvent gEvent = new PAGoalEndEvent(this.arena, this);
        Bukkit.getPluginManager().callEvent(gEvent);

        for (ArenaTeam arenaTeam : this.arena.getNotEmptyTeams()) {
            this.setWinnerAndBroadcastEndMessages(arenaTeam);
            ArenaPlayer maybeWinningPlayer = this.getWinningPlayerIfNeeded(arenaTeam);

            if (ArenaModuleManager.commitEnd(this.arena, arenaTeam, maybeWinningPlayer)) {
                if (this.arena.realEndRunner == null) {
                    this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(
                            CFG.TIME_ENDCOUNTDOWN));
                }
                return;
            }
        }

        this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(ArenaPlayer arenaPlayer, boolean doesRespawn, PADeathInfo deathInfo) {

        if (!this.getPlayerLifeMap().containsKey(arenaPlayer)) {
            return;
        }

        final PAGoalPlayerDeathEvent gEvent = new PAGoalPlayerDeathEvent(this.arena, this, arenaPlayer, deathInfo, doesRespawn);
        Bukkit.getPluginManager().callEvent(gEvent);

        final int currentPlayerOrTeamLive = this.getPlayerLifeMap().get(arenaPlayer);

        debug(arenaPlayer, "lives before death: " + currentPlayerOrTeamLive);
        if (currentPlayerOrTeamLive <= 1) {
            this.getPlayerLifeMap().remove(arenaPlayer);

            debug(arenaPlayer, "no remaining lives -> LOST");
            arenaPlayer.handleDeathAndLose(deathInfo);

            WorkflowManager.handleEnd(this.arena, false);
        } else {
            int nextPlayerOrTeamLive = currentPlayerOrTeamLive - 1;
            this.getPlayerLifeMap().put(arenaPlayer, nextPlayerOrTeamLive);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (this.arena.getConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, arenaPlayer, deathInfo, nextPlayerOrTeamLive);
                } else {
                    this.broadcastSimpleDeathMessage(arenaPlayer, deathInfo);
                }
            }

            arenaPlayer.setMayDropInventory(true);
            arenaPlayer.setMayRespawn(true);
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: " + this.getLivesConfigValue());
    }

    @Override
    public void initiate(final ArenaPlayer arenaPlayer) {
        this.updateLives(arenaPlayer, this.getLivesConfigValue());
    }

    @Override
    public void parseLeave(final ArenaPlayer arenaPlayer) {
        if (arenaPlayer == null) {
            PVPArena.getInstance().getLogger().warning(this.getName() + ": player NULL");
            return;
        }
        this.getPlayerLifeMap().remove(arenaPlayer);
    }

    @Override
    public void parseStart() {
        for (ArenaTeam team : this.arena.getTeams()) {
            for (ArenaPlayer ap : team.getTeamMembers()) {
                this.updateLives(ap, this.getLivesConfigValue());
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
    }
}
