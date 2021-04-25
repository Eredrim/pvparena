package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Team class</pre>
 * <p/>
 * contains Arena Team methods and variables for quicker access
 *
 * @author slipcor
 * @version v0.10.2
 */

public class ArenaTeam {

    private final Set<ArenaPlayer> players;
    private final ChatColor color;
    private final String name;

    /**
     * create an arena team instance
     *
     * @param name  the arena team name
     * @param color the arena team color string
     */
    public ArenaTeam(final String name, final String color) {
        this.players = new HashSet<>();
        this.color = ColorUtils.getChatColorFromDyeColor(color);
        this.name = name;
    }

    /**
     * add an arena player to the arena team
     *
     * @param arenaPlayer the player to add
     */
    public void add(final ArenaPlayer arenaPlayer) {
        this.players.add(arenaPlayer);
        debug(arenaPlayer, "Added player {} to team {}", arenaPlayer.getName(), this.name);
        arenaPlayer.getArena().increasePlayerCount();
    }

    /**
     * colorize a player name
     *
     * @param player the player to colorize
     * @return the colorized player name
     */
    public String colorizePlayer(final Player player) {
        return this.color + player.getName();
    }

    /**
     * return the team color
     *
     * @return the team color
     */
    public ChatColor getColor() {
        return this.color;
    }

    /**
     * colorize the team name
     *
     * @return the colorized team name
     */
    public String getColoredName() {
        return this.color + this.name;
    }

    /**
     * return the team color code
     *
     * @return the team color code
     */
    public String getColorCodeString() {
        return '&' + Integer.toHexString(this.color.ordinal());
    }

    /**
     * return the team name
     *
     * @return the team name
     */
    public String getName() {
        return this.name;
    }

    /**
     * return the team members
     *
     * @return a HashSet of all arena players
     */
    public Set<ArenaPlayer> getTeamMembers() {
        return this.players;
    }

    public boolean hasPlayer(final Player player) {
        return this.players.contains(ArenaPlayer.fromPlayer(player));
    }

    public boolean isEveryoneReady() {
        for (ArenaPlayer ap : this.players) {
            if (ap.getStatus() != PlayerStatus.READY) {
                return false;
            }
        }
        return true;
    }

    /**
     * remove a player from the team
     *
     * @param player the player to remove
     */
    public void remove(final ArenaPlayer player) {
        this.players.remove(player);
    }

    /**
     * Is a Team without any team member
     *
     * @return true if no team member
     */
    public boolean isEmpty() {
        return this.getTeamMembers().isEmpty();
    }

    /**
     * Is a Team with at least one team member
     *
     * @return true if at least one team member
     */
    public boolean isNotEmpty() {
        return !this.getTeamMembers().isEmpty();
    }

    @Override
    public String toString() {
        return this.name;
    }

    public void sendMessage(ArenaPlayer sender, String msg) {
        debug(sender, "@{}: {}", this.name, msg);
        synchronized (this) {
            this.getTeamMembers().forEach(member -> {
                if (sender == null) {
                    member.getPlayer().sendMessage(String.format("%s[%s]%s: %s", this.color, this.name, ChatColor.RESET, msg));
                } else {
                    String reset = sender.getArena().getConfig().getBoolean(Config.CFG.CHAT_COLORNICK) ? "" : ChatColor.RESET.toString();
                    member.getPlayer().sendMessage(String.format("%s[%s] %s%s%s: %s", this.color, this.name, reset, sender.getName(), ChatColor.RESET, msg));
                }
            });
        }
    }
}
