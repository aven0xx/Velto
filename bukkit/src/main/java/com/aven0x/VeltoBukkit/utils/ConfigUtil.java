package com.aven0x.VeltoBukkit.utils;

import com.aven0x.VeltoBukkit.VeltoBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

public class ConfigUtil {

    // Do NOT make this final — we want to always get the latest config
    private static FileConfiguration getConfig() {
        return VeltoBukkit.getInstance().getConfig();
    }

    // === SPAWN ===

    public static void setSpawn(Location location) {
        getConfig().set("spawn", location);
        VeltoBukkit.getInstance().saveConfig();
    }

    public static Location getSpawn() {
        return getConfig().isLocation("spawn") ? getConfig().getLocation("spawn") : null;
    }

    // === AFK ===

    public static long getAfkTimeoutMillis() {
        int seconds = getConfig().getInt("afk-timeout-seconds", 300);
        return seconds * 1000L;
    }

    // === AFK ZONE ===

    public static boolean isAfkzoneOn() {
        return getConfig().getBoolean("afkzone.enabled", true);
    }

    /** Reads afkzone.location from config.yml. Returns null if the section is missing or the world is not loaded. */
    public static Location getAfkzone() {
        ConfigurationSection section = getConfig().getConfigurationSection("afkzone.location");
        if (section == null) return null;

        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) {
            VeltoBukkit.getInstance().getLogger().warning("[Velto] AFK zone world name is not set in config.yml.");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            VeltoBukkit.getInstance().getLogger().warning("[Velto] AFK zone world '" + worldName + "' is not loaded or does not exist.");
            return null;
        }

        double x = section.getDouble("x", 0);
        double y = section.getDouble("y", 0);
        double z = section.getDouble("z", 0);
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    /** Convenience setter if you ever add a /setafkzone command */
    public static void setAfkzone(Location location) {
        getConfig().set("afkzone.location", location);
        VeltoBukkit.getInstance().saveConfig();
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
        List<String> raw = getConfig().getStringList("auto-messages.messages");
        if (raw == null) return Collections.emptyList();

        return raw.stream()
                .map(entry -> entry != null && entry.startsWith("key: ") ? entry.substring(5) : entry)
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    // === CHAT CONFIGURATION ===

    /** REQUIRED fallback chat format */
    public static String getChatFormat() {
        return getConfig().getString("messages.chat", "<%player_name%> %message%");
    }

    /** Optional: defines group order (first match wins) */
    public static List<String> getChatPriority() {
        List<String> list = getConfig().getStringList("messages.chat-priority");
        return (list == null) ? Collections.emptyList() : list;
    }

    /** Optional: returns the section for a given group name */
    public static ConfigurationSection getChatGroupSection(String group) {
        if (group == null || group.isBlank()) return null;
        return getConfig().getConfigurationSection("messages.chat-groups." + group);
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
        VeltoBukkit.getInstance().reloadConfig();
    }

    public static void save() {
        VeltoBukkit.getInstance().saveConfig();
    }
}
