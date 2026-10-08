package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAKillEvent;
import net.slipcor.pvparena.statistics.dao.PlayerArenaStatsDao;
import net.slipcor.pvparena.statistics.dao.PlayerArenaStatsDaoImpl;
import net.slipcor.pvparena.statistics.model.PlayerArenaStats;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Statistics Manager class</pre>
 * <p/>
 * Provides static methods to manage Statistics
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class StatisticsManager {

    private StatisticsManager() {}

    /**
     * commit damage
     *
     * @param arena    the arena where that happens
     * @param entity   an eventual attacker
     * @param defender the attacked player
     * @param dmg      the damage value
     */
    public static void damage(final Arena arena, final Entity entity, final Player defender, final double dmg) {

        debug(arena, defender, "adding damage to player " + defender.getName());


        if (entity instanceof Player) {
            final Player attacker = (Player) entity;
            debug(arena, defender, "attacker is player: " + attacker.getName());
            if (arena.hasPlayer(attacker)) {
                debug(arena, defender, "attacker is in the arena, adding damage!");
                final ArenaPlayer apAttacker = ArenaPlayer.fromPlayer(attacker.getName());
                final Long maxDamage = apAttacker.getStats().getMaxDamage();
                apAttacker.getStats().addDamage((long) dmg);
                if (dmg > maxDamage) {
                    apAttacker.getStats().setMaxDamage((long) dmg);
                }
            }
        }
        final ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender.getName());

        final Long maxDamageTake = apDefender.getStats().getMaxDamageTaken();
        apDefender.getStats().addDamageTake((long) dmg);
        if (dmg > maxDamageTake) {
            apDefender.getStats().setMaxDamageTaken((long) dmg);
        }
    }

    /**
     * Get stats map for a given stat type
     * @param arena the arena to check
     * @param statType the kind of stat
     * @return A map with player name and stat value
     */
    public static List<SimpleEntry<String, Long>> getStats(Arena arena, StatEntry statType, Long limit) {
        debug("getting stats: {} sorted by {}", (arena == null ? "global" : arena.getName()), statType);
        PlayerArenaStatsDao statsDao = PlayerArenaStatsDaoImpl.getInstance();

        List<PlayerArenaStats> statsResult;
        if (arena == null) {
            statsResult = statsDao.findBestStat(statType, limit);
        } else {
            statsResult = statsDao.findBestStatByArena(statType, arena, limit);
        }

        return statsResult.stream()
                .map(stat -> new SimpleEntry<>(stat.getPlayerName(), stat.getValueByStatType(statType)))
                .filter(entry -> entry.getKey() != null)
                .toList();
    }

    /**
     * commit a kill
     *
     * @param arena    the arena where that happens
     * @param entity   an eventual attacker
     * @param defender the attacked player
     */
    public static void kill(final Arena arena, final Entity entity, final Player defender,
                            final boolean willRespawn) {
        final PADeathEvent dEvent = new PADeathEvent(arena, defender, willRespawn, entity instanceof Player);
        Bukkit.getPluginManager().callEvent(dEvent);

        if (entity instanceof Player) {
            final Player attacker = (Player) entity;
            if (arena.hasPlayer(attacker) && !attacker.equals(defender)) {
                final PAKillEvent kEvent = new PAKillEvent(arena, attacker);
                Bukkit.getPluginManager().callEvent(kEvent);

                ArenaPlayer.fromPlayer(attacker.getName()).getStats().incKills();
            }
        }
        ArenaPlayer.fromPlayer(defender.getName()).getStats().incDeaths();
    }
}
