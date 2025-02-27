# WorldEdit

## About

This module adds WorldEdit hooking for BATTLE [region](../regions.md) backup, restoring, even automatically.

You can also use this module to design your arena [regions](../regions.md) with a WorldEdit selection (cuboid shape only).

## Setup

Install the module in a normal way (you'll find installation process in 
[modules page](../modules.md#installing-modules) of the doc).  
After installation, the module needs a full server restart to hook into WorldEdit properly, for the first time.

## Config settings

*These settings can be found under `mods.worldedit` node in your arena config file.*

- autoload - automatically load the arena's regions of the list (or BATTLE regions if list is empty) after fight (default: false)
- autosave - automatically save the arena's regions of the list (or BATTLE regions if list is empty) before fight (default: false)
- regions - specify individual regions to load/save rather than all BATTLE regions (default: empty)
- schematicpath - the path where worldedit schematic files will be stored (default: `/plugins/pvparena/schematics`)
- replaceair - if true, air blocks will be pasted on restore (default: true)

## Commands

- `/pa <arena> !we load <regionname>` \- load and paste the previously saved region (using its schematic)
- `/pa <arena> !we save <regionname>` \- save the region into a schematic file
- `/pa <arena> !we create <regionname>` \- create a region based on an active WorldEdit selection
- `/pa <arena> !we autoload` \- toggle general automatic loading
- `/pa <arena> !we autosave` \- toggle general automatic saving
- `/pa <arena> !we list add <regionname>` \- add a region to the list to be automatically saved/loaded (depending on autoload/autosave config)
- `/pa <arena> !we list remove <regionname>` \- remove a region from the list
- `/pa <arena> !we list show <regionname>` \- show the list of the regions that are automatically saved/loaded (depending on autoload/autosave config)


## Use case

You just want to restore your arena after the match ? Just follow these instructions:
- Save your BATTLE region with command `/pa <arena> !we save <regionname>`
- Add the previously saved region name to the list with `/pa <arena> !we list add <regionname>`.
- Enable autoload with `/pa <arena> !we autoload`

<br>

> ⚙ **Technical precision:**  
> Worldedit pasting may freeze your server due to its working way. 
> So be sure to regen only destroyable/buildable areas to reduce reloading time.
