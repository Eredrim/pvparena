# PowerUps

## Description

This mod allows spawning of items that give special powers / bad things, fully customizable

## Installation

Unzip the module files (files tab, "PA Files v*.*.*") into the /pvparena/files folder and install them via

- `/pa modules install powerups`, activate per arena via
- `/pa [arenaname] !tm powerups`

## Setup

Sorry, but you have to add a freaking block to your arena config under `module.powerups.items`. E.g.:

```yaml
items:
  Shield:
    item: OBSIDIAN
    dmg_receive:
      factor: 0.6
  Minions:
    item: BONE
    spawn_mob:
      type: skeleton
      duration: 10
  Sprint:
    item: FEATHER
    sprint:
      duration: 10
  QuadDamage:
    item: IRON_INGOT
    dmg_cause:
      factor: 4.0
      duration: 10
  Dodge:
    item: IRON_DOOR
    dmg_receive:
      chance: 0.2
      factor: 0.0
      duration: 5
  Reflect:
    item: OAK_DOOR
    dmg_reflect:
      chance: 0.5
      factor: 0.3
      uses: 5
  Ignite:
    item: FLINT_AND_STEEL
    ignite:
      chance: 0.66
      duration: 10
  IceBlock:
    item: ICE
    freeze:
      factor: 0.0
      duration: 8
    dmg_receive:
      factor: 0.0
      duration: 8
  Invulnerability:
    item: EGG
    dmg_receive:
      factor: 0.0
      duration: 5
  OneUp:
    item: BROWN_MUSHROOM
    lives:
      diff: 1
  Death:
    item: RED_MUSHROOM
    lives:
      diff: -1
  Slippery:
    item: WATER_BUCKET
    slip:
      duration: 10
  Dizzyness:
    item: COMPASS
    portal:
      duration: 10
  Rage:
    item: ROTTEN_FLESH
    dmg_cause:
      factor: 0.0
      chance: 0.2
      duration: 5
  Berserk:
    item: CACTUS
    dmg_cause:
      factor: 1.5
      duration: 5
    dmg_receive:
      factor: 1.5
      duration: 5
  Healing:
    item: APPLE
    heal:
      factor: 1.5
      duration: 10
  Heal:
    item: BREAD
    health:
      diff: 3
  Repair:
    item: CRAFTING_TABLE
    repair:
      items: helmet,chestplate,leggins,boots
      factor: 0.2
```

So the first layer defines the name, the second layer defines item and adds all the effects it has. This example features all possible ways of doing good and bad things, I hope it is clear oO

## Config settings

- dropspawn \- should the powerup spawn require defined spawns? `/pa [arena] spawn set powerupX` (where X is an integer)
- usage \- by default it is "off", so please set this to either every X kills ("death:X") or every X seconds ("time:X")

## Commands


- `/pa [arena] !pu time 6` \- spawn every 6 seconds
- `/pa [arena] !pu death 4` \- spawn every 4 kills

## Warnings

\-

## Dependencies

\-
