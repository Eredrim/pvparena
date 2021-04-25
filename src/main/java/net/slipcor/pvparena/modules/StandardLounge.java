package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
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
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Module class "StandardLounge"
 * </pre>
 * <p/>
 * Enables joining to lounges instead of the battlefield
 *
 * @author slipcor
 */

public class StandardLounge extends ArenaModule {

    private static final int PRIORITY = 2;
    public static final String LOUNGE = "lounge";

    public StandardLounge() {
        super("StandardLounge");
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
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        debug("checking missing lounge spawn(s)");
        List<String> ignoredTeams = Arrays.asList("infected", "tank");
        final Set<PASpawn> missing = new HashSet<>();

        if (this.arena.isFreeForAll()) {
            if (spawns.stream().noneMatch(spawn ->
                    (spawn.getName().equals(LOUNGE))
                            && spawn.getTeamName() == null)) {
                missing.add(new PASpawn(null, LOUNGE, null, null));
            }
            return missing;
        } else {
            return this.arena.getTeams().stream()
                    .filter(team -> !ignoredTeams.contains(team.getName()))
                    .map(team -> new PASpawn(null, LOUNGE, team.getName(), null))
                    .filter(teamSpawn -> spawns.stream()
                            .noneMatch(spawn -> spawn.getName().equals(teamSpawn.getName())
                                    && spawn.getTeamName().equals(teamSpawn.getTeamName())))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public boolean handleJoin(Player player) throws GameplayException {
        if (this.arena.isLocked() && !player.hasPermission("pvparena.admin")
                && !(player.hasPermission("pvparena.create") && this.arena.getOwner().equals(player.getName()))) {
            throw new GameplayException(Language.parse(MSG.ERROR_DISABLED));
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (aPlayer.getArena() != null) {
            debug(aPlayer.getArena(), player, this.getName());
            throw new GameplayException(Language.parse(
                    MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(aPlayer.getArena())));
        }

        if (aPlayer.getArenaClass() == null) {
            String autoClass = this.arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS);
            if (this.arena.getConfig().getBoolean(CFG.USES_PLAYER_OWN_INVENTORY) && this.arena.getClass(player.getName()) != null) {
                autoClass = player.getName();
            }
            if (autoClass != null && this.arena.getClass(autoClass) == null) {
                throw new GameplayException(Language.parse(MSG.ERROR_CLASS_NOT_FOUND, "autoClass"));
            }
        }

        return true;
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String teamName) {
        if (this.arena.isFreeForAll()) {
            return spawnName.startsWith(LOUNGE);
        }
        return this.arena.getTeams().stream()
                .anyMatch(team -> spawnName.startsWith(LOUNGE)
                        && team.getName().equals(teamName));
    }

    @Override
    public void commitJoin(final Player player, final ArenaTeam arenaTeam) {
        // standard join --> lounge
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        // ArenaPlayer.prepareInventory(arena, ap.getPlayer());
        arenaPlayer.setArena(this.arena);
        arenaTeam.add(arenaPlayer);
        arenaPlayer.setStatus(PlayerStatus.LOUNGE);

        TeleportManager.teleportPlayerToSpawnForJoin(this.arena, arenaPlayer, SpawnManager.selectSpawnsForPlayer(this.arena, arenaPlayer, LOUNGE), true);

        this.arena.msg(player, Language.parse(this.arena, CFG.MSG_LOUNGE));
        if (this.arena.isFreeForAll()) {
            this.arena.msg(player,
                    Language.parse(this.arena, CFG.MSG_YOUJOINED,
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(CFG.READY_MAXPLAYERS))
                    ));
            this.arena.broadcastExcept(
                    player,
                    Language.parse(this.arena, CFG.MSG_PLAYERJOINED,
                            player.getName(),
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(CFG.READY_MAXPLAYERS))
                    ));
        } else {

            this.arena.msg(player,
                    Language.parse(this.arena, CFG.MSG_YOUJOINEDTEAM,
                            arenaTeam.getColoredName() + ChatColor.COLOR_CHAR + 'r',
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(CFG.READY_MAXPLAYERS))
                    ));

            this.arena.broadcastExcept(
                    player,
                    Language.parse(this.arena, CFG.MSG_PLAYERJOINEDTEAM,
                            player.getName(),
                            arenaTeam.getColoredName() + ChatColor.COLOR_CHAR + 'r',
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(CFG.READY_MAXPLAYERS))
                    ));
        }

        if (arenaPlayer.getState() == null) {

            final Arena arena = arenaPlayer.getArena();

            arenaPlayer.createState(arenaPlayer.getPlayer());
            ArenaPlayer.backupAndClearInventory(arena, arenaPlayer.getPlayer());
            arenaPlayer.dump();


            if (arenaPlayer.getArenaTeam() != null && arenaPlayer.getArenaClass() == null) {
                final String autoClass = arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS);
                if (autoClass != null && arena.getClass(autoClass) != null) {
                    arena.chooseClass(arenaPlayer.getPlayer(), null, autoClass);
                }
            }
        } else {
            PVPArena.getInstance().getLogger().warning("Player has a state while joining: " + arenaPlayer.getName());
        }
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        if (this.arena.startRunner != null) {
            this.arena.countDown();
        }
    }
}
