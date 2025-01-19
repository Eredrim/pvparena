# RespawnRelay

## Description

This mod puts respawns players in a "relay" spawn arena in order to respawn them. This will happen after a set amount of time.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Setup

You need to set a spawn called "relay" -> `/pa <arena> spawn set relay`

## Config settings

- respawnseconds \- The respawn time in seconds (default: 10)
- choosespawn \- Should players be able to choose the next spawn during relay time, by writing it in chat? (default: false)


> 🚩 **Tip:**  
> Players can change their class with [`/pa -ac`](../commands/arenaclass.md) command during relay time


> ℹ️ **Limitations**:  
> `choosespawn`setting is only available in free goals (not team ones)