package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.api.IArenaCommandHandler;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.AbstractGlobalCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.regions.RegionFlag;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.bukkit.util.StringUtil.startsWithIgnoreCase;

public final class TabManager {
    private TabManager() {}

    public static List<String> getMatches(final CommandSender sender, final List<AbstractArenaCommand> arenaCommands, final List<AbstractGlobalCommand> globalCommands, String[] args) {
        final Set<String> matches = new LinkedHashSet<>();
        final String firstArg = args[0];
        Arena arena = null;
        if (sender instanceof Player) {
            if (PAA_Edit.activeEdits.containsKey(sender.getName())){
                arena = PAA_Edit.activeEdits.get(sender.getName());
            } else {
                arena = ArenaPlayer.fromPlayer((Player) sender).getArena();
            }
        }

        if (arena == null) {
            // player is not inside an arena or in edit mode

            arena = ArenaManager.getArenaByExactName(firstArg);

            if (arena == null && ArenaManager.getArenas().size() == 1) {
                // still no arena, get the only arena

                arena = ArenaManager.getFirst();
                // continue with one arg less
                args = Arrays.copyOfRange(args, 1, args.length);
                if (args.length < 1) {
                    // empty -> turn to catchall
                    args = new String[]{""};
                }

            } else if (arena != null) {
                // first argument matches with an arena

                if (args.length < 2) {
                    // return the exact arena name
                    return singletonList(arena.getName());
                } else {
                    // if more args, we remove the first one (arena name)
                    args = Arrays.copyOfRange(args, 1, args.length);
                    if (args.length < 1) {
                        // empty -> turn to catchall
                        args = new String[]{""};
                    }
                }
            } else if(args.length == 1) {
                // else, if only one arg, suggest arena names and global commands

                matches.addAll(ArenaManager.getArenas().stream()
                        .filter(a -> startsWithIgnoreCase(a.getName(), firstArg))
                        .map(Arena::getName)
                        .collect(Collectors.toList()));

                addCommandsStartingWithPrefix(matches, sender, arena, globalCommands, firstArg);
                return new ArrayList<>(matches);
            }
        }

        if (args.length == 1) {
            addCommandsStartingWithPrefix(matches, sender, arena, arenaCommands, args[0]);

            if (arena != null) {
                // an invalid arena config file may produce NPE here
                if (arena.getGoal() != null) {
                    addCommandsStartingWithPrefix(matches, sender, arena, singletonList(arena.getGoal()), args[0]);
                }
                if (CollectionUtils.isNotEmpty(arena.getMods())) {
                    addCommandsStartingWithPrefix(matches, sender, arena, new ArrayList<>(arena.getMods()), args[0]);
                }
            }
            return new ArrayList<>(matches);
        }

        final List<CommandTree<String>> commands = new ArrayList<>();
        addTreesMatchingValueInHandlerList(commands, arenaCommands, arena, args[0]);
        addTreesMatchingValueInHandlerList(commands, globalCommands, arena, args[0]);
        if (arena != null) {
            if(arena.getGoal() != null) {
                addTreesMatchingValueInHandlerList(commands, singletonList(arena.getGoal()), arena, args[0]);
            }
            if(CollectionUtils.isNotEmpty(arena.getMods())) {
                addTreesMatchingValueInHandlerList(commands, new ArrayList<>(arena.getMods()), arena, args[0]);
            }
        }

        for (CommandTree<String> tree : commands) {
            addMatchesFromCommandTree(matches, Arrays.copyOfRange(args, 1, args.length), tree);
        }
        return new ArrayList<>(matches);
    }

    /**
     * Find a Set of main sub commands and main shortcuts matching a prefix in a List of ArenaCommandHandler
     *
     * @param matches the Set to add to
     * @param list    the ArenaCommandHandler list to search
     * @param prefix  the prefix to look for
     */
    private static void addCommandsStartingWithPrefix(final Set<String> matches, final CommandSender sender,
                                                      final Arena arena, final List<? extends IArenaCommandHandler> list,
                                                      final String prefix) {
        for (IArenaCommandHandler ach : list) {
            if (ach.hasPerms(sender, arena)) {
                if(prefix.startsWith("!") || prefix.startsWith("-")) {
                    for (String value : ach.getShort()) {
                        if (startsWithIgnoreCase(value, prefix)) {
                            matches.add(value);
                        }
                    }
                } else {
                    for (String value : ach.getMain()) {
                        if (startsWithIgnoreCase(value, prefix)) {
                            matches.add(value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Add matching entries of an Enum to a List of String
     *
     * @param result the List to add to
     * @param key    the key to match
     * @param list   the Enum list to search
     */
    private static void addEnumMatchesToList(final List<String> result, final String key, final List<? extends Enum> list) {
        for (Enum e : list) {
            if (startsWithIgnoreCase(e.name(), key)) {
                result.add(e.name());
            }
        }
    }

    /**
     * Search a CommandTree (recursively), add found matches to the matches Set
     *
     * @param matches the set to add to
     * @param args    the arguments to search for
     * @param sub     the current CommandTree
     */
    private static void addMatchesFromCommandTree(final Set<String> matches, final String[] args, final CommandTree<String> sub) {
        if (args.length < 1) {
            return;
        }
        String override = args[0];
        if (args.length == 1) {
            // we have the last argument
            for (String key : sub.getContent()) {
                matches.addAll(getKeyMatchesInsideDefinition(override, key));
            }
            return;
        }
        if (override.isEmpty()) {
            for (String key : sub.getContent()) {
                matches.addAll(getKeyMatchesInsideDefinition(override, key));
            }
        } else {
            // should have a subvalue
            for (String key : sub.getContent()) {
                String newOverride = getOverrideFromDefinition(override, key);
                if (getKeyMatchesInsideDefinition(newOverride, key).size() > 0) {
                    override = newOverride;
                    break;
                }
            }
        }
        if (sub.contains(override)) {
            addMatchesFromCommandTree(matches, Arrays.copyOfRange(args, 1, args.length), sub.get(override));
        }
    }

    /**
     * Read a list of ArenaCommandHandler, add exact matches to a list of CommandTree
     *
     * @param treeList    the list of CommandTree to add to
     * @param handlerList the list of ArenaCommandHandler to search
     * @param arena       the arena instance to apply for subvalues (can be null)
     * @param value       the value to search for
     */
    private static void addTreesMatchingValueInHandlerList(final List<CommandTree<String>> treeList, final List<? extends IArenaCommandHandler> handlerList, final Arena arena, final String value) {

        outer:
        for (IArenaCommandHandler aac : handlerList) {
            for (String entry : aac.getMain()) {
                if (entry.equals(value)) {
                    treeList.add(aac.getSubs(arena));
                    continue outer;
                }
            }
            for (String entry : aac.getShort()) {
                if (entry.equals(value)) {
                    treeList.add(aac.getSubs(arena));
                    continue outer;
                }
            }
        }
    }

    /**
     * Return matches of a key inside a special node definition
     *
     * @param key        the key to match
     * @param definition the node definition ("{Enum}")
     * @return a set of matching nodes
     */
    private static List<String> getKeyMatchesInsideDefinition(final String key, final String definition) {
        final List<String> result = new ArrayList<>();
        if (key != null && (!key.isEmpty() && startsWithIgnoreCase(definition, key) || key.isEmpty() && !definition.startsWith("{"))) {
            result.add(definition);
        }
        if (definition.startsWith("{")) {
            if ("{Material}".equals(definition)) {
                final Material[] mats = Material.values();
                addEnumMatchesToList(result, key, asList(mats));
            } else if ("{Player}".equals(definition)) {
                final Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                if (key != null && key.isEmpty()) {
                    for (Player val : players) {
                        result.add(val.getName());
                    }
                } else if (key != null) {
                    for (Player val : players) {
                        if (startsWithIgnoreCase(val.getName(), key)) {
                            result.add(val.getName());
                        }
                    }
                }
            } else if ("{RegionProtection}".equals(definition)) {
                final RegionProtection[] protections = RegionProtection.values();
                addEnumMatchesToList(result, key, asList(protections));
            } else if ("{RegionFlag}".equals(definition)) {
                final RegionFlag[] flags = RegionFlag.values();
                addEnumMatchesToList(result, key, asList(flags));
            } else if ("{RegionType}".equals(definition)) {
                final RegionType[] types = RegionType.values();
                addEnumMatchesToList(result, key, asList(types));
            } else if ("{Boolean}".equals(definition)) {
                final List<String> values = new ArrayList<>();
                values.addAll(StringParser.negative);
                values.addAll(StringParser.positive);
                if (key != null && key.isEmpty()) {
                    result.addAll(values);
                } else if (key != null) {
                    for (String val : values) {
                        if (startsWithIgnoreCase(val, key)) {
                            result.add(val);
                        }
                    }
                }
            } else if ("{PotionEffectType}".equals(definition)) {
                final PotionEffectType[] pet = PotionEffectType.values();
                if (key != null && key.isEmpty()) {
                    for (PotionEffectType val : pet) {
                        result.add(val.getName());
                    }
                } else if (key != null) {
                    for (PotionEffectType val : pet) {
                        if (startsWithIgnoreCase(val.getName(), key)) {
                            result.add(val.getName());
                        }
                    }
                }
            } else if ("{EntityType}".equals(definition)) {
                final EntityType[] entityTypes = EntityType.values();
                addEnumMatchesToList(result, key, asList(entityTypes));
            }
        }
        return result;
    }

    /**
     * Find an override key inside a special node definition
     *
     * @param key        the key to find
     * @param definition the node definition ("{Enum}")
     * @return the definition if found, the key, if not
     */
    private static String getOverrideFromDefinition(final String key, final String definition) {
        if (definition.startsWith("{")) {
            if ("{Material}".equals(definition)) {
                final Material[] mats = Material.values();
                return getOverrideKey(key, definition, asList(mats));
            } else if ("{String}".equals(definition)) {
                return definition;
            } else if ("{Player}".equals(definition)) {
                final Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                for (Player val : players) {
                    if (val.getName().equals(key)) {
                        return definition;
                    }
                }
                return key;
            } else if ("{RegionProtection}".equals(definition)) {
                final RegionProtection[] protections = RegionProtection.values();
                return getOverrideKey(key, definition, asList(protections));
            } else if ("{RegionFlag}".equals(definition)) {
                final RegionFlag[] flags = RegionFlag.values();
                return getOverrideKey(key, definition, asList(flags));
            } else if ("{RegionType}".equals(definition)) {
                final RegionType[] types = RegionType.values();
                return getOverrideKey(key, definition, asList(types));
            } else if ("{Boolean}".equals(definition)) {
                final List<String> values = new ArrayList<>();
                values.addAll(StringParser.negative);
                values.addAll(StringParser.positive);
                for (String val : values) {
                    if (val.equals(key)) {
                        return definition;
                    }
                }
                return key;
            } else if ("{int}".equals(definition)) {
                try {
                    final int i = Integer.parseInt(key);
                    return definition;
                } catch (final NumberFormatException e) {
                    return key;
                }
            } else if ("{PotionEffectType}".equals(definition)) {
                final PotionEffectType[] pet = PotionEffectType.values();

                for (PotionEffectType val : pet) {
                    if (val.getName().equals(key)) {
                        return definition;
                    }
                }
                return key;
            } else if ("{EntityType}".equals(definition)) {
                final EntityType[] entityTypes = EntityType.values();
                return getOverrideKey(key, definition, asList(entityTypes));
            } else if ("{GameMode}".equals(definition)) {
                return getOverrideKey(key, definition, asList(GameMode.values()));
            }
        }
        return key;
    }

    /**
     * Find a key inside a List of Enum
     *
     * @param key        the key to find
     * @param definition the node definition ("{Enum}")
     * @param list       the Enum list to search
     * @return the definition if found, the key, if not
     */
    private static String getOverrideKey(final String key, final String definition, final List<? extends Enum> list) {
        for (Enum e : list) {
            if (e.name().equals(key)) {
                return definition;
            }
        }
        return key;
    }
}
