package net.slipcor.pvparena.core;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

/**
 * <pre>
 * Language class
 * </pre>
 * <p/>
 * provides methods to display configurable texts
 *
 * @author slipcor
 */

public final class Language {
    private Language() {
    }

    private static FileConfiguration config;

    public enum MSG {
        __VERSION__("__version__", "2.0.0"),
        ARENA_CREATE_DONE("arena.create.done", "Arena '%1%' with goal %2% created!"),
        ARENA_DISABLE_DONE("arena.disable.done", "Arena disabled!"),
        ARENA_EDIT_DISABLED("arena.edit.disabled", "Disabled edit mode for arena: %1%"),
        ARENA_EDIT_ENABLED("arena.edit.enabled", "Enabled edit mode for arena: %1%"),
        ARENA_ENABLE_DONE("arena.enable.done", "Arena enabled!"),
        ARENA_ENABLE_FAIL("arena.enable.fail", "&cArena failed to load. Please check the config."),
        ARENA_LIST("arena.arenalist", "Arenas: &a%1%&r"),
        ARENA_RELOAD_DONE("arena.reload.done", "Arena reloaded!"),
        ARENA_REMOVE_DONE("arena.remove.done", "Arena removed: &e%1%&r"),
        ARENA_STARTING_IN("arena.startingin", "Enough players ready. Starting in %1%!"),
        ARENA_START_DONE("arena.start.done", "Arena force started!"),
        ARENA_STOP_DONE("arena.stop.done", "Arena force stopped!"),

        CMD_BLACKLIST_ADDED("cmd.blacklist.added", "Added &a%1%&r to &e%2%&r blacklist!"),
        CMD_BLACKLIST_ALLCLEARED("cmd.blacklist.allcleared", "All blacklists cleared!"),
        CMD_BLACKLIST_CLEARED("cmd.blacklist.cleared", "Blacklist &e%1%&r cleared!"),
        CMD_BLACKLIST_HELP("cmd.blacklist.help", "Usage: /pa blacklist clear | /pa blacklist <type> <clear|add|remove> <id>"),
        CMD_BLACKLIST_REMOVED("cmd.blacklist.removed", "Removed &a%1%&r from &e%2%&r blacklist!"),
        CMD_BLACKLIST_SHOW("cmd.blacklist.show", "Blacklist &e%1%&r:"),

        CMD_WHITELIST_ADDED("cmd.whitelist.added", "Added &a%1%&r to &e%2%&r whitelist!"),
        CMD_WHITELIST_ALLCLEARED("cmd.whitelist.allcleared", "All whitelists cleared!"),
        CMD_WHITELIST_CLEARED("cmd.whitelist.cleared", "Whitelist &e%1%&r cleared!"),
        CMD_WHITELIST_HELP("cmd.whitelist.help", "Usage: /pa whitelist clear | /pa whitelist <type> <clear|add|remove> <id>"),
        CMD_WHITELIST_REMOVED("cmd.whitelist.removed", "Removed &a%1%&r from &e%2%&r whitelist!"),
        CMD_WHITELIST_SHOW("cmd.whitelist.show", "Whitelist &e%1%&r:"),

        CMD_CLASS_LIST("cmd.class.list", "Available classes: %1%"),
        CMD_CLASS_PREVIEW("cmd.class.preview", "You are now previewing the class: %1%. Run &e/pa leave&r to quit preview mode."),
        CMD_CLASS_REMOVED("cmd.class.removed", "Class removed: %1%"),
        CMD_CLASS_SAVED("cmd.class.saved", "Class saved: %1%"),
        CMD_ARENACLASS_SELECTED("cmd.arenaclass.selected", "You have switched to the &e%1%&r class."),
        CMD_ARENACLASS_SELECTED_RESPAWN("cmd.arenaclass.selectedrespawn", "You will switch to the &e%1%&f class on next respawn."),

        CMD_CLASSCHEST("cmd.classchest.done", "Successfully set the class items of %1% to the contents of %2%. Please reload the arena when you are done setting chests!"),

        CMD_GOAL_SET("cmd.goal.added", "Goal set: &a%1%&r"),
        CMD_GOAL_EDITING("cmd.goal.editing", "Edit arena goal with command: &a/pa <arena> goal %1%&r"),

        CMD_HELP_MESSAGE("cmd.help.message", "Documentation and all help about commands are available online on &9%1%&r"),
        CMD_HELP_LINK("cmd.help.link", "https://github.com/Eredrim/pvparena"),

        CMD_SETOWNER_DONE("cmd.setowner.done", "&a%1%&r is now owner of arena &a%2%&r!"),

        CMD_SPAWN_NOTSET("cmd.spawn.notset", "Spawn not set: &a%1%&r"),
        CMD_SPAWN_REMOVED("cmd.spawn.removed", "Spawn removed: %1%"),
        CMD_SPAWN_SET("cmd.spawn.set", "Spawn set: %1%"),
        CMD_SPAWN_SET_AGAIN("cmd.spawn.setagain", "Spawn set again: %1%"),
        CMD_SPAWN_UNKNOWN("cmd.spawn.unknown", "Spawn not found: &a%1%&r"),

        CMD_TOGGLEMOD_BATTLE("cmd.togglemod.battle", "&cYou activated a module that requires a BATTLE region! If this arena already has a region, type: &r/pa <arena> !rt <region> BATTLE"),
        CMD_TOGGLEMOD_REPLACEMENT("cmd.togglemod.replacement", "Module &a%1%&r will replace &a%2%&r module."),
        CMD_TOGGLEMOD_DISABLED("cmd.togglemod.disabled", "Module disabled: &a%1%&r"),
        CMD_TOGGLEMOD_NOT_REMOVABLE("cmd.togglemod.notremovable", "Module &a%1%&r can't be removed. It have to be replaced by a module having the same type (%2%)"),
        CMD_TOGGLEMOD_ENABLED("cmd.togglemod.enabled", "Module enabled: &a%1%&r"),

        CHECK_DONE("check.done", "Check done! No errors!"),

        DEATHCAUSE_BLOCK_EXPLOSION("deathcause.BLOCK_EXPLOSION", "an explosion"),
        DEATHCAUSE_CONTACT("deathcause.CONTACT", "a cactus"),
        DEATHCAUSE_CUSTOM("deathcause.CUSTOM", "Herobrine"),
        DEATHCAUSE_DROWNING("deathcause.DROWNING", "water"),
        DEATHCAUSE_ENTITY_EXPLOSION("deathcause.ENTITY_EXPLOSION", "an Explosion"),
        DEATHCAUSE_FALL("deathcause.FALL", "gravity"),
        DEATHCAUSE_FIRE_TICK("deathcause.FIRE_TICK", "fire"),
        DEATHCAUSE_FIRE("deathcause.FIRE", "a fire"),
        DEATHCAUSE_LAVA("deathcause.LAVA", "lava"),
        DEATHCAUSE_LIGHTNING("deathcause.LIGHTNING", "Thor"),
        DEATHCAUSE_MAGIC("deathcause.MAGIC", "Magical Powers"),
        DEATHCAUSE_POISON("deathcause.POISON", "Poison"),
        DEATHCAUSE_PROJECTILE("deathcause.PROJECTILE", "something they didn't see coming"),
        DEATHCAUSE_STARVATION("deathcause.STARVATION", "hunger"),
        DEATHCAUSE_SUFFOCATION("deathcause.SUFFOCATION", "lack of air"),
        DEATHCAUSE_SUICIDE("deathcause.SUICIDE", "self"),
        DEATHCAUSE_THORNS("deathcause.THORNS", "thorns"),
        DEATHCAUSE_VOID("deathcause.VOID", "the Void"),
        DEATHCAUSE_FALLING_BLOCK("deathcause.FALLING_BLOCK", "a falling block"),
        DEATHCAUSE_HOT_FLOOR("deathcause.HOT_FLOOR", "a magma block"),
        DEATHCAUSE_CRAMMING("deathcause.CRAMMING", "a collision surplus"),
        DEATHCAUSE_DRAGON_BREATH("deathcause.DRAGON_BREATH", "dragon breath"),

        DEATHCAUSE_CREEPER("deathcause.CREEPER", "a creeper"),
        DEATHCAUSE_SKELETON("deathcause.SKELETON", "a skeleton"),
        DEATHCAUSE_SPIDER("deathcause.SPIDER", "a spider"),
        DEATHCAUSE_GIANT("deathcause.GIANT", "a giant"),
        DEATHCAUSE_ZOMBIE("deathcause.ZOMBIE", "a zombie"),
        DEATHCAUSE_SLIME("deathcause.SLIME", "a slime"),
        DEATHCAUSE_GHAST("deathcause.GHAST", "a ghast"),
        DEATHCAUSE_PIG_ZOMBIE("deathcause.PIG_ZOMBIE", "a pig zombie"),
        DEATHCAUSE_ENDERMAN("deathcause.ENDERMAN", "an enderman"),
        DEATHCAUSE_CAVE_SPIDER("deathcause.CAVE_SPIDER", "a cave spider"),
        DEATHCAUSE_SILVERFISH("deathcause.SILVERFISH", "silverfish"),
        DEATHCAUSE_BLAZE("deathcause.BLAZE", "a blaze"),
        DEATHCAUSE_MAGMA_CUBE("deathcause.MAGMA_CUBE", "a magma cube"),
        DEATHCAUSE_ENDER_DRAGON("deathcause.ENDER_DRAGON", "an ender dragon"),
        DEATHCAUSE_WITHER("deathcause.WITHER", "a wither boss"),
        DEATHCAUSE_WITCH("deathcause.WITCH", "a witch"),
        DEATHCAUSE_WOLF("deathcause.WOLF", "a wolf"),
        DEATHCAUSE_IRON_GOLEM("deathcause.IRON_GOLEM", "an iron golem"),
        DEATHCAUSE_SPLASH_POTION("deathcause.SPLASH_POTION", "a splash potion"),

        ERROR_ARENA_ALREADY_PART_OF("error.arena.alreadyplaying", "You are already part of &a%1%&r!"),
        ERROR_ARENA_EXISTS("error.arenaexists", "Arena already exists!"),
        ERROR_ARENA_NOTFOUND("error.arenanotexists", "Arena does not exist: %1%"),
        ERROR_ARENACONFIG("error.arenaconfig", "Error when loading arena config: %1%"),
        ERROR_ARGUMENT_TYPE("error.argumenttype", "&cInvalid argument type:&r &e%1%&r is no proper &a%2%&r."),
        ERROR_ARGUMENT("error.argument", "&cArgument not recognized:&r %1% - possible arguments: &a%2%&r"),
        ERROR_BLACKLIST_DISALLOWED("error.blacklist.disallowed", "You may not %1% this! Blacklisted!"),
        ERROR_BLACKLIST_UNKNOWN_SUBCOMMAND("error.blacklist.unknownsubcommand", "Unknown subcommand. Valid commands: &a%1%&r"),
        ERROR_BLACKLIST_UNKNOWN_TYPE("error.blacklist.unknowntype", "Unknown type. Valid types: &e%1%&r"),
        ERROR_CLASS_FULL("error.class.full", "The class &a%1%&r is full!"),
        ERROR_CLASS_NOTENOUGHEXP("error.class.notenoughexp", "You don't have enough EXP to choose &a%1%&r!"),
        ERROR_CLASS_NOT_FOUND("error.class.notfound", "Class not found: &a%1%&r"),
        ERROR_CLASS_NOT_GIVEN("error.class.notgiven", "No class was given!"),
        ERROR_REQ_NEEDS_AUTOCLASS("error.class.needsautoclass", "%1% module requires autoclass setting to be defined"),
        ERROR_REQ_NEEDS_JOINDURINGMATCHGOAL("error.class.needsjoinduringmatchgoal", "%1% module requires enabling join.allowDuringMatch and a goal that allows join during match"),
        ERROR_REQ_INCOMPATIBLESETTING("error.class.incompatiblesetting", "%1% module is not compatible with %2% setting"),
        ERROR_COMMAND_BLOCKED("error.cmdblocked", "&cCommand blocked: %1%"),
        ERROR_COMMAND_INVALID("error.invalidcmd", "Invalid command: %1%"),
        ERROR_COMMAND_UNKNOWN("error.unknowncmd", "Unknown command"),
        ERROR_DISABLED("error.arenadisabled", "Arena disabled, please try again later!"),
        ERROR_EDIT_MODE("error.editmode", "Edit mode!"),
        ERROR_ERROR("error.error", "&cError: %1%"),
        ERROR_FIGHT_IN_PROGRESS("error.fightinprogress", "A fight is already in progress!"),
        ERROR_GOAL_NOTFOUND("error.goal.goalnotfound", "Goal &a%1%&& unknown found in &a%2%&&. Valid goals: &a%3%&r"),
        ERROR_INSTALL("error.install", "Error while installing &a%1%&r!"),
        ERROR_INVALID_ARGUMENT_COUNT("error.invalid_argument_count", "&cInvalid number of arguments&r (%1% instead of %2%)!"),
        ERROR_INVALID_VALUE("error.valuenotfound", "Invalid value: &a%1%&r!"),
        ERROR_EXISTING_VALUE("error.existingvalue", "&a%1%&r value already exist in &e%2%&r!"),
        ERROR_NON_EXISTING_VALUE("error.nonexistingvalue", "&a%1%&r value doesn't exist in &e%2%&r!"),
        ERROR_INVENTORY_FULL("error.invfull", "Your inventory was full. You did not receive all rewards!"),
        ERROR_JOIN_ARENA_FULL("error.arenafull", "Arena is full!"),
        ERROR_JOIN_RANGE("error.joinrange", "You are too far away to join this arena!"),
        ERROR_JOIN_REGION("error.notjoinregion", "You are not in the join region! Move there to join!"),
        ERROR_JOIN_MODULE("error.nojoinmodule", "There is no join module in your arena configuration. You have to set one!"),
        ERROR_JOIN_TEAM_FULL("error.teamfull", "Team %1% is full!"),
        ERROR_MAT_NOT_FOUND("error.log.matnotfound", "Unrecognized material: %1%"),
        ERROR_MISSING_SPAWN("error.missingspawn", "Spawn(s) missing: &r%1%"),
        ERROR_MISSING_BLOCK("error.missingblock", "Block(s) missing: &r%1%"),
        ERROR_OUT_OF_BOUNDS_SPAWN("error.outofboudsspawn", "Following spawn(s) are out of bounds of your BATTLE region(s): &r%1%"),
        ERROR_NO_ARENAS("error.noarenas", "No arenas found!"),
        ERROR_NEGATIVES("error.valueneg", "Negative values: &c%1%&r"),
        ERROR_NO_CHEST("error.nochest", "You are not looking at a chest!"),
        ERROR_NO_CONTAINER("error.nocontainer", "You are not looking at a container!"),
        ERROR_NO_GOAL("error.nogoal", "You did not add a goal! &a/pa <arena> goal <goalname>"),
        ERROR_NO_SPAWNS("error.nospawns", "No spawns set!"),
        ERROR_NOPERM_CLASS("error.classperms", "You do not have permission for class &a%1%&r!"),
        ERROR_NOPERM_JOIN("error.permjoin", "You don't have permission to join this arena!"),

        ERROR_NOPERM_X_ADMIN("nopermto.madmin", "administrate"),
        ERROR_NOPERM_X_CREATE("nopermto.create", "create an arena"),
        ERROR_NOPERM_X_DISABLE("nopermto.disable", "disable"),
        ERROR_NOPERM_X_EDIT("nopermto.edit", "edit an arena"),
        ERROR_NOPERM_X_ENABLE("nopermto.enable", "enable"),
        ERROR_NOPERM_X_JOIN("nopermto.nopermjoin", "join an arena"),
        ERROR_NOPERM_X_RELOAD("nopermto.reload", "reload"),
        ERROR_NOPERM_X_REMOVE("nopermto.remove", "remove an arena"),
        ERROR_NOPERM_X_SET("nopermto.set", "set a config node"),
        ERROR_NOPERM_X_TP("nopermto.teleport", "teleport to an arena spawn"),
        ERROR_NOPERM_X_USER("nopermto.user", "use PVP Arena"),


        ERROR_NOPERM_C_BLACKLIST("nopermto.cmds.blacklist", "use the blacklist command"),
        ERROR_NOPERM_C_CHECK("nopermto.cmds.check", "use the check command"),
        ERROR_NOPERM_C_CLASS("nopermto.cmds.class", "use the class command"),
        ERROR_NOPERM_C_CREATE("nopermto.cmds.create", "use the create command"),
        ERROR_NOPERM_C_DEBUG("nopermto.cmds.debug", "use the debug command"),
        ERROR_NOPERM_C_DISABLE("nopermto.cmds.disable", "use the disable command"),
        ERROR_NOPERM_C_EDIT("nopermto.cmds.edit", "use the edit command"),
        ERROR_NOPERM_C_ENABLE("nopermto.cmds.enable", "use the enable command"),
        ERROR_NOPERM_C_GAMEMODE("nopermto.cmds.gamemode", "use the gamemode command"),
        ERROR_NOPERM_C_GOAL("nopermto.cmds.goal", "use the goal command"),
        ERROR_NOPERM_C_PLAYERCLASS("nopermto.cmds.playerclass", "use the playerclass command"),
        ERROR_NOPERM_C_PLAYERJOIN("nopermto.cmds.playerjoin", "use the playerjoin command"),
        ERROR_NOPERM_C_PROTECTION("nopermto.cmds.protection", "use the protection command"),
        ERROR_NOPERM_C_REGION("nopermto.cmds.region", "use the region command"),
        ERROR_NOPERM_C_REGIONFLAG("nopermto.cmds.regionflag", "use the regionflag command"),
        ERROR_NOPERM_C_REGIONS("nopermto.cmds.regions", "use the regions command"),
        ERROR_NOPERM_C_REGIONTYPE("nopermto.cmds.regiontype", "use the regiontype command"),
        ERROR_NOPERM_C_RELOAD("nopermto.cmds.reload", "use the reload command"),
        ERROR_NOPERM_C_REMOVE("nopermto.cmds.remove", "use the remove command"),
        ERROR_NOPERM_C_SET("nopermto.cmds.set", "use the set command"),
        ERROR_NOPERM_C_SETOWNER("nopermto.cmds.setowner", "use the setowner command"),
        ERROR_NOPERM_C_SPAWN("nopermto.cmds.spawn", "use the spawn command"),
        ERROR_NOPERM_C_START("nopermto.cmds.start", "use the start command"),
        ERROR_NOPERM_C_STOP("nopermto.cmds.stop", "use the stop command"),
        ERROR_NOPERM_C_TEAMS("nopermto.cmds.teams", "use the teams command"),
        ERROR_NOPERM_C_TELEPORT("nopermto.cmds.teleport", "use the teleport command"),
        ERROR_NOPERM_C_TEMPLATE("nopermto.cmds.template", "use the template command"),
        ERROR_NOPERM_C_TOGGLEMOD("nopermto.cmds.togglemod", "use the togglemod command"),
        ERROR_NOPERM_C_MODULES("nopermto.cmds.uninstall", "use the modules command"),
        ERROR_NOPERM_C_WHITELIST("nopermto.cmds.whitelist", "use the whitelist command"),
        ERROR_NOPERM_C_ARENACLASS("nopermto.cmds.arenaclass", "use the arenaclass command"),
        ERROR_NOPERM_C_CHAT("nopermto.cmds.chat", "use the chat command"),
        ERROR_NOPERM_C_JOIN("nopermto.cmds.join", "use the join command"),
        ERROR_NOPERM_C_LEAVE("nopermto.cmds.leave", "use the leave command"),
        ERROR_NOPERM_C_SPECTATE("nopermto.cmds.spectate", "use the spectate command"),
        ERROR_NOPERM_C_ARENALIST("nopermto.cmds.arenalist", "use the arenalist command"),
        ERROR_NOPERM_C_HELP("nopermto.cmds.help", "use the help command"),
        ERROR_NOPERM_C_INFO("nopermto.cmds.info", "use the info command"),
        ERROR_NOPERM_C_LIST("nopermto.cmds.list", "use the list command"),
        ERROR_NOPERM_C_READY("nopermto.cmds.ready", "use the ready command"),
        ERROR_NOPERM_C_SHUTUP("nopermto.cmds.shutup", "use the shutup command"),
        ERROR_NOPERM_C_STATS("nopermto.cmds.stats", "use the stats command"),
        ERROR_NOPERM_C_VERSION("nopermto.cmds.version", "use the version command"),

        ERROR_NOPERM("error.noperm", "&cNo permission to %1%!"),
        ERROR_NOPLAYERFOUND("error.noplayerfound", "No player found!"),
        ERROR_NOT_IN_ARENA("error.notinarena", "You are not part of an arena!"),
        ERROR_NOT_NUMERIC("error.notnumeric", "&cArgument not numeric:&r %1%"),
        ERROR_ONLY_PLAYERS("error.onlyplayers", "&cThis command can only be used by players!"),
        ERROR_PLAYER_NOTFOUND("error.playernotfound", "&cPlayer not found: &r%1%&c"),
        ERROR_POSITIVES("error.positives", "Positive values: &b%1%&r"),
        ERROR_POTIONEFFECTTYPE_NOTFOUND("error.potioneffecttypenotfound", "PotionEffectType not found: &e%1%&r"),
        ERROR_NEED_SAME_BLOCK_TYPE("error.needsameblocktype", "All of your blocks must have the same type/material"),

        ERROR_READY_0_ONE_PLAYER_NOT_READY("error.ready.notready0", "At least one player is not ready!"),
        ERROR_READY_1_ALONE("error.ready.notready1", "You are alone in the arena!"),
        ERROR_READY_2_TEAM_ALONE("error.ready.notready2", "Your team is alone in the arena!"),
        ERROR_READY_3_TEAM_MISSING_PLAYERS("error.ready.notready3", "A team is missing players!"),
        ERROR_READY_4_MISSING_PLAYERS("error.ready.notready4", "The arena is missing players!"),
        ERROR_READY_5_ONE_PLAYER_NO_CLASS("error.ready.notready5", "At least one player has not chosen a class!"),

        ERROR_READY_NOCLASS("error.ready.noclass", "You don't have a class!"),
        ERROR_REGION_FLAG_NOTFOUND("error.region.flagnotfound", "RegionFlag &a%1%&r unknown! Valid values: %2%"),
        ERROR_REGION_INVALID("error.region.invalid", "Region selection is invalid. Region will have no volume and will be useless!"),
        ERROR_REGION_NOTFOUND("error.region.notfound", "Region &a%1%&r not found!"),
        ERROR_REGION_PROTECTION_NOTFOUND("error.region.protectionnotfound", "RegionProtection &a%1%&r unknown!"),
        ERROR_REGION_SELECT_2("error.select2", "Select two points before trying to save."),
        ERROR_REGION_TYPE_NOTFOUND("error.region.typenotfound", "RegionType &a%1%&r unknown! Valid values: %2%"),
        ERROR_REGION_YOUSELECT("error.region.youselect", "You are already selecting a region for an arena!"),
        ERROR_REGION_YOUSELECT2("error.region.youselect2", "Type the command again to cancel selection mode!"),
        ERROR_REGION_YOUSELECTEXIT("error.region.youselectexit", "Region selection cancelled!"),


        ERROR_TEAM_NOT_FOUND("error.teamnotfound", "Team not found: &a%1%&r"),
        ERROR_NO_TEAM_AVAILABLE("error.noteamavailable", "No Team available."),
        ERROR_UNINSTALL("error.uninstall", "Error while uninstalling: &a%1%&r"),
        ERROR_UNINSTALL2("error.uninstall2", "PVP Arena will try to uninstall on server restart!"),
        ERROR_UNKNOWN_MODULE("error.unknownmodule", "Module not found: %1%"),
        ERROR_WHITELIST_DISALLOWED("error.whitelist.disallowed", "You may not %1% this! (not whitelisted)"),
        ERROR_WHITELIST_UNKNOWN_SUBCOMMAND("error.whitelist.unknownsubcommand", "Unknown subcommand. Valid commands: &a%1%&r"),
        ERROR_WHITELIST_UNKNOWN_TYPE("error.whitelist.unknowntype", "Unknown type. Valid types: &e%1%&r"),

        FIGHT_BEGINS("fight.begins", "Let the fight begin!"),
        FIGHT_DRAW("fight.draw", "This match was a draw! No winners!"),
        FIGHT_KILLED_BY_REMAINING("fight.killedbyremaining", "%1% has been killed by %2%! %3% lives remaining."),
        FIGHT_KILLED_BY_REMAINING_FRAGS("fight.killedbyremainingfrags", "%1% has been killed by %2%! %3% kills remaining."),
        FIGHT_KILLED_BY_REMAINING_TEAM("fight.killedbyremainingteam", "%1% has been killed by %2%! %3% lives remaining for %4%."),
        FIGHT_KILLED_BY_REMAINING_TEAM_FRAGS("fight.killedbyremainingteamfrags", "%1% has been killed by %2%! %3% kills remaining for %4%."),
        FIGHT_KILLED_BY("fight.killedby", "%1% has been killed by %2%!"),
        FIGHT_PLAYER_LEFT("fight.playerleft", "%1% has left the fight!"),

        PERMISSION_BREAK("permission.break", "break"),
        PERMISSION_PLACE("permission.place", "place"),

        INFO_CLASSES("info.classes", "Classes: &a%1%&r"),
        INFO_GOAL_ACTIVE("info.goal_active", "Goal: &a%1%&r"),
        INFO_HEAD_HEADLINE("info.head_headlin", "Arena Information about: &a%1%&r | [&a%2%&r]"),
        INFO_HEAD_TEAMS("info.head_teams", "Teams: &a%1%&r"),
        INFO_MOD_ACTIVE("info.mod_active", "Module: &a%1%&r"),
        INFO_OWNER("info.owner", "Owner: &a%1%&r"),
        INFO_REGIONS("info.regions", "Regions: &a%1%&r"),
        INFO_SECTION("info.section", "----- &a%1%&r -----"),

        GENERAL_INSTALL_DONE("general.installed", "Installed: &a%1%&r"),
        GENERAL_UNINSTALL_DONE("general.uninstalled", "Uninstalled: &a%1%&r"),
        GENERAL_PLUGIN_DISABLED("general.plugindisabled", "disabled (version %1%)"),
        GENERAL_PLUGIN_ENABLED("general.pluginenabled", "enabled (version %1%)"),

        LIST_ARENAS("list.arenas", "Available arenas: %1%"),
        LIST_DEAD("list.dead", "Dead: %1%"),
        LIST_FIGHTING("list.fighting", "Fighting: %1%"),
        LIST_LOST("list.lost", "Lost: %1%"),
        LIST_LOUNGE("list.lounge", "Lounge: %1%"),
        LIST_NULL("list.null", "Glitched: %1%"),
        LIST_PLAYERS("list.players", "Players: %1%"),
        LIST_TEAM("list.team", "Team %1%: %2%"),
        LIST_READY("list.ready", "Ready: %1%"),
        LIST_WARM("list.warm", "Warm: %1%"),
        LIST_WATCHING("list.watching", "Watching: %1%"),

        MESSAGES_TOARENA("messages.toArena", "You are now talking to the arena!"),
        MESSAGES_TOPUBLIC("messages.toPublic", "You are now talking to the public!"),
        MESSAGES_TOTEAM("messages.toTeam", "You are now talking to your team!"),
        MESSAGES_GENERAL("messages.general", "&e[%1%&e] &r%2%"),

        NOTICE_CLOSED_SELECTION("notice.closedselection","&eSelection mode has been closed"),
        NOTICE_NO_DROP_ITEM("notice.nodropitem", "Not so fast! No cheating!"),
        NOTICE_NO_TELEPORT("notice.noteleport", "Please use '/pa leave' to exit the fight!"),
        NOTICE_NOTICE("notice.notice", "Notice: %1%"),
        NOTICE_REWARDEDPLAYER("notice.rewardedplayer", "%1% has earned %2%."),
        NOTICE_REMOVE("notice.remove", "&cThis will permanently remove the arena &a%1%&c. Are you sure? Then commit the command again!&r To disable this message, see 'safeadmin' in your config.yml!"),
        NOTICE_WAITING_EQUAL("notice.waitingequal", "Waiting for the teams to have an equal player number!"),
        NOTICE_WAITING_FOR_ARENA("notice.waitingforarena", "Waiting for a running arena to finish!"),
        NOTICE_WELCOME_SPECTATOR("notice.welcomespec", "Welcome to the spectator's area!"),
        NOTICE_YOU_DEATH("notice.youdeath", "You entered a DEATH region. Goodbye!"),
        NOTICE_YOU_ESCAPED("notice.youescaped", "You escaped the battlefield. Goodbye!"),
        NOTICE_ARENA_BOUNDS("notice.arenabounds", "You reached bounds of the arena."),
        NOTICE_ARENA_INTRUSION("notice.arenaintrusion", "This is an area closed to visitors. If you want to play this arena, type &e/pa %1%&r. If you want to spectate, type &e/pa %1% spectate&r."),
        NOTICE_YOU_LEFT("notice.youleft", "You left the arena!"),
        NOTICE_YOU_NOCAMP("notice.younocamp", "You are in a NOCAMP region. Move!"),

        ANNOUNCE_PLAYER_HAS_WON("announce.playerhaswon", "%1% is the Champion!"),
        ANNOUNCE_PLAYER_READY("announce.playerready", "%1%&e is ready!"),

        PLAYER_PREVENTED_BREAK("player.prevented.break", "&cYou may not break blocks!"),
        PLAYER_PREVENTED_PLACE("player.prevented.place", "&cYou may not place blocks!"),
        PLAYER_PREVENTED_TNT("player.prevented.tnt", "&cYou may not use TNT!"),
        PLAYER_PREVENTED_TNTBREAK("player.prevented.tntbreak", "&cYou may not break TNT!"),
        PLAYER_PREVENTED_DROP("player.prevented.drop", "&cYou may not drop items!"),
        PLAYER_PREVENTED_INVENTORY("player.prevented.inventory", "&cYou may not access this!"),
        PLAYER_PREVENTED_CRAFT("player.prevented.craft", "&cYou may not craft!"),

        READY_LIST("ready.list", "Players: %1%"),
        READY_DONE("ready.done", "You have been flagged as ready!"),

        REGION_CLEAR_ADDED("region.clear.added", "Added to region entity clearing whitelist: &a%1%&r"),
        REGION_CLEAR_LIST("region.clear.list", "Region entity clearing whitelist: &a%1%&r"),
        REGION_CLEAR_REMOVED("region.clear.removed", "Removed from region entity clearing whitelist: &a%1%&r"),
        REGION_FLAG_ADDED("region.flag.added", "Region flag added: &a%1%&r"),
        REGION_FLAG_REMOVED("region.flag.removed", "Region flag removed: &a%1%&r"),
        REGION_EXPANDED("region.expanded", "Region has been extended &a%1%&r blocks to the &a%2%&r direction"),
        REGION_CONTACTED("region.contracted", "Region has been reduced by &a%1%&r blocks to the &a%2%&r direction"),
        REGION_POS1("region.pos1", "First position set."),
        REGION_POS2("region.pos2", "Second position set."),
        REGION_PROTECTION_ADDED("region.protection_added", "RegionProtection added: &a%1%&r"),
        REGION_PROTECTION_REMOVED("region.protection_removed", "RegionProtection removed: &a%1%&r"),
        REGION_SHIFTED("region.shifted", "Region has been moved &a%1%&r blocks to the &a%2%&r direction"),
        REGION_REMOVED("region.removed", "Region removed: %1%"),
        REGION_SAVED("region.saved", "Region saved."),
        REGION_SAVED_NOTICE("region.saved_notice", "&6You created a &oCUSTOM&6 region. It has no function yet! To turn it into a battlefield region, type &r/pvparena %1% !rt %2% BATTLE."),
        REGION_SELECT("region.select", "Select two points with your wand item, left click first and then right click!"),
        REGION_TYPE_SET("region.typeset", "Region Type set: &e%1%"),
        REGION_YOUSELECT("region.youselect", "You are now selecting a region for arena &a%1%&r!"),
        REGION_SHAPE_UNKNOWN("region.shapeunknown", "Arena Shape '%1%' unknown. Available shapes: Cuboid, Spheric, Cylindric."),

        REGIONS_FLAGS("regions.flags", "Region Flags: &a%1%&r"),
        REGIONS_HEAD("regions.head", "--- &aArena Region&r [&e%1%&r]---"),
        REGIONS_LISTHEAD("regions.listhead", "--- &aArena Regions&r [&e%1%&r]---"),
        REGIONS_LISTVALUE("regions.listvalue", "&a%1%&r: %2%, %3%"),
        REGIONS_PROTECTIONS("regions.protections", "Region Protections: &a%1%&r"),
        REGIONS_SHAPE("regions.shape", "Region Shape: &a%1%&r"),
        REGIONS_TYPE("regions.type", "Region Type: &a%1%&r"),

        CFG_RELOAD_DONE("cfg.reloaded", "&2Config of %1% reloaded."),
        CFG_RELOAD_FAILED("cfg.reloadfail", "&cFail to reload arena %1%.&f Please check the arena config."),
        LANG_RELOAD_DONE("lang.reloaded", "Languages reloaded!"),

        CFG_SET_DONE("cfg.set.done", "&a%1%&r set to &e%2%&r!"),
        CFG_ADD_DONE("cfg.add.done", "&e%2%&r added to &a%1%&r!"),
        CFG_REMOVE_DONE("cfg.remove.done", "&e%2%&r removed from &a%1%&r!"),
        CFG_SET_HELP("cfg.set.help", "Use /pa <arena> set [page] to get a node list."),
        CFG_SET_UNKNOWN("cfg.set.unknown", "Unknown node: &e%1%&r!"),
        CFG_SET_ITEMS_NOT("cfg.set.items_not", "Please use either hand or inventory to set an item node!"),

        STATS_HEAD("stats.head", "Statistics TOP %1% (%2%)"),
        STATS_TYPENOTFOUND("stats.typenotfound", "Statistics type not found! Valid values: &e%1%&r"),

        STATTYPE_DAMAGE("stattype.DAMAGE", StatEntry.DAMAGE.getLabel()),
        STATTYPE_DAMAGE_TAKEN("stattype.DAMAGE_TAKEN", StatEntry.DAMAGE_TAKEN.getLabel()),
        STATTYPE_DEATHS("stattype.DEATHS", StatEntry.DEATHS.getLabel()),
        STATTYPE_KILLS("stattype.KILLS", StatEntry.KILLS.getLabel()),
        STATTYPE_LOSSES("stattype.LOSSES", StatEntry.LOSSES.getLabel()),
        STATTYPE_MAX_DAMAGE("stattype.MAX_DAMAGE", StatEntry.MAX_DAMAGE.getLabel()),
        STATTYPE_MAX_DAMAGE_TAKEN("stattype.MAX_DAMAGE_TAKEN", StatEntry.MAX_DAMAGE_TAKEN.getLabel()),
        STATTYPE_WINS("stattype.WINS", StatEntry.WINS.getLabel()),

        TEAM_HAS_WON("team.haswon", "Team %1%&r are the Champions!"),
        TEAM_READY("team.ready", "Team %1%&r is ready!"),
        
        TEAMS_LIST("teams.list", "Available teams: %1%"),
        TEAMS_ADD("teams.add", "Team added: %1%"),
        TEAMS_SET("teams.set", "Team set: %1%"),
        TEAMS_REMOVE("teams.remove", "Team removed: %1%"),

        TIME_MINUTES("time.minutes", "minutes"),
        TIME_SECONDS("time.seconds", "seconds"),

        TIMER_COUNTDOWN_INTERRUPTED("timer.countdowninterrupt", "Countdown interrupted! Waiting for ready players..."),
        TIMER_ENDING_IN("timer.ending", "The match will end in %1%!"),
        TIMER_RESETTING_IN("timer.resetting", "The arena will reset in %1%!"),
        TIMER_WARMINGUP("timer.warmingup", "Warming up... %1%!"),
        TIMER_PVPACTIVATING("timer.pvpactivating", "PVP will be activated in %1%!"),

        UPDATER_PLUGIN("updater.plugin", "PVP Arena"),
        UPDATER_MODULES("updater.modules", "PVP Arena modules pack"),
        UPDATER_ANNOUNCE("updater.announce", "%1% %2% is now available ! Your version: %3%"),
        UPDATER_SUCCESS("updater.success", "%1% has been updated to %2%."),
        UPDATER_RESTART("updater.restart", "Restart your server to apply update."),
        UPDATER_DOWNLOADING("updater.downloading", "Downloading %1%..."),
        UPDATER_DOWNLOAD_ERROR("updater.downloaderror", "Error while downloading %1%"),

        GOAL_BLOCKDESTROY_SCORE("goal.blockdestroy.score", "%1% destroyed the block of team %2%! Remaining destructions: %3%"),
        GOAL_BLOCKDESTROY_SET("goal.blockdestroy.setblock", "Block successfully set for team %1%&r"),
        GOAL_BLOCKDESTROY_TOSET("goal.blockdestroy.tosetflag", "Left click the block for team %1%&r"),
        GOAL_BLOCKDESTROY_NOTFOUND("goal.blockdestroy.notfound", "Block can not be found for team %1%"),
        GOAL_BLOCKDESTROY_REMOVED("goal.blockdestroy.removed", "Block was removed for team %1%"),

        GOAL_CHECKPOINTS_SCORE("goal.checkpoints.score", "%1% &ereached checkpoint #%2%!"),
        GOAL_CHECKPOINTS_YOUMISSED("goal.checkpoints.youmissed", "You missed checkpoint #%1%! This is #%2%"),

        GOAL_DOMINATION_BOSSBAR_CLAIMING("goal.dom.bossbar_claiming", "Claiming..."),
        GOAL_DOMINATION_BOSSBAR_UNCLAIMING("goal.dom.bossbar_claiming", "Unclaiming..."),
        GOAL_DOMINATION_CLAIMING("goal.dom.claiming", "&eTeam %1% is claiming a flag!"),
        GOAL_DOMINATION_CLAIMED("goal.dom.claimed", "&eTeam %1% has claimed a flag!"),
        GOAL_DOMINATION_SCORE("goal.dom.score", "&eTeam %1% scored %2% points by holding a flag!"),
        GOAL_DOMINATION_CONTESTING("goal.dom.contesting", "&eA flag claimed by team %1% is being contested!"),
        GOAL_DOMINATION_UNCLAIMING("goal.dom.unclaiming", "&eA flag claimed by team %1% is being unclaimed!"),
        GOAL_DOMINATION_SET_FLAG("goal.dom.setflag", "&eClick on flags to register them, they need to be colorable blocks. Type this command again to close selection mode."),
        GOAL_DOMINATION_EXISTING_FLAG("goal.dom.existingflag", "&eThis flag was already set"),

        GOAL_FLAGS_BROUGHTHOME("goal.flag.flaghomeleft", "%1% brought home the flag of team %2%! Captures remaining: %3%"),
        GOAL_FLAGS_TOUCHHOME("goal.flag.touchhomeleft", "%1% brought home the touchdown flag! Other teams loses one life!"),
        GOAL_FLAGS_DROPPED("goal.flag.flagsave", "%1% dropped the flag of team %2%!"),
        GOAL_FLAGS_DROPPEDTOUCH("goal.flag.flagsavetouch", "%1% dropped the touchdown flag!"),
        GOAL_FLAGS_GRABBED("goal.flag.flaggrab", "%1% grabbed the flag of team %2%!"),
        GOAL_FLAGS_GRABBEDTOUCH("goal.flag.flaggrabtouch", "%1% grabbed the touchdown flag!"),
        GOAL_FLAGS_NOTSAFE("goal.flag.flagnotsafe", "Your flag is taken! Cannot bring back an enemy flag!'"),
        GOAL_FLAGS_SET("goal.flag.setflag", "%1% flag has been set"),
        GOAL_FLAGS_TOSET("goal.flag.tosetflag", "Left-click the desired flag for the %1% team"),
        GOAL_FLAGS_NOTFOUND("goal.flag.notfound", "Flag not found: &e%1%"),
        GOAL_FLAGS_REMOVED("goal.flag.removed", "Flag &e%1%&r has been removed"),

        GOAL_FOOD_NOTYOURFOOD("goal.food.notyourfood", "This is not your furnace!"),
        GOAL_FOOD_TOSET("goal.food.tosetfood", "&eClick on each &f%1%&e to register it. Then, type this command again to close selection mode"),
        GOAL_FOOD_NOTFOUND("goal.food.notfound", "Block not found: %1% %2% #%3%"),
        GOAL_FOOD_REMOVED("goal.food.removed", "%1% %2% #%3% has been removed"),
        GOAL_FOOD_EXISTING_BLOCK("goal.food.existingblock", "This block has already been set for team %1%"),
        GOAL_FOOD_FURNACE_SET("goal.food.foodfurnaceset", "Furnace #%1% set for team %2%"),
        GOAL_FOOD_CHEST_SET("goal.food.foodchestset", "Food chest #%1% set for team %2%"),
        GOAL_FOOD_ITEMS_PUT("goal.food.itemsput", "Team %1% has put %2% items in their chest! Score: %3%/%4%"),
        GOAL_FOOD_ITEMS_REMOVED("goal.food.itemsremoved", "Team %1% has removed %2% items from their chest! Score: %3%/%4%"),

        GOAL_INFECTED_LOST("goal.infected.lost", "&6The infected players have been killed!"),
        GOAL_INFECTED_PLAYER("goal.infected.player", "&c%1% is infected!"),
        GOAL_INFECTED_YOU("goal.infected.you", "&cYou are infected!"),
        GOAL_INFECTED_WON("goal.infected.won", "&6The infected players have won the game!"),
        GOAL_INFECTED_IPROTECT("goal.infected.iprotect", "The infected team is prevented from: %1%"),
        GOAL_INFECTED_IPROTECT_SET("goal.infected.iprotectset", "&ePlayerProtection &f%1%&f set to: %2%"),

        GOAL_KILLREWARD_ADDED("goal.killreward.added", "Kill reward added: &e%1%&r->&a%2%"),
        GOAL_KILLREWARD_REMOVED("goal.killreward.removed", "Kill reward removed: &e%1%"),

        GOAL_LIBERATION_LIBERATED("goal.liberation.liberated", "Team %1% has been liberated!"),
        GOAL_LIBERATION_SET("goal.liberation.setbutton", "%1% button has been set"),
        GOAL_LIBERATION_SCOREBOARD_HEADING("goal.liberation.scoreboardheading", "Players in jail:"),
        GOAL_LIBERATION_SCOREBOARD_SEPARATOR("goal.liberation.scoreboardseparator", "----------------"),
        GOAL_LIBERATION_TOSET("goal.liberation.tosetbutton", "Click on the button of the %1% team to register it"),
        GOAL_LIBERATION_BTN_NOTFOUND("goal.liberation.buttonnotfound", "%1% button doesn't exist"),
        GOAL_LIBERATION_BTN_REMOVED("goal.liberation.buttonremoved", "%1% button has been removed"),

        GOAL_PHYSICALFLAGS_HOLDFLAG("goal.physicalflags.holdflag", "You have to hold the flag to bring it back!"),

        GOAL_SABOTAGE_IGNITED("goal.sabotage.tntignite", "%1% ignited the TNT of team %2%!"),
        GOAL_SABOTAGE_SETTNT("goal.sabotage.set", "%1% TNT has been set"),
        GOAL_SABOTAGE_TOSET("goal.sabotage.toset", "Click on the TNT of the %1% team to register it"),
        GOAL_SABOTAGE_NOSELFDESTROY("goal.sabotage.noselfdestroy", "You can not ignite your own TNT!"),
        GOAL_SABOTAGE_NOTGOODITEM("goal.sabotage.notgooditem", "You need a flint and steel to ignite the TNT."),
        GOAL_SABOTAGE_YOUTNT("goal.sabotage.youtnt", "You now carry the sabotage tool."),
        GOAL_SABOTAGE_TNT_NOTFOUND("goal.sabotage.tntnotfound", "%1% tnt doesn't exist"),
        GOAL_SABOTAGE_TNT_REMOVED("goal.sabotage.tntremoved", "%1% tnt has been removed"),

        GOAL_TANK_TANKDOWN("goal.tank.tankdown", "The tank is down!"),
        GOAL_TANK_TANKMODE("goal.tank.tankmode", "TANK MODE! Everyone kill %1%, the tank!"),
        GOAL_TANK_TANKWON("goal.tank.tankwon", "The tank has won! Congratulations to %1%!"),

        GOAL_TEAMDEATHCONFIRM_DENIED("goal.tdc.denied", "%1% denied a kill!"),
        GOAL_TEAMDEATHCONFIRM_REMAINING("goal.tdc.remaining", "%1% kills remaining for %2%."),
        GOAL_TEAMDEATHCONFIRM_SCORED("goal.tdc.scored", "%1% scored a kill!"),
        GOAL_TEAMDEATHCONFIRM_YOUDENIED("goal.tdc.youdenied", "You denied a kill!"),
        GOAL_TEAMDEATHCONFIRM_YOUSCORED("goal.tdc.youscored", "You scored a kill!"),

        // -----------------------------------------------

        MODULE_AFTERMATCH_STARTING("mod.aftermatch.aftermatch", "The aftermatch has begun!"),
        MODULE_AFTERMATCH_STARTINGIN("mod.aftermatch.startingin", "AfterMatch in %1%!"),
        MODULE_AFTERMATCH_SPAWNNOTSET("mod.aftermatch.spawnnotset", "Spawn 'after' not set!"),

        MODULE_ANNOUNCEMENTS_IGNOREON("mod.announcements.ignoreon", "You are now ignoring announcements!"),
        MODULE_ANNOUNCEMENTS_IGNOREOFF("mod.announcements.ignoreoff", "You are now receiving announcements!"),

        MODULE_BATTLEFIELDJOIN_REMAININGTIME("mod.battlefieldjoin.lang.remainingtime", "Players can continue joining the arena during the next %1% seconds"),

        MODULE_BANKICK_BANNED("mod.bankick.lang.playerbanned", "Player banned: %1%"),
        MODULE_BANKICK_KICKED("mod.bankick.lang.playerkicked", "Player kicked: %1%"),
        MODULE_BANKICK_NOTONLINE("mod.bankick.lang.playernotonline", "Player is not online: %1%"),
        MODULE_BANKICK_UNBANNED("mod.bankick.lang.playerunbanned", "Player unbanned: %1%"),
        MODULE_BANKICK_YOUBANNED("mod.bankick.lang.youwerebanned", "You are banned from arena %1%!"),
        MODULE_BANKICK_YOUKICKED("mod.bankick.lang.youwerekicked", "You were kicked from arena %1%!"),
        MODULE_BANKICK_YOUUNBANNED("mod.bankick.lang.youwereunbanned", "You are unbanned from arena %1%!"),

        MODULE_BETTERCLASSES_ADD("mod.betterclasses.add", "PotionEffect &e%2%&r added to ArenaClass &e%1%&r!"),
        MODULE_BETTERCLASSES_CLEAR("mod.betterclasses.clear", "ArenaClass &e%1%&r cleared!"),
        MODULE_BETTERCLASSES_LISTHEAD("mod.betterclasses.listhead", "--- Potion Effects for class &e%1%&r ---"),
        MODULE_BETTERCLASSES_REMOVE("mod.betterclasses.remove", "PotionEffect &e%2%&r removed from ArenaClass &e%1%&r!"),
        MODULE_BETTERCLASSES_RESPAWNCOMMAND_REMOVED("mod.betterclasses.respawncommand_remove", "Respawn command removed from ArenaClass &e%1%&r!"),
        MODULE_BETTERCLASSES_CLASSCHANGE_MAXTEAM("mod.betterclasses.classchange.mteam",
                "&cYour team has exceeded the class change limit!"),
        MODULE_BETTERCLASSES_CLASSCHANGE_MAXPLAYER("mod.betterclasses.classchange.mplayer",
                "&cYou have exceeded the class change limit!"),
        
        MODULE_BETTERGEARS_SHOWTEAM("mod.bettergears.showteam", "Team %1% has Color %2%."),
        MODULE_BETTERGEARS_TEAMDONE("mod.bettergears.teamdone", "Team %1% now has Color %2%."),

        MODULE_BLOCKRESTORE_CLEARINVDONE("mod.blockrestore.clearinvdone", "Inventories cleared! Expect lag on next arena start!"),
        MODULE_BLOCKRESTORE_ADDEDTOLIST("mod.blockrestore.addedtolist", "Container (%1%) has been added to inventories to restore!"),

        MODULE_CHESTFILLER_SOURCECHEST("mod.chestfiller.sourceChest", "The container at \"%1%\" becomes the new source chest of the arena."),
        MODULE_CHESTFILLER_SOURCECHEST_REMOVED("mod.chestfiller.removedSource", "The source container of the arena has been removed, now using 'chestfiller.items' config to fill chests."),
        MODULE_CHESTFILLER_CLEAR("mod.chestfiller.clear", "List of chests to fill has been cleared!"),
        MODULE_CHESTFILLER_ADDEDTOLIST("mod.chestfiller.addedToList", "Successfully added to the list to be filled: %1%"),

        MODULE_DUEL_ACCEPTED("mod.duel.accepted", "%1% &eaccepted the challenge! The game is starting."),
        MODULE_DUEL_ANNOUNCE("mod.duel.announce", "%1% &echallenged you! Accept the duel with &r/pa %2% accept&e."),
        MODULE_DUEL_ANNOUNCEMONEY("mod.duel.announcemoney", "&eThey set up a fee of &c%1%&e!"),
        MODULE_DUEL_ANNOUNCE2("mod.duel.announce2", "&eCancel the duel with &r/pa %2% decline&e."),
        MODULE_DUEL_CANCELLED("mod.duel.cancelled","&cThe duel has been cancelled!"),
        MODULE_DUEL_BUSY("mod.duel.busy", "%1% &eis already in a fight Please try again later."),
        MODULE_DUEL_DECLINED_SENDER("mod.duel.declineds", "Your opponent declined the request. The duel has been cancelled."),
        MODULE_DUEL_DECLINED_RECEIVER("mod.duel.declinedr", "You declined the duel request!"),
        MODULE_DUEL_REQUESTED("mod.duel.requested", "You &echallenged &r%1%&e!"),
        MODULE_DUEL_REQUESTED_ALREADY("mod.duel.requestedalready", "You already have challenged someone!"),
        MODULE_DUEL_REQUEST_EXPIRED_SENDER("mod.duel.requestexpireds", "Your opponent did not accept the request in time. The duel has been cancelled."),
        MODULE_DUEL_REQUEST_EXPIRED_RECEIVER("mod.duel.requestexpiredr", "You did not accept the request in time. The duel has been cancelled."),
        MODULE_DUEL_STARTING("mod.duel.starting", "The duel begins!"),
        MODULE_DUEL_NODIRECTJOIN("mod.duel.nodirectjoin", "You can't join this arena directly! Duel someone with: &e/pa %1% duel <player>"),

        MODULE_LATELOUNGE_ANNOUNCE("mod.latelounge.announce", "Arena %1% is starting! Player %2% wants to start. Join with: /pa %1%"),
        MODULE_LATELOUNGE_POSITION("mod.latelounge.position", "You are in queue. Position: #%1%"),
        MODULE_LATELOUNGE_REJOIN("mod.latelounge.rejoin", "Ready check has caught you not being able to join. Rejoin when you can!"),
        MODULE_LATELOUNGE_WAIT("mod.latelounge.wait", "Arena will be starting soon, please wait!"),
        MODULE_LATELOUNGE_LEAVE("mod.latelounge.leave", "You have left the queue of the %1% arena."),

        MODULE_PLAYERFINDER_NEAR("mod.playerfinder.near", "Nearest player: %1% blocks!"),
        MODULE_PLAYERFINDER_POINT("mod.playerfinder.point", "Compass pointing to nearest player!"),

        MODULE_POWERUPS_INVALIDPUEFF("mod.powerups.invalidpowerupeffect", "Invalid PowerupEffect: %1%"),
        MODULE_POWERUPS_PLAYER("mod.powerups.puplayer", "%1% has collected PowerUp: %2%"),
        MODULE_POWERUPS_SERVER("mod.powerups.puserver", "PowerUp deployed!"),
        MODULE_POWERUPS_ADD_LIVES_PLAYER("mod.powerups.livesplayer", "Thanks to a PowerUp, %1% has got %2% lives/points!"),
        MODULE_POWERUPS_ADD_LIVES_TEAM("mod.powerups.livesteams", "Thanks to a PowerUp, %1% team has got %2% lives/points!"),
        MODULE_POWERUPS_REM_LIVES_PLAYER("mod.powerups.livesplayer", "Due to a PowerUp, %1% has lost %2% lives/points!"),
        MODULE_POWERUPS_REM_LIVES_TEAM("mod.powerups.livesteams", "Due to a PowerUp, %1% team has lost %2% lives/points!"),
        MODULE_POWERUPS_FROZEN("mod.powerups.frozen", "You are frozen for the next %1% seconds!"),

        MODULE_REALSPECTATE_INFO("mod.realspectate.info", "You're spectating in immersive view! Sneak to switch to next fighter and press \"throw item\" key to switch back to previous one."),

        MODULE_RESPAWNRELAY_RESPAWNING("mod.respawnrelay.respawning", "Respawning in %1%!"),
        MODULE_RESPAWNRELAY_CHOICE("mod.respawnrelay.choice", "If you want to respawn to a particular spawn point, just type the spawn number in your chat. Available spawns: %1%"),
        MODULE_RESPAWNRELAY_CHOSEN("mod.respawnrelay.chosen", "Your next spawn point choice has been registered."),

        MODULE_SKINS_DISGUISECRAFT("mod.skins.dc", "Hooking into DisguiseCraft!"),
        MODULE_SKINS_LIBSDISGUISE("mod.skins.ld", "Hooking into LibsDisguises!"),
        MODULE_SKINS_NOMOD("mod.skins.nomod", "No disguise plugin found, Skins module is inactive!"),
        MODULE_SKINS_SHOWCLASS("mod.skins.showclass", "Class &e%1%&r will be disguised to: &a%2%"),
        MODULE_SKINS_SHOWTEAM("mod.skins.showteam", "Team %1% will be disguised to: &a%2%"),

        MODULE_SPAWNCOLLECTIONS_SAVED("mod.spawncollections.saved", "The spawn collection \"%1%\" has been saved"),
        MODULE_SPAWNCOLLECTIONS_USE("mod.spawncollections.use", "As of now, arena uses spawns of \"%1%\" spawn collection"),
        MODULE_SPAWNCOLLECTIONS_NOTEXIST("mod.spawncollections.notexist", "Spawn collection \"%1%\" doesn't exist"),
        MODULE_SPAWNCOLLECTIONS_REMOVED("mod.spawncollections.removed", "Spawn collection \"%1%\" has been removed"),
        MODULE_SPAWNCOLLECTIONS_LIST("mod.spawncollections.list", "Saved spawn collections for arena: %1%"),
        MODULE_SPAWNCOLLECTIONS_EMPTY("mod.spawncollections.empty", "There's no saved spawn collection"),

        MODULE_SQUADS_NOSQUAD("mod.squads.nosquad", "No squads loaded! Add some: /pa <arena> !sq add <name>"),
        MODULE_SQUADS_LISTHEAD("mod.squads.listhead", "Squads for arena &b%1%"),
        MODULE_SQUADS_LISTITEM("mod.squads.listitem", "Squad %1% (max: %2%) %3%"),
        MODULE_SQUADS_ADDED("mod.squads.added", "Squad %1% has been added"),
        MODULE_SQUADS_SET("mod.squads.set", "Squad %1% has been set"),
        MODULE_SQUADS_REMOVED("mod.squads.removed", "Squad %1% has been removed"),
        MODULE_SQUADS_NOTEXIST("mod.squads.notexist", "Squad %1% doesn't exist!"),
        MODULE_SQUADS_JOIN("mod.squads.join", "You have joined the squad &b%1%&r!"),
        MODULE_SQUADS_LEAVE("mod.squads.leave", "You left the squad &b%1%&r!"),
        MODULE_SQUADS_ERROR("mod.squads.error", "Error while editing squads, syntax is not correct!"),
        MODULE_SQUADS_FULL("mod.squads.full", "This squad is full!"),
        MODULE_SQUADS_HELP("mod.squads.help", "/pa !sq | show the arena squads\n/pa !sq add <name> <limit> | add squad with player limit (set to 0 for no limit)\n/pa !sq set <name> <limit> | set player limit for squad\n/pa !sq remove <name> | remove squad <name>"),

        MODULE_STARTFREEZE_ANNOUNCE("mod.startfreeze.announce", "The game will start in %1% seconds!"),

        MODULE_TEMPPERMS_NOPERMS("mod.tempperms.noperms", "Permissions plugin not found, defaulting to OP."),
        MODULE_TEMPPERMS_HEAD("mod.tempperms.head", "Temporary permissions of &e%1%&r:"),
        MODULE_TEMPPERMS_ADDED("mod.tempperms.added", "Temporary permissions &e%1%&r added to &a%2%&r."),
        MODULE_TEMPPERMS_REMOVED("mod.tempperms.removed", "Temporary permissions &e%1%&r removed from &a%2%&r."),

        MODULE_VAULT_NOTENOUGH("mod.vault.notenough", "You don't have %1%."),
        MODULE_VAULT_NOACCOUNT("mod.vault.noaccount", "A bank account is required to use this command or play this arena."),
        MODULE_VAULT_THEYNOTENOUGH("mod.vault.theynotenough", "%1% doesn't have enough cash!"),
        ERROR_VAULT_BETTIMEOVER("mod.vault.bettimeover", "Betting time is over!"),
        ERROR_VAULT_BEFOREBETTIME("mod.vault.beforebettime", "Betting time is not started yet! Please wait until beginning of the match."),
        MODULE_VAULT_BETNOTYOURS("mod.vault.betnotyours", "Cannot place bets on your own match!"),
        MODULE_VAULT_BETONLYTEAMS("mod.vault.betonlyteams", "You can only bet on (non-empty) teams!"),
        MODULE_VAULT_BETONLYPLAYERS("mod.vault.betonlyplayers", "You can only bet on players!"),
        MODULE_VAULT_WRONGAMOUNT("mod.vault.wrongamount", "Bet amount must be between %1% and %2%!"),
        MODULE_VAULT_INVALIDAMOUNT("mod.vault.invalidamount", "Invalid amount: %1%"),
        MODULE_VAULT_BETPLACED("mod.vault.betplaced", "Your %1% bet on %2% has been placed."),
        MODULE_VAULT_BETCHANGED("mod.vault.betchanged", "Your bet has been changed. From now on, you bet %1% on %2%."),
        MODULE_VAULT_YOUEARNED("mod.vault.youearned", "You have earned %1%!"),
        MODULE_VAULT_JOINPAY("mod.vault.joinpay", "You paid %1% to join the arena!"),
        MODULE_VAULT_KILLREWARD("mod.vault.killreward", "You received %1% for killing %2%!"),
        MODULE_VAULT_REFUNDING("mod.vault.refunding", "Refunding %1%!"),

        MODULE_WALLS_FALLINGIN("mod.walls.fallingin", "Walls fall in: %1%"),
        MODULE_WALLS_SEPARATOR("mod.walls.separator", "--------------------"),
        MODULE_WALLS_TIMER("mod.walls.timer", "Walls will be removed in %1%!"),


        MODULE_WORLDEDIT_CREATED("mod.worldedit.created", "Region created: &e%1%&r"),
        MODULE_WORLDEDIT_LIST_ADDED("mod.worldedit.list.added", "&e%1%&r has been added to region list."),
        MODULE_WORLDEDIT_LIST_REMOVED("mod.worldedit.list.removed", "&e%1%&r has been removed from region list."),
        MODULE_WORLDEDIT_LIST_SHOW("mod.worldedit.list.show", "These regions will be automatically saved or loaded (depending of autosave/autoload config): &e%1%&r"),
        MODULE_WORLDEDIT_LIST_EMPTY("mod.worldedit.list.empty", "No region has been added to the list. So, all BATTLE regions will be saved or loaded instead (depending of autosave/autoload config)."),
        MODULE_WORLDEDIT_LOADED("mod.worldedit.loaded", "Region loaded: &e%1%&r"),
        MODULE_WORLDEDIT_SAVED("mod.worldedit.saved", "Region saved: &e%1%&r");

        private final String node;
        private String value;

        public static MSG getByNode(final String node) {
            for (MSG m : MSG.values()) {
                if (m.node.equals(node)) {
                    return m;
                }
            }
            return null;
        }

        MSG(final String node, final String value) {
            this.node = node;
            this.value = value;
        }

        public String getNode() {
            return this.node;
        }

        public void setValue(final String sValue) {
            this.value = sValue;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static MSG getByName(final String string) {
            for (MSG m : MSG.values()) {
                if (m.name().equals(string)) {
                    return m;
                }
            }
            return null;
        }
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    /**
     * create a language manager instance
     */
    public static void init(final String langString) {
        PVPArena.getInstance().getDataFolder().mkdir();
        final File configFile = new File(PVPArena.getInstance().getDataFolder().getPath(), String.format("/lang_%s.yml", langString));
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (final Exception e) {
                Bukkit.getLogger().severe("[PVP Arena] Error when creating language file.");
            }
        }
        final YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        for (MSG m : MSG.values()) {
            config.addDefault(m.getNode(), m.toString());
        }

        if (config.get("time_intervals") == null) {
            String prefix = "time_intervals.";
            config.addDefault(prefix + "1", "1..");
            config.addDefault(prefix + "2", "2..");
            config.addDefault(prefix + "3", "3..");
            config.addDefault(prefix + "4", "4..");
            config.addDefault(prefix + "5", "5..");
            config.addDefault(prefix + "10", "10 %s");
            config.addDefault(prefix + "20", "20 %s");
            config.addDefault(prefix + "30", "30 %s");
            config.addDefault(prefix + "60", "60 %s");
            config.addDefault(prefix + "120", "2 %m");
            config.addDefault(prefix + "180", "3 %m");
            config.addDefault(prefix + "240", "4 %m");
            config.addDefault(prefix + "300", "5 %m");
            config.addDefault(prefix + "600", "10 %m");
            config.addDefault(prefix + "1200", "20 %m");
            config.addDefault(prefix + "1800", "30 %m");
            config.addDefault(prefix + "2400", "40 %m");
            config.addDefault(prefix + "3000", "50 %m");
            config.addDefault(prefix + "3600", "60 %m");
        }

        config.options().copyDefaults(true);
        try {
            config.save(configFile);
            Language.config = config;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        for (MSG m : MSG.values()) {
            m.setValue(config.getString(m.getNode()));
        }
    }

    /**
     * read a node from the config and log its value
     *
     * @param message the node name
     */
    public static void logInfo(final MSG message) {
        final String var = message.toString();
        PVPArena.getInstance().getLogger().info(var);
        // log map value
    }

    /**
     * read a node from the config and log its value after replacing
     *
     * @param message   the node name
     * @param arg a string to replace
     */
    public static void logInfo(final MSG message, final String arg) {
        final String var = message.toString();
        PVPArena.getInstance().getLogger().info(var.replace("%1%", arg));
    }

    /**
     * read a node from the config and log its value after replacing
     *
     * @param message   the node name
     * @param arg a string to replace
     */
    public static void logError(final MSG message, final String arg) {
        final String var = message.toString();
        PVPArena.getInstance().getLogger().severe(var.replace("%1%", arg));
    }

    /**
     * read a node from the config and log its value after replacing
     *
     * @param message   the node name
     * @param arg a string to replace
     */
    public static void logWarn(final MSG message, final String arg) {
        final String var = message.toString();
        PVPArena.getInstance().getLogger().warning(var.replace("%1%", arg));
    }

    public static String parse(final Arena arena, final CFG node) {
        debug(arena, "CFG: " + node.getNode());
        return StringParser.colorize(arena.getConfig().getString(node));
    }

    public static String parse(final Arena arena, final CFG node, final String... args) {
        debug(arena, "CFG: " + node.getNode());
        String result = arena.getConfig().getString(node);

        int i = 0;

        for (String word : args) {
            result = result.replace("%" + ++i + '%', word);
        }

        return StringParser.colorize(result);
    }

    /**
     * read a node from the config and return its value
     *
     * @param message the node name
     * @return the node string
     */
    public static String parse(final MSG message) {
        trace("MSG: {}", message.name());
        return StringParser.colorize(message.toString());
    }

    /**
     * read a node from the config and return its value after replacing
     *
     * @param message   the node name
     * @param args strings to replace
     * @return the replaced node string
     */
    public static String parse(final MSG message, final Object... args) {
        trace("MSG: {}", message.name());
        String result = message.toString();
        int i = 0;
        for (Object word : args) {
            result = result.replace("%" + ++i + '%', String.valueOf(word));
        }
        return StringParser.colorize(result);
    }
}
