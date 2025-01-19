# PowerUps

## About

This mod allows spawning of items that give special powers / bad things, fully customizable. Players can get only one 
PowerUp item at time. If they collect a new one, the previous one is replaced and disabled.

> ⚠️ This module is complex to set up, so take time to carefully read this documentation and to test your configuration.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Setup instructions

#### 1. Define where items will spawn

By default, items will randomly spawn within your BATTLE region. If it's ok for you, go next step, otherwise type 
`/pa <arena> !pu dropspawn`. Then create PowerUps spawn locations using `/pa <arena> spawn set powerupX` (with X as a number).

#### 2. Choose how item spawning is triggered

You can choose to spawn PowerUp items with a time frequency or when a number of death is reached in your arena.
Run `/pa <arena> !pu time <number>` to use time frequency, run `/pa <arena> !pu deaths <number>` otherwise.

#### 3. Create your own list of special items!

Edit your arena config file (located in the folder: `plugins/pvparena/arenas`).
Now create a `items` entry under `module.powerups`. Then add your effects using [documentation below](#powerups-items) 
or just copying the preset list [at the end of this page](#example-of-items-configuration).


## Config settings

These settings are under **module.powerups** node in arena config file.

- **usage.trigger**: what is counted to make items spawn. Possible values are `TIME` (default) and `DEATHS`
- **usage.frequency**: each time this value is reached, a powerup item will spawn (default: 0 - i.e. disabled). Should 
be a number of deaths or a number of seconds.
- **dropspawn**: If false, powerups spawn randomly in the BATTLE region. If true, powerups only spawn on dedicated spawn 
points. (default: false)

> 🚩 **Tip:** 
> To create a spawn point for PowerUps in your arena, use `/pa <arena> spawn set powerupX` (where X is an integer)

## Commands

- `/pa <arena> !pu dropspawn` \- toggle dropspawn setting
- `/pa <arena> !pu time <number>` \- use time trigger for spawning and define spawn frequency (in seconds)
- `/pa <arena> !pu deaths <number>` \- use deaths trigger for spawning and define death frequency between item spawns

## PowerUps items

### List of effects

| Effect        | Enabled on      | Who get the effect?                  | Effect description                              | Duration (seconds) | Factor                                                             | Diff                                 |
|---------------|-----------------|--------------------------------------|-------------------------------------------------|--------------------|--------------------------------------------------------------------|--------------------------------------|
| potion_effect | pickup          | player who picks-up the item         | apply a potion effect                           | effect duration    | effect power                                                       | N/A                                  |
| health        | pickup          | player who picks-up the item         | add/remove health to the owner                  | N/A                | ratio of restored/removed health                                   | number of added/removed half-hearts  |
| lives         | pickup          | player who picks-up the item         | add/remove lives/points to player or their team | N/A                | N/A                                                                | number of added/removed arena lives  |
| repair        | pickup          | player who picks-up the item         | repair parts of your stuff                      | N/A                | ratio of restored durability                                       | N/A                                  |
| sprint        | sprint          | player who uses the item             | make sprint with a boost                        | boost duration     | boost power (i.e. value of a speed potion)                         | N/A                                  |
| spawn_mob     | right-click     | player who uses the item             | spawn a mob on the target block                 | mob lifetime       | N/A                                                                | N/A                                  |
| dmg_reflect   | getting a hit   | attacker player                      | damage the attacker as stronger as their hit    | N/A                | ratio of the returned damage                                       | N/A                                  |
| dmg_receive   | getting a hit   | target player (owner of the powerUp) | change damage of received hit                   | N/A                | ratio of the new damage                                            | N/A                                  |
| dmg_cause     | hit             | target player                        | change damage of given hit                      | N/A                | ratio of the new damage                                            | N/A                                  |
| freeze        | hit             | target player                        | freeze the target player                        | freeze duration    | N/A                                                                | N/A                                  |
| ignite        | hit             | target player                        | ignite the target player                        | fire duration      | N/A                                                                | N/A                                  |
| heal          | hit (with item) | target player                        | give a regen effect to target player            | regen duration     | [regeneration level](https://minecraft.wiki/w/Regeneration#Effect) | N/A                                  |

NB: ratio means a number between 0 and 1 used like a percentage. E.g. use 0.5 for 50% or 2.0 to double a value.

### Config settings

Sorry, but you'll need to create a freaking block of settings in your arena config file (c.f. example below).
To create it from zero, create keys under `module.powerups.items` which are the names of your PowerUp items.

Now, for each PowerUp **item**, you can add one or multiple **effects** (see above for effects definition).

Each **effect** has settings you can change, there are:
- **chance**: probability effect may be triggered. Should be a ratio between 0 and 1. (default: 1)
- **duration**: effect duration (c.f. table above for details). -1 means permanent. (default: -1)
- **factor**: c.f. table above for details. (default: 1)
- **diff**: c.f. table above for details. (default: 0)
- **potionEffect**: type of potion effect. A list of available effects can be found [here](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html) \[only for potion_effect effect\]
- **items**: kind of item to repair. Can be `HELMET`, `CHESTPLATE`, `LEGGINGS`, `BOOTS` or any item type (like `SWORD`) 
\[only for repair effect\]
- **mobType**: type of mob to spawn. A list of available mob types can be found [here](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html) \[only for spawn_mob effect\]

Now your PowerUp item has one or several effects, you can add it attributes:
- **item**: type of item. A list of available item names can be found [here](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html). (Required)
- **lore**: item lore as a list of strings (default: empty)
- **uses**: number of times the PowerUp can be used, use `-1` for no limit (default: -1)
- **lifetime**: time (in seconds) during the item can be used, use `-1` for no limit (default: -1)

> ℹ️ **About uses and lifetime:**
> These settings make possible to limit Powerup usage. When the limit is reached, PowerUp item is automatically 
> destroyed. If both settings are active, this will happen when the first of the two limits is reached.
> 
> Effects enabled on **pickup are not affected** by these settings because they are only triggered once.

### Example of items configuration

Here is a full example of configuration. You can copy it as-is or customize it.

```yaml
modules:
  powerups:
    items:
      # Reduce of 40% damages on the owner during 120 seconds
      Shield:
        item: OBSIDIAN
        duration: 120
        dmg_receive:
          factor: 0.6
      # On pickup, give a Jump Boost effect (power 2) during 30 sec
      JumpBoost:
        item: RABBIT_FOOT
        potion_effect:
          potionEffect: JUMP_BOOST
          duration: 30
          factor: 2
      # On pickup, give a poison effect (power 3) during 5 sec
      Poison:
        item: POISONOUS_POTATO
        potion_effect:
          potionEffect: POISON
          duration: 5
          factor: 3
      # Tool to make spawn skeleton with 15 seconds of lifetime. Can be used only 5 times.
      Minions:
        item: BONE
        uses: 5
        lore:
          - Right-click to create a minion!
        spawn_mob:
          mobType: skeleton
          duration: 15
      # Give 50% to give a boost speed effect during 10 seconds each time player sprints
      Sprint:
        item: FEATHER
        sprint:
          duration: 10
          change: 0.5
      # Multiply owner's damage by 4 during 30 sec
      QuadDamage:
        item: IRON_INGOT
        lifetime: 30
        dmg_cause:
          factor: 4.0
      # During 10 sec, each received hit has 20% of chance to be skipped 
      Dodge:
        item: IRON_DOOR
        lifetime: 10
        dmg_receive:
          chance: 0.2
          factor: 0.0
      # Each received damage has 50% of chance to be returned with 30% of its original power. Can be used 5 times.
      Reflect:
        item: OAK_DOOR
        dmg_reflect:
          chance: 0.5
          factor: 0.3
          uses: 5
      # Each given hit has a 2 in 3 chance to ignite the enemy during 10 sec
      Ignite:
        item: FLINT_AND_STEEL
        ignite:
          chance: 0.66
          duration: 10
      # During 5 sec, each hit freezes target players for 8 seconds
      IceBlock:
        item: ICE
        lifetime: 5
        freeze:
          duration: 8
      # During 5 sec, all damage is ignored
      Invulnerability:
        item: EGG
        lifetime: 5
        dmg_receive:
          factor: 0.0
      # Add an arena live/point to owner of the powerUp. Effect is triggered only once, on pickup.
      OneUp:
        item: BROWN_MUSHROOM
        lives:
          diff: 1
      # Remove an arena live/point to owner of the powerUp. Effect is triggered only once, on pickup.
      Death:
        item: RED_MUSHROOM
        lives:
          diff: -1
      # 20% of chance to fail to hit target during one minute
      Rage:
        item: ROTTEN_FLESH
        lifetime: 60
        dmg_cause:
          factor: 0.0
          chance: 0.2
      # Each hit is 50% more powerful but received damage is increased by 50%
      Berserk:
        item: CACTUS
        lore:
          - Stronger but more vulnerable...
        dmg_cause:
          factor: 1.5
        dmg_receive:
          factor: 1.5
      # Hitting player with this item gives them a Regen II effect during 10 seconds. Can be used twice.
      Healing:
        item: APPLE
        lore:
          - Hit your friends with this item to heal them
          - Can be used twice
        uses: 2
        heal:
          factor: 2
          duration: 10
      # Gives 1 heart and a half to player on pickup
      Heal:
        item: BREAD
        health:
          diff: 3
      # Add 20% of durability to all armor slot items
      Repair:
        item: CRAFTING_TABLE
        repair:
          items: 
            - HELMET
            - CHESTPLATE
            - LEGGINGS
            - BOOTS
          factor: 0.2
```
