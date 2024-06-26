# Squads

## About

This mod makes possible to create squads of players. Instead of respawning anywhere / at the team spawn, a squad player 
will respawn to a random mate position.

Squads are an addition to teams or may a way to group players in FFA arenas.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Setup

This mod works signs to join and leave squads. So, you have to create them.
If you want players can switch between squads during a game (c.f. config settings), you should place squads signs in
the battlefield, in the spawn areas. Otherwise, you should place them directly in the lounges.

1. Create a squad using the `/pa [arena] !sq add [squadName] [playerLimit]` command.


2. Then create the join sign using the following template:
    ```
    [Squad name]
    [what you want / ignored]
    empty
    empty
    ```
    
    For instance, for a squad named "icebreakers", you can create a sign like this:
    ```
        Icebreakers
    <=================>
    
    
    ```

    You can add another one or multiple blank signs **below** to display of more player names.


3. Do the same thing for all squads

<br>

> **🚩 Tips:**  
> - All the squads can be created in a first time with the command and then all signs can be placed. 
> Order doesn't matter.
> - The squad name is case-insensitive and supports sign colors

## Config settings

- **modules.squads.ingameSquadSwitch**: allow switching squads ingame (default: false)

## Commands

- `/pa [arena] !sq` \- show the arena squads
- `/pa [arena] !sq add [squadName] [playerLimit]` \- add squad with player limit (set to 0 to remove limit)
- `/pa [arena] !sq remove [squadName]` \- remove squad [name]
- `/pa [arena] !sq set [squadName] [playerLimit]` \- set player limit for squad