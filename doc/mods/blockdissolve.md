# BlockDissolve

## About

Blocks under the player will dissolve just few milliseconds after they walk on it.
Use it with [PlayerLives](../goals/playerlives.md) goal to create TNT Run arenas or 
spice up your spleef games !

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

##### Dictionary of settings:
- **modules.blockdissolve.materials**: List if items corresponding to materials to dissolve.
  Syntax [is the items one](../items.md). (default: SNOW and each kind of WOOL)
- **modules.blockdissolve.startseconds**: the seconds to count down before the match starts (default: 10)
- **modules.blockdissolve.ticks**: the ticks after what time the block under the player should dissolve (20 ticks = 1 second)
  (default: 40). 

<br>

> **🚩 Tips:**  
>- BlockDissolve is compatible with [BlockRestore](./blockrestore.md), so you can use it to regen the floor after each game.
>- If you want to create a TNT Run arena, set ticks parameter to **8**
>- You can edit settings in-game using [`/pa set` command](../commands/set.md). Therefore, materials can be set by 
> filling your inventory with all types of block to dissolve and by using the command
> `/pa <arena> set modules.blockdissolve.materials inventory`.
