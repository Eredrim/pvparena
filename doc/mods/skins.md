# Skins

## Description

This module activates certain skins for teams / classes. 

Compatible plugins:
- [LibsDisguises](https://www.spigotmc.org/resources/libs-disguises-free.81/)

If you don't use a disguise plugin, 
you can use a player name or one of the following:

- SKELETON
- WITHER_SKELETON
- ZOMBIE
- CREEPER 


## Installation

Unzip the module files (files tab, "PA Files v*.*.*") into the /pvparena/files folder and install them via

- `/pa install [modname]`, activate per arena via
- `/pa [arenaname] !tm [modname]`

## Setup

This module needs a full server restart to properly hook into a disguise plugin.

## Config settings

- vanilla \- should we use player/mob heads instead of modded skins? 

## Commands

- `/pa [arena] !sk [teamName or ClassName] [player's name or mob's name]` \- Set team/class to skin. See [EntityType](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html).

## Warnings

If you have issues with PVP, check your pvp settings of the disguise plugin, and make sure that the teams have different skins, if applicable.

## Dependencies

Optional (defaults to player heads): 
- [LibsDisguises](https://www.spigotmc.org/resources/libs-disguises-free.81/)


