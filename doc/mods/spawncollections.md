# SpawnCollections

## Description

This mod allows to run one arena with several spawn setups. Save and load spawn setup to quickly change the pace of the game.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Setup

Simply create spawns as usual, and use commands below to save current spawn configuration to a collection.

## Commands

> ℹ All of the commands are only available out of arena fight times

- `/pa !sc list` \- list available spawn collections
- `/pa !sc save <name>` \- save current spawns configuration to a collection, if this collection exists it will be 
overridden
- `/pa !sc switch <name>` \- replace current spawns configuration with the requested collection
- `/pa !sc remove <name>` \- remove a spawn collection from your config 

> 🚩 **Tips:**  
> * You can edit spawns of a collection by switching to this collection, editing spawn with 
> [`/pa spawn`](../commands/spawn.md) command and saving this collection again
> * Arena spawns configuration is overridden each time you switch to a collection. So if you change arena spawns config,
> keep in mind to save modifications in a collection.