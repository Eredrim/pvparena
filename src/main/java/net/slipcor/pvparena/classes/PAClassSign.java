package net.slipcor.pvparena.classes;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>PVP Arena Class Sign class</pre>
 * <p/>
 * A sign displaying the players being part of the class
 *
 * @author slipcor
 * @version v0.9.1
 */

public class PAClassSign {
    private final PABlockLocation location;

    /**
     * create an arena class sign instance
     *
     * @param loc the location the sign resides
     */
    public PAClassSign(final Location loc) {
        this.location = new PABlockLocation(loc);
        debug("adding arena class sign: {}", this.location);
        this.clear();
    }

    /**
     * add a player name to a sign
     *
     * @param player the player name to add
     * @return true if successful, false otherwise
     */
    public boolean add(final Player player) {
        return this.setFreeLine(player.getName());
    }

    /**
     * clear the sign contents
     */
    public final void clear() {
        try {
            final Sign sign = (Sign) this.location.toLocation().getBlock().getState();
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
            this.clearNext();
        } catch (final Exception e) {
        }
    }

    /**
     * clear the next sign
     */
    private void clearNext() {
        try {
            final Sign sign = (Sign) this.location.toLocation().getBlock().getRelative(BlockFace.DOWN)
                    .getState();
            sign.setLine(0, "");
            sign.setLine(1, "");
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
        } catch (final Exception e) {
        }
    }

    /**
     * remove a player from all signs he may be on
     *
     * @param signs  the signs to check
     * @param player the player to remove
     */
    public static void remove(final Set<PAClassSign> signs, final Player player) {
        for (PAClassSign s : signs) {
            s.remove(player.getName());
        }
    }

    public PABlockLocation getLocation() {
        return this.location;
    }

    /**
     * remove a player name from a string
     *
     * @param name the name to remove
     */
    private void remove(final String name) {

        final String playerName = name.length() > 15 ? name.substring(0, 15) : name;
        try {
            Sign sign = (Sign) this.location.toLocation().getBlock().getState();
            for (int i = 2; i < 4; i++) {
                if (sign.getLine(i) != null && sign.getLine(i).equals(playerName)) {
                    sign.setLine(i, "");
                }
                if (sign.getLine(i) != null && sign.getLine(i).equals(name)) {
                    sign.setLine(i, "");
                }
            }
            sign.update();
            if (this.location.toLocation().getBlock().getRelative(BlockFace.DOWN)
                    .getState() instanceof Sign) {
                sign = (Sign) this.location.toLocation().getBlock().getRelative(BlockFace.DOWN)
                        .getState();
                for (int i = 0; i < 4; i++) {
                    if (sign.getLine(i) != null && sign.getLine(i).equals(playerName)) {
                        sign.setLine(i, "");
                    }
                    if (sign.getLine(i) != null && sign.getLine(i).equals(name)) {
                        sign.setLine(i, "");
                    }
                }
                sign.update();
            }
        } catch (final ClassCastException e) {
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * add a player name to the first free line on a sign group
     *
     * @param name the name to set
     * @return true if successful, false otherwise
     */
    private boolean setFreeLine(final String name) {
        try {
            Sign sign = (Sign) this.location.toLocation().getBlock().getState();
            for (int i = 2; i < 4; i++) {
                if (sign.getLine(i) == null || sign.getLine(i) != null && sign.getLine(i).isEmpty()) {
                    sign.setLine(i, name);
                    sign.update();
                    return true;
                }
            }
            sign = (Sign) this.location.toLocation().getBlock().getRelative(BlockFace.DOWN)
                    .getState();
            for (int i = 0; i < 4; i++) {
                if (sign.getLine(i) == null || sign.getLine(i) != null && sign.getLine(i).isEmpty()) {
                    sign.setLine(i, name);
                    sign.update();
                    return true;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return false;
    }

    /**
     * check if a location already is reserved by a class sign
     *
     * @param loc   the location to check
     * @param signs the set of signs to check against
     * @return the sign instance if reserved, null otherwise
     */
    public static PAClassSign used(final Location loc,
                                   final Set<PAClassSign> signs) {
        for (PAClassSign sign : signs) {
            if (sign.location.getDistanceSquared(new PABlockLocation(loc)) < 1) {
                return sign;
            }
        }
        return null;
    }
}
