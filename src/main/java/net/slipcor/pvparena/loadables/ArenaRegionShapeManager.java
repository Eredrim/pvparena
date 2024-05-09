package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.exceptions.GameplayRuntimeException;
import net.slipcor.pvparena.loader.JarLoader;
import net.slipcor.pvparena.loader.Loadable;
import net.slipcor.pvparena.regionshapes.CuboidRegion;
import net.slipcor.pvparena.regionshapes.CylindricRegion;
import net.slipcor.pvparena.regionshapes.SphericRegion;

import java.io.File;
import java.util.Optional;
import java.util.Set;

/**
 * <pre>Arena Region Shape Manager class</pre>
 * <p/>
 * Loads and manages arena region shapes
 */
public class ArenaRegionShapeManager {
    private Set<Loadable<? extends ArenaRegionShape>> shapeLoadables;
    private final JarLoader<ArenaRegionShape> loader;

    /**
     * create an arena region manager instance
     *
     * @param plugin the plugin instance
     */
    public ArenaRegionShapeManager(final PVPArena plugin) {
        final File path = new File(plugin.getDataFolder() + "/regionshapes");
        if (!path.exists()) {
            path.mkdir();
        }
        this.loader = new JarLoader<>(path, ArenaRegionShape.class);
        this.shapeLoadables = this.loader.loadClasses();
        this.addInternalShapes();
    }

    private void addInternalShapes() {
        this.addInternalLoadable(CuboidRegion.class);
        this.addInternalLoadable(CylindricRegion.class);
        this.addInternalLoadable(SphericRegion.class);
    }

    public ArenaRegionShape getNewInstance(String name) {
        try {
            Optional<Loadable<? extends ArenaRegionShape>> shapeLoadable = this.shapeLoadables.stream()
                    .filter(loadable -> loadable.getName().equalsIgnoreCase(name))
                    .findFirst();

            if(shapeLoadable.isPresent()) {
                return shapeLoadable.get().getNewInstance();
            }

        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        } catch (GameplayRuntimeException e) {
            PVPArena.getInstance().getLogger().severe(e.getMessage());
        }
        return null;
    }

    public Set<Loadable<? extends ArenaRegionShape>> getAllLoadables() {
        return this.shapeLoadables;
    }

    public void reload() {
        this.shapeLoadables = this.loader.reloadClasses();
        this.addInternalShapes();
    }

    private void addInternalLoadable(Class<? extends ArenaRegionShape> loadableClass) {
        String shapeName = loadableClass.getSimpleName().replace("Region", "").toLowerCase();
        this.shapeLoadables.add(new Loadable<>(shapeName, true, loadableClass));
    }
}
