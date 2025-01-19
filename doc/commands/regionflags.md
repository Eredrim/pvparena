# Regionflags Command

## Description

This command manages the [regions](../regions.md) behaviour by enabling/disabling flags.

## Usage

Command |  Definition
------------- | -------------
/pa \<arena\> regionflag \<region\> \<flag\> (true/false) | toggle regionflag for an arena region

Example: `/pa ctf !rf win2 WIN true` - enable the WIN regionflag for the region "win2" of the arena "ctf"

## Details

There are several region flags that can be set:

- NOCAMP - move in here, or get punished !
- DEATH - come in, die !
- WIN - come in, win !
- LOSE - come in, lose !
- NODAMAGE - players are invincible ! 

Giving no second argument will just toggle the flag status and show you the result, valid arguments to activate/deactivate include:

on | 1 | true || off | 0 | false
