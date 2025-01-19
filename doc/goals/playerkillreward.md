# PlayerKillReward

> ℹ This goal is designed to be played in free-for-all

## Description

In this goal, players have to make kills to wins. When they kill someone, their remaining kill number decreases and 
they get a new item set.

## Setup

By default, there are 5 default kill gears:
- 5 \- Leather Armor & Wooden Sword
- 4 \- Chain Armor & Stone Sword
- 3 \- Gold Armor & Iron Sword
- 2 \- Iron Armor & Diamond Sword
- 1 \- Diamond Armor & Diamond Sword 

> Note that the Kill number is to be understood as "kills left to win"

You can alter them or set new ones by doing that:
- Equip your own inventory with wanted gears (place armor and off-hand in good slots)
- Type `/pa <arenaname> !kr <remainingKillNumber>`

## Config settings

- `graduallyDown` - on death, keep current number of remaining kills instead of resetting it (default: false)
- `onlyGive` - just give new items to players instead of replacing their inventory (default: false) 
