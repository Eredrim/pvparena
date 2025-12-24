package net.slipcor.pvparena.api;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.config.Debugger;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.statistics.dao.PlayerArenaStatsDao;
import net.slipcor.pvparena.statistics.dao.PlayerArenaStatsDaoImpl;
import net.slipcor.pvparena.statistics.model.PlayerArenaStats;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.core.StringUtils.startsWithIgnoreCase;

public class PVPArenaPlaceholderExpansion extends PlaceholderExpansion {

    private static final long MULTILINE_LIMIT = 10L;

    private final PlaceholderMultilineCache cache = new PlaceholderMultilineCache();

    public PVPArenaPlaceholderExpansion() {}

    /**
     * Name of the Expansion author
     */
    @Override
    public @NotNull String getAuthor() {
        return "Eredrim";
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "pvpa";
    }

    /**
     * Version of the expansion
     */
    @Override
    public @NotNull String getVersion() {
        return "1.1.0";
    }

    /**
     * This is required or else PlaceholderAPI will unregister the Expansion on reload
     * @return true to persist through reloads
     */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        identifier = PlaceholderAPI.setBracketPlaceholders(player, identifier);
        String[] params = identifier.split("_");
        Arena arena;
        if("cur".equalsIgnoreCase(params[0])) {
            arena = ArenaPlayer.fromPlayer(player.getPlayer()).getArena();
        } else if ("stats".equalsIgnoreCase(params[0]) && params.length >= 4) {
            PlaceholderArgs phArgs = new PlaceholderArgs(identifier);
            return this.getGlobalStatsPlaceholder(phArgs);
        } else {
            arena = ArenaManager.getArenaByExactName(params[0]);
            if (arena == null) {
                // try to find arena by UUID (at least a partial one) if name match failed
                arena = ArenaManager.getArenas().stream()
                        .filter(a -> startsWithIgnoreCase(a.getUuid(), params[0]))
                        .findAny()
                        .orElse(null);
            }
        }

        if (arena != null && params.length > 1) {
            PlaceholderArgs phArgs = new PlaceholderArgs(arena, identifier);
            return this.getArenaPlaceholder(phArgs, player);
        }
        return null;
    }

    private String getArenaPlaceholder(PlaceholderArgs phArgs, OfflinePlayer player) {
        switch (phArgs.getAction()) {
            case "name":
                return phArgs.getArena().getName();
            case "status":
                return this.getArenaStatus(phArgs);
            case "timer":
                return this.getRemainingTime(phArgs);
            case "pcount":
                return String.valueOf(phArgs.getArena().getFighters().size());
            case "pmincount":
                return String.valueOf(phArgs.getArena().getConfig().getInt(Config.CFG.READY_MINPLAYERS));
            case "pmaxcount":
                return String.valueOf(phArgs.getArena().getConfig().getInt(Config.CFG.READY_MAXPLAYERS));
            case "capacity":
                return this.getCapacityPlaceholder(phArgs);
            case "topscore":
                return this.getTopScorePlaceholder(phArgs);
            case "stats":
                return this.getArenaStatsPlaceholder(phArgs);
            case "pscore":
                return this.getPlayerScorePlaceholder(phArgs, player.getPlayer());
            case "tscore":
                return this.getTeamScorePlaceholder(phArgs, player.getPlayer());
            case "pcolor":
                return this.colorPlayer(phArgs);
            case "tcolor":
                return this.colorTeam(phArgs);
            case "tcolorcode":
                return this.getTeamColor(phArgs);
            case "class":
                return ArenaPlayer.fromPlayer(player.getPlayer()).getArenaClass().getName();
            case "team":
                return ArenaPlayer.fromPlayer(player.getPlayer()).getArenaTeam().getName();
            default:
                return null;
        }
    }

    private String colorPlayer(PlaceholderArgs phArgs) {
        ArenaTeam team =  phArgs.getArena().getEveryone().stream()
                .filter(ap -> ap.getName().equalsIgnoreCase(phArgs.getArg(2)))
                .findAny()
                .map(ArenaPlayer::getArenaTeam)
                .orElse(null);

        if(team != null) {
            return team.getColor() + phArgs.getArg(2) + ChatColor.RESET;
        }
        return null;
    }

    private String colorTeam(PlaceholderArgs phArgs) {
        ArenaTeam team = phArgs.getArena().getTeam(phArgs.getArg(2));
        if(team != null) {
            return team.getColor() + phArgs.getArg(2) + ChatColor.RESET;
        }
        return null;
    }

    private String getTeamColor(PlaceholderArgs phArgs) {
        ArenaTeam team = phArgs.getArena().getTeam(phArgs.getArg(2));
        if(team != null) {
            return team.getColor().toString();
        }
        return null;
    }

    private String getRemainingTime(PlaceholderArgs phArgs) {
        String timeFormat = ofNullable(phArgs.getArg(2)).orElse("mm:ss");
        long remainingSeconds = phArgs.getArena().getRemainingSeconds();
        if(remainingSeconds > -1) {
            return DurationFormatUtils.formatDuration(remainingSeconds * 1000, timeFormat, true);
        }
        return null;
    }

    private String getArenaStatus(PlaceholderArgs phArgs) {
        Arena arena = phArgs.getArena();
        boolean isRaw = phArgs.argEquals(2, "raw");
        if (arena.isLocked()) {
            return ArenaStatus.DISABLED.getPlaceholder(isRaw);
        }

        if (arena.isFightInProgress()) {
            return ArenaStatus.FIGHTING.getPlaceholder(isRaw);
        }

        if (arena.isResetting()) {
            return ArenaStatus.RESETTING.getPlaceholder(isRaw);
        }

        if (!arena.getFighters().isEmpty()) {
            return ArenaStatus.LOUNGE.getPlaceholder(isRaw);
        }

        return ArenaStatus.IDLE.getPlaceholder(isRaw);
    }

    private String getCapacityPlaceholder(PlaceholderArgs phArgs) {
        Arena arena = phArgs.getArena();
        if(phArgs.getArgsLength() <= 2) {
            int nbPlayers = arena.getFighters().size();
            int maxPlayers = arena.getConfig().getInt(Config.CFG.READY_MAXPLAYERS);
            return this.getFormattedCapacity(nbPlayers, maxPlayers);

        } else if(phArgs.argEquals(2, "team")) {
            ArenaTeam team = arena.getTeam(phArgs.getArg(3));
            if(team != null) {
                int nbPlayers = team.getTeamMembers().size();
                int maxPlayers = arena.getConfig().getInt(Config.CFG.READY_MAXTEAMPLAYERS);
                return this.getFormattedCapacity(nbPlayers, maxPlayers);
            }
        }
        return null;
    }

    private String getFormattedCapacity(int nbPlayers, int maxPlayers) {
        if(maxPlayers > 0) {
            return String.format("%d / %d", nbPlayers, maxPlayers);
        }
        return String.valueOf(nbPlayers);
    }

    private String getArenaStatsPlaceholder(PlaceholderArgs phArgs) {
        PlayerArenaStatsDao statsDao = PlayerArenaStatsDaoImpl.getInstance();
        try {
            int rowIndex = Integer.parseInt(phArgs.getArg(4));
            if(rowIndex >= 0 && rowIndex < MULTILINE_LIMIT) {
                StatEntry statEntry = StatEntry.parse(phArgs.getArg(2));
                Supplier<List<PlayerArenaStats>> statsSupplier = () -> statsDao.findBestStatByArena(statEntry, phArgs.getArena(), MULTILINE_LIMIT);
                List<String> playerStatList = this.cache.getPlayerStat(phArgs, statEntry, statsSupplier);

                if (playerStatList.isEmpty()) {
                    return "";
                }
                return playerStatList.get(rowIndex);
            }
        } catch (IndexOutOfBoundsException e) {
            Debugger.trace("Stat index is out of bounds '{}'", phArgs.getIdentifier());
            return "";
        } catch (NullPointerException | NumberFormatException e) {
            Debugger.trace("Exception caught while parsing stat placeholder '{}': {}", phArgs.getIdentifier(), e);
        }
        return null;
    }

    private String getGlobalStatsPlaceholder(PlaceholderArgs phArgs) {
        PlayerArenaStatsDao statsDao = PlayerArenaStatsDaoImpl.getInstance();
        try {
            int rowIndex = Integer.parseInt(phArgs.getArg(4));
            if(rowIndex >= 0 && rowIndex < MULTILINE_LIMIT) {
                StatEntry statEntry = StatEntry.parse(phArgs.getArg(2));
                Supplier<List<PlayerArenaStats>> statsSupplier = () -> statsDao.findBestStat(statEntry, MULTILINE_LIMIT);
                List<String> playerStatList = this.cache.getPlayerStat(phArgs, statEntry, statsSupplier);

                if (playerStatList.isEmpty()) {
                    return "";
                }
                return playerStatList.get(rowIndex);
            }
        } catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException e) {
            Debugger.trace("Exception caught while parsing global stat placeholder '{}': {}", phArgs.getIdentifier(), e);
        }
        return null;
    }

    private String getTopScorePlaceholder(PlaceholderArgs phArgs) {
        try {
            int rowIndex = Integer.parseInt(phArgs.getArg(3));
            if(phArgs.getArena().getGoal().isFreeForAll()) {
                return this.cache.getFreeForAllScore(phArgs).get(rowIndex);
            } else {
                return this.cache.getTeamsScore(phArgs).get(rowIndex);
            }
        } catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException e) {
            Debugger.trace("Exception caught while parsing stat placeholder '{}': {}", phArgs.getIdentifier(), e);
        }
        return null;
    }

    private String getTeamScorePlaceholder(PlaceholderArgs phArgs, Player player) {
        Arena arena = phArgs.getArena();
        String teamName = phArgs.getArg(2);
        ArenaTeam teamToCheck;

        if (teamName != null) {
            teamToCheck = arena.getTeam(teamName);
        } else {
            teamToCheck = ArenaPlayer.fromPlayer(player).getArenaTeam();
        }

        return ofNullable(teamToCheck)
                .map(team -> String.valueOf(arena.getGoal().getScore(team)))
                .orElse("");
    }

    private String getPlayerScorePlaceholder(PlaceholderArgs phArgs, Player player) {
        Arena arena = phArgs.getArena();
        String playerName = phArgs.getArg(2);
        ArenaPlayer playerToCheck;

        if (playerName != null) {
            playerToCheck = ArenaPlayer.fromPlayer(playerName);
        } else {
            playerToCheck = ArenaPlayer.fromPlayer(player);
        }

        return String.valueOf(WorkflowManager.handleGetScore(arena, playerToCheck));
    }
}
