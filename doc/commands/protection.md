# Protection command

## Description

Manage protections of a certain [arena region](../regions.md). **When a protection is enabled, player actions
are prevented.**

## Usage

Command |  Definition
------------- | -------------
/pa \<arena\> protection \<region\> \<protection\> (true/false)    | toggle a protection of an arena region

Example: `/pa main protection battlefield DROP` - toggle the DROP protection of the arena "main"s region called "battlefield"

## Details

Here is a quick list supported flags, they include:

- BREAK - prevent player block breaking
- FIRE - prevent fire spreading/burning
- MOBS - prevent mob spawning
- NATURE - prevent water flow/growth
- PAINTING - prevent painting/itemframe breakage
- PISTON - prevent piston usage
- PLACE - prevent player block placing
- TNT - prevent tnt interaction
- TNTBREAK - prevent tnt block damage (explosion still hurts)
- DROP - prevent player item dropping
- INVENTORY - prevent inventory interaction
- PICKUP - prevent player item pickup
- TELEPORT - prevent player teleportation

<br>

> **🚩 Tip:**  
> Check your protections with [`/pa <arenaname> !rs <regionname>`](regions.md)
