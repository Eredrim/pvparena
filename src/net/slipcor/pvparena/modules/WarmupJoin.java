package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.runnables.ArenaWarmupRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * <pre>Arena Module class "WarmupJoin"</pre>
 * <p/>
 * Enables a warmup countdown before joining the arena
 *
 * @author slipcor
 */

public class WarmupJoin extends ArenaModule {

    private static final int PRIORITY = 3;

    private Set<ArenaPlayer> playerSet;

    private boolean announced = false;

    public WarmupJoin() {
        super("WarmupJoin");
        debug = new Debug(300);
    }

    public static boolean didNotAnnounceYet(Arena arena) {
        for (ArenaModule mod : arena.getMods()) {
            if (mod instanceof WarmupJoin) {
                return !((WarmupJoin) mod).announced;
            }
        }
        return true;
    }

    @Override
    public String version() {
        return PVPArena.instance.getDescription().getVersion();
    }

    @Override
    public PACheck checkJoin(final CommandSender sender, final PACheck result, final boolean join) {

        if (result.getPriority() > PRIORITY) {
            return result; // Something already is of higher priority, ignore!
        }

        final Player player = (Player) sender;

        if (arena == null) {
            return result; // arena is null - maybe some other mod wants to handle that? ignore!
        }


        if (arena.isLocked() && !PermissionManager.hasAdminPerm(player) && !PermissionManager.hasBuilderPerm(player, arena)) {
            result.setError(this, Language.parse(arena, MSG.ERROR_DISABLED));
            return result;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        if (getPlayerSet().contains(aPlayer)) {
            return result;
        }

        if (aPlayer.getArena() != null) {
            aPlayer.getArena().getDebugger().i(getName(), sender);
            result.setError(this, Language.parse(arena, MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(aPlayer.getArena())));
            return result;
        }
        getPlayerSet().add(aPlayer);

        result.setPriority(this, PRIORITY);
        return result;
    }

    @Override
    public void commitJoin(final Player sender, final ArenaTeam team) {
        new ArenaWarmupRunnable(arena, ArenaPlayer.parsePlayer(sender.getName()), team.getName(), false, arena.getArenaConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
        announced = true;
    }

    @Override
    public void commitSpectate(final Player sender) {
        new ArenaWarmupRunnable(arena, ArenaPlayer.parsePlayer(sender.getName()), null, true, arena.getArenaConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: " +
                arena.getArenaConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    private Set<ArenaPlayer> getPlayerSet() {
        if (playerSet == null) {
            playerSet = new HashSet<>();
        }
        return playerSet;
    }

    @Override
    public void reset(final boolean force) {
        getPlayerSet().clear();
        announced = false;
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        getPlayerSet().remove(ArenaPlayer.parsePlayer(player.getName()));
    }
}