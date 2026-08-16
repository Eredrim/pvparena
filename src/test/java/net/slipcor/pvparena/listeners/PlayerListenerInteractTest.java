package net.slipcor.pvparena.listeners;

import be.seeseemelk.mockbukkit.Coordinate;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.inventory.PlayerInventoryMock;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import net.slipcor.pvparena.regionshapes.CuboidRegion;
import net.slipcor.pvparena.testUtils.ArenaPlayerTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerListenerInteractTest {

    private static final String PLAYER_NAME = "Roger";

    @AutoClose
    private static final MockedStatic<Utils> UTILS_MOCK = Mockito.mockStatic(Utils.class);

    @AutoClose
    private MockedStatic<ArenaPlayer> arenaPlayerMock;

    @Mock
    private PlayerInteractEvent event;

    @Mock
    private Player player;

    @Mock
    private Block clickedBlock;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private Config config;

    @Mock
    private PVPArena pluginInstance;

    @Mock
    private ArenaGoal arenaGoal;

    @InjectMocks
    private PlayerListener listener;

    private Arena arena;

    private ServerMock server;

    private WorldMock world;

    @BeforeAll
    static void beforeAll() {
        // Needed to avoid issue loading while using config mock
        UTILS_MOCK.when(() -> Utils.getSerializableItemStacks(any(ItemStack[].class))).thenReturn(new ArrayList<>());
    }

    @BeforeEach
    void beforeEach() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.arena = new Arena("Test");
        this.arena.setConfig(this.config);
        this.arena.setGoal(this.arenaGoal, false);
        this.arenaPlayerMock = Mockito.mockStatic(ArenaPlayer.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @AfterEach
    void afterEach() {
        MockBukkit.unmock();
    }

    @Test
    void shouldNotCancelExternalInteract() {
        // Given
        ArenaPlayer apt = new ArenaPlayerTest(this.player);

        when(this.event.getPlayer()).thenReturn(this.player);
        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        // When
        this.listener.onPlayerInteract(this.event);

        // Then
        verify(this.event, never()).setCancelled(true);
    }

    @ParameterizedTest
    @MethodSource("argumentsForExternalBlockSelection")
    void shouldPrioritizeRegionSelectIfWandItem(Material itemInHand, boolean inRegionSelect, boolean inBlockSelect, boolean shouldSetRegion, boolean shouldSetBlock) {
        // Given
        PlayerInventoryMock inventoryMock = new PlayerInventoryMock(this.player);
        inventoryMock.setItemInMainHand(new ItemStack(itemInHand));
        ArenaPlayer apt = new ArenaPlayerTest(this.player);

        if (inRegionSelect) {
            PAA_Region.activeSelections.put(PLAYER_NAME, this.arena);
        }

        if (inBlockSelect) {
            SpawnManager.activeSelections.put(PLAYER_NAME, this.arena);
        } else {
            when(this.player.getInventory()).thenReturn(inventoryMock);
            when(this.player.hasPermission(anyString())).thenReturn(true);

            when(this.pluginInstance.getWandItem()).thenReturn(Material.STICK);
        }

        when(this.event.getPlayer()).thenReturn(this.player);
        when(this.event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        Location location = new Location(this.world, 1, 2, 3);
        when(this.event.getClickedBlock()).thenReturn(new BlockMock(location.clone()));

        when(this.player.getName()).thenReturn(PLAYER_NAME);
        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        MockedConstruction<PABlockLocation> pablMock = mockConstruction(PABlockLocation.class);
        MockedStatic<ArenaManager> amMock = Mockito.mockStatic(ArenaManager.class);
        amMock.when(() -> ArenaManager.getArenaByRegionLocation(any(PABlockLocation.class))).thenReturn(null);
        MockedStatic<PVPArena> pluginMock = Mockito.mockStatic(PVPArena.class);
        pluginMock.when(PVPArena::getInstance).thenReturn(this.pluginInstance);

        // When
        this.listener.onPlayerInteract(this.event);
        pablMock.closeOnDemand();
        amMock.closeOnDemand();
        pluginMock.closeOnDemand();
        PAA_Region.activeSelections.remove(PLAYER_NAME);

        // Then
        if (shouldSetRegion) {
            assertThat(apt.getSelection()[0]).isNull();
            assertThat(apt.getSelection()[1]).isNotNull();
            verify(this.arenaGoal, never()).checkSetBlock(any(), any());
            verify(this.event).setCancelled(true);
        } else if (shouldSetBlock) {
            verify(this.arenaGoal).checkSetBlock(any(), any());
            assertThat(apt.getSelection()[0]).isNull();
            assertThat(apt.getSelection()[1]).isNull();
        } else {
            verify(this.event, never()).setCancelled(true);
        }
    }

    @ParameterizedTest
    @MethodSource("argumentsForExternal")
    void shouldHandleExternalInteractInArenaRegion(boolean foundArena, boolean editMode, boolean settingBlock, boolean settingRegion, boolean shouldCancel) {
        // Given
        ArenaPlayer apt = new ArenaPlayerTest(this.player);

        if (foundArena) {
            when(this.player.getName()).thenReturn(PLAYER_NAME);
        }
        when(this.event.getPlayer()).thenReturn(this.player);
        when(this.event.getClickedBlock()).thenReturn(this.clickedBlock);
        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        MockedConstruction<PABlockLocation> pablMock = mockConstruction(PABlockLocation.class);
        MockedStatic<ArenaManager> amMock = Mockito.mockStatic(ArenaManager.class);
        amMock.when(() -> ArenaManager.getArenaByRegionLocation(any(PABlockLocation.class)))
                .thenReturn(foundArena ? this.arena : null);

        MockedStatic<WorkflowManager> wmMock = Mockito.mockStatic(WorkflowManager.class);
        wmMock.when(() -> WorkflowManager.handleSetBlock(any(), any())).thenReturn(settingBlock);

        MockedStatic<ArenaRegion> arMock = Mockito.mockStatic(ArenaRegion.class);
        arMock.when(() -> ArenaRegion.handleSetRegionPosition(any(), any())).thenReturn(settingRegion);

        // When
        if (editMode) {
            PAA_Edit.activeEdits.put(PLAYER_NAME, this.arena);
        }

        this.listener.onPlayerInteract(this.event);
        PAA_Edit.activeEdits.clear();

        pablMock.closeOnDemand();
        amMock.closeOnDemand();
        wmMock.closeOnDemand();
        arMock.closeOnDemand();

        // Then
        if (shouldCancel) {
            verify(this.event).setCancelled(true);
        } else {
            verify(this.event, never()).setCancelled(true);
        }
    }

    @ParameterizedTest
    @MethodSource("argumentsForFighter")
    void shouldHandleEventForFighter(boolean moduleCancel, Action action, boolean shouldCancel) {
        // Given
        ArenaPlayerTest apt = new ArenaPlayerTest(this.player);
        apt.setArena(this.arena);
        apt.setTeamMock(new ArenaTeam("free", "WHITE"));

        when(this.event.getPlayer()).thenReturn(this.player);
        when(this.event.getAction()).thenReturn(action);

        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        MockedStatic<ArenaModuleManager> ammMock = Mockito.mockStatic(ArenaModuleManager.class);
        ammMock.when(() -> ArenaModuleManager.onPlayerInteract(any(), any())).thenReturn(moduleCancel);

        // When
        this.listener.onPlayerInteract(this.event);
        ammMock.closeOnDemand();

        // Then
        if (shouldCancel) {
            verify(this.event).setCancelled(true);
        } else {
            verify(this.event, never()).setCancelled(true);
        }
    }

    @ParameterizedTest
    @MethodSource("argumentsForLounge")
    void shouldHandleEventForLounge(PlayerStatus status, Action action, boolean inLoungeRegion, boolean hasLoungeInteract, boolean shouldCancelBlock, boolean shouldCancelItem) {
        // Given
        ArenaPlayerTest apt = new ArenaPlayerTest(this.player);
        apt.setArena(this.arena);
        apt.setTeamMock(new ArenaTeam("free", "WHITE"));
        apt.setStatus(status);

        when(this.event.getPlayer()).thenReturn(this.player);
        when(this.event.getAction()).thenReturn(action);

        if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
            WorldMock world = this.server.addSimpleWorld("world");
            Block block = world.createBlock(new Coordinate(0, 0, 0));

            when(this.event.getClickedBlock()).thenReturn(block);

            PABlockLocation[] rgBounds = new PABlockLocation[] {
                    new PABlockLocation("world", 0, 0, 0),
                    new PABlockLocation("world", 1, 1, 1)
            };

            if (inLoungeRegion) {
                ArenaRegion lounge = new ArenaRegion(this.arena, "lounge", new CuboidRegion(), rgBounds);
                lounge.setType(RegionType.LOUNGE);
                this.arena.addRegion(lounge);
            }
        }

        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        MockedStatic<ArenaModuleManager> ammMock = Mockito.mockStatic(ArenaModuleManager.class);
        ammMock.when(() -> ArenaModuleManager.onPlayerInteract(any(), any())).thenReturn(false);

        // When
        this.listener.onPlayerInteract(this.event);
        ammMock.closeOnDemand();

        // Then
        if (shouldCancelBlock) {
            verify(this.event).setUseInteractedBlock(Event.Result.DENY);
        }

        if (shouldCancelItem) {
            verify(this.event).setUseItemInHand(Event.Result.DENY);
        }

        if(!shouldCancelBlock && !shouldCancelItem) {
            verify(this.event, never()).setCancelled(true);
            verify(this.event, never()).setUseInteractedBlock(any());
            verify(this.event, never()).setUseItemInHand(any());
        }
    }

    @ParameterizedTest
    @MethodSource("argumentsForSpectator")
    void shouldHandleEventForSpectator(PlayerStatus playerStatus, boolean hasSpecInteract, boolean shouldCancelBlock, boolean shouldCancelItem) {
        // Given
        ArenaPlayer apt = new ArenaPlayerTest(this.player);
        apt.setArena(this.arena);
        apt.setStatus(playerStatus);

        when(this.event.getPlayer()).thenReturn(this.player);
        when(this.config.getBoolean(eq(Config.CFG.PERMS_SPECINTERACT))).thenReturn(hasSpecInteract);
        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        // When
        this.listener.onPlayerInteract(this.event);

        // Then
        if (shouldCancelBlock || shouldCancelItem) {
            if (shouldCancelBlock) {
                verify(this.event).setUseInteractedBlock(eq(Event.Result.DENY));
            }
            if (shouldCancelItem) {
                verify(this.event).setUseItemInHand(eq(Event.Result.DENY));
            }
        } else {
            verify(this.event, never()).setCancelled(true);
            verify(this.event, never()).setUseInteractedBlock(any());
            verify(this.event, never()).setUseItemInHand(any());
        }
    }

    private static Stream<Arguments> argumentsForExternal() {
        return Stream.of(
                Arguments.of(false, true, true, true, true),
                Arguments.of(false, true, true, false, true),
                Arguments.of(false, true, false, true, true),
                Arguments.of(false, true, false, false, false),
                Arguments.of(true, true, true, true, true),
                Arguments.of(true, true, true, false, true),
                Arguments.of(true, true, false, true, true),
                Arguments.of(true, true, false, false, false),
                Arguments.of(true, false, false, false, true)
        );
    }

    private static Stream<Arguments> argumentsForFighter() {
        return Stream.of(
                Arguments.of(true, Action.LEFT_CLICK_BLOCK, true),
                Arguments.of(false, Action.LEFT_CLICK_AIR, false),
                Arguments.of(false, Action.RIGHT_CLICK_AIR, false)
        );
    }

    private static Stream<Arguments> argumentsForLounge() {
        return Stream.of(
                Arguments.of(PlayerStatus.LOUNGE, Action.LEFT_CLICK_BLOCK, false, false, true, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.LEFT_CLICK_BLOCK, false, true, false, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.LEFT_CLICK_BLOCK, true, false, false, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.LEFT_CLICK_AIR, false, false, false, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.LEFT_CLICK_AIR, true, false, false, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.RIGHT_CLICK_BLOCK, false, false, true, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.RIGHT_CLICK_BLOCK, false, true, false, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.RIGHT_CLICK_BLOCK, true, false, false, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.RIGHT_CLICK_AIR, false, false, false, true),
                Arguments.of(PlayerStatus.LOUNGE, Action.RIGHT_CLICK_AIR, true, false, false, true),
                Arguments.of(PlayerStatus.READY, Action.LEFT_CLICK_BLOCK, false, false, true, true),
                Arguments.of(PlayerStatus.READY, Action.LEFT_CLICK_BLOCK, false, true, false, true),
                Arguments.of(PlayerStatus.READY, Action.LEFT_CLICK_BLOCK, true, false, false, true),
                Arguments.of(PlayerStatus.READY, Action.LEFT_CLICK_AIR, false, false, false, true),
                Arguments.of(PlayerStatus.READY, Action.LEFT_CLICK_AIR, true, false, false, true),
                Arguments.of(PlayerStatus.READY, Action.RIGHT_CLICK_BLOCK, false, false, true, true),
                Arguments.of(PlayerStatus.READY, Action.RIGHT_CLICK_BLOCK, false, true, false, true),
                Arguments.of(PlayerStatus.READY, Action.RIGHT_CLICK_BLOCK, true, false, false, true),
                Arguments.of(PlayerStatus.READY, Action.RIGHT_CLICK_AIR, false, false, false, true),
                Arguments.of(PlayerStatus.READY, Action.RIGHT_CLICK_AIR, true, false, false, true)
        );
    }

    private static Stream<Arguments> argumentsForSpectator() {
        return Stream.of(
                Arguments.of(PlayerStatus.WATCH, true, false, true),
                Arguments.of(PlayerStatus.WATCH, false, true, true),
                Arguments.of(PlayerStatus.NULL, true, true, true)
        );
    }

    private static Stream<Arguments> argumentsForExternalBlockSelection() {
        return Stream.of(
                // Name / Item / inRegionSelect / inBlockSelect / shouldSetRegion / shouldSetBlock
                argumentSet("with Wand item - select region", Material.STICK, true, false, true, false),
                argumentSet("with Wand item - select block", Material.STICK, false, true, false, true),
                argumentSet("with Wand item - select block during region selection", Material.STICK, true, true, false, true),
                argumentSet("without Wand item - select region", Material.AIR, true, false, false, false),
                argumentSet("without Wand item - select block", Material.AIR, false, true, false, true),
                argumentSet("without Wand item - select block during region selection", Material.AIR, true, true, false, true)
        );
    }
}