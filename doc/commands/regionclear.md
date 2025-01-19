# Regionclear Command

## Description

By default, at the end of a match, all entities are removed from [arena regions](../regions.md). If you want to keep some kind of entities
(like armor stands for instance), you can manage an exception list with this command.

## Usage

Command |  Definition
------------- | -------------
/pa \<arena\> regionclear \<entity\> (true/false) | toggle entity clearing of an arena


Example:
- `/pa ctf regionclear VILLAGER` - toggle the VILLAGER clearing of the arena "ctf"
- `/pa ctf !rc TNT true` - add a clearing exception for TNT in the arena "ctf" (ALLOW TNT to stay after clearing!)

## Details

Valid entitytypes will be told when you mistype, or you can find them 
[on this page](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html).

Giving no second argument will just toggle exception status and show you the result, valid arguments to activate/deactivate include:

on | 1 | true || off | 0 | false
