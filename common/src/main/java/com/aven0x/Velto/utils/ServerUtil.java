package com.aven0x.Velto.utils;

public class ServerUtil {
    private static final boolean isPaper;
    private static final boolean isFolia;

    static {
        boolean paper;
        try {
            // Check for modern Paper class (Paper 1.19+)
            Class.forName("io.papermc.paper.configuration.Configuration");
            paper = true;
        } catch (ClassNotFoundException e) {
            paper = false;
        }
        isPaper = paper;

        boolean folia;
        try {
            // Folia's regionised server type; absent on Paper and Spigot.
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        isFolia = folia;
    }

    public static boolean isPaper() {
        return isPaper;
    }

    // True on Folia (a regionised server); false on Paper and Spigot.
    public static boolean isFolia() {
        return isFolia;
    }
}
