# CheckPoints

> ℹ This goal is designed to be played in free-for-all

## Description

Players have to take a path composed by checkpoints. The first one every checkpoint up to the last (in order) wins!


> 🚩 **One more thing:**  
> Players can get back to the latest checkpoint with /pa checkpoint

## Setup

Spawns have to be added. In order to do that, use `/pa <arenaname> checkpoint <number>`. This sets checkpoint number \<number\>.
Make sure you start with 1 and don't forget to add every single number, or else it will not be possible to win 😋

## Config settings  

- `cpclaimrange` - how near need players to be? (default: 5)
- `cplives` - number of checkpoints to reach (default: 10)
- `cptickinterval` - the amount of ticks to wait before checking for position (default: 20 = 1 second)

<br>

> ⚙ **Technical precision:**  
> This goal has to check for player's position. Based on the player and checkpoint count this can lag your server. 
> Unfortunately, there is no other way to determine a claimed checkpoint.
