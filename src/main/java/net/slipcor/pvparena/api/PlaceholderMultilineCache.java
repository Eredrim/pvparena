package net.slipcor.pvparena.api;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.statistics.model.PlayerArenaStats;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.arena.PlayerStatus.*;

/**
 * Cache class used to limit DB/code calls when multiline placeholders are displayed. Stores a cache of a response for a
 * common placeholder identifier (eg: pvpa_mystat for placeholders pvpa_mystat_1 and pvpa_mystat_2). This cache provide
 * a kind of garbage collector which each tick will clear outdated cached response and will mark as "outdated" current
 * cache values. In consequences, cache will be stored for a duration of only two ticks.
 */
public class PlaceholderMultilineCache {
    private final ConcurrentMap<String, List<String>> cacheMap;
    private final List<String> purgeableKeys;

    public PlaceholderMultilineCache() {
        this.cacheMap = new ConcurrentHashMap<>();
        this.purgeableKeys = new ArrayList<>();

        Bukkit.getScheduler().runTaskTimer(PVPArena.getInstance(), () -> {
                purgeableKeys.forEach(cacheMap::remove);
                purgeableKeys.clear();
                purgeableKeys.addAll(cacheMap.keySet());
        }, 1, 0);
    }

    /**
     * Returns the list of interpreted placeholders using the same stat key, aka all the lines of a stat placeholder.
     * Ask for the cache and if it is empty, make the request passed the supplier and refill the cache before returning
     * the response.
     *
     * @param phArgs Placeholder arguments
     * @param statEntry The stat asked by user (eg: WINS)
     * @param statsSupplier The supplier of statistics (that executing SQL requests)
     * @return list of interpreted placeholders
     */
    public List<String> getPlayerStat(PlaceholderArgs phArgs, StatEntry statEntry, Supplier<List<PlayerArenaStats>> statsSupplier) {
        String commonId = phArgs.getIdentifierUntil(2);
        String accessKey = phArgs.getIdentifierUntil(3);
        return ofNullable(this.cacheMap.get(accessKey))
                .orElseGet(() -> {
                    List<PlayerArenaStats> statsList = statsSupplier.get();
                    String scoreKey = String.format("%s_%s", commonId, "score");
                    String playerKey = String.format("%s_%s", commonId, "player");
                    List<String> scoreListCache = new ArrayList<>();
                    List<String> playerListCache = new ArrayList<>();
                    statsList.forEach(st -> {
                        ofNullable(st.getPlayerName()).ifPresent(playerName -> {
                            scoreListCache.add(st.getValueByStatType(statEntry).toString());
                            playerListCache.add(playerName);
                        });
                    });
                    this.cacheMap.put(scoreKey, scoreListCache);
                    this.cacheMap.put(playerKey, playerListCache);
                    return this.cacheMap.get(accessKey);
                });
    }

    /**
     * Returns the list of interpreted placeholder for FFA score.
     * Ask for the cache and if it is empty, make the request passed the supplier and refill the cache before returning
     * the response.
     *
     * @param phArgs Placeholder arguments
     * @return list of interpreted placeholders
     */
    public List<String> getFreeForAllScore(PlaceholderArgs phArgs) {
        String commonId = phArgs.getIdentifierUntil(1);
        String accessKey = phArgs.getIdentifierUntil(2);
        return ofNullable(this.cacheMap.get(accessKey))
                .orElseGet(() -> {
                    Map<String, String> sortableMap = new HashMap<>();
                    List<String> scoreListCache = new ArrayList<>();
                    List<String> playerListCache = new ArrayList<>();
                    String scoreKey = String.format("%s_%s", commonId, "value");
                    String playerKey = String.format("%s_%s", commonId, "player");
                    phArgs.getArena().getEveryone().forEach(arenaPlayer -> {
                        int value = WorkflowManager.handleGetLives(phArgs.getArena(), arenaPlayer);
                        if (value >= 0 && asList(FIGHT, DEAD, LOST).contains(arenaPlayer.getStatus())) {
                            sortableMap.put(arenaPlayer.getName(), String.valueOf(value));
                        }
                    });
                    sortableMap.entrySet().stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .forEach(entry -> {
                                playerListCache.add(entry.getKey());
                                scoreListCache.add(entry.getValue());
                            });
                    this.cacheMap.put(scoreKey, scoreListCache);
                    this.cacheMap.put(playerKey, playerListCache);
                    return this.cacheMap.get(accessKey);
                });
    }

    /**
     * Returns the list of interpreted placeholder for Team score.
     * Ask for the cache and if it is empty, make the request passed the supplier and refill the cache before returning
     * the response.
     *
     * @param phArgs Placeholder arguments
     * @return list of interpreted placeholders
     */
    public List<String> getTeamsScore(PlaceholderArgs phArgs) {
        String commonId = phArgs.getIdentifierUntil(1);
        String accessKey = phArgs.getIdentifierUntil(2);
        return ofNullable(this.cacheMap.get(accessKey))
                .orElseGet(() -> {
                    Map<String, String> sortableMap = new HashMap<>();
                    List<String> scoreListCache = new ArrayList<>();
                    List<String> teamListCache = new ArrayList<>();
                    String scoreKey = String.format("%s_%s", commonId, "value");
                    String teamKey = String.format("%s_%s", commonId, "team");
                    phArgs.getArena().getTeams().forEach(team ->
                        team.getTeamMembers().stream().findAny().ifPresent(randomTeamPlayer -> {
                            int value = WorkflowManager.handleGetLives(phArgs.getArena(), randomTeamPlayer);
                            sortableMap.put(team.getName(), String.valueOf(value));
                        })
                    );
                    sortableMap.entrySet().stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .forEach(entry -> {
                                teamListCache.add(entry.getKey());
                                scoreListCache.add(entry.getValue());
                            });
                    this.cacheMap.put(scoreKey, scoreListCache);
                    this.cacheMap.put(teamKey, teamListCache);
                    return this.cacheMap.get(accessKey);
                });
    }
}
