package net.slipcor.pvparena.api;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.config.Debugger;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.statistics.dao.PlayerArenaStatsDao;
import net.slipcor.pvparena.statistics.dao.PlayerArenaStatsDaoImpl;
import net.slipcor.pvparena.statistics.model.PlayerArenaStats;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

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
        return "1.0.0";
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
        } else {
            arena = ArenaManager.getArenaByExactName(params[0]);
        }

        if (arena != null && params.length > 1) {
            PlaceholderArgs phArgs = new PlaceholderArgs(arena, identifier);
            return this.getArenaPlaceholder(phArgs, player);
        }
        return null;
    }

    private String getArenaPlaceholder(PlaceholderArgs phArgs, OfflinePlayer player) {
        Arena arena = phArgs.getArena();
        switch (phArgs.getAction()) {
            case "capacity":
                return this.getCapacityPlaceholder(phArgs);
            case "topscore":
                return this.getScorePlaceholder(phArgs);
            case "stats":
                return this.getArenaStatsPlaceholder(phArgs);
            case "pcolor":
                return this.colorPlayer(phArgs);
            case "tcolor":
                return this.colorTeam(phArgs);
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
                return this.cache.getPlayerStat(phArgs, statEntry, statsSupplier).get(rowIndex);
            }
        } catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException e) {
            Debugger.trace("Exception caught while parsing stat placeholder '{}': {}", phArgs.getIdentifier(), e);
        }
        return null;
    }

    private String getScorePlaceholder(PlaceholderArgs phArgs) {
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
}
