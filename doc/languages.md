# Languages

Here are the language files for PVP Arena, not (all) created or updated by me.

## How to install a language file
Download the file and copy it in the folder `plugins/pvparena/`. Then edit the file `config.yml` and 
set the `language` setting to the new language code (second column below).
Finally, reload your plugin configuration by running [`/pa reload`](commands/reload.md)

| download                       | language code  | language | author          | last update |
|--------------------------------|----------------|----------|-----------------|-------------|
| generated                      | 'en' (default) | English  | slipcor/Eredrim | 2025-02     |
| [link](../lang/lang_fr.yml)    | 'fr'           | Français | Eredrim         | 2025-02     |
| [link](../lang/lang_zh-CN.yml) | 'zh-CN'        | 简体中文     | Harry_H2O       | 2025-02     |

<br>

> 💡 **Did you know?**  
> PVP Arena is powered by community! You can offer your own language file by [creating an issue](https://github.com/Eredrim/pvparena/issues).

## How to create your own language file?

Browse your PVPArena repository (`plugins/pvparena/`) and copy the file named `lang_en.yml` to `lang_<lang_code>.yml`
where `<lang_code>` is any code to distinguish your language (ideally an 
[ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes) language code, but in reality it doesn't matter).  
Then you can edit all values of the file. Mind to respect YAML rules (for instance using quotes around the message if 
you need to write a colon character).

Finally, edit the file `config.yml`, set the `language` setting to the new language code and reload your plugin 
configuration by running [`/pa reload`](commands/reload.md).

> 🚩 **Tips:**
> * As soon as your configuration is set to use your own language file, you can edit it and reload it as much as you 
> want to test it.
> * You can use color codes and styling codes in language file
> * Some messages contain argument values, their format is `%<number>%`. You can move them within the message but don't
> remove them. However, a part of the meaning will be lost.