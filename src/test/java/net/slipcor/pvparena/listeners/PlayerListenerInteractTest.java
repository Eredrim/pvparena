package net.slipcor.pvparena.listeners;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.testUtils.ArenaPlayerTest;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
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

    @Mock
    private World world;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private Config config;

    @InjectMocks
    private PlayerListener listener;

    private Arena arena;

    @BeforeAll
    static void beforeAll() {
        // Needed to avoid issue loading while using config mock
        UTILS_MOCK.when(() -> Utils.getSerializableItemStacks(any(ItemStack[].class))).thenReturn(new ArrayList<>());
    }

    @BeforeEach
    void beforeEach() {
        this.arena = new Arena("Test");
        this.arena.setConfig(this.config);
        this.arenaPlayerMock = Mockito.mockStatic(ArenaPlayer.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    void shouldNotCancelExternalInteract() {
        // Given
        ArenaPlayer apt = new ArenaPlayerTest(this.player);

        when(this.event.getPlayer()).thenReturn(this.player);
        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        // When
        listener.onPlayerInteract(this.event);

        // Then
        verify(this.event, never()).setCancelled(true);
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
        if(editMode) {
            PAA_Edit.activeEdits.put(PLAYER_NAME, this.arena);
        }

        listener.onPlayerInteract(this.event);
        PAA_Edit.activeEdits.clear();

        // Then
        if (shouldCancel) {
            verify(this.event).setCancelled(true);
        } else {
            verify(this.event, never()).setCancelled(true);
        }

        pablMock.closeOnDemand();
        amMock.closeOnDemand();
        wmMock.closeOnDemand();
        arMock.closeOnDemand();
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
        listener.onPlayerInteract(this.event);

        // Then
        if (shouldCancel) {
            verify(this.event).setCancelled(true);
        } else {
            verify(this.event, never()).setCancelled(true);
        }

        ammMock.closeOnDemand();
    }

    @ParameterizedTest
    @MethodSource("argumentsForSpectator")
    void shouldHandleEventForSpectator(PlayerStatus playerStatus, boolean hasSpecInteract, boolean shouldCancel) {
        // Given
        ArenaPlayer apt = new ArenaPlayerTest(this.player);
        apt.setArena(this.arena);
        apt.setStatus(playerStatus);

        when(this.event.getPlayer()).thenReturn(this.player);
        when(this.config.getBoolean(eq(Config.CFG.PERMS_SPECINTERACT))).thenReturn(hasSpecInteract);
        this.arenaPlayerMock.when(() -> ArenaPlayer.fromPlayer(eq(this.player))).thenReturn(apt);

        // When
        listener.onPlayerInteract(this.event);

        // Then
        if (shouldCancel) {
            verify(this.event).setCancelled(true);
        } else {
            verify(this.event, never()).setCancelled(true);
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

    private static Stream<Arguments> argumentsForSpectator() {
        return Stream.of(
                Arguments.of(PlayerStatus.WATCH, true, false),
                Arguments.of(PlayerStatus.WATCH, false, true),
                Arguments.of(PlayerStatus.NULL, true, true)
        );
    }
}