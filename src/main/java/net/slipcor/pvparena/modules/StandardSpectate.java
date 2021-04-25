package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * <pre>
 * Arena Module class "StandardLounge"
 * </pre>
 * <p/>
 * Enables joining to lounges instead of the battlefield
 *
 * @author slipcor
 */

public class StandardSpectate extends ArenaModule {

    public static final String SPECTATOR = "spectator";

    public StandardSpectate() {
        super("StandardSpectate");
    }

    private static final int PRIORITY = 2;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        final Set<PASpawn> missing = new HashSet<>();
        if (spawns.stream().noneMatch(spawn ->
                (spawn.getName().equals(SPECTATOR))
                        && spawn.getTeamName() == null)) {
            missing.add(new PASpawn(null, SPECTATOR, null, null));
        }
        return missing;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean handleSpectate(Player player) throws GameplayException {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        if (arenaPlayer.getArena() != null) {
            throw new GameplayException(Language.parse(MSG.ERROR_ARENA_ALREADY_PART_OF, arenaPlayer.getArena().getName()));
        }

        return true;
    }

    @Override
    public void commitSpectate(final Player player) {
        // standard join --> lounge
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(player.getLocation()));

        arenaPlayer.setArena(this.arena);
        arenaPlayer.setStatus(PlayerStatus.WATCH);

        TeleportManager.teleportPlayerToRandomSpawn(this.arena, arenaPlayer, SpawnManager.getPASpawnsStartingWith(this.arena, SPECTATOR));
        this.arena.msg(player, MSG.NOTICE_WELCOME_SPECTATOR);

        if (arenaPlayer.getState() == null) {

            final Arena arena = arenaPlayer.getArena();

            arenaPlayer.createState(player);
            ArenaPlayer.backupAndClearInventory(arena, player);
            arenaPlayer.dump();


            if (arenaPlayer.getArenaTeam() != null && arenaPlayer.getArenaClass() == null) {
                String autoClass = arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS);
                if(arena.getConfig().getBoolean(CFG.USES_PLAYER_OWN_INVENTORY) && arena.getClass(player.getName()) != null) {
                    autoClass = player.getName();
                }

                if (autoClass != null && arena.getClass(autoClass) != null) {
                    arena.chooseClass(player, null, autoClass);
                }
            }
        }
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String teamName) {
        return SPECTATOR.equalsIgnoreCase(spawnName);
    }
}
