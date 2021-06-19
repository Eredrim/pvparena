package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.List;

public class PAA_ForceWin extends AbstractArenaCommand {

    public PAA_ForceWin() {
        super(new String[]{"pvparena.cmds.forcewin"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{1})) {
            return;
        }

        final PADeathInfo pluginDeathCause = new PADeathInfo(EntityDamageEvent.DamageCause.LIGHTNING);

        // /pa {arenaname} forcewin [playername]
        // /pa {arenaname} forcewin [teamname]

        if (Bukkit.getPlayer(args[0]) == null && arena.isFreeForAll()) {
            arena.msg(sender, MSG.ERROR_PLAYER_NOTFOUND, args[0]);
        } else if (Bukkit.getPlayer(args[0]) == null) {
            ArenaTeam aTeam = arena.getTeam(args[0]);
            if (aTeam == null) {
                arena.msg(sender, MSG.ERROR_PLAYER_NOTFOUND, args[0]);
                arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, args[0]);
                return;
            }
            // existing team
            for (ArenaTeam team : arena.getTeams()) {
                if (team.getName().equalsIgnoreCase(aTeam.getName())) {
                    // skip winner
                    continue;
                }
                for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                        arenaPlayer.getPlayer().getWorld().strikeLightningEffect(arenaPlayer.getPlayer().getLocation());
                        arenaPlayer.handleDeathAndLose(pluginDeathCause);
                    }
                }
            }
        } else {
            // existing player name
            ArenaPlayer aplayer = ArenaPlayer.fromPlayer(args[0]);
            if (!arena.equals(aplayer.getArena())) {
                arena.msg(sender, MSG.ERROR_PLAYER_NOTFOUND, args[0]);
                return;
            }
            if (arena.isFreeForAll()) {
                for (ArenaPlayer arenaPlayer : arena.getFighters()) {
                    if (arenaPlayer.equals(aplayer)) {
                        continue;
                    }
                    if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                        arenaPlayer.getPlayer().getWorld().strikeLightningEffect(arenaPlayer.getPlayer().getLocation());
                        arenaPlayer.handleDeathAndLose(pluginDeathCause);
                    }
                }
            } else {
                for (ArenaTeam team : arena.getTeams()) {
                    if (team.getName().equalsIgnoreCase(aplayer.getArenaTeam().getName())) {
                        // skip winner
                        continue;
                    }
                    for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                        if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                            arenaPlayer.getPlayer().getWorld().strikeLightningEffect(arenaPlayer.getPlayer().getLocation());
                            arenaPlayer.handleDeathAndLose(pluginDeathCause);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.FORCEWIN);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("forcewin");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!fw");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        for (ArenaTeam team : arena.getTeams()) {
            result.define(new String[]{team.getName()});
        }
        result.define(new String[]{"{Player}"});
        return result;
    }
}
