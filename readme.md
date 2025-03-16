![PVP-Arena](doc/images/logo.png)

<p align="center">
   <b>
       IF YOU'RE UPGRADING FROM 1.15.x VERSION OR BELOW, PLEASE READ 
       <a href="doc/update-version.md">UPGRADE DOCUMENTATION</a>
   </b>
</p>

***
[What is PVP Arena?](#What-is-PVP-Arena?) | [Dependencies](#Dependencies) | [Downloads](#Downloads) | 
[Installation](#Installation) | [Documentation](#Documentation) | [Support](#Support) | 
[Telemetry](#Telemetry) | [Credits](#Credits)
***
<br>

## What is PVP Arena?

PVP Arena is a plugin for Spigot based servers which enables creation of customizable fight and mini-games arenas.
Define your own teams, classes, lobbies, spawn points, messages, gear colors, rewards and even game modes!
These game modes (included in the plugin) can be combined with tens of modules to enhance your gameplay!
In addition, all arenas can be protected with an embedded protection system based on regions.

Anyway, here's a quick (non-exhaustive) list of plugin features:
- Multiple arenas
- Arena regions and protections
- Player-state saving
- Customizable classes
- Customizable spawn points, lobbies and spectator zones
- In-game configuration access and simple config files
- Arena disabling
- Scoreboards
- Spawn protection
- Flag coloring
- Inventory drops
- Announcements
- Time limited match
- Battlefield regeneration

So take time to read the docs, it's full of useful information 😉
***

## Dependencies

- Spigot 1.16.5+ (or derivatives like Paper, Purpur, Pufferfish, etc.)
- Java 11+

***

## Downloads

PVP Arena release version can be downloaded on following pages:
- [PVP Arena - SpigotMC](https://www.spigotmc.org/resources/pvp-arena.16584/)
- [Github releases page](https://github.com/Eredrim/pvparena/releases)

Development builds (experimental) can be downloaded on our discord:
- [Discord server](https://discord.gg/a8NhSsXKVQ) (channel #dev-builds)

***

## Installation

Place PVP Arena `.jar` file in the plugin repository of your server and restart. 

***

## Documentation

- [_What's new in PVPArena 2.0?_](doc/2.0-whats-new.md)
- [_Updating from 1.15.x_](doc/update-version.md)
<br>

- [Getting started](doc/getting-started.md)
- [Playing an arena](doc/player-guide.md)
- [Commands](doc/commands.md)
- [Permissions](doc/permissions.md)
- [Regions](doc/regions.md)
- [Goals](doc/goals.md)
- [Modules](doc/modules.md)
- [Items in config files](doc/items.md)
- [Languages](doc/languages.md)
- [Configuration](doc/configuration.md)
- [Placeholders](doc/placeholders.md)
- [Update checker](doc/update-checker.md)
- [FAQ](doc/faq.md)

***

## Support

You can contact us to ask your questions or just discuss, you can go here:
- <img src="https://www.spigotmc.org/favicon.ico" height="16"></img> [Spigot Forums](https://www.spigotmc.org/threads/pvp-arena.113406/)
- <img src="https://discord.com/assets/07dca80a102d4149e9736d4b162cff6f.ico" height="16"></img> [Discord server](https://discord.gg/a8NhSsXKVQ)

To report issues and make suggestion, please use our [Github issues page](https://github.com/Eredrim/pvparena/issues).

***

## Telemetry

PVPArena uses bStats to get statistics about basic information like plugin version, java version,
kind of used Minecraft server, etc. You can disable it in the dedicated config file `/plugins/bStats/config.yml`

***

## Credits

- SlipCor, the wonderful guy who created this plugin
- Deminetix for the very root, the Fight plugin
- Bradley Hilton for the fork until version v0.0.3
- Carbon131 for adding features until version v0.0.5
- Drehverschluss for great support during the v0.6+v0.7 rewrite
- Oruss7 for his valued help in 2.0 version and for documentation

***
