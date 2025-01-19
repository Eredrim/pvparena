# BetterKillstreaks

## Description

This mod adds a complete management of kill streaks: custom messages, item rewards and dedicated potion effects!
A kill streak is reset when killer player dies.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

Reload your arena (with `/pa <arena> reload`) after installation to generate a default config.

## Config settings

##### Example of configuration:
```yaml
modules:
  betterkillstreaks:
    definitions:
      l1:
        msg: First blood
        effects:
          - HEAL:2:3
          - STRENGHT:1:10
      l2:
        msg: Double Kill!
        items:
        - type: BOW
        - type: ARROW
          amount: 64
      l3:
        msg: Triple Kill!
```

##### Dictionary of settings:
- **msg**: message displayed in killer chat when kill level is reached (`l1` = 1 kill). Use "none" to remove it.
- **effects**: List of custom potion effects applied for this kill level. The syntax is `EFFECT_TYPE:EFFECT_LEVEL:DURATION_IN_SECONDS`
- **items**: List of item rewards for this kill level. Syntax [is standard](../items.md).

## Commands

- `/pa <arena> !bk <level>` \- show defined settings for this level of kills. (e.g. "!bk 2" to show config when player makes a double kill)
- `/pa <arena> !bk <level> message` \- set message for this kill level, use "none" to remove it. (e.g. "!bk 10 message Genius of murder!")
- `/pa <arena> !bk <level> addEffect <effectType> <amplifier> <duration_seconds>` \- Add a potion effect for this kill level. (e.g. "!bk 2 HEAL 2 3")
- `/pa <arena> !bk <level> removeEffect <effectType>` \- remove all effects of this type from the kill level configuration.
- `/pa <arena> !bk <level> items` \- copy **current inventory content** to create a reward for this item level
- `/pa <arena> !bk <level> clear` \- clear all kill level configuration and remove it

> **ℹ️ Notice**:
> Potion effects are not reset when a killer reaches a new kill level. Keep in mind to not set too long effect durations
> to avoid effect stacking.


## Potion Effect Types

The full list of available effects is [available here](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html)