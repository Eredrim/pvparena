# Class command

## Description

This command manages the arena classes. You can use it to show, edit and remove arena classes.

## Usage

| Command                              | Definition                                                      |
|--------------------------------------|-----------------------------------------------------------------|
| /pa \<arena\> class load \<class\>   | Preview the class items of the class directly in your inventory |
| /pa \<arena\> class save \<class\>   | Save your inventory to the class items of a class               |
| /pa \<arena\> class remove \<class\> | Remove a class from an arena                                    |

Example: use `/pa temp class save Test` to save your inventory to the class "Test" of the arena "temp"

## How to use it

Fill you own inventory with all you want for your arena class (including armor slots and second hand), then just type 
`/pa <arena> class save <class>`. Repeat for each class.  
Afterward, if you want to edit an existing class, use `/pa <arena> class load <class>` to load this class in your
current inventory. Use save command to apply your modifications.

> **🚩 Tips:**  
> - Type `/pa leave` to leave class preview
> - If you've loaded a class with `/pa <arena> class load <class>`, you don't need to specify class name in next 
> commands to save or remove it. Just type `/pa class save` or `/pa class remove`

> **⚠️ Warning:**  
> Don't run this command when the arena is running, otherwise current match will be impacted by your changes.

## Details

As of v1.0.1.*, the following is saved:

- Enchantments
- Data values
- Dyes
- Dyed armor
- Custom potions
- Written books
- Renamed items
- Player Heads
- Item Lore 

> **ℹ️ Note:**  
> If you need to save other kinds of items (like items from mods or datapacks), you can use the 
> [`/pa classchest` command](classchest.md) to save them "as is" in a chest.

## ToDo

Save Fireworks ... maybe