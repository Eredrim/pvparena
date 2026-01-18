package net.slipcor.pvparena.compatibility;

import org.bukkit.entity.EntityType;

/**
 * Enum used to map newer EntityTypes for compatibility purposes.
 * Add new entity types here as needed.
 */
public enum NewerEntityType {
    WIND_CHARGE;

    public boolean equates(EntityType entityType) {
        return this.name().equals(entityType.name());
    }
}
