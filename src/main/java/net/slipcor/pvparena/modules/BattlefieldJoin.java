package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;

import static net.slipcor.pvparena.classes.PASpawn.SPAWN;
import static net.slipcor.pvparena.config.Debugger.debug;

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
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean handleJoin(Player player) throws GameplayException {
        if (this.arena.isLocked() && !player.hasPermission("pvparena.admin")
                && !(player.hasPermission("pvparena.create") && this.arena.getOwner().equals(player.getName()))) {
            throw new GameplayException(Language.parse(MSG.ERROR_DISABLED));
        }

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if (arenaPlayer.getArena() != null) {
            debug(arenaPlayer.getArena(), player, this.getName());
            throw new GameplayException(Language.parse(
                    MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(arenaPlayer.getArena())));
        }

        return true;
    }

    @Override
    public void commitJoin(final Player player, final ArenaTeam arenaTeam) {
        // battlefield --> spawn
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.setArena(this.arena);
        arenaTeam.add(arenaPlayer);
        final Set<PASpawn> spawns = SpawnManager.selectSpawnsForPlayer(this.arena, arenaPlayer, SPAWN);

        TeleportManager.teleportPlayerToSpawnForJoin(this.arena, arenaPlayer, spawns, true);

        if (arenaPlayer.getState() == null) {
            arenaPlayer.createState(arenaPlayer.getPlayer());
            ArenaPlayer.backupAndClearInventory(this.arena, arenaPlayer.getPlayer());
            arenaPlayer.dump();

            if (arenaPlayer.getArenaTeam() != null && arenaPlayer.getArenaClass() == null) {
                String autoClass = this.arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS);
                if (this.arena.getConfig().getBoolean(CFG.USES_PLAYER_OWN_INVENTORY) && this.arena.getClass(arenaPlayer.getName()) != null) {
                    autoClass = arenaPlayer.getName();
                }
                if (autoClass != null && this.arena.getClass(autoClass) != null) {
                    this.arena.chooseClass(arenaPlayer.getPlayer(), null, autoClass);
                }
            }
        } else {
            PVPArena.getInstance().getLogger().warning(String.format("Player %s already have a state while joining arena %s",
                    arenaPlayer.getName(), this.arena.getName()));
        }

        class RunLater implements Runnable {

            @Override
            public void run() {
                Boolean check = WorkflowManager.handleStart(BattlefieldJoin.this.arena, player, true);
                if (check == null || !check) {
                    Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), this, 10L);
                }
            }

        }

        if (this.runner == null) {
            this.runner = new RunLater();
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), this.runner, 10L);
        }
    }

    @Override
    public void reset(boolean force) {
        this.runner = null;
    }
}
