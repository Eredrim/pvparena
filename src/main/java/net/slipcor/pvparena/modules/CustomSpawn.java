package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.loadables.ArenaModule;

public class CustomSpawn extends ArenaModule {

    public CustomSpawn() {
        super("CustomSpawn");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String teamName) {
        return true;
    }
}
