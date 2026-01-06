package com.aven0x.VeltoBukkit.utils;

import com.aven0x.VeltoBukkit.VeltoBukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistent storage for AFK "pending return" locations.
 * File: plugins/Velto/afkposition.yml
 * Loads once at startup, keeps a memory map, saves only when modified.
 */
public final class AfkPositionStorage {

    private AfkPositionStorage() {}

    private static final Map<UUID, Location> pendingReturns = new ConcurrentHashMap<>();

    private static File file;
    private static YamlConfiguration config;
    private static Logger logger;

    private static volatile boolean initialized = false;

    public static void init(File dataFolder) {
        logger = VeltoBukkit.getInstance().getLogger();

        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                logger.severe("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
                // We can still continue; file operations will fail with clear logs.
            }
        }

        file = new File(dataFolder, "afkposition.yml");

        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (!created) {
                    logger.warning("afkposition.yml already exists (race condition?)");
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create afkposition.yml", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        loadToMemory();

        initialized = true;
        logger.info("AfkPositionStorage loaded. Pending returns: " + pendingReturns.size());
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static void loadToMemory() {
        pendingReturns.clear();

        ConfigurationSection pending = config.getConfigurationSection("pending");
        if (pending == null) return;

        for (String key : pending.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Location loc = config.getLocation("pending." + key);
                if (loc != null && loc.getWorld() != null) {
                    pendingReturns.put(uuid, loc.clone());
                } else {
                    // if world is missing, keep it out to avoid teleport errors
                    logger.warning("Skipping pending return for " + key + " (location/world missing).");
                }
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid UUID in afkposition.yml: " + key);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed reading pending return for key: " + key, ex);
            }
        }
    }

    public static Location get(UUID uuid) {
        Location loc = pendingReturns.get(uuid);
        return (loc == null) ? null : loc.clone();
    }

    public static boolean has(UUID uuid) {
        return pendingReturns.containsKey(uuid);
    }

    public static void put(UUID uuid, Location location) {
        if (location == null) return;
        pendingReturns.put(uuid, location.clone());
    }

    public static void remove(UUID uuid) {
        pendingReturns.remove(uuid);
    }

    /**
     * Writes memory map to YAML and saves to disk.
     * Call only when the map changes (quit AFK / join after teleport).
     */
    public static void save() {
        if (config == null || file == null) {
            if (logger != null) logger.warning("AfkPositionStorage.save() called before init().");
            return;
        }

        // rewrite section from scratch (small file, simple & safe)
        config.set("pending", null);

        for (Map.Entry<UUID, Location> entry : pendingReturns.entrySet()) {
            config.set("pending." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save afkposition.yml", e);
        }
    }
}
