package net.slipcor.pvparena.compatibility;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import static net.slipcor.pvparena.compatibility.Constants.INFINITE_EFFECT_DURATION;
import static net.slipcor.pvparena.core.VersionUtils.isApiVersionNewerThan;

/**
 * Util class to freeze/unfreeze player using either negative jump boost (for API <= 1.20.2)
 * or using an entity attribute (for 1.20.5+)
 */
public class EntityFreezeUtil {
    private static final boolean USE_120_VERSION = isApiVersionNewerThan("1.20.5");
    private static final float DEFAULT_WALK_SPEED = 0.2f;

    public static void freezePlayer(Player player) {
        player.setWalkSpeed(0);
        if (USE_120_VERSION) {
            player.getAttribute(AttributeAdapter.JUMP_STRENGTH.getValue()).setBaseValue(0);
        } else {
            player.removePotionEffect(EffectTypeAdapter.JUMP_BOOST);
            player.addPotionEffect(new PotionEffect(EffectTypeAdapter.JUMP_BOOST, INFINITE_EFFECT_DURATION, -7, false, false, false));
        }
    }

    public static void unfreezePlayer(Player player) {
        if(player.getWalkSpeed() == 0) {
            player.setWalkSpeed(DEFAULT_WALK_SPEED);
            if (USE_120_VERSION) {
                AttributeInstance playerJumpStrengthAttr = player.getAttribute(AttributeAdapter.JUMP_STRENGTH.getValue());
                playerJumpStrengthAttr.setBaseValue(playerJumpStrengthAttr.getDefaultValue());
            } else {
                player.removePotionEffect(EffectTypeAdapter.JUMP_BOOST);
            }
        }
    }
}
