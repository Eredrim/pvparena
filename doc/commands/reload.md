# Reload command

## Description

The reload command does what you might think it does. It reloads the arena by force stopping it, removing it from the arena list, and loading it from the config definition. If you don't specify an 
arena and are not part of an arena, this command reloads all arenas.

## Usage

Command |  Definition
------------- | -------------
/pa reload | reload everything or the arena you're part of
/pa reload ymls | reload main config, language and help ymls 
/pa \<arena\> reload | reload a specific arena

Example: `/pa ctf reload` - reload the arena called CTF

## Details

This command should be used when fumbling around with regions a lot, and after re-setting a region/spawn, after updating class definitions, and after changing the config file in terms of teams, 
class definitions and other BIG stuff.