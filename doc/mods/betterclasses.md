# BetterClasses

## Description

This mod adds more to your classes, POTION effects and maximum player count!

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

Reload your arena (with `/pa <arena> reload`) after installation to generate a default config.

## Config settings

##### Example of configuration:
```yaml
modules:
  betterclasses:
    Tank:
      maxGlobalPlayers: -1
      maxTeamPlayers: -1
      neededEXPLevel: 0
      permEffects:
        DAMAGE_RESISTANCE: 1
        SLOW: 2
    Archer:
      maxGlobalPlayers: -1
      maxTeamPlayers: 2
      neededEXPLevel: 0
      respawnCommand: 'give %player% minecraft:arrow 1'
```

##### Dictionary of settings:
- **maxGlobalPlayers**: number of players in the whole arena that can have this class at the same time
- **maxTeamPlayers**: number of players in the same team that can have this class at the same time
- **neededEXPLevel**: Required XP level to get this class. Uses the XP level of the player before joining the arena.
- **respawnCommand**: Command (without leading `/`) applied at the respawn of this class. You can use the placeholder `%player%` to get current 
player name.
- **permEffects**: List of applied effects with syntax `EffectType: amplifier`. 

## Commands

- `/pa <arena> !bc <className>` \- show info about that class
- `/pa <arena> !bc <className> add <effectType> (amplifier)` \- add a potion effect to a class (e.g. "add SLOW 2")
- `/pa <arena> !bc <className> remove <effectType>` \- remove all potion effects of type `<effectType>` to a class
- `/pa <arena> !bc <className> clear` \- remove all potion effects from that class
- `/pa <arena> !bc <classname> set <maxTeamPlayers/maxGlobalPlayers/neededEXPLevel> <value>` \- change value of one setting of a class
- `/pa <arena> !bc <className> respawncommand (command)` \- set a class respawn command (empty to remove)

## Potion Effect Types

The full list of available effects is [available here](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html)