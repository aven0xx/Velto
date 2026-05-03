package com.aven0x.Velto.utils;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
        logger = VeltoPlugin.get().getLogger();

        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                logger.severe("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
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
     * Writes memory map to YAML asynchronously.
     * Snapshots the map on the calling thread for consistency, then does I/O off-thread.
     */
    public static void save() {
        if (!initialized) {
            if (logger != null) logger.warning("AfkPositionStorage.save() called before init().");
            return;
        }

        Map<UUID, Location> snapshot = new HashMap<>(pendingReturns);

        VeltoPlugin.get().getServer().getScheduler().runTaskAsynchronously(VeltoPlugin.get(), () -> {
            YamlConfiguration fresh = new YamlConfiguration();
            for (Map.Entry<UUID, Location> entry : snapshot.entrySet()) {
                fresh.set("pending." + entry.getKey().toString(), entry.getValue());
            }
            try {
                fresh.save(file);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to save afkposition.yml", e);
            }
        });
    }
}
