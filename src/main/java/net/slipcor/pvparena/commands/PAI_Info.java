package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * <pre>PVP Arena INFO Command class</pre>
 * <p/>
 * A command to display the active modules of an arena and settings
 *
 * @author slipcor
 */

public class PAI_Info extends AbstractArenaCommand {

    public PAI_Info() {
        super(new String[]{"pvparena.user", "pvparena.cmds.info"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        String displayMode = null;

        if (args.length > 0) {
            displayMode = args[0];
        }

        arena.msg(sender, MSG.INFO_HEAD_HEADLINE, arena.getName(), arena.getPrefix());

        arena.msg(sender, MSG.INFO_HEAD_TEAMS, StringParser.joinSet(arena.getTeamNamesColored(), ChatColor.COLOR_CHAR + "r, "));

        arena.msg(sender, StringParser.colorVar("fighting", arena.isFightInProgress()) + " | " +
                StringParser.colorVar("enabled", !arena.isLocked()));

        final Set<String> classes = new HashSet<>();
        for (final ArenaClass ac : arena.getClasses()) {
            if (!"custom".equalsIgnoreCase(ac.getName())) {
                classes.add(ac.getName());
            }
        }

        arena.msg(sender, MSG.INFO_CLASSES, StringParser.joinSet(classes, ", "));
        arena.msg(sender, MSG.INFO_OWNER, arena.getOwner() == null ? "server" : arena.getOwner());

        Config cfg = arena.getConfig();
        if (displayMode == null || "chat".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "chat");
            arena.msg(sender,
                    StringParser.colorVar("colorNick", cfg.getBoolean(CFG.CHAT_COLORNICK)) + " | " +
                    StringParser.colorVar("defaultTeam", cfg.getBoolean(CFG.CHAT_DEFAULTTEAM)) + " | " +
                    StringParser.colorVar("enabled", cfg.getBoolean(CFG.CHAT_ENABLED)) + " | " +
                    StringParser.colorVar("onlyPrivate", cfg.getBoolean(CFG.CHAT_ONLYPRIVATE)));
        }

        if (displayMode == null || "command".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "command");
            arena.msg(sender, StringParser.colorVar("defaultjoin", cfg.getBoolean(CFG.CMDS_DEFAULTJOIN)));
        }

        if (displayMode == null || "damage".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "damage");
            arena.msg(sender, StringParser.colorVar("armor",
                    cfg.getBoolean(CFG.DAMAGE_ARMOR)) + " | " +
                    "spawnCamp: " + cfg.getInt(CFG.DAMAGE_SPAWNCAMP) + " | " +
                    StringParser.colorVar("weapons", cfg.getBoolean(CFG.DAMAGE_WEAPONS)));
        }

        if (displayMode == null || "general".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "general");
            arena.msg(sender,
                    StringParser.colorVar("classspawn", cfg.getBoolean(CFG.GENERAL_CLASSSPAWN)) + " | " +
                    StringParser.colorVar("leavedeath", cfg.getBoolean(CFG.GENERAL_LEAVEDEATH)) + " | " +
                    StringParser.colorVar("quickspawn", cfg.getBoolean(CFG.GENERAL_QUICKSPAWN)) + " | " +
                    StringParser.colorVar("smartspawn", cfg.getBoolean(CFG.GENERAL_SMARTSPAWN)));

            arena.msg(sender,
                    "gameMode: " + cfg.getGameMode(CFG.GENERAL_GAMEMODE) + " | " +
                            "time end: " + cfg.getInt(CFG.GENERAL_TIMER) + " | " +
                            "time end winner: " + cfg.getString(CFG.GENERAL_TIMER_WINNER));

        }

        if (displayMode == null || "command".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "goal");
            arena.msg(sender, StringParser.colorVar("addLivesPerPlayer", cfg.getBoolean(CFG.GENERAL_ADDLIVESPERPLAYER)));
        }

        if (displayMode == null || "item".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "item");
            arena.msg(sender, "minplayers: " + cfg.getInt(CFG.ITEMS_MINPLAYERS) + " | " +
                    "rewards: " + StringParser.getItems(cfg.getItems(CFG.ITEMS_REWARDS)) + " | " +
                    StringParser.colorVar("random", cfg.getBoolean(CFG.ITEMS_RANDOM)));

        }

        if (displayMode == null || "join".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "join");
            arena.msg(sender, "range: " + cfg.getInt(CFG.JOIN_RANGE) + " | " +
                    StringParser.colorVar("forceregionjoin", cfg.getBoolean(CFG.JOIN_FORCE)));

        }

        if (displayMode == null || "perms".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "perms");
            arena.msg(sender,
                    StringParser.colorVar("explicitarena", cfg.getBoolean(CFG.PERMS_EXPLICITARENA)) + " | " +
                    StringParser.colorVar("explicitclass", cfg.getBoolean(CFG.PERMS_EXPLICITCLASS)) + " | " +
                    StringParser.colorVar("joininbattle", cfg.getBoolean(CFG.PERMS_JOININBATTLE)) + " | " +
                    StringParser.colorVar("teamkill", cfg.getBoolean(CFG.PERMS_TEAMKILL)));

        }

        if (displayMode == null || "player".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "player");
            arena.msg(sender,
                    StringParser.colorVar("autoIgniteTNT", cfg.getBoolean(CFG.PLAYER_AUTOIGNITE)) + " | " +
                    StringParser.colorVar("dropsInventory", cfg.getBoolean(CFG.PLAYER_DROPSINVENTORY)) + " | " +
                    StringParser.colorVar("dropsExp", cfg.getBoolean(CFG.PLAYER_DROPSEXP)) + " | " +
                    StringParser.colorVar("refillInventory", cfg.getBoolean(CFG.PLAYER_REFILLINVENTORY)));

            String healthDisplay = String.valueOf(cfg.getInt(CFG.PLAYER_HEALTH) < 1 ? "FULL" : cfg.getInt(CFG.PLAYER_HEALTH));
            healthDisplay += "/" + (cfg.getInt(CFG.PLAYER_MAXHEALTH) < 1 ? "DEFAULT" : cfg.getInt(CFG.PLAYER_MAXHEALTH));

            arena.msg(sender,
                    "exhaustion: " + cfg.getDouble(CFG.PLAYER_EXHAUSTION) + " | " +
                            "foodLevel: " + cfg.getInt(CFG.PLAYER_FOODLEVEL) + " | " +
                            "health: " + healthDisplay + " | " +
                            "saturation: " + cfg.getInt(CFG.PLAYER_SATURATION) + " | " +
                            "time: " + cfg.getInt(CFG.PLAYER_TIME)
            );
        }

        if (displayMode == null || "protect".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "protect");
            arena.msg(sender, StringParser.colorVar("enabled", cfg.getBoolean(CFG.PROTECT_ENABLED)) + " | " +
                    StringParser.colorVar("punish", cfg.getBoolean(CFG.PROTECT_PUNISH)) + " | " +
                    "spawn: " + cfg.getInt(CFG.PROTECT_SPAWN));

        }

        if (displayMode == null || "ready".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "ready");
            arena.msg(sender,
                    StringParser.colorVar("checkEachPlayer", cfg.getBoolean(CFG.READY_CHECKEACHPLAYER)) + " | " +
                    StringParser.colorVar("checkEachTeam", cfg.getBoolean(CFG.READY_CHECKEACHTEAM)) + " | " +
                    "autoClass: " + cfg.getString(CFG.READY_AUTOCLASS));


            arena.msg(sender,
                    "block: " + arena.getReadyBlock() + " | " +
                            "minPlayers: " + cfg.getInt(CFG.READY_MINPLAYERS) + " | " +
                            "maxPlayers: " + cfg.getInt(CFG.READY_MAXPLAYERS) + " | " +
                            "maxTeam: " + cfg.getInt(CFG.READY_MAXTEAMPLAYERS) + " | " +
                            "neededRatio: " + cfg.getDouble(CFG.READY_NEEDEDRATIO));
        }

        if (displayMode == null || "time".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "time");
            arena.msg(sender,
                    "endCountDown: " + cfg.getInt(CFG.TIME_ENDCOUNTDOWN) + " | " +
                            "startCountDown: " + cfg.getInt(CFG.TIME_STARTCOUNTDOWN) + " | " +
                            "regionTimer: " + cfg.getInt(CFG.TIME_REGIONTIMER));
            arena.msg(sender,
                    "teleportProtect: " + cfg.getInt(CFG.TIME_TELEPORTPROTECT) + " | " +
                            "warmupCountDown: " + cfg.getInt(CFG.TIME_WARMUPCOUNTDOWN) + " | " +
                            "pvp: " + cfg.getInt(CFG.TIME_PVP));

        }

        if (displayMode == null || "tp".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "tp");
            arena.msg(sender,
                    "death: " + cfg.getString(CFG.TP_DEATH) + " | " +
                            "exit: " + cfg.getString(CFG.TP_EXIT) + " | " +
                            "lose: " + cfg.getString(CFG.TP_LOSE) + " | " +
                            "win: " + cfg.getString(CFG.TP_WIN));

        }

        if (displayMode == null || displayMode.isEmpty() || "chat".equals(displayMode)) {
            arena.msg(sender, MSG.INFO_SECTION, "chat");
            arena.msg(sender,
                    StringParser.colorVar("classSignsDisplay", cfg.getBoolean(CFG.USES_CLASSSIGNSDISPLAY)) + " | " +
                    StringParser.colorVar("deathMessages", cfg.getBoolean(CFG.USES_DEATHMESSAGES)) + " | " +
                    StringParser.colorVar("evenTeams", cfg.getBoolean(CFG.USES_EVENTEAMS)));

            arena.msg(sender,
                    StringParser.colorVar("ingameClassSwitch", cfg.getBoolean(CFG.USES_INGAMECLASSSWITCH)) + " | " +
                    StringParser.colorVar("overlapCheck", cfg.getBoolean(CFG.USES_OVERLAPCHECK)) + " | " +
                    StringParser.colorVar("woolHead", cfg.getBoolean(CFG.USES_WOOLHEAD)));
        }

        if (displayMode == null || "region".equalsIgnoreCase(displayMode)) {

            if (arena.getRegions() != null) {
                final Set<String> regions = new HashSet<>();
                for (final ArenaRegion ar : arena.getRegions()) {
                    regions.add(ar.getRegionName());
                }

                arena.msg(sender, MSG.INFO_REGIONS, StringParser.joinSet(regions, ", "));
            }
        }


        if (displayMode == null || "goal".equalsIgnoreCase(displayMode)) {
            arena.msg(sender, MSG.INFO_GOAL_ACTIVE,
                    Optional.ofNullable(arena.getGoal()).map(ArenaGoal::getName).orElse("None"));
            arena.getGoal().displayInfo(sender);
        }

        if (displayMode == null || "mod".equalsIgnoreCase(displayMode)) {
            for (final ArenaModule mod : arena.getMods()) {
                arena.msg(sender, MSG.INFO_MOD_ACTIVE, mod.getName());
                mod.displayInfo(sender);
            }
        }

        if (displayMode == null || "region".equalsIgnoreCase(displayMode)) {
            for (final ArenaRegion reg : arena.getRegions()) {
                reg.getShape().displayInfo(sender);
            }
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.INFO);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("info");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-i");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"chat"});
        result.define(new String[]{"command"});
        result.define(new String[]{"damage"});
        result.define(new String[]{"general"});
        result.define(new String[]{"item"});
        result.define(new String[]{"join"});
        result.define(new String[]{"perms"});
        result.define(new String[]{"player"});
        result.define(new String[]{"protect"});
        result.define(new String[]{"ready"});
        result.define(new String[]{"time"});
        result.define(new String[]{"tp"});
        result.define(new String[]{"region"});
        result.define(new String[]{"mod"});
        result.define(new String[]{"goal"});
        return result;
    }
}
