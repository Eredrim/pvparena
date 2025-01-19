# Domination

> ℹ This goal is designed to be played in teams

## Description

The game is simple :

There are one or several flags that can be claimed by players.
When players are in the range of a flag, a load bar appears, and they will claim the flag after few seconds.
Obviously, if a player on another team come also within the flag range, loading stops.

When a flag is claimed, it takes team color. Player of other team can get it back by the same process.
In this case, flag will be released in a first time (it takes white color) and only then it will take color of
second team.

Each claimed flag gives points every few seconds (tickinterval) that add up to a score, the first team to have enough 
points wins.

## Setup

Flags have to be added. In order to do that, use `/pa <arenaname> flag add`. This toggles edit mode. 
In toggle mode you can register as many flags as you want by clicking on them.  
Don't forget to type command again in order to exit edit mode after setting the flags.

Given that flag must be able to change color, you can use the following blocks as flag blocks:                                                                                  
* BANNERS
* CARPETS
* CONCRETE
* CONCRETE_POWDER 
* GLAZED_TERRACOTTA 	
* SHULKER_BOX
* STAINED_GLASS 
* STAINED_GLASS_PANES
* TERRACOTTA
* WALL_BANNERS

I suggest you to try glass block with a beacon bottom the flag. When flag will be claimed, glass blocks will change its
color, altering beacon light ray in the same time :wink:

## Config settings  

- spamoffset => after how many updates should the arena announce? (default: 3)
- claimrange => how near need players to be? (default: 3)
- dlives => domination lives (max points). (default: 10)
- onlywhenmore => only score when more than half of the points are claimed. (default: false)
- particlecircle => creates a circle of particles around each flag to mark capture radius. (default: true)
- tickinterval => the amount of ticks to wait before doing an update. (default: 60 = 3 seconds)
- tickreward => the amount of points to give for each score. (default: 1)

<br>

> ⚙ **Technical precision:**  
> This goal has to check for player's position. Based on the player and checkpoint count this can lag your server. 
> Unfortunately, there is no other way to determine a claimed checkpoint.

