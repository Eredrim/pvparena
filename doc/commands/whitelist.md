# Whitelist command

## Description

This command manages the block place / break whitelist for an arena. With this, you're able to restrict actions
in your arena.

## Usage

Command |  Definition
------------- | -------------
/pa \<arena\> whitelist clear             | clear the general whitelist
/pa \<arena\> whitelist \<break/place\> clear       | clear the BREAK or the PLACE whitelist
/pa \<arena\> whitelist \<break/place\> show       | show a whitelist content
/pa \<arena\> whitelist \<break/place\> add \<block\>    | add a block to a whitelist
/pa \<arena\> whitelist \<break/place\> remove \<block\>    | remove a block from a whitelist

Example: `/pa ctf whitelist break add SNOW` - add SNOW block to the BREAK whitelist

> **🚩 Tip:**  
> [`/pa blacklist`](blacklist.md) command works exactly in the same way