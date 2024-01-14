# Titles
## Description

Add a new way to send arena messages by using the vanilla "title" command feature!

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

All above settings are under **modules.titles** key in arena configuration. They can be changed directly in config file
or with [`/pa set`](../commands/set.md) command. Don't forget to [reload](../commands/reload.md) the arena after your 
changes 😉

- **color**: the color of titles (default: AQUA)
- **join**: if true, adds a title when player is joining the arena (default: false)
- **start**: if true, adds a title the match begins (default true)
- **end**: if true, adds a title the match ends (default false)
- **death**: if true, adds a title a player dies (default true)
- **leave**: if true, adds a title a player is leaving (default true)
- **advert**: if true, adds a title to invite **external players** to join the arena after the first player joined (default: false)
- **count**: if true, adds a title for all countdowns like start or respawn (default true)
- **loser**: if true, adds a title when game is lost like in infected or tank goals (default true)
- **winner**: if true, adds a title to announce winners (default true)

> ⚙️ **Technical precision:**  
> Available colors are: AQUA, BLACK, BLUE, DARK_AQUA, DARK_BLUE, DARK_GRAY, DARK_GREEN, DARK_PURPLE, DARK_RED, GOLD, 
> GRAY, GREEN, LIGHT_PURPLE, RED, WHITE and YELLOW.