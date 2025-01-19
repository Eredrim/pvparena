# Setowner Command

## Description

This command hands over ownership. This has to be either "%server%" or a player name. By default every arena created by a player with permission `pvparena.admin` is owned by the server. Note that the player name is 
NOT checked, but you can verify it with [`/pa info`](../commands.md#arena-standard-commands)

## Usage Examples

Command |  Definition
------------- | -------------
/pa \<arena\> setowner \<player\> | give player ownership of an arena


Example: `/pa global setowner %server%` - set the server as owner of the "global" arena

