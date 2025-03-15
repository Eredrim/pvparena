# Upgrading from 1.15.x

## Can I keep all my arena settings with this new version?
Unfortunately, PVP Arena 2.0 is **not retro-compatible** with settings of previous versions. Some settings no longer exist anymore,
some modules have been removed or merged in main plugin, some other settings have a new format, etc. So, a new 
installation is required and you'll recreate your arenas from scratch.
In any case, the new version of the plugin will backup you previous arena settings on its first startup.

## How to install PVP Arena 2.0?
Once your server is turned off, replace the jar file of PVP Arena in your plugin folder.
If you want to directly install 2.0 modules, clear the directories `plugins/pvparena/files/` and `plugins/pvparena/mods/`,
then unzip the 2.0 modules pack within `plugins/pvparena/files/`. Otherwise, both directories will be cleanup on first
startup.

Start your server. The global `config.yml` is now reset and all your previous arena configuration have been moved to
`plugins/pvparena/arenas_old/`. Don't try to copy-back those files in `arenas` directory, they're not compatible.

Now, you can re-create your arenas following the new ["getting started"](getting-started.md) process.

## What about statistics from the former version?
All saved statistics in `stats.yml` are now useless and can not be recovered in this version. You can delete this file if
you think you don't need it anymore.
New stats are saved in database (by default, in a local SQLite database). You can change it for MySQL or disable stats
recording in your global `config.yml`. See [configuration](configuration.md#global-config-file) chapter for details.

## Do I have to update my language file?
Language entries have been fully updated. If you use the English language file (default one), you have nothing to do. File 
is automatically updated on first startup.

If you use a custom language file, check in [language page](languages.md) if a new version is available. If not, please 
update yours basing on `lang_en.yml`. 
Don't use a language file from a previous version, multiple entries have been renamed, moved or
deleted.