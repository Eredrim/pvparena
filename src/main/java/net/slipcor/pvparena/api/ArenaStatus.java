package net.slipcor.pvparena.api;

import net.slipcor.pvparena.core.Language;

import static net.slipcor.pvparena.core.Language.MSG.*;

/**
 * Enum used to show status of the Arena for Placeholders.
 * No other use!
 */
public enum ArenaStatus {
    IDLE(ARENA_STATUS_IDLE),
    LOUNGE(ARENA_STATUS_LOUNGE),
    FIGHTING(ARENA_STATUS_FIGHTING),
    RESETTING(ARENA_STATUS_RESETTING),
    DISABLED(ARENA_STATUS_DISABLED);

    private final Language.MSG languageNode;

    ArenaStatus(Language.MSG languageNode) {
        this.languageNode = languageNode;
    }

    public String getParsedMsg() {
        return Language.parse(this.languageNode);
    }

    public String getPlaceholder(boolean isRaw) {
        return isRaw ? this.name() : this.getParsedMsg();
    }
}
