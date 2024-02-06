package net.slipcor.pvparena.core;

import org.bukkit.Bukkit;

public class VersionUtils {
    /**
     * Checks if current version is up to date
     * @param currentVersion version currently installed as x.x.x format
     * @param newVersion latest version available as x.x.x format
     * @return true if current version is up to date, false otherwise
     */
    public static boolean isSameVersionOrNewer(String currentVersion, String newVersion) {
        String[] fullCurrentVerArr = currentVersion.split("-");
        boolean isSnapshot = currentVersion.contains("SNAPSHOT");
        String[] currentVerArr = fullCurrentVerArr[0].split("\\.");
        String[] newVerArr = newVersion.split("\\.");
        int currentVerVal = 0;
        int newVerVal = 0;

        final int versionLen = 3;
        for(int i = 0; i < versionLen; i++) {
            int weight = (versionLen - i) * 2;
            long currentVerChunk = Long.parseLong(currentVerArr[i]);
            long newVerChunk = Long.parseLong(newVerArr[i]);
            currentVerVal += (int) (currentVerChunk * Math.pow(100, weight));
            newVerVal += (int) (newVerChunk * Math.pow(100, weight));
        }

        if(currentVerVal == newVerVal) {
            //Release > snapshot if there are the same number
            return !isSnapshot;
        }
        return currentVerVal > newVerVal;
    }

    public static String getApiVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    /* Version compatibility hooks below */

    public static int INFINITE_EFFECT_DURATION =
            isSameVersionOrNewer(getApiVersion(), "1.19.0") ? -1 : Integer.MAX_VALUE;
}
