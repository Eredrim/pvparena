package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * <pre>
 * Arena Module class "BattlefieldJoin"
 * </pre>
 * <p/>
 * Enables direct joining to the battlefield
 *
 * @author slipcor
 */

public class BattlefieldJoin extends ArenaModule {

    private static final int PRIORITY = 1;

    Runnable runner;

    public BattlefieldJoin() {
        super("BattlefieldJoin");
        debug = new Debug(300);
    }

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    @Override
    public PACheck checkJoin(final CommandSender sender, final PACheck result, final boolean join) {
        if (!join) {
            return result; // we only care about joining, ignore spectators
        }
        if (result.getPriority() > PRIORITY) {
            return result; // Something already is of higher priority, ignore!
        }

        final Player player = (Player) sender;

        if (arena == null) {
            return result; // arena is null - maybe some other mod wants to
            // handle that? ignore!
        }

        if (arena.isLocked()
                && !PermissionManager.hasAdminPerm(player)
                && !PermissionManager.hasBuilderPerm(player, arena)) {
            result.setError(this, Language.parse(arena, MSG.ERROR_DISABLED));
            return result;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        if (aPlayer.getArena() != null) {
            aPlayer.getArena().getDebugger().i(getName(), sender);
            result.setError(this, Language.parse(arena,
                    MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(aPlayer.getArena())));
            return result;
        }

        result.setPriority(this, PRIORITY);
        return result;
    }

    @Override
    public void commitJoin(final Player sender, final ArenaTeam team) {
        // standard join --> lounge
        final ArenaPlayer player = ArenaPlayer.parsePlayer(sender.getName());
        player.setLocation(new PALocation(player.get().getLocation()));

        player.setArena(arena);
        player.setStatus(Status.LOUNGE);
        team.add(player);
        final Set<PASpawn> spawns = new HashSet<>();
        if (arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            final String arenaClass = player.getArenaClass().getName();
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName() + arenaClass + "spawn"));
        } else if (arena.isFreeForAll()) {
            if ("free".equals(team.getName())) {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, "spawn"));
            } else {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName()));
            }
        } else {
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(arena, team.getName() + "spawn"));
        }

        int pos = new Random().nextInt(spawns.size());

        for (final PASpawn spawn : spawns) {
            if (--pos < 0) {
                this.arena.tpPlayerToCoordNameForJoin(player, spawn.getName(), true);
                break;
            }
        }

        if (player.getState() == null) {

            final Arena arena = player.getArena();


            player.createState(player.get());
            ArenaPlayer.backupAndClearInventory(arena, player.get());
            player.dump();


            if (player.getArenaTeam() != null && player.getArenaClass() == null) {
                final String autoClass =
                        arena.getArenaConfig().getBoolean(CFG.USES_PLAYERCLASSES) ?
                                arena.getClass(player.getName()) != null ? player.getName() : arena.getArenaConfig().getString(CFG.READY_AUTOCLASS)
                                : arena.getArenaConfig().getString(CFG.READY_AUTOCLASS);
                if (autoClass != null && !"none".equals(autoClass) && arena.getClass(autoClass) != null) {
                    arena.chooseClass(player.get(), null, autoClass);
                }
                if (autoClass == null) {
                    arena.msg(player.get(), Language.parse(arena, MSG.ERROR_CLASS_NOT_FOUND, "autoClass"));
                    return;
                }
            }
        } else {
            PVPArena.instance.getLogger().warning("Player has a state while joining: " + player.getName());
        }

        class RunLater implements Runnable {

            @Override
            public void run() {
                Boolean check = PACheck.handleStart(arena, sender, true);
                if (check == null || !check) {
                    Bukkit.getScheduler().runTaskLater(PVPArena.instance, this, 10L);
                }
            }

        }

        if (runner == null) {
            runner = new RunLater();
            Bukkit.getScheduler().runTaskLater(PVPArena.instance, runner, 10L);
        }
    }

    @Override
    public void reset(boolean force) {
        runner = null;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}