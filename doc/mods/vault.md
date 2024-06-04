# Vault

## About

The Vault module adds an economy hook to add multiple features like join fee, money rewards and bets.


## Requirements

As you can guess it, this module requires the **Vault** plugin (downloadable [here](https://www.spigotmc.org/resources/vault.34315/))
and an economy plugin which is compatible with it like EssentialX, Ultimate_Economy, iConomy Reloaded, XConomy, etc.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

*These settings can be found under `modules.vault` node in your arena config file.*

- **conditions.entryFee** \- fee players will pay to join the arena (default: 0)
- **conditions.winFeePot** \- if true, all collected fee will be shared between winning players (default: false)
- **conditions.minPlayTime** \- minimum playing time (in seconds) to get a reward (default: 0)
- **conditions.minPlayers** \- minimum of players to get a reward - leaving players are not counted (default: 2)


- **reward.death** \- reward a player when they die (default: 0)
- **reward.kill** \- reward a player when they kill another player (default: 0)
- **reward.win** \- reward a player when they or their team win the game. For [team goals](../goals.md), each player of the winning 
  team earn this amount. (default: 0)
- **reward.score** \- reward a player when they or their make actions to score points/lives/kills for the goal. (default: 0 - 🚧 incomplete feature)
- **reward.winFactor** \- if `conditions.winFeePot` is enabled, defines how much the won share of the fee pot will be multiplied (default: 1)


- **bet.enabled** \- enables betting features (default: false)
- **bet.time** \- defines during how many seconds players can bet after the beginning of the match (default: 60)
- **bet.minAmount** \- minimum amount to bet (default: 0)
- **bet.maxAmount** \- maximum amount to bet (default: 0 - i.e. no maximum)
- **bet.winFactor** \- how much the bet gain should be multiplied (default: 1)

## Commands

- `/pa [arena] bet [name] [amount]` \- bet [amount] on team / player. 

**NB:** Betting features are only accessible for players that are not playing the match (obviously).

> 🚩 **Tips:**
> * This command is only available when `bet.enabled` is set to `true`
> * Players don't have to specify arena name in the command if they are spectating the arena
> * Players can change their bet by running the command again (if bet time is not over)

## Additional information

> ⚠️ **Warning:**  
> Due to their special working way, it's not currently recommended to use win rewards and betting with 
> [Tank](../goals/tank.md) and [Infect](../goals/infect.md) goals.

> ⚙️ **Technical precisions for nerds:**
> * Calculation of win prize is made with this formula: `(totalPotAmount / winnerNumber * winFactor) + winReward`
> * Calculation of bet gain is made with this formula: `(betAmount / totalAmountOfWinnersBets) * totalAmountOfGamblersBets * betFactor`
> * All reward are rounded with a two digits precision. For instance, $9.675 will be rounded to $9.68
