# TempPerms

## About

This module activates temporary permission system during a match. You can set specific permissions to any player in the
arena or more specifically to a team or a class.

## Config settings

There is no dedicated config setting for TempPerms, every permission you set are written in the perms part of your arena 
configuration file.

## Setup

Either administrate via commands (recommended) OR complete the `perms` config block inside the arena config, like this:

```yaml
perms:
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
> * For negative permissions, you can use either `-node` or `^node` syntax, it's the same thing. However when the config
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

## Dependencies

Any superperms compatible permissions plugin (like GroupManager, LuckyPerms, etc)
