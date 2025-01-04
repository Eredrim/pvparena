package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.PAG_Arenaclass;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.arena.PlayerStatus.*;
import static net.slipcor.pvparena.commands.PAI_Ready.checkReadyRequirementsBeforeFight;
import static net.slipcor.pvparena.commands.PAI_Ready.checkReadyRequirementsDuringFight;
import static net.slipcor.pvparena.config.Debugger.debug;

public final class InteractionManager {

    public static void handleClassSignInteract(PlayerInteractEvent event, Arena arena, ArenaPlayer arenaPlayer) {
        debug(arenaPlayer, "sign click!");
        final Sign sign = (Sign) event.getClickedBlock().getState();

        if ("custom".equalsIgnoreCase(sign.getLine(0)) || arena.getArenaClass(sign.getLine(0)) != null) {
            // Player status checks are handled by arenaClass command
            PAG_Arenaclass ac = new PAG_Arenaclass();
            ac.commit(arena, arenaPlayer.getPlayer(), new String[]{sign.getLine(0)});

            debug(arenaPlayer, "[Cancel #4] Interaction with a class sign");
            event.setUseInteractedBlock(Event.Result.DENY);

        } else {
            debug(arenaPlayer, "Unknown sign interaction. Sign first line: {}", sign.getLine(0));
        }
    }

    public static void handleContainerInteract(PlayerInteractEvent event, Arena arena, ArenaPlayer arenaPlayer) {
        Block block = event.getClickedBlock();
        String lcTeamName = arenaPlayer.getArenaTeam().getName().toLowerCase();

        boolean isContainerBlacklisted = arena.getRegionsByType(RegionType.BL_INV).stream()
                .filter(reg -> reg.containsLocation(new PALocation(block.getLocation())))
                .map(reg -> reg.getRegionName().toLowerCase())
                .anyMatch(regName -> regName.contains(lcTeamName) ||
                        regName.contains(arenaPlayer.getArenaClass().getName().toLowerCase())
                );

        if (isContainerBlacklisted) {
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        boolean isContainerNotWhitelisted = arena.getRegionsByType(RegionType.WL_INV).stream()
                .filter(reg -> reg.containsLocation(new PALocation(block.getLocation())))
                .map(reg -> reg.getRegionName().toLowerCase())
                .anyMatch(regName -> !regName.contains(lcTeamName) &&
                        !regName.contains(arenaPlayer.getArenaClass().getName().toLowerCase())
                );

        if (isContainerNotWhitelisted) {
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }


        if (arena.getConfig().getBoolean(Config.CFG.PLAYER_QUICKLOOT) && block.getState() instanceof Chest) {
            final Chest c = (Chest) block.getState();
            InventoryManager.transferItems(arenaPlayer.getPlayer(), c.getInventory());
        }
    }

    public static void handleReadyBlockInteract(PlayerInteractEvent event, Arena arena, ArenaPlayer arenaPlayer) {
        debug(arenaPlayer, "Clicked ready block!");

        if (arenaPlayer.getStatus() != LOUNGE && arenaPlayer.getStatus() != READY) {
            return;
        }

        event.setCancelled(true);
        debug(arenaPlayer, "[Cancel #5] Interaction with ready block");
        debug(arenaPlayer, "Current arena class: {}", arenaPlayer.getArenaClass());

        if (arena.startRunner != null) {
            debug(arenaPlayer, "out: offhand!");
            return;
        }

        try {
            if(arena.isFightInProgress()) {
                checkReadyRequirementsDuringFight(arena, arenaPlayer);
            } else {
                checkReadyRequirementsBeforeFight(arena, arenaPlayer);
            }
        } catch (GameplayException e) {
            arena.msg(arenaPlayer.getPlayer(), e.getMessage());
        }
    }

    /**
     * try to join an arena via sign click
     *
     * @param event  the PlayerInteractEvent
     * @param player the player trying to join
     */
    public static void handleJoinSignInteract(PlayerInteractEvent event, Player player) {
        debug(player, "onInteract: sign check");
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block block = event.getClickedBlock();
            if (block.getState() instanceof Sign) {
                String[] lines = ((Sign) block.getState()).getLines();
                List<String> signHeaders = PVPArena.getInstance().getConfig().getStringList("signHeaders");
                if (CollectionUtils.containsIgnoreCase(signHeaders, ChatColor.stripColor(lines[0]))) {
                    event.setUseInteractedBlock(Event.Result.DENY);
                    final String sName = ChatColor.stripColor(lines[1]).toLowerCase();
                    String[] newArgs = new String[0];
                    final Arena arena = ArenaManager.getArenaByExactName(sName);

                    if (arena == null) {
                        Arena.pmsg(player, Language.MSG.ERROR_ARENA_NOTFOUND, sName);
                        return;
                    }

                    String secondLine = ChatColor.stripColor(lines[2]);
                    if (StringUtils.notBlank(secondLine) && arena.getTeam(secondLine) != null) {
                        newArgs = new String[]{secondLine};
                    }

                    final AbstractArenaCommand command = new PAG_Join();
                    command.commit(arena, player, newArgs);
                }
            }
        }
    }

    public static void handleNotFightingPlayersWithTeam(PlayerInteractEvent event, Arena arena, ArenaPlayer arenaPlayer) {

        if (asList(LOUNGE, READY).contains(arenaPlayer.getStatus())) {
            if(arena.getConfig().getBoolean(Config.CFG.PERMS_LOUNGEINTERACT)) {
                debug(arenaPlayer, "allowing lounge interaction due to config setting!");
            } else {
                boolean isInLounge = arena.getRegions().stream()
                        .filter(reg -> reg.getType() == RegionType.LOUNGE)
                        .anyMatch(reg -> reg.containsLocation(new PALocation(event.getClickedBlock().getLocation())));

                if (isInLounge) {
                    debug(arenaPlayer, "Lounge player is in a lounge region => allow interaction");
                } else {
                    debug(arenaPlayer, "[Cancel #6] Lounge player not in the lounge area");
                    event.setCancelled(true);
                }
            }
        } else if (asList(WATCH, LOST).contains(arenaPlayer.getStatus()) && arena.getConfig().getBoolean(Config.CFG.PERMS_SPECINTERACT)) {
            if(arena.getConfig().getBoolean(Config.CFG.PERMS_SPECINTERACT)) {
                debug(arenaPlayer, "allowing spectator interaction due to config setting!");
            } else {
                boolean isInSpectateArea = arena.getRegions().stream()
                        .filter(reg -> reg.getType() == RegionType.LOUNGE)
                        .anyMatch(reg -> reg.containsLocation(new PALocation(event.getClickedBlock().getLocation())));

                if (isInSpectateArea) {
                    debug(arenaPlayer, "Spectate player is in a spectate region => allow interaction");
                } else {
                    debug(arenaPlayer, "[Cancel #7] Spectate player not in the spectate area");
                    event.setCancelled(true);
                }
            }
        } else {
            debug(arenaPlayer, "[Cancel #8] Not fighting nor in the lounge/spectate area");
            event.setCancelled(true);
        }
    }
}
