package net.slipcor.pvparena.core;

import org.bukkit.Bukkit;

public class VersionUtils {
    /**
     * Checks if current version is up to date
     * @param currentVersion version currently installed as x.x.x format
     * @param newVersion latest version available as x.x.x format
     * @return true if current version is up to date, false otherwise
     */
    public static boolean isSameVersionOrNewer(String currentVersion, String newVersion, boolean semVer) {
        String[] fullCurrentVerArr = currentVersion.split("-");
        boolean isSnapshot = currentVersion.contains("SNAPSHOT");
        String[] currentVerArr = fullCurrentVerArr[0].split("\\.");
        String[] newVerArr = newVersion.split("\\.");
        int currentVerVal = 0;
        int newVerVal = 0;

        final int versionLen = (semVer) ? 3 : Math.max(currentVerArr.length, newVerArr.length);
        for(int i = 0; i < versionLen; i++) {
            int weight = (versionLen - 1 - i) * 2;
            long currentVerChunk = parseLongOrZeroForIndex(currentVerArr, i);
            long newVerChunk = parseLongOrZeroForIndex(newVerArr, i);
            currentVerVal += (int) (currentVerChunk * Math.pow(10, weight));
            newVerVal += (int) (newVerChunk * Math.pow(10, weight));
        }

        if(currentVerVal == newVerVal) {
            //Release > snapshot if there are the same number
            return !isSnapshot;
        }
        return currentVerVal > newVerVal;
    }

    public static boolean isSameVersionOrNewer(String currentVersion, String newVersion) {
        return isSameVersionOrNewer(currentVersion, newVersion, true);
    }

    public static String getApiVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    public static boolean isApiVersionNewerThan(String versionNumber) {
        return isSameVersionOrNewer(getApiVersion(), versionNumber);
    }

    private static long parseLongOrZeroForIndex(String[] strArray, int idx) {
        try {
            return Long.parseLong(strArray[idx]);
        } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
            return 0L;
        }
    }
}
