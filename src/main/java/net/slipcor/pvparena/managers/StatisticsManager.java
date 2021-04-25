package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PAStatMap;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAKillEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Comparator.reverseOrder;
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
    private static File playersFile;
    private static YamlConfiguration config;

    private StatisticsManager() {}

    public enum Type {
        WINS("matches won", "Wins"),
        LOSSES("matches lost", "Losses"),
        KILLS("kills", "Kills"),
        DEATHS("deaths", "Deaths"),
        MAXDAMAGE("max damage dealt", "MaxDmg"),
        MAXDAMAGETAKE("max damage taken", "MaxDmgTaken"),
        DAMAGE("full damage dealt", "Damage"),
        DAMAGETAKE("full damage taken", "DamageTagen"),
        NULL("player name", "Player");

        private final String fullName;
        private final String niceDesc;

        Type(final String name, final String desc) {
            this.fullName = name;
            this.niceDesc = desc;
        }

        /**
         * return the next stat type
         *
         * @param tType the type
         * @return the next type
         */
        public static Type next(final Type tType) {
            final Type[] types = Type.values();
            final int ord = tType.ordinal();
            if (ord >= types.length - 2) {
                return types[0];
            }
            return types[ord + 1];
        }

        /**
         * return the previous stat type
         *
         * @param tType the type
         * @return the previous type
         */
        public static Type last(final Type tType) {
            final Type[] types = Type.values();
            final int ord = tType.ordinal();
            if (ord <= 0) {
                return types[types.length - 2];
            }
            return types[ord - 1];
        }

        /**
         * return the full stat name
         */
        public String getName() {
            return this.fullName;
        }

        /**
         * get the stat type by name
         *
         * @param string the name to find
         * @return the type if found, null otherwise
         */
        public static Type getByString(final String string) {
            for (Type t : Type.values()) {
                if (t.name().equalsIgnoreCase(string)) {
                    return t;
                }
            }
            return null;
        }

        public String getNiceName() {
            return this.niceDesc;
        }
    }

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
                final int maxdamage = apAttacker.getStatistics(arena).getStat(Type.MAXDAMAGE);
                apAttacker.getStatistics(arena).incStat(Type.DAMAGE, (int) dmg);
                if (dmg > maxdamage) {
                    apAttacker.getStatistics(arena).setStat(Type.MAXDAMAGE, (int) dmg);
                }
            }
        }
        final ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender.getName());

        final int maxdamage = apDefender.getStatistics(arena).getStat(Type.MAXDAMAGETAKE);
        apDefender.getStatistics(arena).incStat(Type.DAMAGETAKE, (int) dmg);
        if (dmg > maxdamage) {
            apDefender.getStatistics(arena).setStat(Type.MAXDAMAGETAKE, (int) dmg);
        }
    }

    /**
     * get an array of stats for arena boards and with a given stats type
     *
     * @param arena  the arena to check
     * @param statType the type to sort
     * @return an array of stats values
     */
    public static String[] getStatsValuesForBoard(final Arena arena, final Type statType) {
        debug("getting stats values: {} sorted by {}", (arena == null ? "global" : arena.getName()), statType);

        if (arena == null) {
            return ArenaPlayer.getAllArenaPlayers().stream()
                    .map(ap -> (statType == Type.NULL) ? ap.getName() : String.valueOf(ap.getTotalStatistics(statType)))
                    .sorted(reverseOrder())
                    .limit(8)
                    .toArray(String[]::new);
        }

        return arena.getFighters().stream()
                .map(ap -> (statType == Type.NULL) ? ap.getName() : String.valueOf(ap.getStatistics().getStat(statType)))
                .sorted(reverseOrder())
                .limit(8)
                .toArray(String[]::new);
    }

    /**
     * Get stats map for a given stat type
     * @param arena the arena to check
     * @param statType the kind of stat
     * @return A map with player name and stat value
     */
    public static Map<String, Integer> getStats(final Arena arena, final Type statType) {
        debug("getting stats: {} sorted by {}", (arena == null ? "global" : arena.getName()), statType);

        if (arena == null) {
            return ArenaPlayer.getAllArenaPlayers().stream()
                    .collect(Collectors.toMap(ArenaPlayer::getName, ap -> ap.getTotalStatistics(statType)));
        }

        return arena.getFighters().stream()
                .collect(Collectors.toMap(ArenaPlayer::getName, ap -> ap.getStatistics().getStat(statType)));
    }

    /**
     * get the type by the sign headline
     *
     * @param line the line to determine the type
     * @return the Statistics type
     */
    public static Type getTypeBySignLine(final String line) {
        final String stripped = ChatColor.stripColor(line).replace("[PA]", "").toUpperCase();

        for (Type t : Type.values()) {
            if (t.name().equals(stripped)) {
                return t;
            }
            if (t.getNiceName().equals(stripped)) {
                return t;
            }
        }
        return Type.NULL;
    }

    public static void initialize() {
        if (!PVPArena.getInstance().getConfig().getBoolean("stats")) {
            return;
        }
        config = new YamlConfiguration();
        playersFile = new File(PVPArena.getInstance().getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
                Arena.pmsg(Bukkit.getConsoleSender(), MSG.STATS_FILE_DONE);
            } catch (final Exception e) {
                Arena.pmsg(Bukkit.getConsoleSender(), MSG.ERROR_STATS_FILE);
                e.printStackTrace();
            }
        }

        try {
            config.load(playersFile);
        } catch (final Exception e) {
            Arena.pmsg(Bukkit.getConsoleSender(), MSG.ERROR_STATS_FILE);
            e.printStackTrace();
        }
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
            if (arena.hasPlayer(attacker)) {
                final PAKillEvent kEvent = new PAKillEvent(arena, attacker);
                Bukkit.getPluginManager().callEvent(kEvent);

                ArenaPlayer.fromPlayer(attacker.getName()).addKill();
            }
        }
        ArenaPlayer.fromPlayer(defender.getName()).addDeath();
    }

    public static void save() {
        if (config == null) {
            return;
        }
        try {
            config.save(playersFile);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadStatistics(final Arena arena) {
        if (!PVPArena.getInstance().getConfig().getBoolean("stats")) {
            return;
        }
        if (config == null) {
            initialize();
        }
        if (config.getConfigurationSection(arena.getName()) == null) {
            return;
        }

        debug(arena, "loading statistics not implemented yet.");
    }

    public static void update(final Arena arena, final ArenaPlayer aPlayer) {
        if (config == null) {
            return;
        }

        final PAStatMap map = aPlayer.getStatistics(arena);

        String node = aPlayer.getPlayer().getUniqueId().toString();

        final int losses = map.getStat(Type.LOSSES);
        config.set(arena.getName() + '.' + node + ".losses", losses);

        final int wins = map.getStat(Type.WINS);
        config.set(arena.getName() + '.' + node + ".wins", wins);

        final int kills = map.getStat(Type.KILLS);
        config.set(arena.getName() + '.' + node + ".kills", kills);

        final int deaths = map.getStat(Type.DEATHS);
        config.set(arena.getName() + '.' + node + ".deaths", deaths);

        final int damage = map.getStat(Type.DAMAGE);
        config.set(arena.getName() + '.' + node + ".damage", damage);

        final int maxdamage = map.getStat(Type.MAXDAMAGE);
        config.set(arena.getName() + '.' + node + ".maxdamage", maxdamage);

        final int damagetake = map.getStat(Type.DAMAGETAKE);
        config.set(arena.getName() + '.' + node + ".damagetake", damagetake);

        final int maxdamagetake = map.getStat(Type.MAXDAMAGETAKE);
        config.set(arena.getName() + '.' + node + ".maxdamagetake", maxdamagetake);

        if (!node.equals(aPlayer.getName())) {
            config.set(arena.getName() + '.' + node + ".playerName", aPlayer.getName());
        }

    }
}
