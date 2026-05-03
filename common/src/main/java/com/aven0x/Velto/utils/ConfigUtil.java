package com.aven0x.Velto.utils;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigUtil {

    // === CACHE ===

    private static volatile long cachedAfkTimeoutMillis = 300_000L;
    private static volatile boolean cachedAfkzoneEnabled = false;
    private static volatile Location cachedAfkzone = null;
    private static volatile boolean cachedAutoMessagesEnabled = true;
    private static volatile int cachedAutoMessagesIntervalTicks = 2400;
    private static volatile boolean cachedAutoMessagesRandom = true;
    private static volatile List<String> cachedAutoMessageKeys = Collections.emptyList();
    private static volatile String cachedChatFormat = "<%player_name%> %message%";
    private static volatile List<String> cachedChatPriority = Collections.emptyList();
    private static volatile Map<String, ConfigurationSection> cachedChatGroups = Collections.emptyMap();
    private static volatile String cachedJoinMessage = "&e%player_name% joined the game.";
    private static volatile String cachedQuitMessage = "&c%player_name% left the game.";
    private static volatile String cachedReloadMessage = "&aChat configuration reloaded.";

    private static FileConfiguration getConfig() {
        return VeltoPlugin.get().getConfig();
    }

    public static void refreshCache() {
        FileConfiguration c = getConfig();

        cachedAfkTimeoutMillis = c.getInt("afk-timeout-seconds", 300) * 1000L;
        cachedAfkzoneEnabled = c.getBoolean("afkzone.enabled", true);
        cachedAfkzone = buildAfkzone(c);
        cachedAutoMessagesEnabled = c.getBoolean("auto-messages.enabled", true);
        cachedAutoMessagesIntervalTicks = c.getInt("auto-messages.interval-seconds", 120) * 20;
        cachedAutoMessagesRandom = c.getBoolean("auto-messages.random", true);
        cachedAutoMessageKeys = buildAutoMessageKeys(c);
        cachedChatFormat = c.getString("messages.chat", "<%player_name%> %message%");
        List<String> prio = c.getStringList("messages.chat-priority");
        cachedChatPriority = (prio == null) ? Collections.emptyList() : Collections.unmodifiableList(prio);
        cachedChatGroups = buildChatGroups(c, cachedChatPriority);
        cachedJoinMessage = c.getString("messages.join", "&e%player_name% joined the game.");
        cachedQuitMessage = c.getString("messages.quit", "&c%player_name% left the game.");
        cachedReloadMessage = c.getString("messages.reload", "&aChat configuration reloaded.");
    }

    private static Location buildAfkzone(FileConfiguration c) {
        ConfigurationSection section = c.getConfigurationSection("afkzone.location");
        if (section == null) return null;

        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) {
            VeltoPlugin.get().getLogger().warning("[Velto] AFK zone world name is not set in config.yml.");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            VeltoPlugin.get().getLogger().warning("[Velto] AFK zone world '" + worldName + "' is not loaded or does not exist.");
            return null;
        }

        double x = section.getDouble("x", 0);
        double y = section.getDouble("y", 0);
        double z = section.getDouble("z", 0);
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private static List<String> buildAutoMessageKeys(FileConfiguration c) {
        List<String> raw = c.getStringList("auto-messages.messages");
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                .map(entry -> entry != null && entry.startsWith("key: ") ? entry.substring(5) : entry)
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    private static Map<String, ConfigurationSection> buildChatGroups(FileConfiguration c, List<String> priority) {
        if (priority.isEmpty()) return Collections.emptyMap();
        Map<String, ConfigurationSection> map = new HashMap<>();
        for (String group : priority) {
            ConfigurationSection sec = c.getConfigurationSection("messages.chat-groups." + group);
            if (sec != null) map.put(group, sec);
        }
        return Collections.unmodifiableMap(map);
    }

    // === SPAWN ===

    public static void setSpawn(Location location) {
        getConfig().set("spawn", location);
        VeltoPlugin.get().saveConfig();
    }

    public static Location getSpawn() {
        FileConfiguration c = getConfig();
        return c.isLocation("spawn") ? c.getLocation("spawn") : null;
    }

    // === AFK ===

    public static long getAfkTimeoutMillis() {
        return cachedAfkTimeoutMillis;
    }

    // === AFK ZONE ===

    public static boolean isAfkzoneOn() {
        return cachedAfkzoneEnabled;
    }

    public static Location getAfkzone() {
        Location loc = cachedAfkzone;
        return (loc == null) ? null : loc.clone();
    }

    public static void setAfkzone(Location location) {
        getConfig().set("afkzone.location", location);
        VeltoPlugin.get().saveConfig();
        cachedAfkzone = (location != null) ? location.clone() : null;
    }

    // === AUTO MESSAGES ===

    public static boolean isAutoMessagesEnabled() {
        return cachedAutoMessagesEnabled;
    }

    public static int getAutoMessagesIntervalTicks() {
        return cachedAutoMessagesIntervalTicks;
    }

    public static boolean isAutoMessagesRandom() {
        return cachedAutoMessagesRandom;
    }

    public static List<String> getAutoMessageKeys() {
        return cachedAutoMessageKeys;
    }

    // === CHAT CONFIGURATION ===

    public static String getChatFormat() {
        return cachedChatFormat;
    }

    public static List<String> getChatPriority() {
        return cachedChatPriority;
    }

    public static ConfigurationSection getChatGroupSection(String group) {
        if (group == null || group.isBlank()) return null;
        return cachedChatGroups.get(group);
    }

    public static String getJoinMessage() {
        return cachedJoinMessage;
    }

    public static String getQuitMessage() {
        return cachedQuitMessage;
    }

    public static String getReloadMessage() {
        return cachedReloadMessage;
    }

    // === RAW + UTILITIES ===

    public static FileConfiguration getRawConfig() {
        return getConfig();
    }

    public static void reload() {
        VeltoPlugin.get().reloadConfig();
        refreshCache();
    }

    public static void save() {
        VeltoPlugin.get().saveConfig();
    }
}
