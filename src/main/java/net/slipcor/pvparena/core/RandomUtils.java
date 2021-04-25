package net.slipcor.pvparena.core;

import java.util.*;

public class RandomUtils {

    private RandomUtils() {
    }

    /**
     * Return a random weighted object E
     *
     * @param weights weighted objects
     * @param random  random generator
     * @param <E>     random object
     * @return E the object randomly selected
     */
    public static <E> E getWeightedRandom(Map<E, Double> weights, Random random) {
        return weights.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), -Math.log(random.nextDouble()) / e.getValue()))
                .min(Map.Entry.comparingByValue())
                .orElseThrow(IllegalArgumentException::new).getKey();
    }

    /**
     * Return a random object E
     *
     * @param objects objects
     * @param random  random generator
     * @param <E>     random object
     * @return E the object randomly selected
     */
    public static <E> E getRandom(Collection<E> objects, Random random) {
        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() == 1) {
            return objects.iterator().next();
        }
        final ArrayList<E> list = new ArrayList<>(objects);
        return list.get(random.nextInt(list.size()));
    }
}
