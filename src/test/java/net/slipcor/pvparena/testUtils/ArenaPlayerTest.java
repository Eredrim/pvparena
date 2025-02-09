package net.slipcor.pvparena.testUtils;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static java.util.Optional.ofNullable;

public class ArenaPlayerTest extends ArenaPlayer {
    private ArenaTeam teamMock;

    public ArenaPlayerTest(@NotNull Player player) {
        super(player);
    }

    public void setTeamMock(ArenaTeam teamMock) {
        this.teamMock = teamMock;
    }

    public ArenaTeam getArenaTeam() {
        return ofNullable(this.teamMock).orElse(super.getArenaTeam());
    }
}
