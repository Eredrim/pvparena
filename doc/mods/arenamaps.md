# ArenaMaps

## Description

This mod hands players a map of the arena. Optional display of score and players/spawns/blocks (flags) position can be 
displayed on the map. Everything is refreshed in real time.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

- alignToPlayer \- if true, map will "follow" the player when they're moving. (default: false)
- showScore \- should team lives be shown on the top of the map? (default: true)
- showSpawns \- show spawns on the map (as colored squares). (default: ALL) 
- showBlocks \- show blocks (or flags) on the map (as colored crosses). (default: ALL)
- showPlayers \- show player positions on the map (as colored circles). (default: TEAM)

Values of 3 latter settings can be:
* ALL \- to display everything/everyone
* TEAM \- to display only elements related to player's team
* OTHER \- to display only elements **not** related to player's team
* NONE \- to disable displaying of this kind of element

## Commands

- `/pa <arena> !map align` \- toggle the alignToPlayer setting
- `/pa <arena> !map score` \- toggle the showScore setting
- `/pa <arena> !map players <value>` \- set the showPlayers setting
- `/pa <arena> !map blocks <value>` \- set the showBlocks setting
- `/pa <arena> !map spawns <value>` \- set the showSpawns setting

> ⚙ **Technical precisions:**  
> * Score displaying takes a lot of space at the top of the map. It's recommended to disable it for 3 teams or more.
> * Map is auto-scaled to arena size depending of your arena BATTLE region.