package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UserdataManager {

    private UserdataManager() {}

    private static final int AUTOSAVE_BATCH_SIZE = 10;

    private static final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingSave> pendingSaves = new ConcurrentHashMap<>();
    private static final Set<UUID> dirtyUsers = ConcurrentHashMap.newKeySet();
    private static File userdataFolder;
    private static Logger logger;
    private static volatile boolean initialized = false;
    private static BukkitTask autosaveTask = null;
    private static BukkitTask autosaveFlushTask = null;

    private static final class PendingSave {
        YamlConfiguration snapshot;
        boolean running;

        PendingSave(YamlConfiguration snapshot) {
            this.snapshot = snapshot;
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

    public static void set(UUID uuid, String key, Object value) {
        YamlConfiguration yaml = getData(uuid);
        synchronized (yaml) {
            yaml.set(key, value);
        }
        dirtyUsers.add(uuid);
    }

    // === Persistence ===

    public static void save(UUID uuid) {
        saveSnapshot(uuid, true);
    }

    public static void startAutosave(long intervalTicks) {
        if (autosaveTask != null) return;
        autosaveTask = VeltoPlugin.get().getServer().getScheduler()
                .runTaskTimer(VeltoPlugin.get(), UserdataManager::flushDirtyUsers, intervalTicks, intervalTicks);
    }

    public static void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (autosaveFlushTask != null) {
            autosaveFlushTask.cancel();
            autosaveFlushTask = null;
        }
    }

    // Synchronous — call only from onDisable where async tasks won't run.
    public static void saveAll() {
        if (!initialized) return;
        dirtyUsers.clear();
        for (Map.Entry<UUID, YamlConfiguration> entry : cache.entrySet()) {
            try {
                YamlConfiguration snapshot = copyOf(entry.getValue());
                snapshot.save(fileFor(entry.getKey()));
                pendingSaves.remove(entry.getKey());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[Velto] Failed to save userdata for " + entry.getKey() + " on shutdown", e);
            }
        }
    }

    // === Helpers ===

    private static File fileFor(UUID uuid) {
        return new File(userdataFolder, uuid.toString() + ".yml");
    }

    private static void flushDirtyUsers() {
        if (autosaveFlushTask != null || dirtyUsers.isEmpty()) return;

        List<UUID> users = new ArrayList<>(dirtyUsers);
        autosaveFlushTask = VeltoPlugin.get().getServer().getScheduler().runTaskTimer(VeltoPlugin.get(), new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < users.size() && processed < AUTOSAVE_BATCH_SIZE) {
                    UUID uuid = users.get(index++);
                    if (dirtyUsers.remove(uuid)) {
                        saveSnapshot(uuid, false);
                    }
                    processed++;
                }

                if (index >= users.size()) {
                    autosaveFlushTask.cancel();
                    autosaveFlushTask = null;
                }
            }
        }, 0L, 1L);
    }

    private static void saveSnapshot(UUID uuid, boolean clearDirty) {
        if (!initialized) return;
        YamlConfiguration yaml = cache.get(uuid);
        if (yaml == null) return;

        if (clearDirty) dirtyUsers.remove(uuid);
        YamlConfiguration snapshot = copyOf(yaml);
        enqueueSave(uuid, snapshot);
    }

    private static void enqueueSave(UUID uuid, YamlConfiguration snapshot) {
        PendingSave save = pendingSaves.compute(uuid, (ignored, existing) -> {
            if (existing == null) return new PendingSave(snapshot);
            synchronized (existing) {
                existing.snapshot = snapshot;
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
