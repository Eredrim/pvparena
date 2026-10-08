package net.slipcor.pvparena.statistics.model;

import java.util.Arrays;

public enum StatEntry {
    PLAYER_UUID("player_uuid", "player UUID", false),
    ARENA_UUID("arena_uuid", "arena UUID", false),
    LOSSES("losses", "matches lost", true),
    WINS("wins", "matches won", true),
    KILLS("kills", "kills", true),
    DEATHS("deaths", "deaths", true),
    DAMAGE("damage", "total damage given", true),
    MAX_DAMAGE("max_damage", "maximum damage given", true),
    DAMAGE_TAKEN("damage_taken", "total damage taken", true),
    MAX_DAMAGE_TAKEN("max_damage_taken", "maximum damage taken", true);

    private final String column;

    private final String label;

    private final boolean statType;

    StatEntry(String column, String label, boolean statType) {
        this.column = column;
        this.label = label;
        this.statType = statType;
    }

    public String getColumn() {
        return this.column;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isStatType() {
        return this.statType;
    }

    public static StatEntry[] getStatTypes() {
        return Arrays.stream(values())
                .filter(StatEntry::isStatType)
                .toArray(StatEntry[]::new);
    }

    /**
     * get the stat entry by name
     *
     * @param string the name to find
     * @return the type if found, null otherwise
     */
    public static StatEntry parse(String string) {
        for (StatEntry e : StatEntry.getStatTypes()) {
            if (e.name().equalsIgnoreCase(string)) {
                return e;
            }
        }
        return null;
    }

    public static StatEntry parseCompact(String string) {
        for (StatEntry e : StatEntry.getStatTypes()) {
            if (e.name().replace("_", "").equalsIgnoreCase(string)) {
                return e;
            }
        }
        return null;
    }
}
