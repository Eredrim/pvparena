package net.slipcor.pvparena.classes;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

public class PAClaimBar {
    final Arena arena;
    final BossBar bar;
    BukkitTask task;
    float progress = 0;

    public PAClaimBar(Arena arena, String title, ChatColor color, Location location, int range, long millis) {
        this.bar = Bukkit.getServer().createBossBar(title, fromChatColor(color), BarStyle.SEGMENTED_10);
        this.arena = arena;

        for (Entity entity : location.getWorld().getNearbyEntities(location, range, range, range)) {
            if (entity instanceof Player) {
                this.bar.addPlayer((Player) entity);
            }
        }
        this.bar.setProgress(0);

        long interval = millis / 10L;

        this.task = Bukkit.getScheduler().runTaskTimer(PVPArena.getInstance(), new ClaimRunner(), interval, interval);

        debug(arena, "interval: " + interval);
    }

    public void restart(String title, ChatColor color, Location location, int range, long millis) {
        try {
            this.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.bar.setTitle(title);
        this.bar.setColor(fromChatColor(color));
        for (Entity entity : location.getWorld().getNearbyEntities(location, range, range, range)) {
            if (entity instanceof Player) {
                this.bar.addPlayer((Player) entity);
            }
        }
        this.bar.setProgress(0);

        long interval = millis / 10L;

        this.task = Bukkit.getScheduler().runTaskTimer(PVPArena.getInstance(), new ClaimRunner(), interval, interval);

        debug(this.arena, "interval: " + interval);
    }

    public void stop() {
        this.progress = 0;
        this.bar.removeAll();
        this.task.cancel();
    }

    private class ClaimRunner implements Runnable {
        @Override
        public void run() {
            trace(PAClaimBar.this.arena, "progress: " + PAClaimBar.this.progress);
            if (++PAClaimBar.this.progress > 9) {
                PAClaimBar.this.bar.setProgress(PAClaimBar.this.progress /10f);
                PAClaimBar.this.stop();
            } else {
                PAClaimBar.this.bar.setProgress(PAClaimBar.this.progress /10f);
            }
        }
    }

    private static BarColor fromChatColor(ChatColor chatColor) {
        switch (chatColor) {
            case RED:
                return BarColor.PINK;
            case BLUE:
            case DARK_BLUE:
            case DARK_AQUA:
            case AQUA:
                return BarColor.BLUE;
            case DARK_RED:
                return BarColor.RED;
            case GREEN:
            case DARK_GREEN:
                return BarColor.GREEN;
            case YELLOW:
            case GOLD:
                return BarColor.YELLOW;
            case DARK_PURPLE:
            case LIGHT_PURPLE:
                return BarColor.PURPLE;
            default:
                return BarColor.WHITE;
        }
    }
}
