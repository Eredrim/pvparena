# Install command

## Description

This command manages modules. You can list installed modules, download latest version, install, uninstall, update and 
upgrade your modules.

## Usage

Command |  Definition
------------- | -------------
/pa modules (list) | list all available modules (installed modules are highlighted)
/pa modules install \<moduleName\> | install a module (from the `files` folder)
/pa modules uninstall \<moduleName\> |  uninstall a module (removing from `mods` folder)
/pa modules update |  update all modules in `mods` folder with versions which are in `files` folder
/pa modules download |  download latest release of modules pack and place it in `files` folder
/pa modules upgrade |  upgrade all your modules (perform a download and an update)


## Directory management

In order to increase performances, PVP Arena has two directories for mods:
- `/mods` contains only mods used in arenas and installed by admins
- `/files` contains all downloaded mods

When you install a mod, the plugin copies it from `/files` to `/mods`.


## Details

The result of list command contains all available modules, with their respective version. Yellow names mark installed 
plugins, green version numbers mark as up to date, red version numbers show that you need to update the files.