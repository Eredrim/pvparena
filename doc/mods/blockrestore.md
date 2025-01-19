# BlockRestore

## About

This mod restores blocks (not entities) of BATTLE [region](../regions.md) after the match.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

*These settings can be found under `modules.blockrestore` node in your arena config file.*

- **hard** \- the mod will restore EVERY block of your battle region, regardless of a known changed state (default: false)
- **offset** \- the time in TICKS (1/20 second) that the scheduler waits for the next block set to be replaced (default: 1)
- **restoreblocks** \- restore blocks (default: true)
- **restorecontainers** \- restore containers (chests, furnaces, brewing stands, etc) content (default: false) 
- **restoreinteractions** \- restore player interactions with blocks like opened doors or toggled levers (default: false) 
- **containerlist** \- list of coordinates of all registered containers 

## Commands

- `/pa <arena> !br hard` \- toggle the hard setting
- `/pa <arena> !br restoreblocks` \- toggle the restoreblocks setting
- `/pa <arena> !br restorecontainers` \- toggle the restorecontainers setting
- `/pa <arena> !br restoreinteractions` \- toggle the restoreinteractions setting
- `/pa <arena> !br addinv` \- add a new saved container location (uses the container you're looking)
- `/pa <arena> !br clearinv` \- clear saved containers locations
- `/pa <arena> !br offset X` \- set the restore offset in TICKS! 

<br>

> **🚩 Tips:**  
> - There's no need to manually add containers location to config. By default, all containers of your BATTLE region will
>   be registered on the first startup.
> - If you physically add new chests to your map, don't forget to register them. Even manually with 
>   `/pa <arena> !br addinv`, or by forcing a new complete registration with `/pa <arena> !br clearinv`.
> - BlockRestore is designed for block destruction, chest and block usage only. If you need advanced restoring 
>   (especially entities), please prefer [WorldEdit](./worldedit.md) mod. 

<br>

> ⚙ **Technical precisions:**  
> - BlockRestore is fully asynchronous and may take some time restore the battlefield (few seconds in most cases). 
    This one is not reachable during the process.
> - Chest restoring may lag on the first startup or after a `clearinv`, because it searches the BATTLE region(s) for 
>   chests and saves location of each of them.
> - Due to API limitations on servers that are **not based on PaperMC**, destruction of linked blocks (like wall torches
>   when the support block is broken) is limited to doors and non-solid blocks on the top of the support block, along 
>   with directional blocks on the other faces.



