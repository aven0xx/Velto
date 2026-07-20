package com.aven0x.Velto.utils;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigUtil {

    // === CACHE ===

    private static volatile long cachedAfkTimeoutMillis = 300_000L;
    private static volatile boolean cachedAfkzoneEnabled = false;
    private static volatile String cachedAfkzoneWorld = null;
    private static volatile double cachedAfkzoneX = 0, cachedAfkzoneY = 0, cachedAfkzoneZ = 0;
    private static volatile float cachedAfkzoneYaw = 0, cachedAfkzonePitch = 0;
    private static volatile boolean cachedAutoMessagesEnabled = true;
    private static volatile int cachedAutoMessagesIntervalTicks = 2400;
    private static volatile boolean cachedAutoMessagesRandom = true;
    private static volatile List<String> cachedAutoMessageKeys = Collections.emptyList();
    private static volatile List<String> cachedBackBlacklistedWorlds = Collections.emptyList();
    private static volatile int cachedUserdataAutosaveIntervalTicks = 6000;
    private static volatile String cachedChatFormat = "<%player_name%> %message%";
    private static volatile List<String> cachedChatPriority = Collections.emptyList();
    private static volatile Map<String, ConfigurationSection> cachedChatGroups = Collections.emptyMap();
    private static volatile String cachedJoinMessage = "&e%player_name% joined the game.";
    private static volatile String cachedQuitMessage = "&c%player_name% left the game.";
    private static volatile String cachedReloadMessage = "&aChat configuration reloaded.";
    private static volatile boolean cachedTeleportCancelOnMove = true;
    private static volatile int cachedTeleportCountdownDefault = 5;
    private static volatile LinkedHashMap<String, Integer> cachedTeleportCountdownPermissions = new LinkedHashMap<>();
    private static volatile int cachedTpaExpireSeconds = 60;

    private static FileConfiguration getConfig() {
        return VeltoPlugin.get().getConfig();
    }

    public static void refreshCache() {
        FileConfiguration c = getConfig();

        cachedAfkTimeoutMillis = c.getInt("afk-timeout-seconds", 300) * 1000L;
        cachedAfkzoneEnabled = c.getBoolean("afkzone.enabled", true);
        buildAfkzone(c);
        cachedAutoMessagesEnabled = c.getBoolean("auto-messages.enabled", true);
        cachedAutoMessagesIntervalTicks = c.getInt("auto-messages.interval-seconds", 120) * 20;
        cachedAutoMessagesRandom = c.getBoolean("auto-messages.random", true);
        cachedAutoMessageKeys = buildAutoMessageKeys(c);
        List<String> backWorlds = c.getStringList("back.blacklisted-worlds");
        cachedBackBlacklistedWorlds = (backWorlds == null) ? Collections.emptyList() : Collections.unmodifiableList(backWorlds);
        cachedUserdataAutosaveIntervalTicks = c.getInt("userdata.autosave-interval-seconds", 300) * 20;
        cachedChatFormat = c.getString("messages.chat", "<%player_name%> %message%");
        List<String> prio = c.getStringList("messages.chat-priority");
        cachedChatPriority = (prio == null) ? Collections.emptyList() : Collections.unmodifiableList(prio);
        cachedChatGroups = buildChatGroups(c, cachedChatPriority);
        cachedJoinMessage = c.getString("messages.join", "&e%player_name% joined the game.");
        cachedQuitMessage = c.getString("messages.quit", "&c%player_name% left the game.");
        cachedReloadMessage = c.getString("messages.reload", "&aChat configuration reloaded.");
        cachedTeleportCancelOnMove = c.getBoolean("teleport.cancel-on-move", true);
        cachedTeleportCountdownDefault = c.getInt("teleport.countdown.default", 5);
        cachedTeleportCountdownPermissions = buildTeleportCountdownPermissions(c);
        cachedTpaExpireSeconds = c.getInt("tpa.expire-seconds", 60);
    }

    private static void buildAfkzone(FileConfiguration c) {
        ConfigurationSection section = c.getConfigurationSection("afkzone.location");
        if (section == null) {
            cachedAfkzoneWorld = null;
            return;
        }

        String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) {
            VeltoPlugin.get().getLogger().warning("[Velto] AFK zone world name is not set in config.yml.");
            cachedAfkzoneWorld = null;
            return;
        }

        cachedAfkzoneWorld = worldName;
        cachedAfkzoneX = section.getDouble("x", 0);
        cachedAfkzoneY = section.getDouble("y", 0);
        cachedAfkzoneZ = section.getDouble("z", 0);
        cachedAfkzoneYaw = (float) section.getDouble("yaw", 0);
        cachedAfkzonePitch = (float) section.getDouble("pitch", 0);
    }

    private static List<String> buildAutoMessageKeys(FileConfiguration c) {
        List<String> raw = c.getStringList("auto-messages.messages");
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                .map(entry -> entry != null && entry.startsWith("key: ") ? entry.substring(5) : entry)
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    private static LinkedHashMap<String, Integer> buildTeleportCountdownPermissions(FileConfiguration c) {
        ConfigurationSection sec = c.getConfigurationSection("teleport.countdown.permissions");
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        if (sec == null) return map;
        // Permission nodes contain dots (e.g. velto.teleport.instant), which Bukkit treats
        // as path separators — so they load as nested sections, not one literal key. A
        // shallow getKeys(false) would only see "velto" and read its value as 0. Walk the
        // full (deep) key set instead and keep the leaves that actually hold a number,
        // using each leaf's full dotted path as the permission node.
        for (String key : sec.getKeys(true)) {
            if (sec.isInt(key)) {
                map.put(key, sec.getInt(key));
            }
        }
        return map;
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
        String worldName = cachedAfkzoneWorld;
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, cachedAfkzoneX, cachedAfkzoneY, cachedAfkzoneZ, cachedAfkzoneYaw, cachedAfkzonePitch);
    }

    public static void setAfkzone(Location location) {
        getConfig().set("afkzone.location", location);
        VeltoPlugin.get().saveConfig();
        if (location == null || location.getWorld() == null) {
            cachedAfkzoneWorld = null;
        } else {
            cachedAfkzoneWorld = location.getWorld().getName();
            cachedAfkzoneX = location.getX();
            cachedAfkzoneY = location.getY();
            cachedAfkzoneZ = location.getZ();
            cachedAfkzoneYaw = location.getYaw();
            cachedAfkzonePitch = location.getPitch();
        }
    }

    // === BACK ===

    public static List<String> getBackBlacklistedWorlds() {
        return cachedBackBlacklistedWorlds;
    }

    // === USERDATA ===

    public static int getUserdataAutosaveIntervalTicks() {
        return cachedUserdataAutosaveIntervalTicks;
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

    // === TELEPORT ===

    public static boolean isTeleportCancelOnMove() {
        return cachedTeleportCancelOnMove;
    }

    public static int getTeleportCountdownDefault() {
        return cachedTeleportCountdownDefault;
    }

    public static LinkedHashMap<String, Integer> getTeleportCountdownPermissions() {
        return cachedTeleportCountdownPermissions;
    }

    public static int getTpaExpireSeconds() {
        return cachedTpaExpireSeconds;
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
