package com.aven0x.Velto.utils;

import com.aven0x.Velto.Velto;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigUtil {

    // Ne pas le mettre statique final !
    private static FileConfiguration getConfig() {
        return Velto.getInstance().getConfig();
    }

    // === SPAWN ===

    public static void setSpawn(Location location) {
        getConfig().set("spawn", location);
        Velto.getInstance().saveConfig();
    }

    public static Location getSpawn() {
        return getConfig().contains("spawn") ? getConfig().getLocation("spawn") : null;
    }

    // === AUTO MESSAGES ===

    public static boolean isAutoMessagesEnabled() {
        return getConfig().getBoolean("auto-messages.enabled", true);
    }

    public static int getAutoMessagesIntervalTicks() {
        int seconds = getConfig().getInt("auto-messages.interval-seconds", 120);
        return seconds * 20;
    }

    public static boolean isAutoMessagesRandom() {
        return getConfig().getBoolean("auto-messages.random", true);
    }

    public static List<String> getAutoMessageKeys() {
        return getConfig().getStringList("auto-messages.messages")
                .stream()
                .map(entry -> {
                    if (entry.startsWith("key: ")) return entry.substring(5);
                    return entry;
                })
                .toList();
    }

    public static FileConfiguration getRawConfig() {
        return getConfig();
    }

    public static void reload() {
        Velto.getInstance().reloadConfig();
    }

    public static void save() {
        Velto.getInstance().saveConfig();
    }
}
