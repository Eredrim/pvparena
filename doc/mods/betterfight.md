# BetterFight

## Description

This mod enhances fighting by adding one-shot items, with editable sound, and explosions on death (like in Worms WMD).

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

Reload your arena (with `/pa <arena> reload`) after installation to generate a default config.

## Config settings

##### Example of configuration:
```yaml
modules:
  betterfight:
    sounds:
      snowball: BLOCK_SNOW_HIT
      arrow: ENTITY_PILLAGER_CELEBRATE
      egg: none
      fireball: none
    explodeOnDeath: true
    explodeOnlyWithOneShotItem: false
    oneShotItems:
      - ARROW
      - SNOWBALL
```

##### Dictionary of settings:
- **explodeOnDeath**: Create an explosion when a player dies. Destroys battlefield if there's no [region protection](../regions.md#region-protection) (default: true)
- **explodeOnlyWithOneShotItem**: Restrict explosions on death to one-shot items only (default: false)
- **oneShotItems**: List of throwable items that one-shot players (default: empty)
- **sounds**: Sounds played when a player is one-shot with a oneShotItem (default: none for each item)

## Commands

- `/pa <arena> !bf explode` \- toggle `explodeOnDeath` setting
- `/pa <arena> !bf explodeOnlyWithOneShot` \- toggle `explodeOnlyWithOneShot` setting
- `/pa <arena> !bf add <itemType>` \- add a throwable item to one-shot item list (limited to : arrow, snowball, egg, fireball)
- `/pa <arena> !bf remove <itemType>` \- remove a throwable item from one-shot item list
- `/pa <arena> !bf sound <itemType> <sound>` \- set the sound one-shot when it hits player (list of sounds below).


## Sounds list

The full list of available sounds is [available here](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html)
