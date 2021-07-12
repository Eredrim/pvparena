package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Iterator;
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

public class StandardLounge extends ArenaModule {

    private static final int PRIORITY = 2;

    public StandardLounge() {
        super("StandardLounge");
        debug = new Debug(300);
    }

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        // not random! we need teams * 2 (lounge + spawn) + exit + spectator
        debug.i("parsing not random");
        final Iterator<String> iter = list.iterator();
        int lounges = 0;
        while (iter.hasNext()) {
            final String spawnName = iter.next();
            debug.i("parsing '" + spawnName + '\'');
            if (arena.isFreeForAll()) {
                if ("lounge".equals(spawnName)) {
                    lounges++;
                }
            } else {
                if (spawnName.endsWith("lounge") && !"lounge".equals(spawnName)) {
                    lounges++;
                }
            }

        }

        int neededCount = arena.getTeams().size();

        for (final ArenaTeam team : arena.getTeams()) {
            if ("infected".equals(team.getName()) ||
                    "tank".equalsIgnoreCase(team.getName())) {
                neededCount--;
            }
        }
        if (lounges == neededCount) {
            return null;
        }

        return lounges + "/" + arena.getTeams().size() + "x lounge";
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

        if (arena.isLocked() && !PermissionManager.hasAdminPerm(player)
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

        if (aPlayer.getArenaClass() == null) {
            final String autoClass =
                    arena.getArenaConfig().getBoolean(CFG.USES_PLAYERCLASSES) ?
                            arena.getClass(player.getName()) != null ? player.getName() : arena.getArenaConfig().getString(CFG.READY_AUTOCLASS)
                            : arena.getArenaConfig().getString(CFG.READY_AUTOCLASS);
            if (autoClass != null && !"none".equals(autoClass) && arena.getClass(autoClass) == null) {
                result.setError(this, Language.parse(arena,
                        MSG.ERROR_CLASS_NOT_FOUND, "autoClass"));
                return result;
            }
        }

        result.setPriority(this, PRIORITY);
        return result;
    }
/*
    @Override
    public PACheck checkStart(final ArenaPlayer player, final PACheck result) {
        if (result.getPriority() > PRIORITY) {
            return result; // Something already is of higher priority, ignore!
        }

        if (arena == null) {
            return result; // arena is null - maybe some other mod wants to
            // handle that? ignore!
        }

        final String error = String.valueOf(arena.ready());

        if (error != null) {
            result.setError(this, error);
            return result;
        }
        result.setPriority(this, PRIORITY);
        return result;
    }
*/
    @Override
    public boolean hasSpawn(final String spawnName) {
        if (arena.isFreeForAll()) {
            return spawnName.startsWith("lounge");
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (spawnName.startsWith(team.getName() + "lounge")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public void commitJoin(final Player sender, final ArenaTeam team) {
        // standard join --> lounge
        final ArenaPlayer player = ArenaPlayer.parsePlayer(sender.getName());
        player.setLocation(new PALocation(player.get().getLocation()));

        // ArenaPlayer.prepareInventory(arena, ap.get());
        player.setArena(this.arena);
        team.add(player);

        if (this.arena.isFreeForAll()) {
            this.arena.tpPlayerToCoordNameForJoin(player, "lounge", true);
        } else {
            this.arena.tpPlayerToCoordNameForJoin(player, team.getName() + "lounge", true);
        }

        player.setStatus(Status.LOUNGE);
        arena.msg(sender, Language.parse(arena, CFG.MSG_LOUNGE));
        if (arena.isFreeForAll()) {
            arena.msg(sender,
                    Language.parse(arena, CFG.MSG_YOUJOINED,
                    Integer.toString(team.getTeamMembers().size()),
                    Integer.toString(arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS))
            ));
            arena.broadcastExcept(
                    sender,
                    Language.parse(arena, CFG.MSG_PLAYERJOINED,
                            sender.getName(),
                            Integer.toString(team.getTeamMembers().size()),
                            Integer.toString(arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS))
                    ));
        } else {

            arena.msg(sender,
                    Language.parse(arena, CFG.MSG_YOUJOINEDTEAM,
                            team.getColoredName() + ChatColor.COLOR_CHAR + 'r',
                            Integer.toString(team.getTeamMembers().size()),
                            Integer.toString(arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS))
            ));

            arena.broadcastExcept(
                    sender,
                    Language.parse(arena, CFG.MSG_PLAYERJOINEDTEAM,
                            sender.getName(),
                            team.getColoredName() + ChatColor.COLOR_CHAR + 'r',
                            Integer.toString(team.getTeamMembers().size()),
                            Integer.toString(arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS))
            ));
        }

        if (player.getState() == null) {

            final Arena arena = player.getArena();

            player.createState(player.get());
            ArenaPlayer.backupAndClearInventory(arena, player.get());
            player.dump();


            if (player.getArenaTeam() != null && player.getArenaClass() == null) {
                final String autoClass = arena.getArenaConfig().getString(CFG.READY_AUTOCLASS);
                if (autoClass != null && !"none".equals(autoClass) && arena.getClass(autoClass) != null) {
                    arena.chooseClass(player.get(), null, autoClass);
                }
                if (autoClass == null) {
                    arena.msg(player.get(), Language.parse(arena, MSG.ERROR_CLASS_NOT_FOUND, "autoClass"));
                }
            }
        } else {
            PVPArena.instance.getLogger().warning("Player has a state while joining: " + player.getName());
        }
    }

    @Override
    public void parseJoin(final CommandSender player, final ArenaTeam team) {
        if (arena.startRunner != null) {
            arena.countDown();
        }
    }
}