# PVP Arena Modules

## About

PvPArena modules are ways to enhance your arenas. They could modify a lot of things like configuration, fights, classes 
or spectating...

To install arena mods, please check [documentation below](#installing-modules). The `/pa modules` command allows for 
managing your arena mods (download, install, remove, etc.), please check [documentation](commands/modules.md) to get 
more information.

## List of PVP Arena Mods

Hook into many different aspects of the game!

| Mod                                            | Description                                                                      | Type     | Status |
|------------------------------------------------|----------------------------------------------------------------------------------|----------|--------|
| [AfterMatch](mods/aftermatch.md)               | could also be called "Sudden Death"                                              |          | ✔️️    |
| [Announcements](mods/announcements.md)         | announce events happening                                                        |          | ✔️️    |
| [ArenaMaps](mods/arenamaps.md)                 | a live refreshed map to never lose yourself ever again!                          |          | ✔️️    |
| [BanKick](mods/bankick.md)                     | ban or kick a player of an arena                                                 |          | ✔️️    |
| [BattlefieldJoin](mods/battlefieldjoin.md)     | join directly your battlefield without using the lounge                          | JOIN     | ✔️️    |
| [BetterClasses](mods/betterclasses.md)         | add potion effects and more to specific classes                                  |          | ✔️️    |
| [BetterGears](mods/bettergears.md)             | automatically colorize classes armors to suit team color                         |          | ✔️️    |
| [BetterFight](mods/betterfight.md)             | pack of fun features : one-shot items, kill sounds and explosions on death       |          | ✔️️    |
| [BetterKillstreaks](mods/betterkillstreaks.md) | reward kill streaks with items or potion effects                                 |          | ✔️️    |
| [BlockDissolve](mods/blockdissolve.md)         | dissolve blocks under fighting players                                           |          | ✔️️    |
| [BlockRestore](mods/blockrestore.md)           | restore the battlefield                                                          |          | ✔️️    |
| [ChestFiller](mods/chestfiller.md)             | fill battlefield containers with random and customizable content!                |          | ✔️️    |
| [Duel](mods/duel.md)                           | duel someone!                                                                    |          | ❌️     |
| [EventActions](mods/eventactions.md)           | trigger actions at different moments of the arena lifecycle                      |          | ✔️️    |
| [FlySpectate](mods/flyspectate.md)             | have players spectating a fight in fly mode                                      | SPECTATE | ✔️️    |
| [ItemSpawners](mods/itemspawners.md)           | spawn (random) items on different points                                         |          | ✔️️    |
| [LateLounge](mods/latelounge.md)               | keep playing until enough players joined the arena                               |          | ✔️️    |
| [PlayerFinder](mods/playerfinder.md)           | allow players to find others with a compass                                      |          | ✔️️    |
| [Points](mods/points.md)                       | allow to restrict certain classes to require players to fight for better classes |          | ❌️     |
| [PowerUps](mods/powerups.md)                   | spawn items giving special powers                                                |          | ✔️️    |
| [Projectiles](mods/projectiles.md)             | add knockback to throwable items (snowballs, eggs, etc)                          |          | ✔️️    |
| [QuickLounge](mods/quicklounge.md)             | join a lounge which starts the game automatically                                | JOIN     | ✔️️    |
| [RealSpectate](mods/realspectate.md)           | spectate the game, CounterStrike style!                                          | SPECTATE | ✔️️    |
| [RespawnRelay](mods/respawnrelay.md)           | add a relay for respawning players                                               |          | ✔️️    |
| [Skins](mods/skins.md)                         | add custom skins to teams/classes                                                |          | ❌️     |
| [SpawnCollections](mods/spawncollections.md)   | save different spawn configurations of the battlefield and switch between them   |          | ✔️️    |
| [Spectate](mods/spectate.md)                   | use the Minecraft SPECTATOR gamemode to allow flying and POV spectating          | SPECTATE | ✔️️    |
| StandardLounge                                 | Default lounge module                                                            | JOIN     | ✔️️    |
| StandardSpectate                               | Default spectate module (survival mode)                                          | SPECTATE | ✔️️    |
| [Squads](mods/squads.md)                       | add squads to the game, create group of players respawning together              |          | ✔️️    |
| [StartFreeze](mods/startfreeze.md)             | freeze players at start                                                          |          | ✔️️    |
| [TeamSizeRestrict](mods/teamsizerestrict.md)   | a small mod to restrict the size of specific teams                               |          | ✔️️    |
| [TempPerms](mods/tempperms.md)                 | add temporary perms                                                              |          | ✔️️    |
| [Titles](mods/titles.md)                       | send messages to players as the "title" command would do                         |          | ✔️️    |
| [Turrets](mods/turrets.md)                     | add turrets where players fire projectiles                                       |          | ❌️     |
| [Vault](mods/vault.md)                         | add economy, money rewards and bet features                                      |          | ✔️️    |
| [Walls](mods/walls.md)                         | define wall regions to simulate "The Walls"                                      |          | ❌️     |
| [WarmupJoin](mods/warmupjoin.md)               | ???                                                                              | JOIN     | ❌️     |
| [WorldEdit](mods/worldedit.md)                 | backup/restore regions                                                           |          | ✔️️    |

**Key :** ✔️️ Optimized (even rewritten) and fully tested for 2.0 | ❌ Temporarily unavailable

> ℹ **NB:** StandardLounge, StandardSpectate, QuickLounge, BattlefieldJoin and WarmupJoin are bundled in the plugin. Consequently, they're missing from the modules pack.

### Why are there different statuses?

PVP Arena exists since 2011 and Minecraft servers evolution make modules follow-up complicated. The objective of 2.0 
version was to make a great check-up of all of them, fix their issues and make them more performant and easy-to-use.

Unfortunately, rewriting modules is a long and tedious task, and maintaining two versions of PVPArena and two versions 
of each module became too complicated. That's why I decided to focus on most-used modules and on the release of the 2.0
version of the plugin.

Anyway, the unavailable modules will come back soon, just the time to fix (or rewrite them).

### What happened to BattlefieldManager and Items?
The modules have been renamed to SpawnCollections and ItemSpawners respectively.

### What happened to other missing modules?

As you maybe saw it, several modules has been removed. They are ArenaBoards, AutoSneak, AutoVote, BattlefieldGuard, 
Factions, FixInventoryLoss, MatchResultsStats, RedstoneTriggers, SinglePlayerSupport, SpecialJoin and WorldGuard.

These modules either used discontinued features (like arena rounds) or their features have been integrated in core of
PVPArena plugin.

<br>

## Installing modules

### Download the module pack

> ℹ This has to be done only once
 
Use the [`/pa modules download`](commands/modules.md) command to download the release version of modules. If you want to
install a dev build version, download the zip archive directly on our [discord](https://discord.gg/a8NhSsXKVQ) 
and deflate it in the `/files` directory of pvparena.

After this step, if you type [`/pa modules list`](commands/modules.md), you will show the list of all installable 
modules.

### Installing a module

> ℹ This has to be done for each module you want to install

Modules aren't loaded by default, a quick installation is required. 
Type [`/pa modules install <moduleName>`](commands/modules.md) to install one of them.


### Enable a module for an arena

> ℹ This has to be done for each arena

Last step: your module is installed, and you want to use it in some of your arenas. 
Type [`/pa <arena> !tm <moduleName>`](commands/togglemod.md) to enable it in your arena.

<br>

## Updating modules

### With latest release version

Just run [`/pa modules upgrade`](commands/modules.md). In some cases, a server restart may be needed.

### With a snapshot/dev version

Browse the `plugins/pvparena` directory. Delete the `files` folder and unzip the new modules archive.  
Then type [`/pa modules update`](commands/modules.md). In some cases, a server restart may be needed.