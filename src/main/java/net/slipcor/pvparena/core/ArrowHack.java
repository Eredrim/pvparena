package net.slipcor.pvparena.core;

import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ArrowHack {
    public static void processArrowHack(Player player) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Method mGetHandle = player.getClass().getMethod("getHandle");
        Object cHandle = mGetHandle.invoke(player);

        try {
            // >= 1.10
            Method setArrowCount = cHandle.getClass().getMethod("f", int.class);
            setArrowCount.invoke(cHandle, 0);
        } catch (NoSuchMethodException e) {
            // 1.9
            Method setOlderArrowCount = cHandle.getClass().getMethod("k", int.class);
            setOlderArrowCount.invoke(cHandle, 0);
        }

    }
}
