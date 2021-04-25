# Liberation

> ℹ This goal is designed for **team** gamemode

## Description

A medium complex game mode. Dead players are teleported to the killer team's jail. 
Other team member of jailed players can liberate them by clicking on a button, giving them one life back.

## Setup

You have to add spawns for the jails, set them with command [`/pa [arena] spawn [team]jail`](../commands/spawn.md).

Then you have to set a button, at the outside of the jail, that will trigger the liberation of jailed players. To do 
that:
- place a **button** where you want
- type `/pa [arenaname] button [teamname]` (to open selection mode)
- do a left-click on your button

## Config settings

- `llives` \- the lives players have before being put to prison (default: 3)
