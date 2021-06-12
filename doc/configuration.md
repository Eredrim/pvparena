# Configuration file

This page explains what are each parameter of arena config file. 
All those parameters can be changed via in-game command ([/pa set](commands/set.md)).

> ℹ This is a default configuration file. 

```yaml
configversion: 1.3.3.217
chat:
  colorNick: true #Use team color in chat
  defaultTeam: false #Limit chat to team only
  enabled: true #Allows chat usage
  onlyPrivate: false #Limit chat to the arena
  toGlobal: none #Begin word to talk to all the arena of onlyPrivate is active. E.g. @all
cmds:
  defaultjoin: true #Join the arena if just typing /pa <arena>
  #List of allowed commands in the arena
  whitelist:
  - ungod
  - login
damage:
  armor: true #Allow armor damage - false = unbreakable
  bloodParticles: false #Show blood particles on fight
  fromOutsiders: false #Allow external arena players to attack
  spawncamp: 1 #If nocamp region flag is enabled, damage set to player
  weapons: true #Allow weapon damage - false = unbreakable
general:
  classspawn: false #Create specific class spawns. E.g. blueTankSpawn
  classSwitchAfterRespawn: false #If IngameClassSwitch is enabled, switch class only on next respawn
  customReturnsGear: false #If player has custom inventory, reload it after the match
  enabled: true #Make arena accessible or not
  gm: 0 #Arena game mode
  leavedeath: false #Kill the player on battleground leaving
  lang: none
  owner: server #Set owner of the arena
  regionclearexceptions: [] #List of regions where entities are not cleared
  quickspawn: true #Spawn all players at the same time. If false, spawn player one by one.
  prefix: MyArena #Name of the arena displayed in chat messages
  showRemainingLives: true #Brodcast ramaning lives in chat
  smartspawn: false #Spread players on spawn points in a balanced way
  time: -1 #Arena day time in ticks, allow to make night arenas
  type: free #Arena type : free or team
  wand: STICK #Wand item for region selection
goal:
  #Goal specific configurations, see 'Enhancement' part of documentation
  livesPerPlayer: false
  endCountDown: 5
  playerlives:
    plives: 3
  teamlives:
    tlives: 10
items:
  keepAllOnRespawn: false #Keep inventory on respawn
  excludeFromDrops: none #List of items not dropped on kill
  keepOnRespawn: none #List of items kept on respawn (if keepAllOnRespawn is disabled)
  minplayers: 2 #Minimum number of players to start fighting
  random: true
  rewards: none #List of reward items given to win team team/player
  takeOutOfGame: none #List of items kept from player inventory out the game
join:
  range: 0 #Max distance from battleground to join arena. Set 0 to disable.
  forceregionjoin: false #Limit arena join from "join" type region.
  onlyifhasplayed: false #Allow player to join arena during game only if he played and left (with disconnection). Useful only if joinInBattle is enabled.
block:
  blacklist: [] #Blacklist of blocks player can't interact
  whitelist: [] #Whitelist of blocks player can interact
goals:
  #List if enabled goals, see 'Enhancement' part of documentation
- PlayerLives
mods:
  #List of enabled mods, see 'Enhancement' part of documentation
- BattlefieldJoin
- StandardLounge
- StandardSpectate
msg:
  #Arena specific messages you can configure
  lounge: Welcome to the arena lounge! Hit a class sign and then the iron block to
    flag yourself as ready!
  playerjoined: '%1% joined the Arena!'
  playerjoinedteam: '%1% joined team %2%!'
  starting: Arena is starting! Type &e/pa %1% to join!
  youjoined: You have joined the FreeForAll Arena!
  youjoinedteam: You have joined team %1%!
perms:
  alwaysJoinInBattle: false #Allow join during game in any case
  explicitArenaNeeded: false #Player needs permission pvparena.join.arenaName to play arena
  explicitClassNeeded: false #Player needs permission pvparena.class.className to get a class
  fly: false #Enable/Disable fly for players
  loungeinteract: false #If true, players can interact with other players and blocks within lounge
  joinInBattle: false #Allow join during a game
  joinWithScoreboard: true #Allow to join arena with scoreboard with AutoVote Mod
  teamkill: true #Allow players to kill players of their own team
  specTalk: true #Allow spectators to use chat
  spectatorinteract: false #Allow spectator to interact with others
player:
  autoIgniteTNT: false #Ignite TNT on place
  clearInventory: NONE #Clear player inventory on join. Set a specific game mode or ALL for any kind. 
  collision: true #Allow player collision with ENTITIES (players, arrows, tridents, armor stands, etc)
  dropsEXP: false #Killed players drop XP
  dropsInventory: false #Killed players drop their inventory
  exhaustion: 0.0 #Set player exhaustion
  hungerforkill: 0 #Not used
  foodLevel: 20 #Initial food level
  health: -1 #Set initial player health. Use -1 for default server value. Must be lower or equal than maxHealth.
  healforkill: false #Heal player who kills another one
  hunger: true #Enable hunger, if false feed level never decreases
  itemsonkill: none #List of items given to player who kills another one
  mayChangeArmor: true #Allow players to edit their armor slots in game
  maxhealth: -1 #Set maximum health for player. 1 heart = 2 pts. Use -1 for default server value.
  preventDeath: true #Not really kill player in order to avoid "you are died" message
  refillCustomInventory: true #Refill custom player inventory after death
  refillInventory: true #Players keeps inventory they had before their death
  refillforkill: false #Reset class inventory of a player who killed another one
  removearrows: false #Remove arrows on body after death
  saturation: 20 #Set hunger saturation
  quickloot: false #Automatically transfer chest content to inventory
protection:
  enabled: true #Enable protections on regions. See "regions" part of documentation for more informations
  punish: false #Damage players who don't respect protections
  spawn: 0 #Radius around spawns where player fight is disallowed
ready:
  autoClass: none #Name of class set automatically when player joins an arena. Set "none" to disable. Set to "custom" if you use "playerClasses".
  block: IRON_BLOCK #Block player can hit to be ready. Has the same effect than typing /pa ready.
  checkEachPlayer: false #Check if each player is ready before start
  checkEachTeam: true #Check if each team is ready before start
  enforceCountdown: false #If everyone is ready, game start before the end of countdown
  minPlayers: 2 #Minimum number of players to play the arena
  maxPlayers: 4 #Maximum number of players to play the arena
  maxTeam: 0 #Maximim number of player in each team
  neededRatio: 0.5 #Ratio of ready player needed to start countdown
time:
  startCountDown: 10 #Start countdown in seconds
  regionTimer: 10 #Time in ticks for region tasks. Don't change this.
  teleportProtect: 3 #Number of seconds of invulnerability after teleport
  resetDelay: -1 #Wait time (in ticks) to reset players when they exit arena
  warmupCountDown: 0 #Warmup time (in seconds)
  pvp: 0 #Time before PVP is enabled (in seconds)
tp:
  #Spawnpoints where players go after specific event. Use "old" to use player location before arena.
  death: old #Spawnpoint where player goes after death (only if he can't respawn after death)
  exit: exit #Spawnpoint where player goes after he leaves the arena
  lose: exit #Spawnpoint where player or team goes if they lose the match
  win: exit #Spawnpoint where player or team goes if they win the match
  offsets: [] #Spawnpoint offsets. List of strings with following format : "spawnName:xOffset;yOffset;zOffset"
uses:
  classSignsDisplay: false #Display player names on class signs
  deathMessages: true #Show death messages (from language file)
  deathMessagesCustom: true #Not used
  evenTeams: false #If true, requires the same number of players in each team
  ingameClassSwitch: false #Allow to switch player class during a game
  invisibilityfix: false #Force player to be visible
  evilinvisibilityfix: false #Use this param if the previous one doesn't work
  overlapCheck: true #Set to not check if the arena region collides with a running arena
  playerclasses: false #Use players own inventory
  scoreboard: true #Enable scoreboard
  scoreboardrounddisplay: false #Show rounds number in scoreboard
  suicidepunish: false #Increase other players score where someone commit suicide
  teamrewards: false #Give reward to winning team
  teleportonkill: false #Respawn KILLER after a kill
  woolHead: false #Use a colored wool head as helmet
flagColors:
  #Flag colors with format  team: DYE_COLOR
  #Use color among ORANGE, MAGENTA, LIGHT_BLUE, LIME, PINK, GRAY, LIGHT_GRAY, PURPLE, BLUE, GREEN, RED, CYAN, YELLOW, BLACK, WHITE
  red: WHITE
  blue: BLACK
classitems:
  #Class items. See "items" part of documentation for more information
  warrior:
    items:
    - type: IRON_SWORD
    - type: SPLASH_POTION
      meta:
        potion-type: minecraft:regeneration
    - type: SPLASH_POTION
      meta:
        potion-type: minecraft:weakness
    - type: BOW
      meta:
        enchants:
          ARROW_INFINITE: 1
    - type: ARROW
    offhand:
    - type: AIR
      amount: 0
    armor:
    - type: IRON_BOOTS
    - type: IRON_LEGGINGS
    - type: IRON_CHESTPLATE
teams:
  #Team colors with format "team: DYE_COLOR"
  #Use color among ORANGE, MAGENTA, LIGHT_BLUE, LIME, PINK, GRAY, LIGHT_GRAY, PURPLE, BLUE, GREEN, RED, CYAN, YELLOW, BLACK, WHITE
  red: RED
  blue: BLUE
spawns:
  #List of registered spawn. See "Creation" part of documentation for more information
  lounge: world,863,68,-997,0,0
  spectator: world,876,63,-997,90,0
  exit: world,321,67,221,0,5
  spawn1: world,873,58,-997,90,0
  spawn2: world,863,58,-1007,0,0
  spawn3: world,853,58,-997,-90,0
  spawn4: world,863,58,-987,180,0
```
