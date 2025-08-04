# GlobalClass command

## Description

This command manages the global classes. Global classes are common to arenas and can be enabled in each arena by
setting `uses.globalClasses` to `true` in arena config.

You can use this command to show, edit and remove global classes, and to create global class chests (just like
[class chests](classchest.md)).

## Usage

| Command                               | Definition                                                      |
|---------------------------------------|-----------------------------------------------------------------|
| /pa globalclass load \<class\>        | Preview the class items of the class directly in your inventory |
| /pa globalclass leave                 | Leave class preview                                             |
| /pa globalclass save \<class\>        | Save your inventory to the class items of a class               |
| /pa globalclass remove \<class\>      | Remove a class from an arena                                    |
| /pa globalclass savechest \<class\>   | Use the chest your are looking to store items of a class.       |
| /pa globalclass removechest \<class\> | Remove a classchest                                             |

Example: use `/pa !gc save Test` to save your inventory to the global class "Test"

> **🚩 Tips:**  
> - Type `/pa !gc leave` to leave class preview
> - If you've loaded a class with `/pa !gc load <class>`, you don't need to specify class name in next 
> commands to save or remove it. Just type `/pa !gc save` or `/pa !gc remove`
> - As for arena classes, use class chests if you need to save special kinds of items (like items from mods or datapacks)
> - You can edit classchest content just by placing items in the chest. Mind to [reload](reload.md) your arena to
> apply changes

> **⚠️ Warning:**  
> Don't run this command when the arena is running, otherwise current match will be impacted by your changes.
> If you use class chests, chests must physically exist in the world, otherwise you will get an error.