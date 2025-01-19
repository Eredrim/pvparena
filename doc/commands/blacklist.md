# Blacklist command

## Description

This command manages the block place / break blacklist for an arena. With this, you are able to forbid
some actions to arena players.

## Usage

Command |  Definition
------------- | -------------
/pa \<arena\> blacklist clear             | clear the general blacklist
/pa \<arena\> blacklist \<break/place\> clear       | clear the BREAK or the PLACE blacklist
/pa \<arena\> blacklist \<break/place\> show       | show a blacklist content
/pa \<arena\> blacklist \<break/place\> add \<block\>    | add a block to a blacklist
/pa \<arena\> blacklist \<break/place\> remove \<block\>    | remove a block from a blacklist

Example: `/pa ctf blacklist break add SNOW` - add SNOW block to the BREAK blacklist

> **🚩 Tip:**  
> [`/pa whitelist`](whitelist.md) command works exactly in the same way