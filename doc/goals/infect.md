# Infect

> ℹ This goal is designed to be played in free-for-all

## Description

A random player is infected, he can have special gear and everyone is against that player. 
Killing an infected player kicks him out of the game, killing a non infected player infects him. 
Either the infected win by killing everyone else, or the other way round. 

## Setup

At least one special "infected" spawn has to be set. Type the following command: 
[`/pa <arena> spawn infected*`](../commands/spawn.md)

The other spawns are set like the standard free for all goals, with spawns named like _spawn1_ through _spawn10_ or alike.

You can set a class called `%infected%` in order to give infected players special gear.

## Config settings

- `iilives` \- infected player's lives (default: 1)
- `inlives` \- normal player's lives (default: 1) 
