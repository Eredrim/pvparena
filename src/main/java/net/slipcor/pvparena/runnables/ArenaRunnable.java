package net.slipcor.pvparena.runnables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <pre>Arena Runnable class</pre>
 * <p/>
 * The interface for arena timers
 *
 * @author slipcor
 * @version v0.10.0.1
 */

public abstract class ArenaRunnable extends BukkitRunnable {

    protected static final Map<Integer, String> MESSAGES = new HashMap<>();
    final String sSeconds = Language.parse(MSG.TIME_SECONDS);
    final String sMinutes = Language.parse(MSG.TIME_MINUTES);

    protected final String message;
    protected Integer seconds;
    protected final String sPlayer;
    protected final Arena arena;
    protected final Boolean global;

    /**
     * Spam the message of the remaining time to... someone, probably:
     *
     * @param message the Language.parse("**") String to wrap
     * @param arena   the arena to spam to (!global) or to exclude (global)
     * @param player  the player to spam to (!global && !arena) or to exclude (global && arena)
     * @param seconds the seconds remaining
     * @param global  the trigger to generally spam to everyone or to specific arenas/players
     */
    protected ArenaRunnable(final String message, final Integer seconds, final Player player, final Arena arena, final Boolean global) {
        super();
        this.message = message;
        this.seconds = ArenaModuleManager.parseStartCountDown(seconds, message, arena, global);
        this.sPlayer = player == null ? null : player.getName();
        this.arena = arena;
        this.global = global;

        ConfigurationSection section = null;

        if (arena == null) {
            if (Language.getConfig() != null) {
                section = Language.getConfig().getConfigurationSection("time_intervals");
            }
        } else {
            section = arena.getConfig().getYamlConfiguration().getConfigurationSection("time_intervals");
        }
        if (section == null) {
            PVPArena.getInstance().getLogger().warning("Language strings 'time_intervals' not found, loading defaults!");
            MESSAGES.put(1, "1..");
            MESSAGES.put(2, "2..");
            MESSAGES.put(3, "3..");
            MESSAGES.put(4, "4..");
            MESSAGES.put(5, "5..");
            MESSAGES.put(10, "10 " + this.sSeconds);
            MESSAGES.put(20, "20 " + this.sSeconds);
            MESSAGES.put(30, "30 " + this.sSeconds);
            MESSAGES.put(60, "60 " + this.sSeconds);
            MESSAGES.put(120, "2 " + this.sMinutes);
            MESSAGES.put(180, "3 " + this.sMinutes);
            MESSAGES.put(240, "4 " + this.sMinutes);
            MESSAGES.put(300, "5 " + this.sMinutes);
            MESSAGES.put(600, "10 " + this.sMinutes);
            MESSAGES.put(1200, "20 " + this.sMinutes);
            MESSAGES.put(1800, "30 " + this.sMinutes);
            MESSAGES.put(2400, "40 " + this.sMinutes);
            MESSAGES.put(3000, "50 " + this.sMinutes);
            MESSAGES.put(3600, "60 " + this.sMinutes);
        } else {
            for (String key : section.getKeys(true)) {
                String content = section.getString(key);
                try {
                    Integer value = Integer.parseInt(key);
                    MESSAGES.put(value, content.replace("%m", this.sMinutes).replace("%s", this.sSeconds));
                } catch (Exception e) {

                }
            }
        }

        this.runTaskTimer(PVPArena.getInstance(), 20L, 20L);
    }

    protected void spam() {
        if ((this.message == null) || (MESSAGES.get(this.seconds) == null)) {
            return;
        }
        final MSG msg = MSG.getByNode(this.message);
        if (msg == null) {
            PVPArena.getInstance().getLogger().warning("MSG not found: " + this.message);
            return;
        }
        final String message = this.seconds > 5 ? Language.parse(msg, MESSAGES.get(this.seconds)) : MESSAGES.get(this.seconds);
        if (this.global) {
            final Collection<? extends Player> players = Bukkit.getOnlinePlayers();

            for (Player p : players) {
                try {
                    if (this.arena != null && this.arena.hasPlayer(p)) {
                        continue;
                    }
                    if (p.getName().equals(this.sPlayer)) {
                        continue;
                    }
                    Arena.pmsg(p, message);
                } catch (final Exception e) {
                }
            }

            return;
        }
        if (this.arena != null) {
            final Set<ArenaPlayer> players = this.arena.getFighters();
            for (ArenaPlayer ap : players) {
                if (ap.getName().equals(this.sPlayer)) {
                    continue;
                }
                if (ap.getPlayer() != null) {
                    if (!ArenaModuleManager.checkCountOverride(this.arena, ap.getPlayer(), message)) {
                        this.arena.msg(ap.getPlayer(), message);
                    }
                }
            }
            return;
        }

        if (Bukkit.getPlayer(this.sPlayer) != null) {
            final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(this.sPlayer);
            if (aPlayer.getArena() == null) {
                Arena.pmsg(Bukkit.getPlayer(this.sPlayer), message);
            } else {
                if (!ArenaModuleManager.checkCountOverride(aPlayer.getArena(), aPlayer.getPlayer(), message)) {
                    aPlayer.getArena().msg(aPlayer.getPlayer(), message);
                }
            }
        }
    }

    @Override
    public void run() {
        this.spam();
        if (this.seconds <= 0) {
            this.commit();
            try {
                this.cancel();
            } catch (final IllegalStateException e) {
                this.warn();
            }
        }
        this.seconds--;
    }

    protected abstract void warn();

    protected abstract void commit();
}
