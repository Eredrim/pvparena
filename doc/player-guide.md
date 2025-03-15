# How to use PVP Arena as a player?

## Playing an arena

### Joining an arena

Before anything, you can check all available arenas by running `/pa list`.  
Then, to join an arena, just run [`/pa join <arena>`](commands/join.md) or easier, just `/pa <arena>`. If arena uses a 
team gamemode, you can choose your team by running [`/pa <arena> join <team>`](commands/join.md) instead of using 
previous command. Otherwise, your team will be randomly chosen.

### Once you're in the lounge

Great! You successfully joined the arena and now, you're in the lounge. This place is made to wait for your teammates
and your opponents. During this time, you can choose or change your arena class by running `/pa -ac <class>` or by
clicking on a class sign if the arena contains ones.

Finally, when you're ready, run [`/pa ready`](commands/ready.md) or click on the start block (if arena contains one) to 
mark you as ready. This step makes possible to check if enough players are ready to start the match and, if so, to 
start it.

### During the fight

After the countdown, you'll be teleported to a spawn point. In team goals, each team has its own spawn point. In free
goals, they are random and shared between all fighters.

Each arena has its own goal and rules, depending on its configuration. So, I let your admin explain to you how to play
it, and more important, how to win 😉

In most cases, you or your team have life points. When you lose all of them, you'll be unable to respawn and you'll be
teleported to the spectator zone.

Finally, know you can leave the arena at any time by running `/pa leave`.

## Spectating an arena

Just run `/pa spectate <arena>` to spectate an arena. You will be teleported to the spectator zone of the arena and
if a special spectator module is configured, it will be automatically enabled.

Type `/pa leave` to leave the spectator zone.