package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * <pre>PVP Arena SETUP Command class</pre>
 * <p/>
 * A command to toggle an arena's setup mode
 *
 * @author slipcor
 */

public class PAA_Setup extends AbstractArenaCommand {

    public static final Map<String, Arena> activeSetups = new HashMap<>();

    public PAA_Setup() {
        super(new String[]{"pvparena.cmds.setup"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0})) {
            return;
        }

        final String msg;

        if (activeSetups.containsValue(arena)) {
            activeSetups.remove(sender.getName());
            msg = Language.parse(MSG.ARENA_SETUP_DISABLED, arena.getName());
        } else {
            if (arena.isFightInProgress()) {
                final PAA_Stop cmd = new PAA_Stop();
                cmd.commit(arena, sender, new String[0]);
            }
            activeSetups.put(sender.getName(), arena);
            msg = Language.parse(MSG.ARENA_SETUP_ENABLED, arena.getName());
        }
        arena.msg(sender, msg);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.SETUP);
    }

    public static void chat(final Player player, final String message) {
        final Arena arena = activeSetups.get(player.getName());
        /*
          help | ?

          show [region|spawn|block] | display a region/spawn/block with wool blocks",
          region [region] move 2 up | move region [region] 2 blocks up",
          region [region] expand 10 north | expand region [region] 10 block to the north",
          region [region] expand 15 | expand region [region] 15 blocks in all directions",
          all arguments can be abbreviated, e.g. 'r' = 'region', 'e' = 'expand', 'm' = 'move'",
          done | exit setup mode",
         */
        if (message.length() < 1) {
            return;
        }

        final String[] word = message.toLowerCase().split(" ");
        if (word[0].startsWith("h") || word[0].startsWith("?")) {
            arena.msg(player, MSG.ERROR_ERROR, message);
            arena.msg(player, HELP.SETUP_CMDS);
        }

        if (word[0].startsWith("s")) {
            // show [region|spawn|block] | display a region/spawn/block with wool blocks",
            if (word.length > 2) {
                class Remover implements Runnable {
                    private final Location location;

                    Remover(final Location loc) {
                        this.location = loc;
                        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), this, 100L);
                    }

                    @Override
                    public void run() {
                        player.sendBlockChange(this.location, this.location.getBlock().getType().createBlockData());
                    }

                }
                if (word[1].startsWith("r")) {
                    // show region [name]
                    final PAA_Region cmd = new PAA_Region();
                    // usage: /pa {arenaname} region [regionname] border | check a region border
                    cmd.commit(arena, player, new String[]{word[2], "border"});
                    return;
                } else if (word[1].startsWith("s")) {
                    // show spawn [name]
                    final Set<PASpawn> spawns = SpawnManager.getPASpawnsStartingWith(arena, word[2]);
                    for (PASpawn spawn : spawns) {
                        final Location loc = spawn.getPALocation().toLocation();

                        player.sendBlockChange(loc, Material.WHITE_WOOL.createBlockData());
                        new Remover(loc);
                    }
                    return;
                } else if (word[2].startsWith("b")) {
                    // show block [name]
                    final Set<PABlock> blocks = SpawnManager.getPABlocksContaining(arena, word[2]);
                    for (PABlock block : blocks) {
                        final Location loc = block.getLocation().toLocation();

                        player.sendBlockChange(loc, Material.WHITE_WOOL.createBlockData());
                        new Remover(loc);
                    }
                    return;
                }
            }
        } else if (word[0].startsWith("r")) {
            /*
              region [region] move 2 up | move region [region] 2 blocks up",
              region [region] expand 10 north | expand region [region] 10 block to the north",
              region [region] expand 15 out | expand region [region] 15 blocks in all directions",
             */
            if (word.length == 5) {
                final ArenaRegion region = arena.getRegion(word[1]);
                int amount = 0;
                try {
                    amount = Integer.parseInt(word[3]);
                } catch (final Exception e) {

                }
                if (region == null || amount == 0) {
                    if (region == null) {
                        arena.msg(player, MSG.ERROR_REGION_NOTFOUND, word[1]);
                    } else {
                        arena.msg(player, MSG.ERROR_NOT_NUMERIC, word[3]);
                    }
                } else {
                    if (word[2].startsWith("m")) {
                        // move
                        final BlockFace direction = StringParser.parseToBlockFace(word[4]);
                        if (direction != null) {
                            region.getShape().move(direction, Integer.parseInt(word[3]));
                        }
                    } else if (word[2].startsWith("e")) {
                        // expand
                        final BlockFace direction = StringParser.parseToBlockFace(word[4]);
                        if (direction != null) {
                            region.getShape().extend(direction, Integer.parseInt(word[3]));
                        }
                    }
                }
            } else if (word.length == 3 && word[2].startsWith("r")) {
                final ArenaRegion region = arena.getRegion(word[1]);
                if (region == null) {
                    arena.msg(player, MSG.ERROR_REGION_NOTFOUND, word[1]);
                } else {
                    // remove
                    final PAA_Region cmd = new PAA_Region();
                    // usage: /pa {arenaname} region remove [regionname] | remove a region
                    cmd.commit(arena, player, new String[]{"remove", region.getRegionName()});
                    return;
                }
            }
        } else if (word[0].startsWith("d")) {
            final PAA_Setup cmd = new PAA_Setup();
            cmd.commit(arena, player, new String[]{""});
            return;
        }

        arena.msg(player, MSG.ERROR_ERROR, message);
        arena.msg(player, HELP.SETUP_CMDS);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("setup");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!su");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"done"});
        if (arena == null) {
            return result;
        }

        for (PASpawn spawn : arena.getSpawns()) {
            result.define(new String[]{"show", spawn.getName()});
        }

        for (PABlock block : arena.getBlocks()) {
            result.define(new String[]{"show", block.getName()});
        }

        for (ArenaRegion ar : arena.getRegions()) {
            result.define(new String[]{"show", ar.getRegionName()});
            result.define(new String[]{"region", ar.getRegionName()});
        }

        return result;
    }
}
