## PVP Arena Goals

Goals are ways to win the game or lose the game! You can see active goals of your arena with `/pa <arena> info`.

To manage arena goals, use the [`/pa <arena> goal`](commands/goal.md) command.

| Goal                                          | Description                                   | Game mode | Join during match |
|-----------------------------------------------|-----------------------------------------------|-----------|-------------------|
| [BlockDestroy](goals/blockdestroy.md)         | Destroy block of the other team               | team      | allowed           |
| [CheckPoints](goals/checkpoints.md)           | Reach checkpoints in order to win             | free      | allowed           |
| [Domination](goals/domination.md)             | Dominate flag/beacon positions                | team      | allowed           |
| [Flags](goals/flags.md)                       | Capture flags and bring them at home          | team      | allowed           |
| [Food](goals/food.md)                         | Cook food and bring it home                   | team      | allowed           |
| [Infect](goals/infect.md)                     | Infect people to win or kill infected players | free      | none              |
| [Liberation](goals/liberation.md)             | Jail your enemies, free your allies!          | team      | none              |
| [PhysicalFlags](goals/physicalflags.md)       | Destroy enemy flag and place it at yours      | team      | allowed           |
| [PlayerDeathMatch](goals/playerdeathmatch.md) | Score points by killing players               | free      | allowed           |
| [PlayerKillReward](goals/playerkillreward.md) | Player get better gears when killing          | free      | allowed           |
| [PlayerLives](goals/playerlives.md)           | Last alive players win                        | free      | none              |
| [Sabotage](goals/sabotage.md)                 | Ignite TNT of the opposing team               | team      | allowed           |
| [Tank](goals/tank.md)                         | all vs one                                    | free      | none              |
| [TeamDeathConfirm](goals/teamdeathconfirm.md) | Confirmed Team kills win                      | team      | allowed           |
| [TeamDeathMatch](goals/teamdeathmatch.md)     | Team kills win                                | team      | allowed           |
| [TeamLives](goals/teamlives.md)               | Last alive team wins                          | team      | none              |
| [TeamPlayerLives](goals/playerlives.md)       | Last team with alive players win              | team      | none              |


To enable join during match, set `join.allowDuringMatch` to **true** in your arena config file.