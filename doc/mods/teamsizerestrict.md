# TeamSizeRestrict

## About

This mod makes possible to limit players in each team of the arena.

> ℹ️ **Notice:**  
> This module is useful only if you want to set a different maximum number of players for several teams. On the contrary, 
> if you want to set a global maximum of player per team, you can just set `ready.maxTeam` in your 
> [configuration](../configuration.md#arena-configuration-files) without using this module.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Commands

- `/pa <arena> !tsr <team> <maxNumber>` \- set the maximum of allowed players for a team. Set to `0` to disable.

## Config settings

- **modules.teamsize.[teamname]**: the maximum number of people this team may have, disabled if equals to `0`.