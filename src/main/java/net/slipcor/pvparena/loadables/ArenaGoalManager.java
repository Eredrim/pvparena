package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.exceptions.GameplayRuntimeException;
import net.slipcor.pvparena.goals.*;
import net.slipcor.pvparena.loader.JarLoader;
import net.slipcor.pvparena.loader.Loadable;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <pre>
 * Arena Goal Manager class
 * </pre>
 * <p/>
 * Loads and manages arena goals
 */
public class ArenaGoalManager {
    private Set<Loadable<? extends ArenaGoal>> goalLoadables;
    private final JarLoader<ArenaGoal> loader;

    /**
     * create an arena type instance
     *
     * @param plugin the plugin instance
     */
    public ArenaGoalManager(final PVPArena plugin) {
        final File path = new File(plugin.getDataFolder() + "/goals");
        if (!path.exists()) {
            path.mkdir();
        }
        this.loader = new JarLoader<>(path, ArenaGoal.class);
        this.goalLoadables = this.loader.loadClasses();
        this.addInternalGoals();
    }

    private void addInternalGoals() {
        this.addInternalLoadable(GoalBlockDestroy.class);
        this.addInternalLoadable(GoalCheckPoints.class);
        this.addInternalLoadable(GoalDomination.class);
        this.addInternalLoadable(GoalFlags.class);
        this.addInternalLoadable(GoalFood.class);
        this.addInternalLoadable(GoalInfect.class);
        this.addInternalLoadable(GoalLiberation.class);
        this.addInternalLoadable(GoalPhysicalFlags.class);
        this.addInternalLoadable(GoalPlayerDeathMatch.class);
        this.addInternalLoadable(GoalPlayerKillReward.class);
        this.addInternalLoadable(GoalPlayerLives.class);
        this.addInternalLoadable(GoalSabotage.class);
        this.addInternalLoadable(GoalTank.class);
        this.addInternalLoadable(GoalTeamDeathConfirm.class);
        this.addInternalLoadable(GoalTeamDeathMatch.class);
        this.addInternalLoadable(GoalTeamLives.class);
        this.addInternalLoadable(GoalTeamPlayerLives.class);
    }

    public Set<String> getAllGoalNames() {
        return this.goalLoadables.stream().map(Loadable::getName).collect(Collectors.toSet());
    }

    public Set<Loadable<? extends ArenaGoal>> getAllLoadables() {
        return this.goalLoadables;
    }

    public boolean hasLoadable(String name) {
        return this.goalLoadables.stream().anyMatch(l -> l.getName().equalsIgnoreCase(name));
    }

    public Loadable<? extends ArenaGoal> getLoadableByName(String name) {
        return this.goalLoadables.stream()
                .filter(l -> l.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public ArenaGoal getNewInstance(String name) {
        try {
            Loadable<? extends ArenaGoal> goalLoadable = this.getLoadableByName(name);

            if(goalLoadable != null) {
                return goalLoadable.getNewInstance();
            }

        } catch (ReflectiveOperationException e) {
            PVPArena.getInstance().getLogger().severe(String.format("Goal '%s' seems corrupted", name));
            e.printStackTrace();
        } catch (GameplayRuntimeException e) {
            PVPArena.getInstance().getLogger().severe(e.getMessage());
        }
        return null;
    }

    public void reload() {
        this.goalLoadables = this.loader.reloadClasses();
        this.addInternalGoals();
    }

    private void addInternalLoadable(Class<? extends ArenaGoal> loadableClass) {
        String goalName = loadableClass.getSimpleName().replace("Goal", "");
        this.goalLoadables.add(new Loadable<>(goalName, true, loadableClass));
    }
}
