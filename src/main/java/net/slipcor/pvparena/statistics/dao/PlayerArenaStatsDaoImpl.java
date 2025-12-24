package net.slipcor.pvparena.statistics.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.statistics.model.PlayerArenaStats;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

public class PlayerArenaStatsDaoImpl extends BaseDaoImpl<PlayerArenaStats, Long> implements PlayerArenaStatsDao {
    private static PlayerArenaStatsDaoImpl instance;

    public static PlayerArenaStatsDaoImpl getInstance() {
        return ofNullable(instance).orElseGet(() -> {
            try {
                instance = new PlayerArenaStatsDaoImpl(PVPArena.getInstance().getDbConnector().getConnection());
                return instance;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private PlayerArenaStatsDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, PlayerArenaStats.class);
    }

    @Override
    public Optional<PlayerArenaStats> findByPlayerAndArena(Player player, Arena arena) {
        PlayerArenaStats playerArenaStats = null;
        debug("Getting player {} stats for arena {}", player, arena.getName());

        try {
            PreparedQuery<PlayerArenaStats> preparedQuery = super.queryBuilder()
                    .where()
                    .eq(StatEntry.PLAYER_UUID.getColumn(), player.getUniqueId().toString())
                    .and()
                    .eq(StatEntry.ARENA_UUID.getColumn(), arena.getUuid())
                    .prepare();

            return ofNullable(super.queryForFirst(preparedQuery));

        } catch (SQLException exception) {
            PVPArena.getInstance().getLogger().severe("Database error: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public Optional<PlayerArenaStats> findStatsSumByPlayer(@NotNull Player player) {
        PlayerArenaStats playerArenaStats = null;
        debug("Getting player {} stats for ALL arena", player);

        try {
            // Get the sum of all player stats grouped by arena
            QueryBuilder<PlayerArenaStats, Long> qb = super.queryBuilder()
                    .selectRaw("id", "player_uuid", "SUM(losses)", "SUM(wins)", "SUM(kills)", "SUM(deaths)",
                            "SUM(damage)", "MAX(max_damage)", "SUM(damage_taken)", "MAX(max_damage_taken)");

            qb.where().eq(StatEntry.PLAYER_UUID.getColumn(), new SelectArg());

            playerArenaStats = super.queryRaw(qb.prepareStatementString(), PlayerArenaStats::fromSumQueryResult, player.getUniqueId().toString())
                    .getFirstResult();

            return ofNullable(playerArenaStats);

        } catch (SQLException exception) {
            PVPArena.getInstance().getLogger().severe("Database error: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public List<PlayerArenaStats> findBestStatByArena(StatEntry entryName, Arena arena, Long limit) {
        debug("Getting stats ({}) for arena {}", entryName, arena.getName());

        try {
            PreparedQuery<PlayerArenaStats> preparedQuery = super.queryBuilder()
                    .orderBy(entryName.getColumn(), false)
                    .limit(limit)
                    .where()
                    .eq(StatEntry.ARENA_UUID.getColumn(), arena.getUuid())
                    .prepare();

            return super.query(preparedQuery);

        } catch (SQLException exception) {
            PVPArena.getInstance().getLogger().severe("Database error: " + exception.getMessage());
            return new ArrayList<>();
        }
    }

    public List<PlayerArenaStats> findBestStat(StatEntry entryName, Long limit) {
        debug("Getting best {} stats for all arenas", entryName);

        try {
            // Get the sum of all player stats grouped by arena
            QueryBuilder<PlayerArenaStats, Long> qb = super.queryBuilder()
                    .selectRaw("id", "player_uuid", "SUM(losses)", "SUM(wins)", "SUM(kills)", "SUM(deaths)",
                            "SUM(damage)", "MAX(max_damage)", "SUM(damage_taken)", "MAX(max_damage_taken)");

            qb.groupBy(StatEntry.PLAYER_UUID.getColumn())
                    .orderBy(entryName.getColumn(), false)
                    .limit(limit);

            return super.queryRaw(qb.prepareStatementString(), PlayerArenaStats::fromSumQueryResult).getResults();

        } catch (SQLException exception) {
            PVPArena.getInstance().getLogger().severe("Database error: " + exception.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(@NotNull PlayerArenaStats playerArenaStats) {
        debug("Saving player {} stats for arena {}", UUID.fromString(playerArenaStats.getPlayerUuid()), playerArenaStats.getArenaUuid());

        try {
            super.createOrUpdate(playerArenaStats);
        } catch (SQLException exception) {
            PVPArena.getInstance().getLogger().severe("Can't save or update to database: " + exception.getMessage());
        }
    }
}
