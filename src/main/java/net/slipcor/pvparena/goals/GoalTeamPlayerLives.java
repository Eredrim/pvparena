package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

public class GoalTeamPlayerLives extends AbstractPlayerLivesGoal {

    public GoalTeamPlayerLives() {
        super("TeamPlayerLives");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean checkEnd() {
        debug(this.arena, "checkEnd - " + this.arena.getName());
        final int count = TeamManager.countActiveTeams(this.arena);
        debug(this.arena, "count: " + count);

        return (count <= 1); // yep. only one team left. go!
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingTeamSpawn(this.arena, spawns);
    }

    @Override
    protected void broadcastEndMessagesIfNeeded(ArenaTeam teamToCheck) {
        if(teamToCheck.getTeamMembers().stream().anyMatch(ap -> ap.getStatus() == PlayerStatus.FIGHT)) {
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, teamToCheck.getColoredName()), "END");
            ArenaModuleManager.announce(
                    this.arena,
                    Language.parse(MSG.TEAM_HAS_WON, teamToCheck.getColoredName()), "WINNER");

            this.arena.broadcast(Language.parse(MSG.TEAM_HAS_WON, teamToCheck.getColoredName()));
        }
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        // sum of all team members lives
        return arenaPlayer.getArenaTeam().getTeamMembers().stream()
                .mapToInt(ap -> this.getPlayerLifeMap().getOrDefault(ap.getPlayer(), 0))
                .sum();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (config.get("teams") == null) {
            debug(this.arena, "no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaPlayer ap : this.arena.getFighters()) {
            double score = this.getPlayerLifeMap().getOrDefault(ap.getPlayer(), 0);
            if (ap.getArenaTeam() != null) {
                if (scores.containsKey(ap.getArenaTeam().getName())) {
                    scores.put(ap.getArenaTeam().getName(), scores.get(ap.getName()) + score);
                } else {
                    scores.put(ap.getArenaTeam().getName(), score);
                }
            }
        }

        return scores;
    }
}
