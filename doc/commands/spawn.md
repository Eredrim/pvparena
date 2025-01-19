# Spawn Command

## Description

Set an arena spawn to your current position, including orientation !

## Usage

| Command                                                 | Definition                  |
|---------------------------------------------------------|-----------------------------|
| /pa \<arena\> spawn set \<spawnName\> (teamName) (class)    | Define a spawn for an arena |
| /pa \<arena\> spawn remove \<spawnName\> (teamName) (class) | Remove a spawn for an arena |

For multi-spawn, you can set everything as name, as long as name **start with** the spawn type.  
The spawn will be chosen randomly.

Example with type "exit":
- `/pa ctf spawn set exit` - sets exit spawn of the arena "ctf"

Example with type "fight": 
- `/pa ctf spawn set fight red` - sets the red team's fight spawn of the arena "ctf"
- `/pa ctf spawn remove fight2 red` - removes the second red team's fight spawn of the arena "ctf"
- `/pa free spawn set fightEAST` - sets "fightEAST" of the arena "free"
- `/pa ctf spawn set red fight Pyro` - sets fight spawn only for Pyro class of red team

Example with type "lounge":
- `/pa ctf spawn set lounge red` - sets the red team's lounge spawn of the arena "ctf"
- `/pa free spawn set lounge` - sets lounge  of the arena "free"
- `/pa free spawn removed lounge` - removes lounge  of the arena "free"

## Details

There are two syntax according to the gamemode (free or teams) of your arena goal: 
- if you're using a "free" arena, you can define unlimited spawns using syntax `/pa myArena spawn set fightX team` where X should
 be anything (word, digit, letter, etc).
- if your arena works with teams, you have to use `/pa myArena spawn set fight team` where "team" is the name of one of your 
teams.
- you can set spawn only for some class with `/pa myArena set team spawn class` where "class" is the name of one of your
  arena class.

If you get a message "spawn unknown", this is probably because you did not install/activate a [goal](../goals.md) or 
a [module](../modules.md). 
Be sure you have installed and activated stuff you want to add, for instance the "Flags" goal, or the "StandardSpectate" 
module...

## Spawn requirements for each arena

In general cases, you need to set those spawns:
- for team arenas: one spawn per team, one lounge per team, one spectator area and one exit
- for free arenas: several spawns, one lounge, one spectator area and one exit

Anyway, the plugin tell you which spawns are missing if there.
