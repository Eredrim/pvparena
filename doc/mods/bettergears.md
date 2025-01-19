# BetterGears

## Description

This mod colorize armors to keep track of teams more easily.  
By default, module tries to intelligently guess team colors. If the result doesn't convince you and change them as you 
like.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

- **head** \- apply colored armor on the head slot (default: true)
- **chest** \- apply colored armor on the chest slot (default: true)
- **leg** \- apply colored armor on the leg slot (default: true)
- **foot** \- apply colored armor on the foot slot (default: true)
- **onlyifleather** \- apply colored armor only if slots already contain a leather armor (default: true)

> 🚩 **Tip:**  
> The *onlyifleather* setting makes possible to create different classes with as many leather elements as you want 
> (including zero).

## Commands

- `/pa !bg <name>` \- show class / team
- `/pa !bg <name> color R G B` \- set a team's color values
