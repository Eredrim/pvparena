package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.exceptions.GameplayRuntimeException;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.arena.PlayerStatus.*;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

/**
 * Class to manage player movements inside regions
 */
public final class RegionManager {
    private static RegionManager instance;
    private Set<ArenaRegion> regionsCache;

    private RegionManager() {
        this.reloadCache();
    }

    public static RegionManager getInstance() {
        if(instance == null){
            synchronized (RegionManager.class) {
                if(instance == null){
                    instance = new RegionManager();
                }
            }
        }
        return instance;
    }

    public void reloadCache() {
        this.regionsCache = ArenaManager.getArenas().stream()
                .flatMap(arena -> {
                    if(arena.getConfig().getBoolean(Config.CFG.PROTECT_PREVENT_INTRUSION)) {
                        return arena.getRegions().stream();
                    }
                    return arena.getRegions().stream().filter(rg -> rg.getType() == RegionType.JOIN);
                })
                .collect(Collectors.toSet());
    }

    public void checkPlayerLocation(Player player, PABlockLocation locTo, PlayerMoveEvent event) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if(arenaPlayer.getArena() == null) {
            this.regionsCache.stream()
                    .filter(rg -> rg.getShape().contains(locTo))
                    .findFirst()
                    .ifPresent(enteringRegion -> {
                        if(enteringRegion.getType() == RegionType.JOIN) {
                            this.handleRegionJoin(enteringRegion, player);
                        } else {
                            this.preventRegionIntrusion(enteringRegion, event);
                        }
                    });
        } else if(!arenaPlayer.isTeleporting()) {

            if (arenaPlayer.getStatus() == FIGHT) {
                this.handleFightingPlayerMove(arenaPlayer, locTo, event);

            } else if (arenaPlayer.getStatus() == READY || arenaPlayer.getStatus() == LOUNGE) {
                this.handleEscapeLoungeRegions(arenaPlayer, locTo);

            } else if (arenaPlayer.isSpectating()) {
                this.handleEscapeWatchRegions(arenaPlayer, locTo);
            }
        }
    }

    private void handleRegionJoin(ArenaRegion rg, Player player) {
        if(!rg.getArena().isLocked() && rg.getArena().getConfig().getBoolean(Config.CFG.JOIN_FORCE)) {
            if(!rg.getArena().isFightInProgress() || (rg.getArena().isFightInProgress() && rg.getArena().getGoal().allowsJoinInBattle())) {
                final PAG_Join cmd = new PAG_Join();
                cmd.commit(rg.getArena(), player, new String[]{rg.getRegionName().replace("-join", "")});
            }
        }
    }

    private void preventRegionIntrusion(ArenaRegion rg, PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = rg.getArena();
        if (!PermissionManager.hasAdminPerm(player) && !PermissionManager.hasBuilderPerm(player, arena)) {
            debug(player, "Entering in arena region while outside the arena. Region: {} - {}, loc : {}", rg.getRegionName(), arena.getName(), event.getTo());

            Consumer<Block> checkLowerLocation = (Block block) -> {
                if(rg.containsLocation(new PALocation(block.getLocation()))) {
                    throw new GameplayRuntimeException("Unable to rollback player, they are falling into the region");
                }
            };

            try {
                this.tryRollbackPosition(event, checkLowerLocation);
            } catch (GameplayRuntimeException e) {
                player.teleport(SpawnManager.getExitSpawnLocation(arena));
            } finally {
                arena.msg(player, Language.MSG.NOTICE_ARENA_INTRUSION, arena.getName());
            }
        }

    }

    public void handleFightingPlayerMove(ArenaPlayer arenaPlayer, PABlockLocation locTo, PlayerMoveEvent event) {
        Arena arena = arenaPlayer.getArena();

        if (arena.isFightInProgress()) {
            boolean escaping = this.isEscapingBattleRegions(arenaPlayer, locTo);

            if (escaping) {
                Player player = arenaPlayer.getPlayer();
                debug(player, "escaping BATTLE, loc : {}", locTo);

                try {
                    arena.msg(player, Language.MSG.NOTICE_ARENA_BOUNDS);
                    if (arena.getConfig().getBoolean(Config.CFG.GENERAL_LEAVEDEATH)) {
                        player.setLastDamageCause(new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, 1004.0));
                        player.damage(1000);
                    } else {
                        arenaPlayer.setTelePass(true); // Required to rollback player in a region with teleport protection
                        this.tryRollbackPosition(event, null);
                        Bukkit.getScheduler().runTask(PVPArena.getInstance(), () -> arenaPlayer.setTelePass(false));
                    }
                } catch (GameplayRuntimeException e) {
                    Arena.pmsg(player, Language.MSG.NOTICE_YOU_ESCAPED);
                    arena.playerLeave(player, Config.CFG.TP_EXIT, false, false, false);
                }
            } else {
                arena.getRegions().stream()
                        .filter(rg -> rg.getType() == RegionType.BATTLE || rg.getType() == RegionType.CUSTOM)
                        .forEach(rg -> rg.handleRegionFlags(arenaPlayer, locTo));
            }
        }
    }

    private void handleEscapeLoungeRegions(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        trace("LOUNGE region move check");
        Arena arena = arenaPlayer.getArena();
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.LOUNGE);
        boolean isInRegion = regions.isEmpty() || regions.stream().anyMatch(rg -> rg.getShape().contains(pLoc));

        if (!isInRegion) {
            debug(arenaPlayer, "escaping LOUNGE, loc : {}", pLoc);
            Arena.pmsg(arenaPlayer.getPlayer(), Language.MSG.NOTICE_YOU_ESCAPED);
            arena.playerLeave(arenaPlayer.getPlayer(), Config.CFG.TP_EXIT, false, false, false);
        }
    }

    private void handleEscapeWatchRegions(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        trace("WATCH region move check");
        Arena arena = arenaPlayer.getArena();
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.WATCH);
        boolean isInRegion = regions.isEmpty() || regions.stream().anyMatch(rg -> rg.getShape().contains(pLoc));

        if (!isInRegion) {
            debug(arenaPlayer, "escaping WATCH, loc : {}", pLoc);
            Arena.pmsg(arenaPlayer.getPlayer(), Language.MSG.NOTICE_YOU_ESCAPED);
            arena.playerLeave(arenaPlayer.getPlayer(), Config.CFG.TP_EXIT, false, false, false);
        }
    }

    private boolean isEscapingBattleRegions(ArenaPlayer arenaPlayer, PABlockLocation locTo) {
        Arena arena = arenaPlayer.getArena();
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.BATTLE);
        return !regions.isEmpty() && regions.stream().noneMatch(rg -> rg.getShape().contains(locTo));
    }

    /**
     * Try to rollback player position while entering/leaving arena region. The rollback places player on a solid block
     * from their original location to 10 blocks lowers. Throws an exception is an acceptable location can not be found.
     * @param event Entering/Leaving region event
     * @param optionalCheck Consumer that make additional checks and can throw a GameplayRuntimeException if they're not
     *                      satisfied
     * @throws GameplayRuntimeException if player rollack is not possible until 10 blocks lower their original position
     */
    private void tryRollbackPosition(final PlayerMoveEvent event, Consumer<Block> optionalCheck) throws GameplayRuntimeException {
        if(event instanceof PlayerTeleportEvent || event.getFrom().getBlock().isLiquid()) {
            trace(event.getPlayer(), "escaping/unwanted entering - cancel movement");
            event.setCancelled(true);
            return;
        }

        // Checking if player can be rollback on the floor (solid block under previous position)
        final Location locFrom = event.getFrom();
        for(int i = 1; i <= 10; i++) {
            Location maybeFloor = locFrom.clone().subtract(new Vector(0, i, 0));
            if(maybeFloor.getBlock().getType().isSolid()) {
                ofNullable(optionalCheck).ifPresent(checkConsumer -> checkConsumer.accept(maybeFloor.getBlock()));
                Location rollbackLoc = Utils.getCenteredLocation(locFrom);
                rollbackLoc.setPitch(locFrom.getPitch());
                rollbackLoc.setYaw(locFrom.getYaw());
                event.getPlayer().teleport(rollbackLoc);
                trace(event.getPlayer(), "escaping/unwanted entering - rollback to location : {}", rollbackLoc);
                return;
            }
        }

        throw new GameplayRuntimeException("Unable to rollback player");
    }
}
