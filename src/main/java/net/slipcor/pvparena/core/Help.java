package net.slipcor.pvparena.core;

import net.slipcor.pvparena.PVPArena;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * Help class
 * </pre>
 * <p/>
 * provides methods to display configurable help texts
 *
 * @author slipcor
 */

public final class Help {
    private Help() {

    }

    private static final String LINE = "-------------------------------------------";

    public enum HELP {
        BLACKLIST("nulang.help.msg.blacklist", new String[]{
                "Manages block break/place blacklist entries",
                LINE,
                "shorthand command: !bl",
                LINE,
                "/pa [arenaname] blacklist clear",
                "/pa [arenaname] blacklist [type] clear",
                "    - clear the blacklist [of a special type]",
                "/pa [arenaname] blacklist [type] [sub] [blockID]",
                LINE,
                "Valid types: break | place | use",
                "Valid subs: add | remove | show"}),
        CHECK("nulang.help.msg.check", new String[]{
                "Validate an arena configuration",
                LINE,
                "shorthand command: !ch",
                LINE,
                "/pa [arenaname] check"}),
        CLASS("nulang.help.msg.class", new String[]{
                "Manage arena classes",
                LINE,
                "shorthand command: !cl",
                LINE,
                "/pa [arenaname] class load [classname]",
                "/pa [arenaname] class save [classname]",
                "/pa [arenaname] class remove [classname]"}),
        CLASSCHEST("nulang.help.msg.classchest", new String[]{
                "Manage arena class chests",
                LINE,
                "shorthand command: !cc",
                LINE,
                "/pa [arenaname] classchest [classname]"}),
        CREATE("nulang.help.msg.create", new String[]{
                "Create an arena",
                LINE,
                "shorthand command: !c",
                LINE,
                "/pa create [arenaname] {legacytype}",
                LINE,
                "Valid legacy types: teams, teamdm, dm, free, ctf, ctp, spleef"}),
        DEBUG("nulang.help.msg.debug", new String[]{
                "Debug PVP Arena",
                LINE,
                "shorthand command: !d",
                LINE,
                "/pa debug [values]",
                LINE,
                "Valid values are class numbers (separated by comma) and &enone&r"}),
        DISABLE("nulang.help.msg.disable", new String[]{
                "Disable an arena",
                LINE,
                "shorthand command: !dis",
                LINE,
                "/pa [arenaname] disable"}),
        EDIT("nulang.help.msg.edit", new String[]{
                "Toggle edit mode of an arena",
                LINE,
                "shorthand command: !e",
                LINE,
                "/pa [arenaname] edit"}),
        ENABLE("nulang.help.msg.enable", new String[]{
                "Enable an arena",
                LINE,
                "shorthand command: !en",
                LINE,
                "/pa [arenaname] enable"}),
        FORCEWIN("nulang.help.msg.forcewin", new String[]{
                "Force a player/team to win",
                LINE,
                "shorthand command: !fw",
                LINE,
                "/pa [arenaname] forcewin slipcor",
                "/pa [arenaname] forcewin blue"}),
        GAMEMODE("nulang.help.msg.gamemode", new String[]{
                "Set the general game mode of an arena. Teams or Free for all",
                LINE,
                "shorthand command: !gm",
                LINE,
                "/pa [arenaname] gamemode [free|teams]"}),
        GOAL("nulang.help.msg.goal", new String[]{
                "Manage arena goals",
                LINE,
                "shorthand command: !g",
                LINE,
                "/pa [arenaname] goal [goalname] {value}",
                LINE,
                "Entering an invalid goal will list available goals",
                "No value will toggle and display the result",
                "Valid values: yes, on, 1, true, no, off, 0, false"}),
        INSTALL("nulang.help.msg.install", new String[]{
                "Install modules",
                LINE,
                "shorthand command: !i",
                LINE,
                "/pa install (modules | goals)",
                "/pa install [modulename]",
                LINE,
                "modules / arenas will list available files"}),

        PLAYERJOIN("nulang.help.msg.playerjoin", new String[]{
                "Make a player join an arena",
                LINE,
                "shorthand command: !pj",
                LINE,
                "/pa [arenaname] playerjoin [playerName] (teamName)"}),
        PLAYERSPECTATE("nulang.help.msg.playerspectate", new String[]{
                "Make a player spectate an arena",
                LINE,
                "shorthand command: !ps",
                LINE,
                "/pa [arenaname] playerspectate [playerName]"}),
        PLAYERARENACLASS("nulang.help.msg.playerarenaclass", new String[]{
                "Change arena class of a player",
                LINE,
                "shorthand command: !pac",
                LINE,
                "/pa [arenaname] playerarenaclass [playerName] [className]"}),
        PLAYERLEAVE("nulang.help.msg.playerleave", new String[]{
                "Make a player leave the arena",
                LINE,
                "shorthand command: !pl",
                LINE,
                "/pa [arenaname] playerleave [playerName]"}),
        PLAYERREADY("nulang.help.msg.playerready", new String[]{
                "Make a player ready",
                LINE,
                "shorthand command: !pr",
                LINE,
                "/pa [arenaname] playerready [playerName]"}),
        PROTECTION("nulang.help.msg.protection", new String[]{
                "Manage arena region protections",
                LINE,
                "shorthand command: !p",
                LINE,
                "/pa [arenaname] protection [regionname] [protection] {value}",
                LINE,
                "Entering an invalid protection will list available ones",
                "No value will toggle and display the result",
                "Valid values: yes, on, 1, true, no, off, 0, false"}),
        REGION("nulang.help.msg.region", new String[]{
                "Manage arena regions",
                LINE,
                "shorthand command: !rg",
                LINE,
                "/pa [arenaname] region",
                "    - start selecting a region",
                "/pa [arenaname] region [regionname] {regiontype}",
                "    - save region, default regiontype is a cuboid",
                "    - valid regions: &e/pa version regions",
                "/pa [arenaname] region [regionname] border",
                "    - display the region border for a short time",
                "/pa [arenaname] region remove [regionname]",
                "    - remove a region",
                LINE,
                "/pa [arenaname] region [regionname] [key] [value]",
                "    - Enhanced region editing, valid keys include:",
                "    - radius, height, position"}),
        REGIONFLAG("nulang.help.msg.regionflag", new String[]{
                "Manage arena region flags",
                LINE,
                "shorthand command: !rf",
                LINE,
                "/pa [arenaname] regionflag [regionname] [flagtype] {value}",
                LINE,
                "Entering an invalid flagtype will list available ones",
                "No value will toggle and display the result",
                "Valid values: yes, on, 1, true, no, off, 0, false"}),
        REGIONS("nulang.help.msg.regions", new String[]{
                "Debug arena regions",
                LINE,
                "shorthand command: !rs",
                LINE,
                "/pa [arenaname] regions {regionname}",
                LINE,
                "Show all defined regions / detailed information about a region"}),
        REGIONTYPE("nulang.help.msg.regiontype", new String[]{
                "Manage region types",
                LINE,
                "shorthand command: !rt",
                LINE,
                "/pa [arenaname] regiontype [regionname] [type]",
                LINE,
                "Valid region types "}),
        RELOAD("nulang.help.msg.reload", new String[]{
                "Reload an arena",
                LINE,
                "shorthand command: !rl",
                LINE,
                "/pa [arenaname] reload"}),
        REMOVE("nulang.help.msg.remove", new String[]{
                "Remove an arena",
                LINE,
                "shorthand command: !rem",
                LINE,
                "/pa [arenaname] remove"}),
        SET("nulang.help.msg.set", new String[]{
                "Set an arena config setting",
                LINE,
                "shorthand command: !s",
                LINE,
                "/pa [arenaname] set [node] [value]",
                LINE,
                "Values will be explained if necessary"}),
        SETOWNER("nulang.help.msg.setowner", new String[]{
                "Set an arena owner",
                LINE,
                "shorthand command: !so",
                LINE,
                "/pa [arenaname] setowner [playername]"}),
        SETUP("nulang.help.msg.setup", new String[]{
                "Toggle setup mode of an arena",
                LINE,
                "shorthand command: !su",
                LINE,
                "/pa [arenaname] setup"}),
        SETUP_CMDS("nulang.help.msg.setup_cmds", new String[]{
                "Special commands for setup mode",
                LINE,
                "show [region|spawn|block] | display a region/spawn/block with wool blocks",
                "region [region] move 2 up | move region [region] 2 blocks up",
                "region [region] expand 10 north | expand region [region] 10 block to the north",
                "region [region] expand 15 out | expand region [region] 15 blocks in all directions",
                "all arguments can be abbreviated, e.g. 'r' = 'region', 'e' = 'expand', 'm' = 'move'",
                "done | exit setup mode",
                LINE,
                "/pa [arenaname] setup"}),
        SPAWN("nulang.help.msg.spawn", new String[]{
                "Manage arena spawns",
                LINE,
                "shorthand command: !sp",
                LINE,
                "/pa [arenaname] spawn [spawnname] (teamname) (classname)",
                "/pa [arenaname] spawn [spawnname] remove",
                LINE,
                "/pa [arenaname] spawn [spawnname] offset X Y Z",
                LINE,
                "Spawn names vary based on installed/active modules!"}),
        START("nulang.help.msg.start", new String[]{
                "Force start an arena",
                LINE,
                "shorthand command: !go",
                LINE,
                "/pa [arenaname] start"}),
        STOP("nulang.help.msg.stop", new String[]{
                "Force stop an arena",
                LINE,
                "shorthand command: !st",
                LINE,
                "/pa [arenaname] stop"}),
        TOGGLEMOD("nulang.help.msg.togglemod", new String[]{
                "Toggle an arena module",
                LINE,
                "shorthand command: !tm",
                LINE,
                "/pa [arenaname] togglemod [name]"}),
        TEAMS("nulang.help.msg.teams", new String[]{
                "Manage arena teams",
                LINE,
                "shorthand command: !ts",
                LINE,
                "/pa [arenaname] teams - list teams",
                "/pa [arenaname] teams add [name] [value]",
                "/pa [arenaname] teams set [name] [value]",
                "/pa [arenaname] teams remove [name]"}),
        TELEPORT("nulang.help.msg.teleport", new String[]{
                "Teleport to an arena spawn",
                LINE,
                "/pa [arenaname] teleport [spawnname]",
                "shorthand command: !t"}),
        TEMPLATE("nulang.help.msg.template", new String[]{
                "Save/load arena configs",
                LINE,
                "shorthand command: !tmp",
                LINE,
                "/pa {arenaname} template load {filename}",
                "/pa {arenaname} template save {filename}"}),
        UNINSTALL("nulang.help.msg.uninstall", new String[]{
                "Uninstall a PVP Arena module",
                LINE,
                "shorthand command: !ui",
                LINE,
                "/pa uninstall [modulename]"}),
        UPDATE("nulang.help.msg.update", new String[]{
                "Update a PVP Arena module",
                LINE,
                "shorthand command: !u",
                LINE,
                "/pa update [modulename]"}),
        WHITELIST("nulang.help.msg.whitelist", new String[]{
                "Manages block break/place whitelist entries",
                LINE,
                "shorthand command: !wl",
                LINE,
                "/pa [arenaname] whitelist clear",
                "/pa [arenaname] whitelist [type] clear",
                "    - clear the whitelist [of a special type]",
                "/pa [arenaname] whitelist [type] [sub] [blockID]",
                LINE,
                "Valid types: break | place | use",
                "Valid subs: add | remove | show"}),

        CHAT("nulang.help.msg.chat", new String[]{
                "Set arena team chat mode",
                LINE,
                "shorthand command: -c",
                LINE,
                "/pa [arenaname] chat {value}",
                LINE,
                "Switch between team and global chat",
                "No value will toggle and display the result",
                "Valid values: yes, on, 1, true, no, off, 0, false"}),

        SHUTUP("nulang.help.msg.shutup", new String[]{
                "Ignore arena announcements",
                LINE,
                "shorthand command: -su",
                LINE,
                "/pa [arenaname] shutup {value}",
                LINE,
                "Switch ignore mode",
                "No value will toggle and display the result",
                "Valid values: yes, on, 1, true, no, off, 0, false"}),
        JOIN("nulang.help.msg.join", new String[]{
                "Join an arena",
                LINE,
                "shorthand command: -j",
                LINE,
                "/pa [arenaname] join {teamname}"}),
        LEAVE("nulang.help.msg.leave", new String[]{
                "Leave the arena",
                LINE,
                "shorthand command: -l",
                LINE,
                "/pa leave"}),
        SPECTATE("nulang.help.msg.spectate", new String[]{
                "Spectate an arena",
                LINE,
                "shorthand command: -s",
                LINE,
                "/pa [arenaname] spectate"}),


        ARENACLASS("nulang.help.msg.arenaclass", new String[]{
                "Switch your arena class",
                LINE,
                "shorthand command: -ac",
                LINE,
                "/pa arenaclass [classname]",
                LINE,
                "/pa arenaclass - list classes"}),
        ARENALIST("nulang.help.msg.arenalist", new String[]{
                "List available arenas",
                LINE,
                "shorthand command: -ls",
                LINE,
                "/pa list"}),
        HELP("nulang.help.msg.help", new String[]{
                "Receive help",
                LINE,
                "shorthand command: -h",
                LINE,
                "/pa help [topic/command]"}),
        INFO("nulang.help.msg.info", new String[]{
                "Detailed information about an arena",
                LINE,
                "shorthand command: -i",
                LINE,
                "/pa [arenaname] info"}),
        LIST("nulang.help.msg.list", new String[]{
                "List arena players",
                LINE,
                "shorthand command: -ls",
                LINE,
                "/pa {arenaname} list"}),

        READY("nulang.help.msg.ready", new String[]{
                "Ready up / show ready players",
                LINE,
                "shorthand command: -r",
                LINE,
                "/pa {arenaname} ready {show}"}),
        STATS("nulang.help.msg.stats", new String[]{
                "Show arena/player statistics",
                LINE,
                "shorthand command: -s",
                LINE,
                "/pa {arenaname} stats [stattype] {amount}",
                LINE,
                "An invalid stattype will list all available ones"}),
        VERSION("nulang.help.msg.version", new String[]{
                "Show detailed version information",
                LINE,
                "shorthand command: -v",
                LINE,
                "/pa version"});


        private final String node;
        private final List<String> value;

        HELP(final String node, final String[] sArray) {
            this.node = node;
            final List<String> list = new ArrayList<>();
            Collections.addAll(list, sArray);
            this.value = list;
        }

        public String getNode() {
            return this.node;
        }

        public List<String> get() {
            return this.value;
        }

        public void print(CommandSender sender) {
            String prefix = PVPArena.getInstance().getConfig().getString("globalPrefix", "PVP Arena");
            this.value.forEach(line ->
                sender.sendMessage(Language.parse(Language.MSG.MESSAGES_GENERAL, prefix, StringParser.colorize(line)))
            );
        }
    }

    /**
     * create a language manager instance
     */
    public static void init(final String langString) {
        PVPArena.getInstance().getDataFolder().mkdir();
        final File configFile = new File(PVPArena.getInstance().getDataFolder().getPath()
                + "/help_" + langString + ".yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (final Exception e) {
                Bukkit.getLogger().severe(
                        "[PVP Arena] Error when creating help language file.");
            }
        }
        boolean override = false;

        final YamlConfiguration config = new YamlConfiguration();
        final String version = "v1.3.1.28";
        try {
            config.load(configFile);
            final String ver = config.getString("version", "0");

            if (!ver.equals(version)) {
                override = true;
                final File file = new File(configFile.getParent(), "/help_" + langString + "_backup.yml");
                if (!file.exists()) {
                    file.createNewFile();
                }
                config.save(file);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        for (HELP m : HELP.values()) {
            if (override) {
                config.set(m.getNode(), m.get());
            } else {
                config.addDefault(m.getNode(), m.get());
            }
        }

        config.set("version", version);

        config.options().copyDefaults(true);
        try {
            config.save(configFile);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
