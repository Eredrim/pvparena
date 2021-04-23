package net.slipcor.pvparena.classes;

import net.slipcor.pvparena.core.StringParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * <pre>
 * PVP Arena Location class
 * </pre>
 * <p/>
 * A simple wrapper of the Bukkit Location
 *
 * @author slipcor
 * @version v0.9.6
 */

public class PALocation {
    private final String world;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;

    public PALocation(final String world, final double x, final double y, final double z, final float pitch,
                      final float yaw) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public PALocation(final Location bukkitLocation) {
        this.world = bukkitLocation.getWorld().getName();
        this.x = bukkitLocation.getX();
        this.y = bukkitLocation.getY();
        this.z = bukkitLocation.getZ();
        this.pitch = bukkitLocation.getPitch();
        this.yaw = bukkitLocation.getYaw();
    }

    public PALocation add(final double x, final double y, final double z) {
        return new PALocation(this.world, x + this.x, y + this.y, z + this.z, this.pitch,
                this.yaw);
    }

    public PALocation add(Vector vector) {
        return new PALocation(this.world, vector.getX() + this.x, vector.getY() + this.y,
                vector.getZ() + this.z, this.pitch, this.yaw);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(this.pitch);
        result = prime * result + (this.world == null ? 0 : this.world.hashCode());
        long temp = Double.doubleToLongBits(this.x);
        result = prime * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.y);
        result = prime * result + (int) (temp ^ temp >>> 32);
        result = prime * result + Float.floatToIntBits(this.yaw);
        temp = Double.doubleToLongBits(this.z);
        result = prime * result + (int) (temp ^ temp >>> 32);
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
        final PALocation other = (PALocation) obj;
        if (Float.floatToIntBits(this.pitch) != Float.floatToIntBits(other.pitch)) {
            return false;
        }
        if (this.world == null) {
            if (other.world != null) {
                return false;
            }
        } else if (!this.world.equals(other.world)) {
            return false;
        }
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        if (Float.floatToIntBits(this.yaw) != Float.floatToIntBits(other.yaw)) {
            return false;
        }
        return Double.doubleToLongBits(this.z) == Double.doubleToLongBits(other.z);
    }

    public int getBlockX() {
        return (int) Math.floor(this.x);
    }

    public int getBlockY() {
        return (int) Math.floor(this.y);
    }

    public int getBlockZ() {
        return (int) Math.floor(this.z);
    }

    public double getDistance(final PALocation otherLocation) {
        double squaredDistance = this.getDistanceSquared(otherLocation);
        return Math.sqrt(squaredDistance);
    }

    public double getDistanceSquared(final PALocation otherLocation) {
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

    public double getPitch() {
        return this.pitch;
    }

    public String getWorldName() {
        return this.world;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getYaw() {
        return this.yaw;
    }

    public double getZ() {
        return this.z;
    }

    public void setPitch(final float value) {
        this.pitch = value;
    }

    public void setX(final double value) {
        this.x = value;
    }

    public void setY(final double value) {
        this.y = value;
    }

    public void setYaw(final float value) {
        this.yaw = value;
    }

    public void setZ(final double value) {
        this.z = value;
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(this.world), this.x, this.y, this.z, this.yaw, this.pitch);
    }

    @Override
    public String toString() {
        final String[] aLoc = new String[6];
        aLoc[0] = "w:" + this.world;
        aLoc[1] = "x:" + this.x;
        aLoc[2] = "y:" + this.y;
        aLoc[3] = "z:" + this.z;
        aLoc[4] = "P:" + this.getPitch();
        aLoc[5] = "Y:" + this.getYaw();
        return StringParser.joinArray(aLoc, "|");
    }
}
