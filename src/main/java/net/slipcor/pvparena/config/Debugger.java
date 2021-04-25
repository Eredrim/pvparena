package net.slipcor.pvparena.config;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.logging.Level.*;
import static net.slipcor.pvparena.config.DebugOutputMode.*;


/**
 * <pre>
 * Debug class
 * </pre>
 * <p/>
 * provides methods for logging when in debug mode
 *
 * @author slipcor
 */

public class Debugger {
    private static final String PREFIX = "[PVP Arena - DEBUG]";
    private static Logger logger = null;
    private static boolean active;
    private static DebugOutputMode outputMode;
    private static Level level;

    public static void debug(String string) {
        if (active) {
            print(FINE, string);
        }
    }

    public static void debug(String template, Object... args) {
        if (active) {
            print(FINE, getTemplatedLine(template, args));
        }
    }

    public static void debug(Arena arena, String string) {
        if (active) {
            formatAndPrint(arena, null, FINE, string);
        }
    }

    public static void debug(CommandSender sender, String string) {
        if (active) {
            if (sender instanceof Player) {
                ArenaPlayer ap = ArenaPlayer.fromPlayer((Player) sender);
                debug(ap, string);
            } else {
                print(FINE, string);
            }
        }
    }

    public static void debug(ArenaPlayer aPlayer, String template, Object... args) {
        if (active) {
            debug(aPlayer, getTemplatedLine(template, args));
        }
    }

    public static void debug(ArenaPlayer aPlayer, String string) {
        if (active) {
            if (aPlayer.getArena() != null) {
                formatAndPrint(aPlayer.getArena(), aPlayer.getPlayer(), FINE, string);
            } else {
                formatAndPrint(null, aPlayer.getPlayer(), FINE, string);
            }
        }
    }

    public static void debug(CommandSender sender, String template, Object... args) {
        if (active) {
            debug(sender, getTemplatedLine(template, args));
        }
    }

    public static void debug(Arena arena, CommandSender commandSender, Object object) {
        if (active) {
            formatAndPrint(arena, commandSender, FINE, String.valueOf(object));
        }
    }

    public static void debug(Arena arena, CommandSender commandSender, String string) {
        if (active) {
            formatAndPrint(arena, commandSender, FINE, string);
        }
    }

    public static void debug(Arena arena, String template, Object... args) {
        if (active) {
            formatAndPrint(arena, null, FINE, getTemplatedLine(template, args));
        }
    }

    public static void trace(String string) {
        if (active && FINER.equals(level)) {
            print(FINER, string);
        }
    }

    public static void trace(String template, Object... args) {
        if (active && FINER.equals(level)) {
            print(FINER, getTemplatedLine(template, args));
        }
    }

    public static void trace(Arena arena, String template, Object... args) {
        if (active && FINER.equals(level)) {
            formatAndPrint(arena, null, FINER, getTemplatedLine(template, args));
        }
    }

    public static void trace(CommandSender sender, String template, Object... args) {
        if (active && FINER.equals(level)) {
            formatAndPrint(null, sender, FINER, getTemplatedLine(template, args));
        }
    }

    public static void load(final PVPArena instance, final CommandSender sender) {
        FileConfiguration config = instance.getConfig();
        boolean isDebugEnabled = config.getBoolean("debug.enable", false);
        try {
            outputMode = DebugOutputMode.valueOf(config.getString("debug.output", BOTH.name()));
            level = parse(config.getString("debug.level", FINE.getName()));
        } catch (IllegalArgumentException e) {
            instance.getLogger().severe("Invalid debug configuration found! Resetting your debug config");
            outputMode = BOTH;
            setOutputMode(BOTH);
            level = FINE;
            setLevel(FINE);
        }


        if (isDebugEnabled) {
            enable(sender);
        } else {
            disable(sender);
        }
    }

    public static void enable(CommandSender sender) {
        active = true;
        noticeSender(sender, String.format("debugging: ON - output: %s - level: %s", outputMode, level));
    }

    public static void disable(CommandSender sender) {
        active = false;
        destroy();
        logger = null;
        noticeSender(sender, "debugging: off");
    }

    public static boolean isActive() {
        return active;
    }

    public static DebugOutputMode getOutputMode() {
        return outputMode;
    }

    public static void setOutputMode(DebugOutputMode newDebugOutputMode) {
        FileConfiguration config = PVPArena.getInstance().getConfig();
        config.set("debug.output", newDebugOutputMode.name());
        outputMode = newDebugOutputMode;
    }

    public static void setLevel(Level newLevel) {
        FileConfiguration config = PVPArena.getInstance().getConfig();
        config.set("debug.level", newLevel.getName());
        level = newLevel;
    }

    public static void destroy() {
        if (logger != null) {
            Stream.of(getFileLogger().getHandlers()).forEach(hand -> {
                hand.close();
                getFileLogger().removeHandler(hand);
            });
        }
    }

    private static Logger getFileLogger() {
        if (logger == null) {
            logger = Logger.getAnonymousLogger();
            logger.setLevel(ALL);
            logger.setUseParentHandlers(false);

            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }

            try {
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

                final File debugFolder = new File(PVPArena.getInstance().getDataFolder(), "debug");


                debugFolder.mkdirs();
                final File logFile = new File(debugFolder, dateFormat.format(new Date()) + ".log");
                logFile.createNewFile();

                final FileHandler handler = new FileHandler(logFile.getAbsolutePath());

                handler.setFormatter(new LogFileFormatter());

                logger.addHandler(handler);
            } catch (final IOException | SecurityException ex) {
                PVPArena.getInstance().getLogger().log(SEVERE, null, ex);
            }
        }

        return logger;
    }

    private static void formatAndPrint(Arena arena, CommandSender commandSender, Level level, String string) {
        StringBuilder strBuilder = new StringBuilder();
        if (arena != null) {
            strBuilder.append(String.format("[%s] ", arena.getName()));
        }

        if (commandSender instanceof Player) {
            strBuilder.append(String.format("{%s} ", commandSender.getName()));
        }

        strBuilder.append(string);
        print(level, strBuilder.toString());
    }

    private static void print(Level level, String string) {
        if (outputMode == FILE || outputMode == BOTH) {
            getFileLogger().log(level, string);
        }

        if (outputMode == CONSOLE || outputMode == BOTH) {
            PVPArena.getInstance().getServer().getLogger().info(String.format("%s %s", PREFIX, string));
        }
    }

    private static String getTemplatedLine(String template, Object[] args) {
        Object[] stringArgs = Stream.of(args)
                .map(obj -> {
                    if (obj instanceof Player) {
                        return ((Player) obj).getName();
                    }
                    return String.valueOf(obj);
                })
                .toArray();
        return String.format(template.replace("{}", "%s"), stringArgs);
    }

    private static void noticeSender(CommandSender sender, String message) {
        if (sender instanceof Player) {
            Arena.pmsg(sender, message);
        } else {
            PVPArena.getInstance().getLogger().info(message);
        }
    }
}
