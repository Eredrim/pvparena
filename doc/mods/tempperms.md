# TempPerms

## About

This module activates temporary permission system during a match. You can set specific permissions to any player in the
arena or more specifically to a team or a class.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

There is no dedicated config setting for TempPerms, every permission you set are written in the perms part of your arena 
configuration file.

## Setup

Either administrate via commands (recommended) OR complete the `modules.tempperms` config block inside the arena config, like this:

```yaml
tempperms:
  - default:
    - everyone.has.this
    - -everyone.doesnt.have.this
  - blue:
    - only.blue.has.this
    - -blue.does.not.have.this
  - tank:
    - tank.has.this
    - -tank.does.not.have.this
```

<br>

> ⚙ **Technical precision:**  
> * For negative permissions, you can use either `-node` or `^node` syntax, it's the same thing. However, when the config
> is saved, every negative permissions are rewritten with `-node` syntax.
> * `default` block contains permissions applied to every player 

## Commands

- `/pa [arena] !tps` \- list perms
- `/pa [arena] !tps add [perm]` \- add permission (normal or negative)
- `/pa [arena] !tps rem [perm]` \- remove permission
- `/pa [arena] !tps [name]` \- list perms for a class/team
- `/pa [arena] !tps [name] add [perm]` \- add permission for a class/team
- `/pa [arena] !tps [name] rem [perm]` \- remove permission from a class/team 

## Troubleshooting

If you experience this not working, double check if you GAVE superior permissions or SUB permissions that you didn't 
take away (explicitly), to be sure, add/remove eventual .* nodes and all subnodes.  
If you want to add access to new commands with permissions, keep in mind they have to be added to whitelist in your
arena config file (`cmds.whitelist` node).

<br>

> ⚙️ **Technical precision:**  
> This mod is compatible with native Bukkit permission system and with any superperms compatible permissions plugin 
> (like GroupManager, LuckPerms, zPermissions, etc)
