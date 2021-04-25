package net.slipcor.pvparena.core;

import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static java.util.Optional.ofNullable;

/**
 * <pre>
 * String Parser class
 * </pre>
 * <p/>
 * provides methods to parse Objects to String and back
 *
 * @author slipcor
 * @version v0.10.2
 */

public final class StringParser {

    private StringParser() {
    }

    public static final Set<String> positive = new HashSet<>(Arrays.asList(
            "yes", "on", "true", "1"));
    public static final Set<String> negative = new HashSet<>(Arrays.asList(
            "no", "off", "false", "0"));

    public static String colorize(final String string) {
        return ChatColor.translateAlternateColorCodes('&', string)
                .replace("&&", "&").replace("%%&%%", "&");
    }

    /**
     * color an integer if bigger than 0
     *
     * @param timed the integer to color
     * @return a colored string
     */
    public static String colorVar(final int timed) {
        return colorVar(String.valueOf(timed), timed > 0);
    }

    /**
     * color a boolean based on value
     *
     * @param value the boolean to color
     * @return a colored string
     */
    public static String colorVar(final boolean value) {
        return colorVar(String.valueOf(value), value);
    }

    public static String colorVar(final double value) {
        return colorVar(String.valueOf(value), value > 0);
    }

    /**
     * color a string if set
     *
     * @param string the string to color
     * @return a colored string
     */
    public static String colorVar(final String string) {
        if (string == null || string.isEmpty() || "none".equals(string)) {
            return colorVar("null", false);
        }
        return colorVar(string, true);
    }

    /**
     * color a string based on a given boolean
     *
     * @param string the string to color
     * @param value  true:green, false:red
     * @return a colored string
     */
    public static String colorVar(final String string, final boolean value) {
        return (value ? ChatColor.GREEN.toString() : ChatColor.RED.toString()) + string + ChatColor.WHITE;
    }

    public static String joinArray(final Object[] array, final String glue) {
        final StringBuilder result = new StringBuilder();
        for (Object o : array) {
            result.append(glue);
            result.append(o);
        }
        if (result.length() <= glue.length()) {
            return result.toString();
        }
        return result.substring(glue.length());
    }

    public static String joinList(final List<?> set, final String glue) {
        final StringBuilder result = new StringBuilder();
        for (Object o : set) {
            result.append(glue);
            result.append(o);
        }
        if (result.length() <= glue.length()) {
            return result.toString();
        }
        return result.substring(glue.length());
    }

    public static String joinSet(final Set<?> set, final String glue) {
        final StringBuilder result = new StringBuilder();
        for (Object o : set) {
            result.append(glue);
            result.append(o);
        }
        if (result.length() <= glue.length()) {
            return result.toString();
        }
        return result.substring(glue.length());
    }

    public static String[] shiftArrayBy(final String[] args, final int offset) {
        final String[] newArgs = new String[args.length - offset];
        System.arraycopy(args, offset, newArgs, 0, args.length - offset);
        return newArgs;
    }

    public static String[] unShiftArrayBy(final String[] args, final int offset) {
        final String[] newArgs = new String[args.length + offset];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }

    public static BlockFace parseToBlockFace(final String string) {
        if (string.startsWith("o") || string.startsWith("i")) {
            // out or in are just the same thing, reference to self with positive/negative values
            return BlockFace.SELF;
        }
        final BlockFace[] faces = {
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN};
        for (BlockFace face : faces) {
            if (face.name().startsWith(string.toUpperCase())) {
                return face;
            }
        }
        return null;
    }

    public static String[] splitForScoreBoard(String key) {
        String[] split = new String[]{"","",""};

        int pos = 0;
        StringBuffer buffer = new StringBuffer();

        for (char c : key.toCharArray()) {
            buffer.append(c);
            if (c == ChatColor.COLOR_CHAR) {
                if (buffer.length() >= 15) {
                    if (pos > 2) {
                        return split;
                    }
                    split[pos++] = buffer.toString();
                    buffer.setLength(0);
                }
                continue;
            }

            if (buffer.length() >= 16) {
                if (pos > 2) {
                    return split;
                }
                split[pos++] = buffer.toString();
                buffer.setLength(0);
            }
        }
        split[pos] = buffer.toString();
        return split;
    }

    public static String getItems(ItemStack[] items) {
        if (items == null || items.length < 1) {
            return "none";
        }
        StringBuffer buff = new StringBuffer();
        for (ItemStack item : items) {
            buff.append(", ");
            try {
                buff.append(item.getType());
                buff.append('x');
                buff.append(item.getAmount());
            } catch (Exception e) {
                buff.append("!error!");
            }
        }
        return buff.substring(2);
    }

    public static boolean isPositiveValue(String string) {
        return ofNullable(string).map(str -> positive.contains(str.toLowerCase())).orElse(false);
    }

    public static boolean isNegativeValue(String string) {
        return ofNullable(string).map(str -> negative.contains(str.toLowerCase())).orElse(false);
    }
}
