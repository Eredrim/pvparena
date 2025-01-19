# Announcements
## Description

Broadcast global messages to talk about your game to all connected players (except the ones in the arena).
## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.
## Setup

Valid Announcement types:

- JOIN \- players joining the arena
- ADVERT \- players starting the arena for the first time
- START \- the match begins
- END \- the match ends
- WINNER \- the winner is announced
- LOSER \- the loser is announced
- PRIZE \- the winning prize is announced
- CUSTOM \- not used 

## Config settings

- all of the above \- is this announcement active?
- radius \- the radius where the announcements are heard. 0 for infinite
- color \- the color to format the announcements (default: AQUA).

Available colors are: AQUA, BLACK, BLUE, DARK_AQUA, DARK_BLUE, DARK_GRAY, DARK_GREEN, DARK_PURPLE, DARK_RED, GOLD, GRAY,
GREEN, LIGHT_PURPLE, RED, WHITE and YELLOW.

## Commands

- `/pa !aa <type>` \- toggle the announcing of types, listed above
