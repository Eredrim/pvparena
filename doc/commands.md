# Command list

Click on a command to get its syntax, usage examples and more information about its working. Commands `/pvparena` and
 `/pa` are the same.

> 🚩 **Note:**  
> You always have to precise your arena name in commands (like `/pa <myArena> enable`) except for these cases:
> - For global admin commands
> - After you joined an arena
> - When you're editing an arena (see [`/pa edit`](commands/edit.md) command)
> - When you have only one arena

<br>

## Arena User Commands

> ℹ Permission: pvparena.user (defaults to true)

| Command                                  | Shorthand | Definition                                                          |
|------------------------------------------|-----------|---------------------------------------------------------------------|
| [/pa arenaclass](commands/arenaclass.md) | /pa -ac   | Change your class, if allowed                                       |
| /pa chat                                 | /pa -c    | Set arena chat mode                                                 |
| /pa info                                 | /pa -i    | Display the active modules of an arena and its settings             |
| /pa help                                 | /pa -h    | Basic help. Splits into subsections.                                |
| [/pa join](commands/join.md)             | /pa -j    | Join an arena (specifying a team or not)                            |
| /pa leave                                | /pa -l    | Leave an arena                                                      |
| /pa list                                 | none      | List available arenas (red: disabled, yellow: edit, green: running) |
| [/pa ready](commands/ready.md)           | /pa -r    | Ready you up or list who is ready                                   |
| /pa spectate                             | /pa -s    | Spectate an arena                                                   |
| [/pa stats](commands/stats.md)           | /pa -s    | Show [arena/global] statistics                                      |
| /pa version                              | /pa -v    | Show detailed version information                                   |

## Global Admin Commands

> ℹ Permission: pvparena.admin

| Command                            | Shorthand | Definition           |
|------------------------------------|-----------|----------------------|
| [/pa debug](commands/debug.md)     | /pa !d    | Debug nodes          |
| [/pa modules](commands/modules.md) | /pa !mi   | Manage modules       |
| [/pa reload](commands/reload.md)   | /pa !r    | Reload arena configs |

## Arena Administration Commands

> ℹ Permission: pvparena.admin OR both ownership of an arena and pvparena.create

All of these commands have to be used prefixing the arena name (e.g. `/pa myArena check`) or while edit mode is enabled.

| Command                                    | Shorthand | Definition                              |
|--------------------------------------------|-----------|-----------------------------------------|
| [/pa blacklist](commands/blacklist.md)     | /pa !bl   | Manage arena blacklists                 |
| [/pa check](commands/check.md)             | /pa !ch   | Check an arena configuration            |
| [/pa class](commands/class.md)             | /pa !cl   | Manage arena classes                    |
| [/pa classchest](commands/classchest.md)   | /pa !cc   | Manage arena class chests               |
| [/pa create](commands/create.md)           | /pa !c    | Create an arena                         |
| [/pa disable](commands/disable.md)         | /pa !dis  | Disable an arena.                       |
| [/pa edit](commands/edit.md)               | /pa !e    | Toggle editing of an arena              |
| [/pa enable](commands/enable.md)           | /pa !en   | Enable an arena.                        |
| [/pa forcewin](commands/forcewin.md)       | /pa !fw   | Force a player/team to win.             |
| [/pa goal](commands/goal.md)               | /pa !g    | Manage arena goals                      |
| [/pa protection](commands/protection.md)   | /pa !p    | Manage arena protections                |
| [/pa region](commands/region.md)           | /pa !rg   | Manage arena regions                    |
| [/pa regionclear](commands/regionclear.md) | /pa !rc   | Manage arena region clearing exceptions |
| [/pa regionflags](commands/regionflags.md) | /pa !rf   | Manage arena flags                      |
| [/pa regiontype](commands/regiontype.md)   | /pa !rt   | Change a region type                    |
| [/pa regions](commands/regions.md)         | /pa !rs   | Debug regions                           |
| [/pa reload](commands/reload.md)           | /pa !rl   | Reload arena configs                    |
| [/pa remove](commands/remove.md)           | /pa !rm   | Remove an arena.                        |
| [/pa set](commands/set.md)                 | /pa !s    | Set an arena config setting             |
| [/pa setowner](commands/setowner.md)       | /pa !so   | Sets the owner of an arena              |
| [/pa spawn](commands/spawn.md)             | /pa !sp   | Manage arena spawns                     |
| [/pa start](commands/start.md)             | /pa !go   | Force starts an arena.                  |
| [/pa stop](commands/stop.md)               | /pa !st   | Force stops an arena.                   |
| [/pa teams](commands/teams.md)             | /pa !ts   | Manage arena teams                      |
| [/pa teleport](commands/teleport.md)       | /pa !tp   | Teleport you to an arena spawnpoint     |
| [/pa togglemod](commands/togglemod.md)     | /pa !tm   | Enable or disable a module for an arena |
| [/pa whitelist](commands/whitelist.md)     | /pa !wl   | Manage arena whitelists                 |


## Arena Super-User Commands

> ℹ Permission: pvparena.admin

Use these commands to force users doing things. Works inside command blocks!  
All of these commands have to be used prefixing the arena name.

| Command                                              | Shorthand | Definition                                           |
|------------------------------------------------------|-----------|------------------------------------------------------|
| [/pa playerjoin](commands/playerjoin.md)             | /pa !pj   | Make a player join an arena (specifying team or not) |
| [/pa playerspectate](commands/playerspectate.md)     | /pa !ps   | Make a player spectate an arena                      |
| [/pa playerleave](commands/playerleave.md)           | /pa !pl   | Make a player leave the arena                        |
| [/pa playerarenaclass](commands/playerarenaclass.md) | /pa !pac  | Change a player arena class                          |
| [/pa playerready](commands/playerready.md)           | /pa !pr   | Make a player ready                                  |

> 🚩 **Tip:**  
> All of super-user commands can be used with [Minecraft target selectors](https://minecraft.wiki/w/Target_selectors).