# EventActions

## About

This mod is quite complex. It adds many ways to react to game events. It's really useful to create new interactions in
arenas.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Configuration details

> ℹ️ **Notice:**
> All settings are only configurable from your arena config file

EventActions module works with a combination of three things: game events, actions and placeholders that you
can use in your actions (optionals).

#### Events

Events are triggers you can catch during the arena lifecycle. They are only limited to the arena session, so there's no
risk you catch events (like deaths) from outside the arena.  
Here is the full event list: 

| Event name  | Triggered moment                           |
|-------------|--------------------------------------------|
| classchange | when a player switch its arena class       |
| death       | when a player dies                         |
| end         | when arena ends                            |
| exit        | when a player leaves the arena             |
| join        | when a player joins the arena              |
| kill        | when a player kills another player         |
| leave       | when a player runs `/pa leave` command     |
| lose        | when a player (or its team) loses the game |
| start       | when arena starts                          |
| win         | when a player (or its team) wins the game  |

#### Actions

Actions are things you can do when an event happens. Available actions are:

| Action | Arguments              | Description                                                                 | Usable with start/end events |
|--------|------------------------|-----------------------------------------------------------------------------|------------------------------|
| cmd    | a command              | run a command from the console                                              | ✔️                           |
| pcmd   | a command              | make the player that triggered the event run a command                      | ❌                            |
| brc    | a message              | broadcast a message in you whole server                                     | ✔️                           |
| abrc   | a message              | broadcast a message in your arena                                           | ✔️                           |
| msg    | a message              | the player that triggered the event                                         | ❌                            |
| power  | coordinates            | temporarily put a redstone block somewhere to power a circuit               | ✔️                           |
| clear  | a region name or "all" | clear all entities of an arena region (or for all regions if "all" is used) | ✔️                           |

> ℹ️ **Notice**:  
> For power action, coordinates should follow this syntax: `world,x,y,z`

#### Placeholders

The following placeholders can be used to get dynamic output in your commands/messages:

| Placeholder  | Description                                                      | Compatible events            |
|--------------|------------------------------------------------------------------|------------------------------|
| %arena%      | arena (name) where the event is happening                        | all                          |
| %player%     | name of the player that triggered in the event                   | all except "start" and "end" |
| %team%       | team name of the player that triggered in the event              | all except "start" and "end" |
| %color%      | team color of this player (to colorize a message)                | all except "start" and "end" |
| %allplayers% | all the arena players, the action will be applied to all of them | all                          |
| %players%    | a list of names of arena players, sorted and colored by teams    | all                          |
| %class%      | the class a player has chosen                                    | only "classchange"           |


## How to set up?

Open your arena config file and create a new config block named `eventactions` (in lowercase) under `modules` key.
Then, create a new key for each event you want to listen. As a value, create a list with action entries (explanations 
below).

Here is an example template. You can copy it directly in your arena config (make sure you don't duplicate `modules` key).
```yaml
modules:
  eventactions:
    join:
    - cmd<=>ungod %player%
    - brc<=>%player% is joining %arena%!
    - msg<=>Welcome to %arena%!
    classchange:
    - pcmd<=>me is now equipped with class %class%! # /me is a command of EssentialsX
    start:
    - power<=>main_world,150,82,-36
    - "abrc<=>Let the fight begin! Here are our brave fighters: %players%" #quotes are used to escape colon in text
    end:
    - clear<=>my_battle_region
    - cmd<=>eco give %allplayers% 1
```

As you can see, each event can have multiple contents. They have to be one line, action and value, separated by `<=>`.
In value of action, you are free to use EventActions placeholders or not.

As a reminder, note that some placeholders like `%player%` only makes sense in events that are about a player.
Joining, killing and leaving, but not starting. Check the table above for more details ;)

> ℹ️ **Notice**:  
> Mind to reload your arena config with `/pa <arena> reload` after each change to apply it.
