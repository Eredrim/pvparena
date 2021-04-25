package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.*;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.Utils.getSerializableItemStacks;

/**
 * <pre>
 * Arena Goal class "PlayerKillreward"
 * </pre>
 * <p/>
 * This will feature several ways of altering player rewards
 * <p/>
 * get better gear until you reached the final step and then win
 *
 * @author slipcor
 */

public class GoalPlayerKillReward extends ArenaGoal {

    public static final String SPAWN = "spawn";

    public GoalPlayerKillReward() {
        super("PlayerKillReward");
    }

    private Map<Integer, ItemStack[][]> itemMapCubed;

    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean isFreeForAll() {
        return true;
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public boolean checkCommand(final String string) {
        return "killrewards".equalsIgnoreCase(string) || "!kr".equalsIgnoreCase(string);
    }

    @Override
    public List<String> getGoalCommands() {
        return Collections.singletonList("killrewards");
    }

    @Override
    public List<String> getGoalShortCommands() {
        return Collections.singletonList("!kr");
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{int}", "remove"});
        return result;
    }

    @Override
    public boolean checkEnd() {
        final int count = this.getPlayerLifeMap().size();
        return (count <= 1); // yep. only one player left. go!
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingFFASpawn(this.arena, spawns);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2,
                3})) {
            return;
        }

        // /pa [arena] !kr [number] {remove}

        final int value;

        try {
            value = Integer.parseInt(args[1]);
        } catch (final Exception e) {
            this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[1]);
            return;
        }
        if (args.length > 2) {
            this.getItemMap().remove(value);
            this.arena.msg(sender, MSG.GOAL_KILLREWARD_REMOVED, args[1]);
        } else {
            if (!(sender instanceof Player)) {
                Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
                return;
            }
            final Player player = (Player) sender;

            ItemStack[][] content = new ItemStack[][]{
                    player.getInventory().getStorageContents(),
                    new ItemStack[]{player.getInventory().getItemInOffHand()},
                    player.getInventory().getArmorContents()
            };

            this.getItemMap().put(value, content);
            this.arena.msg(sender, MSG.GOAL_KILLREWARD_ADDED, args[1]);

        }

        this.saveItems();
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[PKW] already ending");
            return;
        }
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);

        for (ArenaTeam team : this.arena.getNotEmptyTeams()) {
            for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                if (arenaPlayer.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                        "END");

                ArenaModuleManager.announce(this.arena,
                        Language.parse(MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                        "WINNER");

                this.arena.broadcast(Language.parse(MSG.PLAYER_HAS_WON,
                        arenaPlayer.getName()));
            }

            if (ArenaModuleManager.commitEnd(this.arena, team)) {
                return;
            }
        }
        this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void parsePlayerDeath(Player player, PADeathInfo deathInfo) {
            if (!this.getPlayerLifeMap().containsKey(player)) {
                return;
            }
            if (!this.arena.getConfig().getBoolean(CFG.GOAL_PLAYERKILLREWARD_GRADUALLYDOWN)) {
                this.getPlayerLifeMap().put(player, this.getDefaultRemainingKills());
            }
        class ResetRunnable implements Runnable {
            private final Player player;

            @Override
            public void run() {
                this.reset(this.player);
            }

            ResetRunnable(final Player player) {
                this.player = player;
            }

            private void reset(final Player player) {
                if (!GoalPlayerKillReward.this.getPlayerLifeMap().containsKey(player)) {
                    return;
                }

                final int iLives = GoalPlayerKillReward.this.getPlayerLifeMap().get(player);
                if (ArenaPlayer.fromPlayer(player).getStatus() != PlayerStatus.FIGHT) {
                    return;
                }
                if (!GoalPlayerKillReward.this.arena.getConfig().getBoolean(CFG.GOAL_PLAYERKILLREWARD_ONLYGIVE)) {
                    InventoryManager.clearInventory(player);
                }
                if (GoalPlayerKillReward.this.getItemMap().containsKey(iLives)) {
                    ArenaClass.equip(player, GoalPlayerKillReward.this.getItemMap().get(iLives));
                } else {
                    ArenaPlayer.fromPlayer(player).getArenaClass()
                            .equip(player);
                }
            }

        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new ResetRunnable(player), 4L);
        final Player killer = deathInfo.getKiller();

        if (killer == null) {
            return;
        }

        int iLives = this.getPlayerLifeMap().get(killer);
        debug(this.arena, killer, "kills to go for " + killer.getName() + ": " + iLives);
        if (iLives <= 1) {
            // player has won!
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "trigger:" + killer.getName(),
                    "playerKill:" + killer.getName() + ':' + player.getName(), "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
            final Set<ArenaPlayer> arenaPlayers = new HashSet<>();
            for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
                if (arenaPlayer.getPlayer().equals(killer)) {
                    continue;
                }
                arenaPlayers.add(arenaPlayer);
            }
            for (ArenaPlayer arenaPlayer : arenaPlayers) {
                this.getPlayerLifeMap().remove(arenaPlayer.getPlayer());

                arenaPlayer.setStatus(PlayerStatus.LOST);
                arenaPlayer.addLosses();
            }

            if (ArenaManager.checkAndCommit(this.arena, false)) {
                return;
            }
            WorkflowManager.handleEnd(this.arena, false);
        } else {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerKill:" +
                    killer.getName() + ':' + player.getName(), "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
            iLives--;
            this.getPlayerLifeMap().put(killer, iLives);
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(),
                    new ResetRunnable(killer), 4L);
        }
    }

    private Map<Integer, ItemStack[][]> getItemMap() {
        if (this.itemMapCubed == null) {
            this.itemMapCubed = new HashMap<>();
        }
        return this.itemMapCubed;
    }

    @Override
    public void initiate(final Player player) {
        this.getPlayerLifeMap().put(player, this.getDefaultRemainingKills());
    }

    private int getDefaultRemainingKills() {
        int max = 0;
        for (int i : this.getItemMap().keySet()) {
            max = Math.max(max, i);
        }
        return max + 1;
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        this.getPlayerLifeMap().remove(player);
    }

    @Override
    public void parseStart() {
        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            for (ArenaPlayer arenaPlayer : arenaTeam.getTeamMembers()) {
                this.getPlayerLifeMap().put(arenaPlayer.getPlayer(), this.getDefaultRemainingKills());
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        final ConfigurationSection cs = (ConfigurationSection) config.get("goal.playerkillrewards");

        if (cs != null) {
            for (String line : cs.getKeys(false)) {
                try {
                    this.getItemMap().put(Integer.parseInt(line.substring(2)),
                        new ItemStack[][] {
                            cs.getList(line + ".items").toArray(new ItemStack[0]),
                            cs.getList(line + ".offhand").toArray(new ItemStack[]{new ItemStack(Material.AIR, 1)}),
                            cs.getList(line + ".armor").toArray(new ItemStack[0])
                        });
                } catch (final Exception ignored) {
                }
            }
        }

        if (this.getItemMap().size() < 1) {

            this.getItemMap().put(5, new ItemStack[][]{
                        new ItemStack[]{new ItemStack(Material.WOODEN_SWORD, 1)},
                        new ItemStack[]{new ItemStack(Material.AIR, 1)},
                        new ItemStack[]{
                                new ItemStack(Material.LEATHER_HELMET, 1),
                                new ItemStack(Material.LEATHER_CHESTPLATE, 1),
                                new ItemStack(Material.LEATHER_LEGGINGS, 1),
                                new ItemStack(Material.LEATHER_BOOTS, 1),
                        },
                    });
            this.getItemMap().put(4, new ItemStack[][]{
                    new ItemStack[]{new ItemStack(Material.STONE_SWORD, 1)},
                    new ItemStack[]{new ItemStack(Material.AIR, 1)},
                    new ItemStack[]{
                            new ItemStack(Material.CHAINMAIL_HELMET, 1),
                            new ItemStack(Material.CHAINMAIL_CHESTPLATE, 1),
                            new ItemStack(Material.CHAINMAIL_LEGGINGS, 1),
                            new ItemStack(Material.CHAINMAIL_BOOTS, 1),
                    },
            });
            this.getItemMap().put(3, new ItemStack[][]{
                    new ItemStack[]{new ItemStack(Material.IRON_SWORD, 1)},
                    new ItemStack[]{new ItemStack(Material.AIR, 1)},
                    new ItemStack[]{
                            new ItemStack(Material.GOLDEN_HELMET, 1),
                            new ItemStack(Material.GOLDEN_CHESTPLATE, 1),
                            new ItemStack(Material.GOLDEN_LEGGINGS, 1),
                            new ItemStack(Material.GOLDEN_BOOTS, 1),
                    },
            });
            this.getItemMap().put(2, new ItemStack[][]{
                    new ItemStack[]{new ItemStack(Material.DIAMOND_SWORD, 1)},
                    new ItemStack[]{new ItemStack(Material.AIR, 1)},
                    new ItemStack[]{
                            new ItemStack(Material.IRON_HELMET, 1),
                            new ItemStack(Material.IRON_CHESTPLATE, 1),
                            new ItemStack(Material.IRON_LEGGINGS, 1),
                            new ItemStack(Material.IRON_BOOTS, 1),
                    },
            });
            this.getItemMap().put(1, new ItemStack[][]{
                    new ItemStack[]{new ItemStack(Material.DIAMOND_SWORD, 1)},
                    new ItemStack[]{new ItemStack(Material.AIR, 1)},
                    new ItemStack[]{
                            new ItemStack(Material.DIAMOND_HELMET, 1),
                            new ItemStack(Material.DIAMOND_CHESTPLATE, 1),
                            new ItemStack(Material.DIAMOND_LEGGINGS, 1),
                            new ItemStack(Material.DIAMOND_BOOTS, 1),
                    },
            });

            this.saveItems();
        }
    }

    private void saveItems() {
        for (int i : this.getItemMap().keySet()) {
            this.arena.getConfig().setManually("goal.playerkillrewards.kr" + i+".items",
                    getSerializableItemStacks(this.getItemMap().get(i)[0]));
            this.arena.getConfig().setManually("goal.playerkillrewards.kr" + i+".offhand",
                    getSerializableItemStacks(this.getItemMap().get(i)[1]));
            this.arena.getConfig().setManually("goal.playerkillrewards.kr" + i+".armor",
                    getSerializableItemStacks(this.getItemMap().get(i)[2]));
        }
        this.arena.getConfig().save();
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.getDefaultRemainingKills() - (this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0));
            if (scores.containsKey(arenaPlayer.getName())) {
                scores.put(arenaPlayer.getName(), scores.get(arenaPlayer.getName()) + score);
            } else {
                scores.put(arenaPlayer.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
    }

}
