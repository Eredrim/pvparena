# Spawn Command

## Description

Set an arena spawn to your current position, including orientation !

## Usage

Command |  Definition
------------- | -------------
/pa [arena] spawn [spawnName] (teamName) (class) | Define a spawn for an arena

For multi-spawn, you can set everything as name, as long as name **start with** the spawn type.  
The spawn will be chosen randomly.

Example with type "exit":
- `/pa ctf spawn exit` - sets exit spawn of the arena "ctf"
pa
Example with type "spawn": 
- `/pa ctf spawn spawn red` - sets the red team's spawn of the arena "ctf"
- `/pa ctf spawn spawn2 red` - sets the second red team's spawn of the arena "ctf"
- `/pa free spawn spawnEAST` - sets "spawnEAST" of the arena "free"
- `/pa ctf spawn spawn red Pyro` - sets spawn only for Pyro class of red team

Example with type "lounge":
- `/pa ctf spawn lounge red` - sets the red team's lounge spawn of the arena "ctf"
- `/pa ctf spawn lounge2 red` - sets the second red team's lounge spawn of the arena "ctf"
- `/pa free spawn loungeEAST` - sets lounge "loungeEAST" of the arena "free"
- `/pa ctf spawn lounge red Pyro` - sets lounge spawn only for Pyro class of red team


## Details

There are two syntax according to the [gamemode](gamemode.md) of your arena : 
- If you're using a "free" arena, you can define unlimited spawns using syntax `/pa myArena spawn spawnX` where X should
 be anything (word, digit, letter, etc).
- If your arena works with teams, you have to use `/pa myArena spawn spawn team` where "team" is the name of one of your 
team.
- you can set spawn only for some class with `/pa myArena spawn spawn team class` where "class" is the name of one of your
  arena class.

If you get a message "spawn unknown", this is probably because you did not install/activate a [goal](../goals.md) or 
a [module](../modules.md). 
Be sure you have installed and activated stuff you want to add, for instance the "Flags" goal, or the "StandardSpectate" 
module...

## Spawn Offset

You can define unique offsets for each spawn name, in order to not be placed on the block center but rather one edge:

- `/pa [arena] spawn [spawnname] offset X Y Z`

For example 0.5 0 0.5 as X Y Z would work setting you on an edge. 
You might want to keep F3 at hand to see if you actually have to add or subtract to get to the right edge.
