package com.aven0x.Velto.utils;

import com.aven0x.Velto.Velto;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigUtil {

    // Do NOT make this final — we want to always get the latest config
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
        return getConfig().getStringList("auto-messages.messages").stream()
                .map(entry -> entry.startsWith("key: ") ? entry.substring(5) : entry)
                .toList();
    }

    // === CHAT CONFIGURATION ===

    public static String getChatFormat() {
        return getConfig().getString("messages.chat", "<%player_name%> %message%");
    }

    public static String getJoinMessage() {
        return getConfig().getString("messages.join", "&e%player_name% joined the game.");
    }

    public static String getQuitMessage() {
        return getConfig().getString("messages.quit", "&c%player_name% left the game.");
    }

    public static String getReloadMessage() {
        return getConfig().getString("messages.reload", "&aChat configuration reloaded.");
    }

    // === RAW + UTILITIES ===

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
