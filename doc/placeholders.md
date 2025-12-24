## PVP Arena Placeholders

Since 2.0, PVPArena supports **placeholderAPI** and makes possible to display arena information with placeholders.
Here is the list of currently implemented placeholders.

> **🚩 Tip:** For all placeholders, you can replace arena name by "**cur**" to get current arena placeholders or by 
> beginning of arena UUID.

## Table of contents
- [Arena information](#arena-information)
  - [Get arena name](#get-arena-name)
  - [Get arena status](#get-arena-status)
  - [Get number of fighters in the arena](#get-number-of-fighters-in-the-arena)
  - [Get minimum required players for the arena](#get-minimum-required-players-for-the-arena)
  - [Get maximum allowed players for the arena](#get-maximum-allowed-players-for-the-arena)
  - [Get current number of players / arena capacity](#get-current-number-of-players--arena-capacity)
- [Getting data about a match](#getting-data-about-a-match)
  - [Get remaining time for the match](#get-remaining-time-for-the-match)
  - [Get score of a player](#get-score-of-a-player)
  - [Get score of a team](#get-score-of-a-team)
  - [Get current top scores](#get-current-top-scores)
  - [Get arena team of the current player](#get-arena-team-of-the-current-player)
  - [Get the arena class of the current player](#get-the-arena-class-of-the-current-player)
  - [Color a team name](#color-a-team-name)
  - [Color a player name](#color-a-player-name)
  - [Get text color code of a team](#get-text-color-code-of-a-team)
- [Getting global stats and scores](#getting-global-stats-and-scores)
  - [Get arena statistics / make a leaderboard](#get-arena-statistics--make-a-leaderboard)
  - [Get global statistics / make a leaderboard](#get-global-statistics--make-a-leaderboard)

<br>

## Arena information

### Get arena name
```
%pvpa_<arena_uuid>_name%
```
**\<arena_uuid\>**: UUID, first characters of arena UUID, or `cur`

E.g.:  
`%pvpa_cur_name%`  
`%pvpa_8ed763_name%`
`%pvpa_123e4567-e89b-42d3-a456-556642440000_name%`

<br>

### Get arena status
```
%pvpa_<arena>_status%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

Returns the arena status, which can be one of these values:
- available
- waiting for players
- game in progress
- restoration in progress
- disabled

All these values can be edited in `arena.status` section of the language file.

<br>

### Get number of fighters in the arena
```
%pvpa_<arena>_pcount%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

<br>

### Get minimum required players for the arena
```
%pvpa_<arena>_pmincount%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

**Note:** Returns the value set from arena configuration.

<br>

### Get maximum allowed players for the arena
```
%pvpa_<arena>_pmaxcount%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

**Note:** Returns the value set from arena configuration.

<br>

### Get number of fighters in the arena
```
%pvpa_<arena>_pcount%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

<br>

### Get current number of players / arena capacity
```
%pvpa_<arena>_capacity%
%pvpa_<arena>_capacity_team_<team>%
```
**\<arena\>**: name/uuid of the arena (or `cur`)  
**\<team\>**: the team name

**Note:** If maximums are set (for arena or teams) in your config file, the placeholder displays a ratio (like 1 / 15),
however it just prints the number of players in the team or in the arena. It's a simple combination of 
`%pvpa_<arena>_pcount%` and `%pvpa_<arena>_pmaxcount%`.

<br>

## Getting data about a match

### Get remaining time for the match
```
%pvpa_<arena>_timer%
%pvpa_<arena>_timer_<format>%
```
**\<arena\>**: name/uuid of the arena (or `cur`)
**\<format\>** (optional): custom time format following the [Java time format](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns)

**Note:** By default, time format is `mm:ss`.

<br>

### Get score of a player
```
%pvpa_<arena>_pscore%
%pvpa_<arena>_pscore_<player>%
```
**\<arena\>**: name/uuid of the arena (or `cur`)  
**\<player\>** (optional): player name. If not specified, the placeholder returns the score of the player for which the placeholder is parsed.

**Note:** This placeholder returns the score of the player for the current goal. If the goal has a **team** game mode, 
it returns the score of player's team.

<br>

### Get score of a team
```
%pvpa_<arena>_tscore%
%pvpa_<arena>_tscore_<team>%
```
**\<arena\>**: name/uuid of the arena (or `cur`)  
**\<team\>** (optional): the team name. If not specified, the placeholder returns the score of the player team for which the placeholder is parsed.

<br>

### Get current top scores
```
%pvpa_<arena>_topscore_team_<nb>%         # Returns the team name matching to this ranking entry
%pvpa_<arena>_topscore_player_<nb>%       # Returns the player name matching to this ranking entry (FFA goals only)
%pvpa_<arena>_topscore_value_<nb>%        # Returns the score matching to this ranking entry
```
**\<arena\>**: name/uuid of the arena (or `cur`)  
**\<nb\>**: the rank from **0** to the max number of teams/players

For instance, if the arena is named CTF and  has 3 teams, you can make a quick score display like this:  
`%pvpa_CTF_topscore_team_0% %pvpa_CTF_topscore_value_0%`  
`%pvpa_CTF_topscore_team_1% %pvpa_CTF_topscore_value_1%`  
`%pvpa_CTF_topscore_team_2% %pvpa_CTF_topscore_value_2%`

<br>

### Get arena team of the current player
```
%pvpa_<arena>_team%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

<br>

### Get the arena class of the current player
```
%pvpa_<arena>_class%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

<br>

### Color a team name
```
%pvpa_<arena>_tcolor_{other_placeholder_without_percents}%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

E.g. `%pvpa_Versus_tcolor_{pvpa_Versus_team}%`

<br>

### Color a player name
```
%pvpa_<arena>_pcolor_{other_placeholder_without_percents}%
```
**\<arena\>**: name/uuid of the arena (or `cur`)

E.g. `%pvpa_Versus_tcolor_{player_name}%`

<br>

### Get text color code of a team
```
%pvpa_<arena>_tcolorcode_<team>%
```
**\<arena\>**: name/uuid of the arena (or `cur`)
**\<team\>**: the team name

**Note:** This returns a simple chat color code (like `&c`) that can be used with any other text or placeholder.

<br>

## Getting global stats and scores

### Get arena statistics / make a leaderboard

You can print arena statistics to make a leaderboard. This placeholder make possible to print top 10 of each statistic
of each arena.
```
%pvpa_<arena>_stats_<stat>_player_<nb>%
%pvpa_<arena>_stats_<stat>_score_<nb>%
```
**\<arena\>**: name/uuid of the arena  
**\<stat\>**: name of the statistic (can be found [here](commands/stats.md#details))  
**\<nb\>**: line number between **0** and **9**  
<br>

Example: to make a top 3 of winners for the arena "Bastion" looking like this
> 17 Warrior55  
> 14 Xx_Duck_xX  
> 8 FluffyMike 

Use the following placeholders:  
`%pvpa_Bastion_stats_WINS_score_0% %pvpa_Bastion_stats_WINS_player_0%`  
`%pvpa_Bastion_stats_WINS_score_1% %pvpa_Bastion_stats_WINS_player_1%`  
`%pvpa_Bastion_stats_WINS_score_2% %pvpa_Bastion_stats_WINS_player_2%`

<br>

### Get global statistics / make a leaderboard

You can print global statistics to make a leaderboard. This placeholder make possible to print top 10 of each statistic
of each arena.
```
%pvpa_stats_<stat>_player_<nb>%
%pvpa_stats_<stat>_score_<nb>%
```
**\<stat\>**: name of the statistic (can be found [here](commands/stats.md#details))  
**\<nb\>**: line number between **0** and **9**  
<br>

Example: to make a top 3 of players with the most kills ever
> 92 BatAng3l  
> 89 Awesome__Dude
> 4 PontiacBandit

Use the following placeholders:  
`%pvpa_stats_KILLS_score_0% %pvpa_stats_KILLS_player_0%`  
`%pvpa_stats_KILLS_score_1% %pvpa_stats_KILLS_player_1%`  
`%pvpa_stats_KILLS_score_2% %pvpa_stats_KILLS_player_2%`
