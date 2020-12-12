# Regions

Regions enhance your game play by adding protections, triggers or special configurations.

You can list existing arena regions with [`/pa [arena] regions`](commands/regions.md)

## Region Creation

*All following commands assume you either have edit mode enabled or just one arena in place.*

Start creating an arena region with [`/pa region`](commands/region.md).

Now select a region by holding your arena wand (a STICK by default) and left click for position 1 and right click for position 2. Detailed information about the special region shapes, i.e. which points to select, check out the tutorial.

After selecting those points, use [`/pa !r [regionname] [regionshape]`](commands/region.md) to save the region.

The following region shapes exist:
- CUBOID (default)
- SPHERIC
- CYLINDRIC

Of course, different shapes cover different areas. Note that a cylinder means an area like a can, so a standing cylinder.

To remove a region, use [`/pa !r [regionname] remove`](commands/region.md)

## Region Types

Set a region's type with [`/pa !rt [regionname] [regiontype]`](commands/regiontype.md)

The following Region Types exist:

- CUSTOM (default) => does nothing
- WATCH => the place where spectators should be, BattleFieldGuard will kick spectators not being part inside of one WATCH region
- LOUNGE => the place where fighters select their class. Required to allow players interactions in the lounge.
- BATTLE => the most important type. It adds battlefield reservation (for overlapping arenas, only caring about that actually battle region), region protection, restoring and check if fighters are in the right place.
- EXIT => the region where players should be after exiting the arena, no functionality atm
- JOIN => the region where players should be when joining, see Configuration page, enforcement is disabled by default
- SPAWN => a spawn region where players are randomly placed in when spawning or respawning 
- BL_INV => block chest access to any team which name is included in the region name, same for classes
- WL_INV => restrict chest access to each team which name is included in the region name, same for classes

#### Usage of BL_INV and WL_INV

For these regions types, the name of the region is important. It is read by the plugin to understand access restrictions.

Example:
* Region name is `RedBlueRanger` and region type is `WL_INV` => allows Red or Blue teams or Ranger class to access chests.
* Region name is `xxblueyy` and region type is `BL_INV`=> disallow chest access to the blue team

## Region Flags

You can enable special player interactions with players by using regions flags. 
Just use the command [`/pa !rf [regionname] [regionflag]`](commands/regionflags.md).

Those are valid Region Flags:

- NOCAMP - players that don't move inside this region will be hurt
- DEATH - players entering this region will die
- WIN - players entering this region will WIN the game
- LOSE - players entering this region will LOSE the game
- NODAMAGE - players being in this region will receive no damage 

## Region Protection

The BATTLE Region parses region protection flags (other regions can assigned protections, too, but atm they don't 
really use them). Set them via [`/pa !p [regionname] [protection]`](commands/protection.md) - you can add on, off, yes, 
no, true, false to specify a setting, or just as just told to toggle the state.

Valid protections are:

- BREAK - prevent player block breaking
- FIRE - prevent fire spreading/burning
- MOBS - prevent mob spawning
- NATURE - prevent water flow/growth
- PAINTING - prevent painting/itemframe breakage
- PISTON - prevent piston usage
- PLACE - prevent player block placing
- TNT - prevent tnt interaction
- TNTBREAK - prevent tnt block damage (explosion still hurts)
- DROP - prevent player item dropping
- INVENTORY - prevent inventory interaction
- PICKUP - prevent player item pickup
- TELEPORT - prevent player teleportation

Example: `/pa !p main break on` - disallow players to break blocks in "main" region of your arena

> 🚩 **Tip:**  
> There is an "ALL" protection argument that toggle all protections

## Remove Region

First, you need to known the region's name. List regions with`/pa <arenaname> regions`.  
Then, use `/pa <arenaName> region <regionName> remove` to remove the region.
