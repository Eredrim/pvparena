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

        final int versionLen = currentVerArr.length;
        for(int i = 0; i < versionLen; i++) {
            int weight = (versionLen - 1 - i) * 2;
            long currentVerChunk = Long.parseLong(currentVerArr[i]);
            long newVerChunk = Long.parseLong(newVerArr[i]);
            currentVerVal += (int) (currentVerChunk * Math.pow(10, weight));
            newVerVal += (int) (newVerChunk * Math.pow(10, weight));
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

    public static boolean isApiVersionNewerThan(String versionNumber) {
        return isSameVersionOrNewer(getApiVersion(), versionNumber);
    }
}
