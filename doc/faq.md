# Frequently Asked Questions

## How I can create a spleef arena?

When you create a minigame with PVPArena, you have to ask yourself "How will work score calculation?". In spleef case,
when a player dies, he looses the match.

So you should use the [playerLives](goals/playerlives.md) goal (enabled by default). Build you arena and configure it
following [this guide](getting-started.md).

You can restore you battlefield (snow ground) using [BlockRestore](mods/blockrestore.md) or 
[Worldedit](mods/worldedit.md) module. Please check [modules documentation](modules.md) to learn more.

<br>

## Can my players use their own inventories?

Yes it is possible, just pass `playerclasses` parameter to `true`. You can do this directly by editing your config file
or use the [`/pa <arena> set`](commands/set.md) command.

Then you can propose player inventories as a class: corresponding class name is `custom`. If you want this class by
default, just set `autoclass` parameter to `custom`.

Finally, if you want to return player inventory as it was before the beginning of the match, pass `customReturnsGear` to
true.

<br>

## How can I use arena commands from a command block?

Maybe you will wish to use buttons, pressure plates and command blocks to allowing players to choose their class, get
ready or leave the arena.

Most of PvPArena commands must be typed by players (in order to keep context). So if you want to use plugins commands in
a command block, you have to use PVPArena super-user commands instead of player ones. They look like 
`playerjoin` or `playerleave` and they are listed in [command documentation page](commands.md#arena-super-user-commands).

> ⚙ **Technical precision:**  
> Since Minecraft 1.13, spigot based servers does no longer support command selectors (like `@p`). If you want to use it
> you will have to use a plugin like [CommandHook](https://www.spigotmc.org/resources/commandhook.61415/).

<br>

## How to create a join sign for an arena?

Create a simple sign with the following pattern:
```
[arena]
yourArenaName
teamName

```

The first line is one of the sign headers configurable in your global 
[config.yml](./configuration.md#global-config-file) file, by default: `[PVP Arena]` and `[arena]`.  
You can keep the third line empty to join a random team.  
The fourth line can still be empty or filled with a custom message.

> 🚩 **Tip:** Color codes are automatically handled, so feel free to color your signs!

<br>

## How to regen my battlefield after a game?

Currently, there are two ways to regen battlefield after a match. You can use either 
[BlockRestore](mods/blockrestore.md) module or [WorldEdit](mods/worldedit.md) module.

Here is a quick list of precisions to support your choice:
* BlockRestore:
    * Reset only blocks, chests, 
    * Asynchronous
    * Perfect for arena where few blocks are destroyed (like spleef)
* WorldEdit:
    * Needs WorldEdit plugin (obviously)
    * Regenerates everything
    * Can regen large areas but is synchronous

Check dedicated documentation pages to get more information.

<br>

## Is there a way to automatically put a player into spectator mode on death instead of them having to leave the match and then rejoin as a spectator?


Just set `tp.death` to `spectator` in your arena config file (or with [`/pa <arena> set`](commands/set.md) command).

<br>

## Is it possible to automatically affect a class to all players or to a specific team?

Yes it is. In your arena config, you can set the `autoclass` the setting according to your needs:
* Use `None` if you don't want to use the auto-class mechanism. (default option)
* Write a simple class name, to affect the class to everyone.  
  Ex: `autoclass: pyro`
* Use the following pattern to affect a class to each team:  
  `autoclass: teamName1:classNameA;teamName2:classNameB`

NB: For the 3rd option, you have to specify a class for each team. There is no default choice.

<br>

## How can I use placeholders in signs/holograms/scoreboard?

PVPArena provides multiple [placeholders](placeholders.md), and they are all based on PlaceholderAPI. So to use them, 
just find plugins that are compatible with it.

Here is a quick (and non-exhaustive) list of compatible plugins:
* **Holograms:** [DecentHolograms](https://www.spigotmc.org/resources/decentholograms-1-8-1-20-4-papi-support-no-dependencies.96927/), 
[FancyHolograms (1.19.4+)](https://www.spigotmc.org/resources/fancy-holograms-text-items-blocks.108694/)
* **Signs:** [SignLink](https://www.spigotmc.org/resources/signlink.39593/), [YColorfulItems (Paper based / 1.20+)](https://modrinth.com/plugin/ycolorfulitems)
* **Scoreboards:** [SimpleScore](https://www.spigotmc.org/resources/simplescore-animated-scoreboard.23243/), 
[RealScoreboard](https://www.spigotmc.org/resources/realscoreboard-1-13-to-1-20-4.22928/), 
[AnimatedScoreboard](https://www.spigotmc.org/resources/animatedscoreboard.20848/)

<br>

## Still have questions?

Don't hesitate to [get in touch](../readme.md#support) with us 😉
