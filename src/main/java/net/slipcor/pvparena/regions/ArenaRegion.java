package net.slipcor.pvparena.regions;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.stream.Stream;

import static net.slipcor.pvparena.config.Debugger.debug;

public class ArenaRegion {

    private final String world;
    private Arena arena;
    private String name;
    private RegionType type;
    private final Set<RegionFlag> flags = new HashSet<>();
    private final Set<RegionProtection> protections = new HashSet<>();
    private final Map<String, Location> playerLocations = new HashMap<>();

    private static final Set<Material> NOWOOLS = new HashSet<>();

    public final PABlockLocation[] locs;

    private final ArenaRegionShape shape;

    static {
        NOWOOLS.add(Material.CHEST);
    }

    /**
     * check if an arena has overlapping battlefield region with another arena
     *
     * @param region1 the arena to check
     * @param region2 the arena to check
     * @return true if it does not overlap, false otherwise
     */
    public static boolean checkRegion(final Arena region1, final Arena region2) {

        final Set<ArenaRegion> ars1 = region1.getRegionsByType(RegionType.BATTLE);
        final Set<ArenaRegion> ars2 = region2.getRegionsByType(RegionType.BATTLE);

        if (ars1.size() < 0 || ars2.size() < 1) {
            return true;
        }

        for (final ArenaRegion ar1 : ars1) {
            for (final ArenaRegion ar2 : ars2) {
                if (ar1.shape.overlapsWith(ar2)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * check if other running arenas are interfering with this arena
     *
     * @return true if no running arena is interfering with this arena, false
     * otherwise
     */
    public static boolean checkRegions(final Arena arena) {
        if (!arena.getConfig().getBoolean(CFG.USES_OVERLAPCHECK)) {
            return true;
        }
        debug(arena, "checking regions");

        return ArenaManager.checkRegions(arena);
    }

    /**
     * check if an admin tries to set an arena position
     *
     * @param event  the interact event to hand over
     * @param player the player interacting
     * @return true if the position is being saved, false otherwise
     */
    public static boolean checkRegionSetPosition(final PlayerInteractEvent event,
                                                 final Player player) {
        if (!PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }
        final Arena arena = PAA_Region.activeSelections.get(player.getName());
        if (arena != null
                && (PVPArena.hasAdminPerms(player) || PVPArena.hasCreatePerms(
                player, arena))
                && player.getEquipment() != null
                && player.getEquipment().getItemInMainHand().getType().equals(PVPArena.getInstance().getWandItem())) {
            // - modify mode is active
            // - player has admin perms
            // - player has wand in hand
            debug(arena, player, "modify&adminperms&wand");
            final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                aPlayer.setSelection(event.getClickedBlock().getLocation(), false);
                arena.msg(player, MSG.REGION_POS1);
                event.setCancelled(true); // no destruction in creative mode :)
                return true; // left click => pos1
            }

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                aPlayer.setSelection(event.getClickedBlock().getLocation(), true);
                arena.msg(player, MSG.REGION_POS2);
                return true; // right click => pos2
            }
        }
        return false;
    }

    public boolean containsRegion(ArenaRegion other) {
        List<PABlockLocation> checkList = other.getShape().getContainBlockCheckList();
        for (PABlockLocation block : checkList) {
            if (!this.getShape().contains(block)) {
                return false;
            }
        }

        // All points are inside
        // This will include all edge cases to account for:

        // CUBE - absolute maximum due to maximum X&Y&Z and minimum X&Y&Z
        // CYLINDER - absolute maximum due to maximum X&Y,Y&Z and minimum X&Y,Y&Z
        // SPHERE - current minimum with only minimum X, Y, Z and minimum X, Y, Z

        return true;
    }

    /**
     * Creates a new Arena Region
     *
     * @param arena the arena to bind to
     * @param name  the region name
     * @param shape the shape (to be cloned)
     * @param locs  the defining locations
     *              <p/>
     *              Does NOT save the region! use region.saveToConfig();
     */
    public ArenaRegion(final Arena arena, final String name,
                       final ArenaRegionShape shape, final PABlockLocation[] locs) {

        this.arena = arena;
        this.name = name;
        this.locs = locs;
        this.shape = shape;
        this.type = RegionType.CUSTOM;
        this.world = locs[0].getWorldName();
        arena.addRegion(this);
        this.shape.initialize(this);
    }

    /**
     * is a player to far away to join?
     *
     * @param player the player to check
     * @return true if the player is too far away, false otherwise
     */
    public static boolean tooFarAway(final Arena arena, final Player player) {
        final int joinRange = arena.getConfig().getInt(CFG.JOIN_RANGE);
        if (joinRange < 1) {
            return false;
        }
        final Set<ArenaRegion> ars = arena
                .getRegionsByType(RegionType.BATTLE);

        if (ars.size() < 1) {
            final PABlockLocation bLoc = SpawnManager.getRegionCenter(arena);
            if (!bLoc.getWorldName().equals(player.getWorld().getName())) {
                return true;
            }
            return bLoc.getDistanceSquared(
                    new PABlockLocation(player.getLocation())) > joinRange * joinRange;
        }

        for (final ArenaRegion ar : ars) {
            if (!ar.world.equals(player.getWorld().getName())) {
                return true;
            }
            if (!ar.shape.tooFarAway(joinRange, player.getLocation())) {
                return false;
            }
        }

        return true;
    }

    public void applyFlags(final int flags) {
        for (final RegionFlag rf : RegionFlag.values()) {
            if ((flags & (int) Math.pow(2, rf.ordinal())) != 0) {
                this.flags.add(rf);
            }
        }
    }

    public void applyProtections(final int protections) {
        for (final RegionProtection rp : RegionProtection.values()) {
            if ((protections & (int) Math.pow(2, rp.ordinal())) == 0) {
                this.protections.remove(rp);
            } else {
                this.protections.add(rp);
            }
        }
    }

    public void flagAdd(final RegionFlag regionFlag) {
        this.flags.add(regionFlag);
    }

    public boolean flagToggle(final RegionFlag regionFlag) {
        if (this.flags.contains(regionFlag)) {
            this.flags.remove(regionFlag);
        } else {
            this.flags.add(regionFlag);
        }
        return this.flags.contains(regionFlag);
    }

    public void flagRemove(final RegionFlag regionFlag) {
        this.flags.remove(regionFlag);
    }

    public Arena getArena() {
        return this.arena;
    }

    public Set<RegionFlag> getFlags() {
        return this.flags;
    }

    public Set<RegionProtection> getProtections() {
        return this.protections;
    }

    public String getRegionName() {
        return this.name;
    }

    public ArenaRegionShape getShape() {
        return this.shape;
    }

    public RegionType getType() {
        return this.type;
    }

    public World getWorld() {
        return Bukkit.getWorld(this.world);
    }

    public String getWorldName() {
        return this.world;
    }

    public boolean isInNoWoolSet(final Block block) {
        return NOWOOLS.contains(block.getType());
    }

    public boolean isInRange(final int offset, final PABlockLocation loc) {
        if (!this.world.equals(loc.getWorldName())) {
            return false;
        }

        return offset * offset < this.shape.getCenter().getDistanceSquared(loc);
    }

    public void protectionAdd(final RegionProtection regionProtection) {
        if (regionProtection == null) {
            this.protectionSetAll(true);
            return;
        }
        this.protections.add(regionProtection);
    }

    public boolean protectionSetAll(final Boolean value) {
        for (final RegionProtection rp : RegionProtection.values()) {
            if (rp == null) {
                this.arena.msg(Bukkit.getConsoleSender(),
                        "&cWarning! RegionProtection is null!");
                return false;
            }
            if (value == null) {
                this.protectionToggle(rp);
            } else if (value) {
                this.protectionAdd(rp);
            } else {
                this.protectionRemove(rp);
            }
        }

        return true;
    }

    public boolean protectionToggle(final RegionProtection regionProtection) {
        if (regionProtection == null) {
            return this.protectionSetAll(null);
        }
        if (this.protections.contains(regionProtection)) {
            this.protections.remove(regionProtection);
        } else {
            this.protections.add(regionProtection);
        }
        return this.protections.contains(regionProtection);
    }

    public void protectionRemove(final RegionProtection regionProtection) {
        if (regionProtection == null) {
            this.protectionSetAll(false);
            return;
        }
        this.protections.remove(regionProtection);
    }

    public void reset() {
        this.removeEntities();
    }

    public void removeEntities() {
        if (this.getWorld() == null || this.getWorld().getEntities().isEmpty()) {
            return;
        }

        for (final Entity entity : this.getWorld().getEntities()) {
            if (entity instanceof Player || !this.shape.contains(new PABlockLocation(entity.getLocation()))) {
                continue;
            }

            if (entity instanceof Hanging) {
                continue;
            }

            if (entity.hasMetadata("NPC")) {
                continue;
            }

            if (this.arena.getConfig().getStringList(CFG.GENERAL_REGIONCLEAREXCEPTIONS.getNode(), new ArrayList<String>()).contains(entity.getType().name())) {
                continue;
            }

            entity.remove();
        }
    }

    public void saveToConfig() {
        this.arena.getConfig().setManually("arenaregion." + this.name,
                Config.parseToString(this, this.flags, this.protections));
        this.arena.getConfig().save();
    }

    public final void setArena(final Arena arena) {
        this.arena = arena;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    public void setType(final RegionType type) {
        this.type = type;
    }

    public void handleRegionFlags(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        if (this.flags.contains(RegionFlag.NOCAMP)) {
            this.handleNoCampRegionFlag(arenaPlayer, pLoc);

        } else if(Stream.of(RegionFlag.DEATH, RegionFlag.WIN, RegionFlag.LOSE).anyMatch(this.flags::contains) && this.shape.contains(pLoc)) {

            if (this.flags.contains(RegionFlag.DEATH)) {
                debug(arenaPlayer, "entering DEATH region");
                this.handleDeathRegionFlag(arenaPlayer);

            } else if (this.flags.contains(RegionFlag.WIN)) {
                debug(arenaPlayer, "entering WIN region");
                this.handleWinRegionFlag(arenaPlayer);

            } else if (this.flags.contains(RegionFlag.LOSE)) {
                debug(arenaPlayer, "entering LOSE region");
                this.handleLoseRegionFlag(arenaPlayer);
            }
        }
    }

    private void handleNoCampRegionFlag(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        if (this.shape.contains(pLoc)) {
            final Location loc = this.playerLocations.get(arenaPlayer.getName());
            Player player = arenaPlayer.getPlayer();

            if (loc == null) {
                Arena.pmsg(player, MSG.NOTICE_YOU_NOCAMP);
            } else {
                if (loc.distance(player.getLocation()) < 3) {
                    debug(player, "damaged in NOCAMP region");
                    int campDamage = this.arena.getConfig().getInt(CFG.DAMAGE_SPAWNCAMP);
                    player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.CUSTOM, campDamage));
                    player.damage(campDamage);
                }
            }
            this.playerLocations.put(arenaPlayer.getName(), player.getLocation().getBlock().getLocation());
        } else {
            this.playerLocations.remove(arenaPlayer.getName());
        }
    }

    private void handleLoseRegionFlag(ArenaPlayer arenaPlayer) {
        if (this.arena.isFreeForAll()) {
            this.killPlayerIfFighting(arenaPlayer);
        } else {
            for (final ArenaTeam team : this.arena.getTeams()) {
                if (!team.getTeamMembers().contains(arenaPlayer)) {
                    // skip winner
                    continue;
                }
                for (final ArenaPlayer ap2 : team.getTeamMembers()) {
                    this.killPlayerIfFighting(ap2);
                }
                return;
            }
        }
    }

    private void handleWinRegionFlag(ArenaPlayer arenaPlayer) {
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (!this.arena.isFreeForAll() && team.getTeamMembers().contains(arenaPlayer)) {
                // skip winning team
                continue;
            }
            for (final ArenaPlayer ap2 : team.getTeamMembers()) {
                if (this.arena.isFreeForAll() && ap2.getName().equals(arenaPlayer.getName())) {
                    continue;
                }
                this.killPlayerIfFighting(ap2);
            }
            return;
        }
    }

    private void handleDeathRegionFlag(ArenaPlayer arenaPlayer) {
        Player player = arenaPlayer.getPlayer();
        Arena.pmsg(player, MSG.NOTICE_YOU_DEATH);
        ArenaGoal goal = this.arena.getGoal();
        if (goal.getName().endsWith("DeathMatch")) {
            if (goal.getPlayerLifeMap().containsKey(arenaPlayer.getPlayer())) {
                final int lives = goal.getPlayerLifeMap().get(arenaPlayer.getPlayer()) + 1;
                goal.getPlayerLifeMap().put(arenaPlayer.getPlayer(), lives);
            } else if (goal.getTeamLifeMap().containsKey(arenaPlayer.getArenaTeam())) {
                final int lives = goal.getTeamLifeMap().get(arenaPlayer.getArenaTeam()) + 1;
                goal.getTeamLifeMap().put(arenaPlayer.getArenaTeam(), lives);
            }
        }

        player.setLastDamageCause(new EntityDamageEvent(player, DamageCause.CUSTOM, 1003.0));
        player.damage(1000);
    }

    private void killPlayerIfFighting(ArenaPlayer aPlayer) {
        if (aPlayer.getStatus() == PlayerStatus.FIGHT) {
            Bukkit.getWorld(this.world).strikeLightningEffect(aPlayer.getPlayer().getLocation());
            PADeathInfo pluginDeathCause = new PADeathInfo(EntityDamageEvent.DamageCause.LIGHTNING);
            aPlayer.handleDeathAndLose(pluginDeathCause);
        }
    }

    public String update(final String key, final String value) {
        // usage: /pa {arenaname} region [regionname] radius [number]
        // usage: /pa {arenaname} region [regionname] height [number]
        // usage: /pa {arenaname} region [regionname] position [position]
        // usage: /pa {arenaname} region [regionname] flag [flag]
        // usage: /pa {arenaname} region [regionname] type [regiontype]

        if ("height".equalsIgnoreCase(key)) {
            final int height;
            try {
                height = Integer.parseInt(value);
            } catch (final Exception e) {
                return Language.parse(MSG.ERROR_NOT_NUMERIC, value);
            }

            this.locs[0].setY(this.shape.getCenter().getY() - (height >> 1));
            this.locs[1].setY(this.locs[0].getY() + height);

            return Language.parse(MSG.REGION_HEIGHT, value);
        }
        if (key.equalsIgnoreCase("radius")) {
            int radius;
            try {
                radius = Integer.parseInt(value);
            } catch (Exception e) {
                return Language.parse(MSG.ERROR_NOT_NUMERIC, value);
            }

            final PABlockLocation loc = this.shape.getCenter();

            this.locs[0].setX(loc.getX() - radius);
            this.locs[0].setY(loc.getY() - radius);
            this.locs[0].setZ(loc.getZ() - radius);

            this.locs[1].setX(loc.getX() + radius);
            this.locs[1].setY(loc.getY() + radius);
            this.locs[1].setZ(loc.getZ() + radius);

            return Language.parse(MSG.REGION_RADIUS, value);
        }
        if (key.equalsIgnoreCase("position")) {
            return null; // TODO insert function to align the arena based on a
            // position setting.
            // TODO see SETUP.creole
        }

        return Language.parse(MSG.ERROR_ARGUMENT, key,
                "height | radius | position");
    }
}
