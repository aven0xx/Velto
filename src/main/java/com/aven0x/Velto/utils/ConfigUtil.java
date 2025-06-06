package com.aven0x.Velto.utils;

import com.aven0x.Velto.Velto;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigUtil {

    private static final FileConfiguration config = Velto.getInstance().getConfig();

    // === SPAWN ===

    public static void setSpawn(Location location) {
        config.set("spawn", location);
        Velto.getInstance().saveConfig();
    }

    public static Location getSpawn() {
        return config.contains("spawn") ? config.getLocation("spawn") : null;
    }

    // === AUTO MESSAGES ===

    public static boolean isAutoMessagesEnabled() {
        return config.getBoolean("auto-messages.enabled", true);
    }

    public static int getAutoMessagesIntervalTicks() {
        int seconds = config.getInt("auto-messages.interval-seconds", 120);
        return seconds * 20;
    }

    public static boolean isAutoMessagesRandom() {
        return config.getBoolean("auto-messages.random", true);
    }

    public static List<String> getAutoMessageKeys() {
        return config.getStringList("auto-messages.messages")
                .stream()
                .map(entry -> {
                    if (entry.startsWith("key: ")) return entry.substring(5);
                    return entry;
                })
                .toList();
    }

    // === GENERIC ACCESS ===

    public static FileConfiguration getRawConfig() {
        return config;
    }

    public static void reload() {
        Velto.getInstance().reloadConfig();
    }

    public static void save() {
        Velto.getInstance().saveConfig();
    }
}
