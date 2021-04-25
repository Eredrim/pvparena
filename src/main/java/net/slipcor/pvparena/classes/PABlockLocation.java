package net.slipcor.pvparena.classes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

/**
 * <pre>
 * PVP Arena Location class
 * </pre>
 * <p/>
 * A simple wrapper of the Bukkit Location, only calculating blocks
 *
 * @author slipcor
 * @version v0.9.1
 */

public class PABlockLocation {
    private final String world;
    private int x;
    private int y;
    private int z;

    // store orientations
    private BlockData blockData;

    public PABlockLocation(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PABlockLocation(String world, int x, int y, int z, BlockData blockData) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockData = blockData;
    }

    public PABlockLocation(final String value) {
        String[] split = value.split(":");
        this.world = split[0];

        String[] ints = split[1].split(",");

        this.x = Integer.parseInt(ints[0]);
        this.y = Integer.parseInt(ints[1]);
        this.z = Integer.parseInt(ints[2]);
    }

    public PABlockLocation(final String value, final String data) {
        String[] split = value.split(":");
        this.world = split[0];

        String[] ints = split[1].split(",");

        this.x = Integer.parseInt(ints[0]);
        this.y = Integer.parseInt(ints[1]);
        this.z = Integer.parseInt(ints[2]);

        this.blockData = Bukkit.createBlockData(data);
    }

    public PABlockLocation(final Location bukkitLocation) {
        this.world = bukkitLocation.getWorld().getName();
        this.x = bukkitLocation.getBlockX();
        this.y = bukkitLocation.getBlockY();
        this.z = bukkitLocation.getBlockZ();

        this.blockData = bukkitLocation.getBlock().getBlockData();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.world == null ? 0 : this.world.hashCode());
        result = prime * result + this.x;
        result = prime * result + this.y;
        result = prime * result + this.z;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final PABlockLocation other = (PABlockLocation) obj;
        if (this.world == null) {
            if (other.world != null) {
                return false;
            }
        } else if (!this.world.equals(other.world)) {
            return false;
        }
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        return this.z == other.z;
    }

    public double getDistance(final PABlockLocation otherLocation) {
        if (otherLocation == null) {
            throw new IllegalArgumentException(
                    "Cannot measure distance to a null location");
        }
        if (!otherLocation.world.equals(this.world)) {
            throw new IllegalArgumentException(
                    "Cannot measure distance between " + this.world + " and "
                            + otherLocation.world);
        }

        return Math.sqrt(Math.pow(this.x - otherLocation.x, 2.0D)
                + Math.pow(this.y - otherLocation.y, 2.0D) + Math.pow(this.z - otherLocation.z, 2.0D));
    }

    public double getDistanceSquared(final PABlockLocation otherLocation) {
        if (otherLocation == null) {
            throw new IllegalArgumentException(
                    "Cannot measure distance to a null location");
        }
        if (!otherLocation.world.equals(this.world)) {
            throw new IllegalArgumentException(
                    "Cannot measure distance between " + this.world + " and "
                            + otherLocation.world);
        }

        return Math.pow(this.x - otherLocation.x, 2.0D)
                + Math.pow(this.y - otherLocation.y, 2.0D) + Math.pow(this.z - otherLocation.z, 2.0D);
    }

    public PABlockLocation getMidpoint(final PABlockLocation location) {
        return new PABlockLocation(this.world, (this.x + location.x) / 2, (this.y + location.y) / 2,
                (this.z + location.z) / 2);
    }

    public String getWorldName() {
        return this.world;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public boolean isInAABB(final PABlockLocation min, final PABlockLocation max) {
        if (this.x < min.x || this.x > max.x) {
            return false;
        }
        if (this.y < min.y || this.y > max.y) {
            return false;
        }
        return !(this.z < min.z || this.z > max.z);
    }

    public PABlockLocation pointTo(final PABlockLocation dest, final Double length) {
        final Vector source = new Vector(this.x, this.y, this.z);
        final Vector destination = new Vector(dest.x, dest.y, dest.z);

        Vector goal = source.subtract(destination);

        goal = goal.normalize().multiply(length);

        return new PABlockLocation(this.world, this.x + this.x + goal.getBlockX(), this.y
                + goal.getBlockY(), this.z + goal.getBlockZ());
    }

    public void setX(final int value) {
        this.x = value;
    }

    public void setY(final int value) {
        this.y = value;
    }

    public void setZ(final int value) {
        this.z = value;
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(this.world), this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return this.world + ':' + this.x + ',' + this.y + ',' + this.z;
    }

    public BlockData getBlockData() {
        return this.blockData;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
    }
}
