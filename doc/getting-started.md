# Getting Started

<br>

> **🚩 Syntax tip:**  
> [required] indicates a required parameter  
> (optional) indicates an optional parameter
> 


## Foreword: what's an arena?

Before creating your first arena, you have to understand what's.  
Arena is immaterial, it's a game configuration you create with goals, teams, classes, etc. It references your arena
config file created at `/plugins/pvparena/yourArenaName/config.yml`. So your arena defines how your game takes place.

In this arena, only two things are bind to locations: spawn point and regions. So if you need to move your arena in 
another place, don't destroy it, just redefine your spawn points and your regions.

<br>

## 1. Choose the goal for the arena

PvpArena provide some predefined goals like team death match, player death match, capture the flag and more. 

You can choose a goal [in this list](goals.md).

Each goal has its own setup, so take the time to read [documentation](goals.md) of the goal you want to use.

<br>

## 2. Create the arena

Just type this command to create your arena:

`/pa create [newArenaName] (goal)`

By default, your arena will use [TeamLives](goals/teamlives.md), a goal with a teams gamemode.

You can change arena goal with the command:
`/pa [arenaName] goal [goalName]`

You will find more information about this command [on this link](commands/goal.md).

<br>

## 3. Set spawn points

Now you have to create game spawn points by using this command:  
`/pa [arenaName] spawn set [spawnType] (teamName)`

Spawn types are: `spawn`, `lounge`, `spectator`, `exit`
Number of spawns you have to create depends of the gamemode of your goal (free or teams).

##### Team arenas
For team arenas, you need: 2 spawns (red & blue) / 2 lounges (red & blue) / 1 spectator zone / 1 exit.

##### Free arenas
For free (FFA) arenas, you need: 4 spawns (spawn1, spawn2, spawn3, spawn4) / 1 lounge / 1 spectator zone / 1 exit.

<br>

> **🚩 Tips:**
>- In free arenas, you can create as many spawn points as you want.
>- You can use `/pa spawn set` command again to move a spawn point

<br>

## 4. Create the battle region

> *This step is optional but really useful in mostly configurations*

Now create the battle region with this command:

`/pa [arenaName] region`

It enables selection mode. Equip your hand with a **stick** and set your region with left and right click. 

Then type :

`/pa [arenaName] region [yourNewRegionName]`

Finally, specify your region type :

`/pa [arenaName] regionType [regionName] BATTLE`

> **🚩 Tips:**
> - By default your region is protected from block destruction and placing
> - Get a look to [the region documentation page](regions.md) to improve your arena regions

<br>

## 5. Place required items in the lounge

By default, four classes already exist : Swordsman, Tank, Pyro and Ranger.  
You can chose to keep these classes or create new ones with the the [class command](commands/class.md).

Then simply place signs near your **lounge** spawn point(s) and write the class names on the first line.

Place the signs in each lobby, and an iron block (configurable). 
The iron block is the default ready block that players can click on when they are ready. The match begins
when all players
are ready.

> **🚩 Tips:**
> - Players can can choose their class with `/pa arenaclass [className]` command
> - You can set a default class using the config parameter `autoClass`
> - Players can also be ready typing `/pa ready`, that's why ready block is not mandatory

<br>

## 6. Join the arena!

Your first arena was created! Join the game with:

`/pa [arenaName] (join) (teamName)`

> **🚩 Tip:**  
> If you just type `/pa [arenaName]` your team will be randomly selected.
