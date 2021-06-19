# PhysicalFlags

> ℹ This goal is designed for **team** gamemode

## Description

There is a flag per team. Team A have to **break** team B flag and brings it at home.
Point is scored when team A place team B flag on its own flag.

## Setup

Firstly, check if flag type is that you want (wool by default) or change it by editing
your arena configuration ou using `/pa set` command.
You can use any solid block as flag. Flags automatically take team color if flag type is one
of the following material (color prefix doesn't matter):

* WHITE_BANNER
* WHITE_CARPET
* WHITE_CONCRETE
* WHITE_CONCRETE_POWDER 
* WHITE_GLAZED_TERRACOTTA 	
* WHITE_SHULKER_BOX
* WHITE_STAINED_GLASS 
* WHITE_STAINED_GLASS_PANE
* WHITE_TERRACOTTA
* WHITE_WALL_BANNER

<br>

Flags have to be added afterwards. In order to do that, use `/pa [arenaname] flag [teamname]` \- this enables setting.
Just left click on your flag block. Clicked block must have same type as defined in your config. However nothing will 
happen.

> 🚩 **One more thing:**  
You can activate a special "touchdown" way of playing. Set a flag called "touchdown", it will be BLACK ingame. 
Players claim this flag and bring it home. Only one team can bring this flag home, obviously :)

<br>

## Config settings

- `flives` \- the count of flags being brought home that lead to winning
- `flagType` \- the material checked for flags (default: WHITE_WOOL). Plugin handle automatically flag colors if flagType 
is a colorable item.
- `mustBeSafe` \- do claimed flags prevent bringing home other flags? \- (default: true)
- `woolFlagHead` \- should PVP Arena enforce putting a wool head on flag carriers? - (default: true)
- `effect` \- the potion effect a player should get when carrying the flag (default: none; possible value: SLOWx2 - 
slowness, level 2) ; [see bukkit docs](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html) 

