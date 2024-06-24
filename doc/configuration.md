# Configuration file

PVP Arena provides two kind of configuration files:
* Arena configuration files, to edit game settings for each arena. They are stored in `/arenas` directory.
* A global config file (like for any plugin), stored as `config.yml` at the root of plugin directory.

## Arena configuration files

The following block explains what are each parameter of arena config file. 
Most of those parameters can be changed via in-game command ([/pa set](commands/set.md)). Other ones can be changed with
dedicated commands (like regions, spawns or arena classes).

If you edit an arena configuration file, you can load changes with [`/pa <arena> reload`](commands/reload.md) command.

> ℹ This is an example configuration file. 

```yaml
configversion: 2.0.0 #version of the config file
uuid: b9422351-6383-4a92-8da2-4e7c7634e5fd #ID of the arena for stats (auto-generated), must not be changed and still be UNIQUE
cmds:
  defaultjoin: true #Join the arena by just typing /pa <arena>
  whitelist: #List of non-PVPArena allowed commands in the arena
    - ungod
general:
  classspawn: false #Create specific class spawns. E.g. blueTankFight
  classSwitchAfterRespawn: false #If IngameClassSwitch is enabled, class switch only happens after next respawn
  customReturnsGear: false #If player uses its own inventory, reload it after the match
  enabled: true #Make arena accessible or not
  gamemode: SURVIVAL #Arena game mode: SURVIVAL/ADVENTURE/CREATIVE
  leavedeath: false #Player is killed if they try to escape battlefield
  lang: none
  owner: server #Player that owns of the arena, use "server" to limit to ops
  regionclearexceptions: [] #List of regions where entities are not cleared after the match
  quickspawn: true #Spawn all players at the same time (tick). If false, spawn player one by one.
  showRemainingLives: true #Brodcast remaining lives in chat after each kill
  smartspawn: true #Spread players on spawn points in a balanced way, i.e. as far apart from each other as possible
  timer:
    end: 0 #Time limit for the arena (in seconds), set to 0 to disable it
    winner: none #Winner team when time limit is reached
  addLivesPerPlayer: false #Multiply the life number by the number of players in the team (or in the arena for FFA)
  prefix: MyArena #Name of the arena displayed in chat messages
  goal: PlayerLives #Current goal of the arena, see 'Goals' part of documentation
goal:
  #Goal specific configuration, see 'Goals' part of documentation. Editable with "/pa goal" command.
  playerlives:
    plives: 2
uses:
  classSignsDisplay: false #Display player names on class signs
  deathMessages: true #Show messages on death (from language file)
  deathEvents: false #Send a Bukkit death event when a player dies. May be useful to get custom death messages from other plugins.
  evenTeams: false #If true, requires the same number of players in each team
  ingameClassSwitch: false #Allow players to switch class during a game
  invisibilityfix: false #Force player to be visible
  evilinvisibilityfix: false #Force player to be visible if any other setting don't work
  overlapCheck: true #Check if the arena region collides with a running arena
  playerclasses: false #Make possible for players to use their own inventory (see FAQ)
  scoreboard: false #Display built-in scoreboard
  showNametags: true #Show nametags of all players. If false, nametags will be hidden of other teams
  suicidepunish: false #Increase other players score if someone commits suicide
  teleportonkill: false #Respawn KILLER after a kill
  woolHead: false #Use a colored wool block as helmet
perms:
  explicitArenaNeeded: false #Players need permission pvparena.join.arenaName to play arena
  explicitClassNeeded: false #Players need permission pvparena.class.className to get a class
  fly: false #Enable/disable fly for players
  loungeinteract: false #If true, players can interact with other players and blocks within lounge. Useless if you created a LOUNGE region.
  joinWithScoreboard: true #Allow to join arena with scoreboard with AutoVote Mod
  teamkill: true #Allow players to kill their own team members (friendly fire)
  specTalk: true #Allow spectators to use chat
  spectatorinteract: false #Allow spectators to interact with other players.
damage:
  armor: true #Allow armor damage - false = unbreakable
  bloodParticles: false #Show blood particles on each hit. Hardcore !!!
  fromOutsiders: false #Allow external players to damage arena ones
  spawncamp: 1 #If nocamp region flag is enabled, damage amount set to player
  weapons: true #Allow weapon damage - false = unbreakable
msg:
  #Arena specific messages that you can configure as you wish
  lounge: Welcome to the arena lounge! Hit a class sign and then the iron block to
    flag yourself as ready!
  playerjoined: '%1% joined the Arena!'
  playerjoinedteam: '%1% joined team %2%!'
  starting: Arena is starting! Type &e/pa %1% to join!
  youjoined: You have joined the FreeForAll Arena!
  youjoinedteam: You have joined team %1%!
chat:
  colorNick: true #Use team color in chat
  defaultTeam: false #If true, chat is limited to team members by default. By using '/pa chat' command, players can talk in general chat or arena chat (depending of "onlyPrivate" value).
  enabled: true #Allow chat usage, if false all arena players are muted
  onlyPrivate: true #Limit chat to the arena (i.e. other players on the server can't read arena chat)
  toGlobal: none #Begin word to talk global server chat if onlyPrivate is active. E.g. @all
player:
  autoIgniteTNT: false #Ignite TNT automatically on place
  clearInventory: NONE #Clear player inventory on join. Set a specific game mode or ALL for any kind.
  collision: true #Allow player collision with ENTITIES (players, arrows, tridents, armor stands, etc)
  dropsEXP: false #Make players drop XP on death
  dropsInventory: false #Make players drop their inventory on death
  exhaustion: 0.0 #Set player exhaustion
  feedForKill: 0 #Food level to add to players after a kill
  foodLevel: 20 #Default food level
  health: -1 #Set initial player health. Use -1 for default server value. Must be lower or equal than maxHealth.
  healforkill: false #Heal players who kill another one
  hunger: true #Enable hunger, if false feed level never decreases
  itemsonkill: [] #List of items given to players who kill another one
  mayChangeArmor: true #Allow players to edit their armor slots in game
  maxhealth: -1 #Set maximum health for player. 1 heart = 2 pts. Use -1 for default server value. Must be higher or equal than health.
  refillCustomInventory: true #Refill inventory of players using "custom" class (i.e. their own inventory) after death
  refillInventory: true #Reset class inventory of players after death
  refillforkill: false #Reset class inventory of a player after a kill
  removearrows: false #Remove arrows on body after death (works only for Spigot 1.16.2+)
  saturation: 20 #Set hunger saturation level
  quickloot: false #If true, makes possible to get whole chest content just clicking on it
  dayTime: -1 #Time of the day seen by players (world time is not changed). Set to -1 to use world time or set a number of ticks. This value is fixed, day/night cycle will not progress.
items:
  excludeFromDrops: [] #List of items that are not dropped on kill
  onlyDrops: [] #List of the only dropped items on kill (opposite system of previous setting)
  keepOnRespawn: [] #List of items of death inventory kept on respawn (if keepAllOnRespawn is disabled)
  keepAllOnRespawn: false #Keep death inventory on respawn
  minPlayersForReward: 2 #Minimum players to give rewards at the end of the game
  random: true #If true, only one (random) reward in the list is given to winners. If false, winners get the whole reward list.
  rewards: [] #List of rewards given to winners
  takeOutOfGame: [] #List of items kept from player inventory out the game
time:
  startCountDown: 10 #Start countdown in seconds
  endCountDown: 5 #End countdown in seconds
  regionTimer: 10 #Time in ticks for region tasks. Don't change this.
  teleportProtect: 3 #Number of seconds of invulnerability after teleport
  resetDelay: -1 #Wait time (in ticks) to reset players when they exit arena
  warmupCountDown: 0 #Warmup time (in seconds)
  pvp: 0 #Time before enabling PVP (in seconds)
ready:
  autoClass: none #Name of class set automatically when player joins an arena. Set "none" to disable. Set to "custom" if you use "playerClasses".
  block: IRON_BLOCK #Block that players can hit to be ready. Has the same effect than typing /pa ready.
  checkEachPlayer: false #Check if each player is ready before start
  checkEachTeam: true #Check if each team is ready before start
  enforceCountdown: false #If everyone is ready, force start of game before the end of countdown
  minPlayers: 2 #Minimum number of players to play the arena
  maxPlayers: 8 #Maximum number of players to play the arena
  maxTeam: 0 #Maximim number of players in each team
  neededRatio: 0.5 #Ratio of ready player needed to start countdown
join:
  range: 0 #Max distance from battleground to join arena. Set 0 to disable.
  forceregionjoin: false #Limit arena join from "join" type region. Joining from other places will be disabled.
  allowRejoin: false #Make possible for disconnected players to re-join the game if they reconnect.
  allowDuringMatch: false #Make possible for players to join during a match. Only possible if arena goal is compatible (see goals documentation)
tp:
  #Spawnpoints where players go after specific event. Use "old" to use player location before arena.
  death: spectator #Spawnpoint where player goes after death (only if he can't respawn after death)
  exit: exit #Spawnpoint where player goes after he leaves the arena
  lose: exit #Spawnpoint where player or team goes if they lose the match
  win: exit #Spawnpoint where player or team goes if they win the match
protection:
  enabled: true #Enable protections on regions. See "regions" part of documentation for more informations
  punish: false #Damage players who don't respect protections
  spawn: 0 #Radius around spawns where player fight is disallowed. Makes possible to prevent spawn kills
  preventIntrusion: false #If true, prevent players entering in all regions of the arena. Ensure placing exit spawnpoint outside of any region.
block:
  blacklist: [] #Blacklist of blocks with players can't interact
  whitelist: [] #Whitelist of blocks with players can interact
arenaregion:
  #Region definitions. See "Regions" part of documentation. Editable with "/pa region" command.
  FightPlace:
    coords: world,-565,99,-536,-419,255,-405
    shape: cuboid
    type: BATTLE
    protections: []
  DeathArea:
    coords: world,-564,91,-535,-420,115,-406
    shape: cuboid
    type: CUSTOM
    flags:
      - DEATH
    protections: []
spawns:
  #List of all registered spawn points. See "Getting started" part of documentation for more information
  #Editable with "/pa spawn" command
  lounge: world,-497,76,-462,-4.9822998046875,29.360103607177734
  fight1: world,-493,211,-480,-0.643310546875,2.9910500049591064
  fight2: world,-485,211,-475,-315.796142578125,5.887644290924072
  fight3: world,-482,211,-467,-268.850830078125,6.786584377288818
  fight4: world,-484,211,-458,-225.3023681640625,9.98283576965332
  fight5: world,-493,211,-455,-181.953369140625,21.968769073486328
  fight6: world,-501,211,-457,-144.7960205078125,13.678470611572266
  fight7: world,-499,211,-467,-84.4664306640625,21.96875
  fight8: world,-500,211,-475,-32.3275146484375,12.979286193847656
  spectator: world,-494,242,-467,-276.9421081542969,12.450007438659668
  exit: world,-449,72,-448,359.3592529296875,2.5499958992004395
  relay: world,-493,242,-467,-173.850830078125,90.0
teams:
  # List of registered teams with format "teamName: DYE_COLOR"
  # Available colors are ORANGE, MAGENTA, LIGHT_BLUE, LIME, PINK, GRAY, LIGHT_GRAY, PURPLE, BLUE, GREEN, RED, CYAN, YELLOW, BLACK, WHITE
  red: RED
  blue: BLUE
mods:
  #List of enabled mods. Use "/pa togglemod" command to change it. More information in "modules" part of documentation
  - QuickLounge
  - StandardSpectate
modules:
  #Settings of modules
  battlefieldjoin:
    joinDuration: 30
  vault:
    winPot: false
  respawnrelay:
    respawnseconds: 10
    choosespawn: false
classitems:
  #Definitions of arena classes (kits). 
  #Use "/pa class" command to change it (really better) or read documentation chapter about "Items in configuration files".
  pearl:
    items:
      - type: ENDER_PEARL
        amount: 16
        meta:
          display-name: Kicker X
          enchants:
            KNOCKBACK: 4
    offhand:
      - type: AIR
        amount: 0
    armor:
      - type: CHAINMAIL_BOOTS
        meta:
          enchants:
            PROTECTION_FALL: 4

```

## Global config file

This file contains general settings of PVP Arena. All changes require a restart.

```yaml
# Default debug settings. In any case, debug can be enabled in-game with `/pa debug` command
debug:
  enable: false
  output: BOTH
  level: FINE
stats: true #Enable arena stats. Requires a correct database configuration (see below)
language: en #Language file to use for all the plugin.
onlyPVPinArena: false #If true, PVP will be disabled everywhere except in arenas
safeadmin: true #If true, asks confirmation before arena removals
#Default offset for teleportation. By default, at center of block.
spawnOffset:
  x: 0.5
  y: 0.5
  z: 0.5
wandItem: STICK #Item to use to select arena regions
#List of whitelisted commands in every arena
whitelist:
- ungod
whitelist_wildcard: false #If true, all commands starting with values of "whitelist" setting (above) will be accepted
#Plugin updater settings. See "Update checker" part of documentation
update:
  plugin: announce
  modules: announce
globalPrefix: 'PVP Arena' #Prefix of global PVPArena messages
#List of sign headers that are accepted to make join signs (more information in FAQ)
signHeaders:
  - '[arena]'
  - '[PVP Arena]'
#Databse settings for stats
database:
  type: sqlite #Database type: sqlite or mysql
  #MySQL specific settings, useless if stats are disabled or if above setting is set to sqlite
  mysql:
    hostname: localhost
    port: 3306
    user: root
    password: ''
    ssl: true
    database: 'pvparena'
ver: 2 #Config file version. Don't change it.
```