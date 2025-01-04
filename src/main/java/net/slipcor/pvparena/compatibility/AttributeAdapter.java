package net.slipcor.pvparena.compatibility;

import org.bukkit.attribute.Attribute;

import static net.slipcor.pvparena.core.VersionUtils.isApiVersionNewerThan;

/**
 * Compatibility class to make PVPArena work with 1.21.2+ names of entity attributes
 * Limited to attributes used in the plugin and its modules.
 */
public enum AttributeAdapter {
    MAX_HEALTH("GENERIC_MAX_HEALTH"),
    JUMP_STRENGTH("GENERIC_JUMP_STRENGTH");

    private static final boolean USE_121_VERSION = isApiVersionNewerThan("1.21.2");

    private final String previousName;

    public Attribute getValue() {
        if(USE_121_VERSION) {
            return Attribute.valueOf(this.name());
        }
        return Attribute.valueOf(this.previousName);
    }

    AttributeAdapter(String previousName) {
        this.previousName = previousName;
    }
}
