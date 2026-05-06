package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UserdataManager {

    private UserdataManager() {}

    private static final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();
    private static File userdataFolder;
    private static Logger logger;
    private static volatile boolean initialized = false;

    public static void init(File dataFolder) {
        logger = VeltoPlugin.get().getLogger();
        userdataFolder = new File(dataFolder, "userdata");

        if (!userdataFolder.exists() && !userdataFolder.mkdirs()) {
            logger.severe("[Velto] Failed to create userdata folder: " + userdataFolder.getAbsolutePath());
        }

        initialized = true;
        logger.info("[Velto] UserdataManager initialized.");
    }

    public static boolean isInitialized() {
        return initialized;
    }

    // === Load / Unload ===

    public static void load(UUID uuid) {
        if (!initialized) return;
        File file = fileFor(uuid);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        cache.put(uuid, yaml);
    }

    public static void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    // === Getters / Setters ===

    public static YamlConfiguration getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> YamlConfiguration.loadConfiguration(fileFor(u)));
    }

    public static Object get(UUID uuid, String key) {
        return getData(uuid).get(key);
    }

    public static String getString(UUID uuid, String key, String def) {
        return getData(uuid).getString(key, def);
    }

    public static int getInt(UUID uuid, String key, int def) {
        return getData(uuid).getInt(key, def);
    }

    public static boolean getBoolean(UUID uuid, String key, boolean def) {
        return getData(uuid).getBoolean(key, def);
    }

    public static void set(UUID uuid, String key, Object value) {
        getData(uuid).set(key, value);
    }

    // === Persistence ===

    public static void save(UUID uuid) {
        if (!initialized) return;
        YamlConfiguration yaml = cache.get(uuid);
        if (yaml == null) return;

        YamlConfiguration snapshot = copyOf(yaml);
        File target = fileFor(uuid);

        VeltoPlugin.get().getServer().getScheduler().runTaskAsynchronously(VeltoPlugin.get(), () -> {
            try {
                snapshot.save(target);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[Velto] Failed to save userdata for " + uuid, e);
            }
        });
    }

    // Synchronous — call only from onDisable where async tasks won't run.
    public static void saveAll() {
        if (!initialized) return;
        for (Map.Entry<UUID, YamlConfiguration> entry : cache.entrySet()) {
            try {
                entry.getValue().save(fileFor(entry.getKey()));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[Velto] Failed to save userdata for " + entry.getKey() + " on shutdown", e);
            }
        }
    }

    // === Helpers ===

    private static File fileFor(UUID uuid) {
        return new File(userdataFolder, uuid.toString() + ".yml");
    }

    private static YamlConfiguration copyOf(YamlConfiguration source) {
        YamlConfiguration copy = new YamlConfiguration();
        for (String key : source.getKeys(true)) {
            copy.set(key, source.get(key));
        }
        return copy;
    }
}
