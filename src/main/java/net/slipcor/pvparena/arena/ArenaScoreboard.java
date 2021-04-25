package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.arena.PlayerStatus.*;
import static net.slipcor.pvparena.config.Debugger.debug;

public class ArenaScoreboard {
    private final Arena arena;
    private final boolean special;
    private Scoreboard scoreboard;

    public ArenaScoreboard(Arena arena) {
        this.arena = arena;
        this.special = arena.getConfig().getBoolean(Config.CFG.USES_SCOREBOARD);

        if(this.special) {
            this.initSpecialScoreboard();
        } else {
            this.initCommonScoreboard(false);
        }
    }

    public void setupPlayer(final ArenaPlayer arenaPlayer) {
        Player player = arenaPlayer.getPlayer();

        debug(this.arena, "ScoreBoards: Initiating scoreboard for player " + player.getName());
        debug(this.arena, "ScoreBoards: has backup: " + arenaPlayer.hasBackupScoreboard());
        if (!arenaPlayer.hasBackupScoreboard()) {
            arenaPlayer.setBackupScoreboard(player.getScoreboard());
            ofNullable(player.getScoreboard().getEntryTeam(arenaPlayer.getName())).ifPresent(team ->
                    arenaPlayer.setBackupScoreboardTeam(team.getName())
            );
        }

        if (this.special) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                this.registerPlayerInTeam(arenaPlayer);
                this.refreshForPlayer(player);
            }, 1L);
        } else {
            player.setScoreboard(this.scoreboard);
            this.registerPlayerInTeam(arenaPlayer);
        }
    }

    public void show() {
        this.getLivesObjective().setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void reset(final Player player, final boolean force, final boolean soft) {
        if (this.special) {
            String msg = "ScoreBoards: " + (soft ? "(soft) " : "") + "remove: " + player.getName();
            debug(this.arena, player, msg);
            try {
                if (this.scoreboard != null) {
                    for (Team team : this.scoreboard.getTeams()) {
                        if (team.hasEntry(player.getName())) {
                            team.removeEntry(player.getName());
                            if (soft) {
                                this.refresh();
                                return;
                            }
                            this.scoreboard.resetScores(player.getName());
                        }
                    }
                } else {
                    debug(this.arena, "ScoreBoards: scoreboard is null!");
                    return;
                }

                final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
                if (ap.hasBackupScoreboard()) {
                    debug(this.arena, "ScoreBoards: restoring " + ap.getPlayer());

                    class RunLater extends BukkitRunnable {
                        @Override
                        public void run() {
                            Scoreboard backupScoreboard = ap.getBackupScoreboard();
                            if (ap.getBackupScoreboardTeam() != null && !force) {
                                backupScoreboard.getTeam(ap.getBackupScoreboardTeam()).addEntry(ap.getName());
                            }
                            player.setScoreboard(backupScoreboard);
                            ap.setBackupScoreboardTeam(null);
                            ap.setBackupScoreboard(null);
                        }
                    }

                    if (force) {
                        new RunLater().run();
                    } else {
                        try {
                            new RunLater().runTaskLater(PVPArena.getInstance(), 2L);
                        } catch (IllegalStateException ignored) {

                        }
                    }
                }

            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            Team team = this.scoreboard.getEntryTeam(player.getName());
            if (team != null) {
                team.removeEntry(player.getName());
                if (soft) {
                    return;
                }
            }
            final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
            if (ap.hasBackupScoreboard()) {
                try {
                    Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                        Scoreboard backupScoreboard = ap.getBackupScoreboard();
                        if (ap.getBackupScoreboardTeam() != null) {
                            backupScoreboard.getTeam(ap.getBackupScoreboardTeam()).addEntry(ap.getName());
                        }
                        player.setScoreboard(backupScoreboard);
                        ap.setBackupScoreboardTeam(null);
                    }, 3L);
                } catch (IllegalPluginAccessException ignored) {

                }
            }
        }
    }

    public void switchPlayerTeam(final Player player, final ArenaTeam oldTeam, final ArenaTeam newTeam) {
        if (this.special) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                this.applyTeamSwitching(player, oldTeam, newTeam);
                this.refreshForPlayer(player);
            }, 1L);
        } else {
            this.applyTeamSwitching(player, oldTeam, newTeam);
        }
    }

    public void refresh() {
        if (this.special) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if (this.arena.isFreeForAll()) {
                    for (ArenaPlayer arenaPlayer : this.arena.getEveryone()) {
                        int value = WorkflowManager.handleGetLives(this.arena, arenaPlayer);
                        if (value >= 0 && asList(FIGHT, DEAD, LOST).contains(arenaPlayer.getStatus())) {
                            ofNullable(this.getLivesObjective()).ifPresent(objective ->
                                    objective.getScore(arenaPlayer.getName()).setScore(value)
                            );
                        }
                        Player player = arenaPlayer.getPlayer();
                        if (!this.scoreboard.equals(player.getScoreboard())) {
                            player.setScoreboard(this.scoreboard);
                        }
                    }
                } else {
                    for (ArenaTeam team : this.arena.getTeams()) {
                        team.getTeamMembers().stream().findFirst().ifPresent(randomTeamPlayer ->
                                this.getLivesObjective()
                                        .getScore(team.getName())
                                        .setScore(WorkflowManager.handleGetLives(this.arena, randomTeamPlayer))
                        );
                    }
                    for (ArenaPlayer arenaPlayer : this.arena.getEveryone()) {
                        Player player = arenaPlayer.getPlayer();
                        if (!this.scoreboard.equals(player.getScoreboard())) {
                            player.setScoreboard(this.scoreboard);
                        }
                    }
                }
            }, 1L);
        }
    }

    public boolean addCustomEntry(final ArenaModule module, final String key, final int value) {
        debug("module " + module + " tries to set custom scoreboard value '" + key + "' to score " + value);
        if (StringUtils.isEmpty(key)) {
            debug("empty -> remove");
            return this.removeCustomEntry(module, value);
        }

        try {
            Team mTeam = null;
            String string;
            String prefix;
            String suffix;

            if (key.length() < 17) {
                string = key;
                prefix = "";
                suffix = "";
            } else {
                String[] split  = StringParser.splitForScoreBoard(key);
                prefix = split[0];
                string = split[1];
                suffix = split[2];
            }
            for (Team team : this.scoreboard.getTeams()) {
                if (team.getName().equals("pa_msg_" + value)) {
                    mTeam = team;
                }
            }

            if (mTeam == null) {
                mTeam = this.scoreboard.registerNewTeam("pa_msg_" + value);
            }

            mTeam.setPrefix(prefix);
            mTeam.setSuffix(suffix);

            for (String entry : this.scoreboard.getEntries()) {
                if (this.getLivesObjective().getScore(entry).getScore() == value) {
                    this.getLivesObjective().getScore(string).setScore(0);
                    this.scoreboard.resetScores(entry);
                    mTeam.removeEntry(entry);
                    break;
                }
            }
            mTeam.addEntry(string);
            this.getLivesObjective().getScore(string).setScore(value);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean removeCustomEntry(final ArenaModule module, final int value) {
        debug("module " + module + " tries to unset custom scoreboard value '" + value + "'");
        if (this.scoreboard == null) {
            debug("scoreboard is not setup!");
            return false;
        }
        try {
            Team mTeam = null;

            for (Team team : this.scoreboard.getTeams()) {
                if (team.getName().equals("pa_msg_" + value)) {
                    mTeam = team;
                }
            }

            if (mTeam == null) {
                return true;
            }

            for (String entry : this.scoreboard.getEntries()) {
                if (this.getLivesObjective().getScore(entry).getScore() == value) {
                    this.getLivesObjective().getScore(entry).setScore(0);
                    this.scoreboard.resetScores(entry);
                    mTeam.removeEntry(entry);
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Objective getLivesObjective() {
        return this.scoreboard.getObjective("lives");
    }

    private void initSpecialScoreboard() {
        this.initCommonScoreboard(true);

        // length = 18 without arena name
        String sbHeaderPrefix = ChatColor.GREEN + "PVP Arena" + ChatColor.RESET + " - " + ChatColor.YELLOW;
        String sbHeaderName = sbHeaderPrefix + this.arena.getName();

        if (sbHeaderName.length() > 32) {
            if (this.arena.getPrefix().length() <= 14) {
                sbHeaderName = sbHeaderPrefix + this.arena.getPrefix();
            } else {
                sbHeaderName = sbHeaderName.substring(0, 32);
            }
        }

        ofNullable(this.getLivesObjective()).ifPresent(objective -> {
            objective.unregister();
            ofNullable(this.scoreboard.getObjective(DisplaySlot.SIDEBAR)).ifPresent(Objective::unregister);
        });

        Objective obj = this.scoreboard.registerNewObjective("lives", "dummy", sbHeaderName); //deathCount

        if (this.arena.isFightInProgress()) {
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    private void initCommonScoreboard(boolean addTeamEntry) {
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        for (ArenaTeam team : this.arena.getTeams()) {
            final Team sbTeam = this.scoreboard.registerNewTeam(team.getName());
            sbTeam.setPrefix(team.getColor().toString());
            sbTeam.setSuffix(ChatColor.RESET.toString());
            sbTeam.setColor(team.getColor());
            sbTeam.setCanSeeFriendlyInvisibles(!this.arena.isFreeForAll());
            sbTeam.setAllowFriendlyFire(this.arena.getConfig().getBoolean(Config.CFG.PERMS_TEAMKILL));
            if (!this.arena.getConfig().getBoolean(Config.CFG.PLAYER_COLLISION)) {
                sbTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }

            if (addTeamEntry) {
                sbTeam.addEntry(team.getName());
            }
        }
    }

    private void refreshForPlayer(final Player player) {
        if (this.special) {
            final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);

            // if player is a spectator, special case. Just update and do not add to the scores
            if (ap.getArenaTeam() != null) {
                this.getLivesObjective()
                        .getScore(this.arena.isFreeForAll() ? player.getName() : ap.getArenaTeam().getName())
                        .setScore(WorkflowManager.handleGetLives(this.arena, ap));
            }

            player.setScoreboard(this.scoreboard);
        }
    }

    private void registerPlayerInTeam(ArenaPlayer arenaPlayer) {
        Optional<Team> optBoardTeam = ofNullable(arenaPlayer.getArenaTeam())
                .map(team -> this.scoreboard.getTeam(team.getName()));

        optBoardTeam.ifPresent(boardTeam -> boardTeam.addEntry(arenaPlayer.getName()));
    }

    private void applyTeamSwitching(final Player player, final ArenaTeam oldTeam, final ArenaTeam newTeam) {
        ofNullable(this.scoreboard.getTeam(oldTeam.getName())).ifPresent(team -> team.removeEntry(player.getName()));

        Team sTeam = this.scoreboard.getTeams().stream()
                .filter(t -> t.getName().equals(newTeam.getName()))
                .findFirst()
                .orElseGet(() -> this.addNewTeam(newTeam));
        sTeam.addEntry(player.getName());
    }

    private Team addNewTeam(ArenaTeam newTeam) {
        final Team sTeam = this.scoreboard.registerNewTeam(newTeam.getName());
        sTeam.setPrefix(newTeam.getColor().toString());
        sTeam.setSuffix(ChatColor.RESET.toString());
        sTeam.setColor(newTeam.getColor());
        sTeam.setCanSeeFriendlyInvisibles(!this.arena.isFreeForAll());
        return sTeam;
    }
}
