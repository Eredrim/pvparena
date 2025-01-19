# Debug command

## Description

The debug command is used to spam the log file with a hell lot more of information, based on your current issue, or just in general when unsure where the issue comes from.

## Usage

Command |  Definition
------------- | -------------
/pa debug all | debug ALL classes
/pa debug \<value\> | debug class or arena where name equals to `value`
/pa debug none | disable debugging

Example: `/pa debug 4,7` - debug class 4 and 7

## Hazards

The way PVP Arena debugs is known to **spam** a LOT. This rises exponentially the more people you have on. So if you want to debug and are unsure if you're debugging the right thing, 
make sure only a few people are online or at best, only you.

## Details

The class numbers are a temporary solution and will be replaced by a more user-friendly way. Especcially as the numbers are changing and are not sorted correctly in some cases, it's hard to tell unless you know the code.
At the moment, this is used by me to tell people what to debug, so they don't have to know what they CAN debug without being spammed by all PVP Arena classes.

## ToDo

I need to reorganize this system. For big servers, it's probably impossible to do that because there are dozens, hundreds of players online. 
So, what I am planning is to add a player name to the values, so it only debugs given player names.