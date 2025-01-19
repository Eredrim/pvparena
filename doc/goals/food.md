# Food

> ℹ This goal is designed to be played in teams

## Description

Players are hungry! The first team gathering enough **cooked** food items (of their type) wins the game.
Teams have to cook in specific furnaces and bring the cooked food in their own chest(s).

Each team will get a random food type among the following ones:
- Beef
- Chicken
- Cod
- Mutton
- Porkchop
- Potato
- Salmon

> 🚩 **Note:**  
> The module does NOT include coal or furnaces, you have to manage that on your own

## Setup

You have to prepare chests.
Those chests will be checked for incoming and outgoing food items (of the team type).

To register them, use `/pa <arenaname> foodchest add <teamname>`, this enables 
a selection mode. Click on the chests that should be the team's chests. Then, type the command again to
close selection mode.  
Repeat the operation for all teams.

 You can optionally prepare furnaces so that a team can ONLY use this furnace. 

Set this by `/pa <arenaname> foodfurnace add <teamname>` and hit the furnace. The selection mode works exactly like
the one for food chests.

> 🚩 **Notes:**  
> - Teams not having a corresponding furnace will be able to access all of them.
> - You can use several chests and furnaces for each team 
> - One furnace can be set multiple times! Just set the same spot for multiple teams. For example,
> red and blue teams can share the same furnace.

## Config settings

- `fmaxitems` \- the cooked item count that triggers win (default: 50)
- `fplayeritems` \- the item count players receive on start and respawn \- (default: 50)
- `fteamitems` \- the item count the team receives on start, divided by team members \- (default: 100) 
