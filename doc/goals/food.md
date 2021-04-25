# Food

> ℹ This goal is designed for **team** gamemode

## Description

Players are hungry! The first team gathering enough **cooked** food items (of their type) wins the game.

The following food types exist:
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

You have to prepare chests. To define those, use `/pa [arenaname] [teamname]_foodchest` \- this enables setting. 

Finish the setting by clicking the chest that should be the team's chest. 

This chest will be checked for incoming and outgoing food items (of the team type). You can optionally prepare furnaces so that a team can ONLY use this furnace. 

Set this by `/pa [arenaname] [teamname]_foodfurnace` and hit the furnace. 

> 🚩 **Notes:**  
> - Teams not having a corresponding furnace will be able to access all of them.
> - One furnace can be set multiple times! Just set the same spot for multiple teams. For example,
> red and blue teams can share the same furnace.

## Config settings

- `fmaxitems` \- the item count that triggers win (default: 50)
- `fplayeritems` \- the item count players receive on start and respawn \- (default: 50)
- `fteamitems` \- the item count the team receives on start, divided by team members \- (default: 100) 
