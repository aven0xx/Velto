package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UserdataManager {

    private UserdataManager() {}

    private static final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingSave> pendingSaves = new ConcurrentHashMap<>();
    private static File userdataFolder;
    private static Logger logger;
    private static volatile boolean initialized = false;
    private static BukkitTask autosaveTask = null;

    private static final class PendingSave {
        YamlConfiguration snapshot;
        YamlConfiguration latestSnapshot;
        boolean running;

        PendingSave(YamlConfiguration snapshot) {
            this.snapshot = snapshot;
            this.latestSnapshot = snapshot;
        }
    }

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

    public static boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
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
        YamlConfiguration yaml = getData(uuid);
        synchronized (yaml) {
            return yaml.get(key);
        }
    }

    public static String getString(UUID uuid, String key, String def) {
        YamlConfiguration yaml = getData(uuid);
        synchronized (yaml) {
            return yaml.getString(key, def);
        }
    }

    public static int getInt(UUID uuid, String key, int def) {
        YamlConfiguration yaml = getData(uuid);
        synchronized (yaml) {
            return yaml.getInt(key, def);
        }
    }

    public static boolean getBoolean(UUID uuid, String key, boolean def) {
        YamlConfiguration yaml = getData(uuid);
        synchronized (yaml) {
            return yaml.getBoolean(key, def);
        }
    }

    public static List<String> getStringList(UUID uuid, String key) {
        YamlConfiguration yaml = getData(uuid);
        synchronized (yaml) {
            return yaml.getStringList(key);
        }
    }

    public static void set(UUID uuid, String key, Object value) {
        YamlConfiguration yaml = getData(uuid);
        synchronized (yaml) {
            yaml.set(key, value);
        }
    }

    // === Persistence ===

    public static void save(UUID uuid) {
        if (!initialized) return;
        YamlConfiguration yaml = cache.get(uuid);
        if (yaml == null) return;

        YamlConfiguration snapshot = copyOf(yaml);
        enqueueSave(uuid, snapshot);
    }

    public static void startAutosave(long intervalTicks) {
        if (autosaveTask != null) return;
        autosaveTask = VeltoPlugin.get().getServer().getScheduler()
                .runTaskTimer(VeltoPlugin.get(), () -> {
                    for (Map.Entry<UUID, YamlConfiguration> entry : cache.entrySet()) {
                        YamlConfiguration snapshot = copyOf(entry.getValue());
                        enqueueSave(entry.getKey(), snapshot);
                    }
                }, intervalTicks, intervalTicks);
    }

    public static void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    // Synchronous — call only from onDisable where async tasks won't run.
    public static void saveAll() {
        if (!initialized) return;
        for (Map.Entry<UUID, YamlConfiguration> entry : cache.entrySet()) {
            try {
                entry.getValue().save(fileFor(entry.getKey()));
                pendingSaves.remove(entry.getKey());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[Velto] Failed to save userdata for " + entry.getKey() + " on shutdown", e);
            }
        }

        for (Map.Entry<UUID, PendingSave> entry : pendingSaves.entrySet()) {
            YamlConfiguration snapshot;
            synchronized (entry.getValue()) {
                snapshot = entry.getValue().latestSnapshot;
            }
            if (snapshot == null) continue;

            try {
                snapshot.save(fileFor(entry.getKey()));
                pendingSaves.remove(entry.getKey());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[Velto] Failed to flush pending userdata for " + entry.getKey() + " on shutdown", e);
            }
        }
    }

    // === Bulk / offline reads (for leaderboard-style scans) ===

    /** Snapshot of every currently-loaded player's UUID. */
    public static Set<UUID> getCachedUuids() {
        return new HashSet<>(cache.keySet());
    }

    /**
     * Reads a double from a player's in-memory data <em>only if they're currently loaded</em>,
     * returning {@code null} otherwise. Unlike {@link #getData(UUID)} this never loads (or
     * re-creates) a cache entry, so it's safe to call while iterating many players without
     * leaking cache entries for offline ones — see DATA_STORAGE.md.
     */
    public static Double getLoadedDouble(UUID uuid, String key) {
        YamlConfiguration yaml = cache.get(uuid);
        if (yaml == null) return null;
        synchronized (yaml) {
            return yaml.contains(key) ? yaml.getDouble(key) : null;
        }
    }

    /**
     * Every UUID that has an on-disk userdata file. Reads the userdata directory directly, so it
     * also sees players who aren't currently online. Safe to call off the main thread.
     */
    public static Set<UUID> listStoredUuids() {
        Set<UUID> uuids = new HashSet<>();
        if (userdataFolder == null) return uuids;

        File[] files = userdataFolder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null) return uuids;

        for (File file : files) {
            String fileName = file.getName();
            String raw = fileName.substring(0, fileName.length() - ".yml".length());
            try {
                uuids.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // Not a UUID-named file — skip it.
            }
        }
        return uuids;
    }

    /**
     * Reads a double straight from a player's file on disk without touching the in-memory cache.
     * Intended for offline players in a bulk scan; prefer {@link #getLoadedDouble(UUID, String)}
     * first for loaded players so their unsaved changes are reflected. Safe off the main thread.
     */
    public static double readDoubleFromDisk(UUID uuid, String key, double def) {
        if (userdataFolder == null) return def;
        return YamlConfiguration.loadConfiguration(fileFor(uuid)).getDouble(key, def);
    }

    // === Helpers ===

    private static File fileFor(UUID uuid) {
        return new File(userdataFolder, uuid.toString() + ".yml");
    }

    private static void enqueueSave(UUID uuid, YamlConfiguration snapshot) {
        PendingSave save = pendingSaves.compute(uuid, (ignored, existing) -> {
            if (existing == null) return new PendingSave(snapshot);
            synchronized (existing) {
                existing.snapshot = snapshot;
                existing.latestSnapshot = snapshot;
            }
            return existing;
        });

        boolean shouldStart;
        synchronized (save) {
            shouldStart = !save.running;
            if (shouldStart) save.running = true;
        }

        if (shouldStart) {
            runNextSave(uuid, save);
        }
    }

    private static void runNextSave(UUID uuid, PendingSave save) {
        YamlConfiguration snapshot;
        synchronized (save) {
            snapshot = save.snapshot;
            save.snapshot = null;
        }

        if (snapshot == null) {
            boolean hasMore;
            synchronized (save) {
                hasMore = save.snapshot != null;
                save.running = hasMore;
            }
            if (hasMore) {
                runNextSave(uuid, save);
            } else {
                pendingSaves.remove(uuid, save);
            }
            return;
        }

        File target = fileFor(uuid);
        YamlConfiguration finalSnapshot = snapshot;

        VeltoPlugin.get().getServer().getScheduler().runTaskAsynchronously(VeltoPlugin.get(), () -> {
            try {
                if (pendingSaves.get(uuid) == save) {
                    finalSnapshot.save(target);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[Velto] Failed to save userdata for " + uuid, e);
            } finally {
                runNextSave(uuid, save);
            }
        });
    }

    private static YamlConfiguration copyOf(YamlConfiguration source) {
        YamlConfiguration copy = new YamlConfiguration();
        synchronized (source) {
            for (String key : source.getKeys(true)) {
                copy.set(key, source.get(key));
            }
        }
        return copy;
    }
}
