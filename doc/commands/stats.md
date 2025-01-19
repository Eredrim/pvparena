# Stats Command

## Description

This command displays the top X players in a specific statistics type.

## Usage

Command |  Definition
------------- | -------------
/pa stats \<statistic\> (number) | show top 10 of a statistic for all the server. Change results size with "number" parameter.
/pa \<arena\> stats \<statistic\> (number) | show top 10 of a statistic for current arena game

Examples:
- `/pa ctf stats DAMAGE` - shows the top 10 player damaged for the current CTF game
- `/pa stats WINS 5` - shops the top 5 winners for all the server

## Details
Valid statistic values are :

- WINS
- LOSSES
- KILLS
- DEATHS
- MAXDAMAGE
- MAXDAMAGETAKE
- DAMAGE
- DAMAGETAKE
- NULL

