package com.aven0x.Velto.utils;

public class ServerUtil {
    private static final boolean isPaper;

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
    }

    public static boolean isPaper() {
        return isPaper;
    }
}
