# Flags

> ℹ This goal is designed to be played in teams

## Description

There is a flag per team. Team A captures the flag of team B and brings it home. 
To do this, simply hit/click on flags.

## Setup

You can use any solid block as flag. Flags automatically take team color if block type is one
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

To set a flag, use `/pa <arenaname> flag set <teamname>`. It enables block selection.
Just left-click on your flag block. All flag blocks must have same type.

> 🚩 **One more thing:**  
You can activate a special "touchdown" way of playing. Set a flag called "touchdown", it will be BLACK ingame. 
Players claim this flag and bring it home. Only one team can bring this flag home, obviously :)

<br>

## Config settings

- `flives` \- the count of flags being brought home that lead to winning - (default: 3)
- `autocolor` \- if true, flag blocks will take teams color on startup - (default: true)
- `mustBeSafe` \- do claimed flags prevent bringing home other flags? \- (default: true)
- `woolFlagHead` \- should PVP Arena enforce putting a wool head on flag carriers? - (default: true)
- `effect` \- the potion effect a player should get when carrying the flag (default: none; possible value: SLOWx2 - 
slowness, level 2) ; [see bukkit docs](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html)
- `alterOnCatch` \- change flag aspect when a player catch it. If flag is colorable (list below), color is passed to white
 otherwise block is replaced by bedrock. (default: true)
