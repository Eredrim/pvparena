# ChestFiller

## About

This mod randomly places stuff you can configure inside containers being in the arena battlefield.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Setup

Firstly, you need to have a BATTLE region. It's a requirement.

Then, you have to set what you want to put inside the chests of the battlefield. Either you can create a source chest
with `sourceChest` subcommand (see below) or you can save your own inventory as source using `/pa <arena> set 
modules.chestfiller.cfitems inventory`.

Finally, you can specifically define which containers will receive the stuff. If so, use the `addContainer` subcommand
(see below). If not, all chests, trapped chests, barrels and shulker boxes on the battlefield during the first launch
of a game will be set as a container to fill.

## Config settings

- **modules.chestfiller.items**: List of items to put in chests to fill. Uses [items syntax](../items.md). 
(default: 1 stone block)
- **modules.chestfiller.maxItems**: maximum number of items to put in a chest. (default: 5)
- **modules.chestfiller.minItems**: minimum number of items to put in a chest. (default: 0)
- **modules.chestfiller.sourceLocation**: a containers location to read the items list from. Overrides 
`modules.chestfiller.items`. (default: none)
- **modules.chestfiller.clear**: if true, clears all chests to fill before filling them
- **modules.chestfiller.containerList**: List of coordinates of all containers to fill

## Commands

- `/pa <arena> !cf sourceLocation` \- set the container that you're looking as source to get the inventory from 
(overrides `modules.chestfiller.items`). Works with double chests.
- `/pa <arena> !cf addContainer` \- add the container that you're looking to the list of chests to be filled
- `/pa <arena> !cf clear` \- clear the list of chests to be filled

<br>

> 🚩 **Tips**:
> * [BlockRestore](blockrestore.md) is fully compatible with ChestFiller! By enabling `restorecontainers` setting you can:
>   * Set distinct containers between containers to fill and containers to restore using commands of the both modules
>   * Blend saved content and random one. To do that, be sure `modules.chestfiller.clear` is false and edit your arena
>   to fill containers with static content.  
>   *On startup of a game, chests will be saved and random items from ChestFiller will be added afterward. At the end 
>   of the game, all containers will be reset at their initial status (before filling).*
> 
> 
> * [WorldEdit](worldedit.md) is also compatible with ChestFiller. Mind to set `modules.chestfiller.clear` to false
> to prevent cleaning of containers content from your schematic.

<br>

> ⚙️ **Technical precision:**  
> Arena startup may lag if you've just reset container list. That's fully normal because it searches the BATTLE 
> region(s) for containers and saves location of each of them.