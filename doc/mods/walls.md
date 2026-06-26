# Walls

## Description

Yep. It's walls. Simple as can be.
This module create walls when the match starts and remove them after a while.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Setup

You need to create region(s) to define the walls (if you don't know how to do it, please [read this](../regions.md)). 
Regions will be filled with your wall material at the beginning of the match. **Only air blocks** will be replaced with 
your wall material.

**Name the wall regions using `wall` prefix** (e.g. *wall1*, *wall*, *wallX*). There's no need to set a specific region 
type and the regions can overlap other ones (battle ones and other walls).

## Config settings

- **modules.walls.wallseconds**: the time (in seconds) during the walls will stay. (default: 300 - i.e. 5 minutes)
- **modules.walls.wallmaterial**: the material the wall is made of (default: SAND)

<br>

> **🚩 Tips:**
>- You can edit settings in-game using [`/pa set` command](../commands/set.md). Therefore, material can be set by
> using item in your hand with the command `/pa <arena> set modules.walls.wallmaterial hand`.  
>- Mind to reload your arena with `/pa <arena> reload` after each setting edition.
>- Remember to include underground layers in your region if they are accessible.
>- You can check region borders using [`/pa <arena> region <region> border`](../commands/region.md)

<br>

> ⚙️ **Technical precisions:**
> - To prevent lags, wall building and removal are asynchronous. Action is run at 2000 blocks created/removed per second.
> - Walls are built from bottom up and removed from the top down. So you can use gravity-affected blocks like sand or
> concrete powder.
> - Avoid making wall regions too large (1 block large is sufficient in most cases). Otherwise, walls may make too much
> time to be built at the beginning of the match.