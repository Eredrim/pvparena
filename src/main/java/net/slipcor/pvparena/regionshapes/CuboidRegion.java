package net.slipcor.pvparena.regionshapes;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * Arena Region Shape class "cuboid"
 * </pre>
 * <p/>
 * Defines a cuboid region, including overlap checks and contain checks
 *
 * @author slipcor
 */

public class CuboidRegion extends ArenaRegionShape {

    private final Set<Block> border = new HashSet<>();
    private ArenaRegion region;

    public CuboidRegion() {
        super("cuboid");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    /**
     * sanitize a pair of locations
     *
     * @param lMin the minimum point
     * @param lMax the maximum point
     * @return a recalculated pair of locations
     */
    private PABlockLocation[] sanityCheck(final PABlockLocation lMin,
                                          final PABlockLocation lMax) {
        final boolean x = lMin.getX() > lMax.getX();
        final boolean y = lMin.getY() > lMax.getY();
        final boolean z = lMin.getZ() > lMax.getZ();

        if (!(x | y | z)) {
            return new PABlockLocation[]{lMin, lMax};
        }
        final PABlockLocation l1 = new PABlockLocation(lMin.getWorldName(), x ? lMax.getX()
                : lMin.getX(), y ? lMax.getY() : lMin.getY(), z ? lMax.getZ()
                : lMin.getZ());
        final PABlockLocation l2 = new PABlockLocation(lMin.getWorldName(), x ? lMin.getX()
                : lMax.getX(), y ? lMin.getY() : lMax.getY(), z ? lMin.getZ()
                : lMax.getZ());

        if (l1.getX() == l2.getX()) {
            l2.setX(l2.getX()+1);
        }

        if (l1.getY() == l2.getY()) {
            l2.setY(l2.getY()+1);
        }

        if (l1.getZ() == l2.getZ()) {
            l2.setZ(l2.getZ()+1);
        }

        return new PABlockLocation[]{l1, l2};
    }

    @Override
    public boolean hasVolume() {
        return this.region != null &&
                this.region.locs[0] != null && this.region.locs[1] != null &&
                this.region.locs[0].getX() != this.region.locs[1].getX() &&
                this.region.locs[0].getY() != this.region.locs[1].getY() &&
                this.region.locs[0].getZ() != this.region.locs[1].getZ();
    }

    @Override
    public final void initialize(final ArenaRegion region) {
        this.region = region;
        final PABlockLocation[] sane = this.sanityCheck(region.locs[0], region.locs[1]);
        region.locs[0] = sane[0];
        region.locs[1] = sane[1];
    }

    @Override
    public boolean overlapsWith(final ArenaRegion paRegion) {
        if (!this.getMinimumLocation().getWorldName().equals(
                paRegion.getShape().getMinimumLocation().getWorldName())) {
            return false;
        }
        if (paRegion.getShape() instanceof CuboidRegion) {
            // compare 2 cuboids
            if (this.getMinimumLocation().getX() > paRegion.locs[1].getX()
                    || this.getMinimumLocation().getY() > paRegion.locs[1].getY()
                    || this.getMinimumLocation().getZ() > paRegion.locs[1].getZ()) {
                return false;
            }
            return !(paRegion.locs[0].getX() > this.getMaximumLocation().getX()
                    || paRegion.locs[0].getY() > this.getMaximumLocation().getY()
                    || paRegion.locs[0].getZ() > this.getMaximumLocation().getZ());
        }
        if (paRegion.getShape() instanceof SphericRegion) {
            // we are cube and search for intersecting sphere

            final PABlockLocation thisCenter = this.getMaximumLocation().getMidpoint(this.getMinimumLocation());
            final PABlockLocation thatCenter = paRegion.locs[1]
                    .getMidpoint(paRegion.locs[0]);

            final Double thatRadius = paRegion.locs[0].getDistance(paRegion
                    .locs[1]) / 2;

            if (this.contains(thatCenter)) {
                return true; // the sphere is inside!
            }

            final PABlockLocation offset = thatCenter.pointTo(thisCenter, thatRadius);
            // offset is pointing from that to this

            return this.contains(offset);
        }
        if (paRegion.getShape() instanceof CylindricRegion) {
            // we are cube and search for intersecting cylinder

            final PABlockLocation thisCenter = this.getMaximumLocation().getMidpoint(
                    this.getMinimumLocation());
            final PABlockLocation thatCenter = paRegion.locs[1]
                    .getMidpoint(paRegion.locs[0]);

            if (this.getMaximumLocation().getY() < paRegion.locs[0].getY()) {
                return false;
            }
            if (this.getMinimumLocation().getY() > paRegion.locs[1].getY()) {
                return false;
            }

            thisCenter.setY(thatCenter.getY());

            if (this.contains(thatCenter)) {
                return true; // the sphere is inside!
            }

            final Double thatRadius = paRegion.locs[0].getDistance(paRegion
                    .locs[1]) / 2;

            final PABlockLocation offset = thatCenter.pointTo(thisCenter, thatRadius);
            // offset is pointing from that to this

            return this.contains(offset);
        }
        PVPArena.getInstance().getLogger()
                .warning(
                        "Region Shape not supported: "
                                + paRegion.getShape().getName());
        return false;
    }

    @Override
    public void showBorder(final Player player) {

        final Location min = this.getMinimumLocation().toLocation();
        final Location max = this.getMaximumLocation().toLocation();
        final World w = Bukkit.getWorld(this.region.getWorldName());

        this.border.clear();

        // move along exclusive x, create miny+maxy+minz+maxz
        for (int x = min.getBlockX() + 1; x < max.getBlockX(); x++) {
            this.border.add(new Location(w, x, min.getBlockY(), min.getBlockZ())
                    .getBlock());
            this.border.add(new Location(w, x, min.getBlockY(), max.getBlockZ())
                    .getBlock());
            this.border.add(new Location(w, x, max.getBlockY(), min.getBlockZ())
                    .getBlock());
            this.border.add(new Location(w, x, max.getBlockY(), max.getBlockZ())
                    .getBlock());
        }
        // move along exclusive y, create minx+maxx+minz+maxz
        for (int y = min.getBlockY() + 1; y < max.getBlockY(); y++) {
            this.border.add(new Location(w, min.getBlockX(), y, min.getBlockZ())
                    .getBlock());
            this.border.add(new Location(w, min.getBlockX(), y, max.getBlockZ())
                    .getBlock());
            this.border.add(new Location(w, max.getBlockX(), y, min.getBlockZ())
                    .getBlock());
            this.border.add(new Location(w, max.getBlockX(), y, max.getBlockZ())
                    .getBlock());
        }
        // move along inclusive z, create minx+maxx+miny+maxy
        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
            this.border.add(new Location(w, min.getBlockX(), min.getBlockY(), z)
                    .getBlock());
            this.border.add(new Location(w, min.getBlockX(), max.getBlockY(), z)
                    .getBlock());
            this.border.add(new Location(w, max.getBlockX(), min.getBlockY(), z)
                    .getBlock());
            this.border.add(new Location(w, max.getBlockX(), max.getBlockY(), z)
                    .getBlock());
        }

        for (Block b : this.border) {
            if (!this.region.isInNoWoolSet(b)) {
                player.sendBlockChange(b.getLocation(), Material.WHITE_WOOL.createBlockData());
            }
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(),
                new Runnable() {

                    @Override
                    public void run() {
                        for (Block b : CuboidRegion.this.border) {
                            player.sendBlockChange(b.getLocation(),
                                    b.getType().createBlockData());
                        }
                        CuboidRegion.this.border.clear();
                    }

                }, 100L);
    }

    @Override
    public boolean contains(final PABlockLocation loc) {
        if (this.getMinimumLocation() == null || this.getMaximumLocation() == null
                || loc == null || !loc.getWorldName().equals(this.region.getWorldName())) {
            return false; // no arena, no container or not in the same world
        }
        return loc.isInAABB(this.getMinimumLocation(), this.getMaximumLocation());
    }

    @Override
    public PABlockLocation getCenter() {
        return this.getMinimumLocation().getMidpoint(this.getMaximumLocation());
    }

    @Override
    public List<PABlockLocation> getContainBlockCheckList() {
        final List<PABlockLocation> result = new ArrayList<>();

        result.add(this.region.locs[0]); // == 0

        result.add(new PABlockLocation(this.region.locs[0].getWorldName(),
                this.region.locs[1].getX(),
                this.region.locs[0].getY(),
                this.region.locs[0].getZ())); // == 1
        result.add(new PABlockLocation(this.region.locs[0].getWorldName(),
                this.region.locs[0].getX(),
                this.region.locs[1].getY(),
                this.region.locs[0].getZ())); // == 2
        result.add(new PABlockLocation(this.region.locs[0].getWorldName(),
                this.region.locs[1].getX(),
                this.region.locs[1].getY(),
                this.region.locs[0].getZ())); // == 3
        result.add(new PABlockLocation(this.region.locs[0].getWorldName(),
                this.region.locs[0].getX(),
                this.region.locs[1].getY(),
                this.region.locs[0].getZ())); // == 4
        result.add(new PABlockLocation(this.region.locs[0].getWorldName(),
                this.region.locs[1].getX(),
                this.region.locs[0].getY(),
                this.region.locs[1].getZ())); // == 5
        result.add(new PABlockLocation(this.region.locs[0].getWorldName(),
                this.region.locs[0].getX(),
                this.region.locs[1].getY(),
                this.region.locs[1].getZ())); // == 6

        result.add(this.region.locs[1]); // == 7
/*
        getRegion().getArena().debug("CUBOID blockCheckList");

        for (PABlockLocation block : result) {
            getRegion().getArena().debug(block.toString());
        }*/

        return result;
    }

    @Override
    public PABlockLocation getMaximumLocation() {
        return this.region.locs[1];
    }

    @Override
    public PABlockLocation getMinimumLocation() {
        return this.region.locs[0];
    }

    @Override
    public boolean tooFarAway(final int joinRange, final Location location) {
        final PABlockLocation reach = new PABlockLocation(location).pointTo(
                this.getCenter(), (double) joinRange);

        return this.contains(reach);
    }

    @Override
    public void move(final BlockFace direction, final int value) {
        final int diffX = direction.getModX();
        final int diffY = direction.getModY();
        final int diffZ = direction.getModZ();

        if (diffX == 0 && diffY == 0 && diffZ == 0) {
            return;
        }
        this.region.locs[0] = new PABlockLocation(this.region.locs[0].toLocation().add(diffX * value, diffY * value, diffZ * value));
        this.region.locs[1] = new PABlockLocation(this.region.locs[1].toLocation().add(diffX * value, diffY * value, diffZ * value));
    }

    @Override
    public void extend(final BlockFace direction, final int value) {
        final int diffX = direction.getModX();
        final int diffY = direction.getModY();
        final int diffZ = direction.getModZ();

        if (diffX == 0 && diffY == 0 && diffZ == 0) {
            return;
        }

        if (diffX > 0) {
            this.region.locs[1] = new PABlockLocation(this.region.locs[1].toLocation().add(diffX * value, 0, 0));
        } else if (diffX < 0) {
            this.region.locs[0] = new PABlockLocation(this.region.locs[0].toLocation().subtract(diffX * value, 0, 0));
        }

        if (diffY > 0) {
            this.region.locs[1] = new PABlockLocation(this.region.locs[1].toLocation().add(0, diffY * value, 0));
        } else if (diffY < 0) {
            this.region.locs[0] = new PABlockLocation(this.region.locs[0].toLocation().subtract(0, diffY * value, 0));
        }

        if (diffZ > 0) {
            this.region.locs[1] = new PABlockLocation(this.region.locs[1].toLocation().add(0, 0, diffZ * value));
        } else if (diffZ < 0) {
            this.region.locs[0] = new PABlockLocation(this.region.locs[0].toLocation().subtract(0, 0, diffZ * value));
        }

    }

    @Override
    public Set<PABlockLocation> getAllBlocks() {
        PABlockLocation min = this.getMinimumLocation();
        PABlockLocation max = this.getMaximumLocation();
        Set<PABlockLocation> result = new HashSet<>();

        for(int x = min.getX(); x <= max.getX(); x++) {
            for(int y = min.getY(); y <= max.getY(); y++) {
                for(int z = min.getZ(); z <= max.getZ(); z++) {
                    result.add(new PABlockLocation(min.getWorldName(), x, y, z));
                }
            }
        }

        return result;
    }
}
