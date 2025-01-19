# ItemSpawners

## About

This mod makes possible to spawn random items (from defined set) at predefined spots.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Setup

Firstly, create as many item spawning places as you want with command: `/pa <arena> spawn itemX` (X is a number). Items
will be dropped on those places during the match.

Then create the sets of items to drop on each spawn point. To do that, type `/pa <arena> !is setItems itemX inventory`,
where `itemX` is the name of a previously defined spawn point. The command uses all your current inventory to create
the item set of the spawner. You can also use `hand` instead of inventory `inventory`, to only use the item you're 
holding in your (right) hand.

## Config settings

- **modules.itemspawners.interval**: spawn interval in seconds (default: 30)
- **modules.itemspawners.itemX**: item set for spawn point `itemX` (X doesn't really exist and should be a number)

## Commands

- `/pa <arena> !is setItems <itemX> <inventory/hand>` \- create or replace the item set for a spawn point using either
inventory of current player or item in hand of current player.


> ⚙️ **Technical precision:**  
> Your [BATTLE](../regions.md#region-types) arena region automatically clears entities after each match, including 
> dropped items of this module.