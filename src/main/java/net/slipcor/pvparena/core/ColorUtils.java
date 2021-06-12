package net.slipcor.pvparena.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.slipcor.pvparena.config.Debugger.debug;

public final class ColorUtils {

    private ColorUtils() {
    }

    public static Material getWoolMaterialFromDyeColor(final String color) {
        return getColoredMaterial(DyeColor.valueOf(color), Material.WHITE_WOOL);
    }

    public static Material getWoolMaterialFromChatColor(final ChatColor color) {
        return getColoredMaterialFromChatColor(color, Material.WHITE_WOOL);
    }

    /**
     * Get a colored Material from a chat color and any colorable material
     * @param color Color from chat
     * @param material Colorable material like Wool, Concrete or Stained glass
     * @return
     */
    public static Material getColoredMaterialFromChatColor(final ChatColor color, final Material material) {
        /*
        Unsupported:
        ChatColor.AQUA
        Material.BROWN_WOOL
        */

        DyeColor dyeColor = getDyeColorFromChatColor(color);
        return getColoredMaterial(dyeColor, material);
    }

    public static DyeColor getDyeColorFromChatColor(ChatColor color) {
        try {
            return DyeColor.valueOf(parseDyeColorToChatColor(color.name(), false));
        } catch (IllegalArgumentException e) {
            debug("ChatColor {} can't be cast to DyeColor => set BROWN", color);
            return DyeColor.BROWN;
        }
    }

    public static ChatColor getChatColorFromDyeColor(final String color) {
        return ChatColor.valueOf(parseDyeColorToChatColor(color, true));
    }

    private static String parseDyeColorToChatColor(final String color, final boolean forward) {
        /*
          following colors are the sames (ignore): WHITE, YELLOW, BLACK
          colors not being able to parse: chat-AQUA, wool-brown
         */

        final List<String> wool = Arrays.asList("ORANGE", "MAGENTA", "LIGHT_BLUE", "LIME", "PINK", "GRAY", "LIGHT_GRAY",
                "PURPLE", "BLUE", "GREEN", "RED", "CYAN");
        final List<String> chat = Arrays.asList("GOLD", "LIGHT_PURPLE", "BLUE", "GREEN", "RED", "DARK_GRAY", "GRAY",
                "DARK_PURPLE", "DARK_BLUE", "DARK_GREEN", "DARK_RED", "DARK_AQUA");

        if (forward) {
            if (wool.contains(color)) {
                return chat.get(wool.indexOf(color));
            }
        } else {
            if (chat.contains(color)) {
                return wool.get(chat.indexOf(color));
            }
        }
        return color;
    }

    /**
     * Check if a material can be colored
     * @param type Material to check
     * @return true if material can be colored
     */
    public static boolean isColorableMaterial(Material type) {
        return getColorableSuffixes().contains(getMaterialSuffix(type));
    }

    /**
     * Get a colored material applying dye color on a material
     * @param dyeColor DyeColor to get color
     * @param typeMaterial Material used to get type
     * @return colored material
     */
    public static Material getColoredMaterial(DyeColor dyeColor, Material typeMaterial) {
        String color = dyeColor.name();
        String materialSuffix = getMaterialSuffix(typeMaterial);
        return Material.valueOf(color + "_" + materialSuffix);
    }

    public static boolean isSubType(Material type, Material check) {
        return (type == check) || (isColorableMaterial(type) && getMaterialSuffix(type).equals(getMaterialSuffix(check)));
    }

    private static String getMaterialSuffix(Material material) {
        return getColorableSuffixes().stream()
                .filter(suffix -> material.name().endsWith(suffix))
                .findFirst()
                .orElse("");
    }

    /**
     * Get the list of all colorable blocks
     */
    private static List<String> getColorableSuffixes() {
        return Stream.of(Material.values())
                .filter(m -> m.name().startsWith("MAGENTA_"))
                .filter(Material::isBlock)
                .map(m -> m.name().split("MAGENTA_", 2)[1])
                .collect(Collectors.toList());
    }

    /**
     * Change flag color keeping rotation and facing
     * @param flagBlock Block (location) of the flag
     * @param flagColor New flag color
     */
    public static void setNewBlockColor(Block flagBlock, ChatColor flagColor) {
        final BlockData originalBlockData = flagBlock.getBlockData().clone();
        Material newMaterial = ColorUtils.getColoredMaterialFromChatColor(flagColor, flagBlock.getType());
        BlockData newData = Bukkit.getServer().createBlockData(newMaterial);

        if(originalBlockData instanceof Directional) {
            ((Directional) newData).setFacing(((Directional) originalBlockData).getFacing());
        }

        if(originalBlockData instanceof Rotatable) {
            ((Rotatable) newData).setRotation(((Rotatable) originalBlockData).getRotation());
        }

        flagBlock.setBlockData(newData);
    }
}
