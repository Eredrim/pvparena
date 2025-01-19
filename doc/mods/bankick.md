# BanKick

## Description

A fully functional kick/ban suite to get rid of derps that don't ready up or troll ppl inside the arena, hence, are BANNED from the arena. Order, yay!

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

\-

## Commands

- `/pa <arena> ban <player>` \- ban player from arena
- `/pa <arena> kick <player>` \- kick player from arena
- `/pa <arena> tempban <player> <time>` \- ban player for \<time\> from arena
- `/pa <arena> unban <player>` \- unban player from arena
- `/pa <arena> tempunban <player> <time>` \- unban player for \<time\> from arena 

\<time\> is e.g. "12m" or "10s" or "20d", if there is no letter, it will be parsed as seconds.  
Use "s" for seconds, "m" for minutes and "d" for days.


> ⚠ **Warning:**  
> `tempban` and `tempunban` are **reset at server restart or arena reload**.   
> These commands save concerned player list in memory.
