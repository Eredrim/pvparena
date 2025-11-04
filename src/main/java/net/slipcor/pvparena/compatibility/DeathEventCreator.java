package net.slipcor.pvparena.compatibility;

import net.slipcor.pvparena.PVPArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.VersionUtils.isApiVersionNewerThan;

/**
 * Util class to send Death Event to server with current API version (for API <= 1.20.6)
 * or using new version (more complete) (for 1.20.6+)
 *
 * SINGLETON - Needs to be instantiated
 */
public class DeathEventCreator {
    private static final boolean USE_NEW_VERSION = isApiVersionNewerThan("1.20.6");
    private static DeathEventCreator INSTANCE;

    private Constructor<PlayerDeathEvent> newConstructor;
    private Method getDamageSourceMethod;

    private DeathEventCreator() throws ReflectiveOperationException {
        if (USE_NEW_VERSION) {
            Class<?> damageSourceClass = Class.forName("org.bukkit.damage.DamageSource");
            Class<?>[] argsClass = new Class[]{Player.class, damageSourceClass, List.class, int.class, String.class};
            this.newConstructor = PlayerDeathEvent.class.getConstructor(argsClass);
            this.getDamageSourceMethod = EntityDamageEvent.class.getMethod("getDamageSource");
        }
    }

    public static DeathEventCreator getInstance() throws ReflectiveOperationException {
        if (INSTANCE == null) {
            INSTANCE = new DeathEventCreator();
        }
        return INSTANCE;
    }

    public void sendDeathEvent(Player player, EntityDamageEvent eventSource, List<ItemStack> droppedInv, int droppedExp)
            throws ReflectiveOperationException {

        PlayerDeathEvent playerDeathEvent;
        if (this.newConstructor != null && this.getDamageSourceMethod != null) {
            debug("Sending fake PlayerDeathEvent using Spigot 1.20.6+ API");
            Object damageSource = this.getDamageSourceMethod.invoke(eventSource);
            playerDeathEvent = this.newConstructor.newInstance(player, damageSource, droppedInv, droppedExp, null);

        } else {
            playerDeathEvent = new PlayerDeathEvent(player, droppedInv, droppedExp, null);
        }

        Bukkit.getScheduler().runTask(PVPArena.getInstance(), () ->
            Bukkit.getPluginManager().callEvent(playerDeathEvent)
        );
    }
}
