# BlockDestroy

> ℹ This goal is designed for **team** gamemode

## Description

Each team have to destroy the block of opponent team(s). Blocks have lives, therefore when a block a no longer life, 
its team is eliminated.

## Setup

Firstly, make sure the `blockType` parameter is set as you want.

If so, you can register destroyable blocks in your arena config.
In order to do that, use `/pa [arenaname] [teamname]_block `. This will enable the selection mode. 
Then left-click of the chosen block (that has to be of the defined block type).

## Config settings

- `blockType` - the material checked for blocks (default: IRON_BLOCK)
- `bdlives` - the maximum count of blocks/block destructions a team needs to win (default: 1)

<br>

> 🚩 **Tip:**  
> If you choose a colorable block (like RED_WOOL or WHITE_CONCRETE) all its variants will be supported. So you will be
> able, for instance, to set a red block for one team and a blue block for the other one.
