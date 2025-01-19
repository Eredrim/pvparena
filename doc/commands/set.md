# Set Command

## Description

Sets a specific config node of an arena configuration, or lists possible nodes.
It's an alternative to manually edit your config files.

> ⚠ If you mess up you might change the wrong node, erase parts of the config and render your config useless. 
So only use this if you know what you're doing.

## Usage

| Command                              | Definition                                         |
|--------------------------------------|----------------------------------------------------|
| /pa \<arena\> set \<pageNumber\>         | show a list of editable config nodes of your arena |
| /pa \<arena\> set \<configNode\> \<value\> | edit the value of an arena config node             |

Examples :
- `/pa test set 0` - get the first page of editable nodes of the arena "test" configuration
- `/pa test set minPlayers 5` - require the "test" arena to have 5 players before starting

## Details

The value of a configuration node depends on its type. These types are show when you use `/pa <arena> set <pageNumber>`.
However, it's quite simple to guess a node type.
There are severals categories of types and here is how to set them :
- **boolean:** Quite simple, just type `true` or `false`
- **numbers:** They can be integer or floating-point numbers. Just type them after the name of your config node
- **string:** The easiest, just type the word you want.
- **material:** The name of a bukkit material. You can directly type material names picked from 
[this list](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html) or type `hand` to get items you're
holding in your hand.
- **items:** A list of items is expected (for instance to give rewards). Type `inventory` to convert all your current
inventory to a list of items (metadata will be saved) or use `hand` get only get the item you're holding.
- **string list:** A list of strings is expected, just use semicolon (`;`) as a separator. Spaces are not accepted.

> 🚩 **NB:** 
> * If you want to define items or material nodes directly in your config file, please check 
> [this documentation](../items.md).
> * To apply modifications made by `/pa set` command, you **[must reload](reload.md)** your arena!