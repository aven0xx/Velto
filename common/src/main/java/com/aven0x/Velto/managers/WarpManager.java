package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WarpManager {

    private WarpManager() {}

    private static File file;
    private static YamlConfiguration data;
    private static volatile boolean initialized = false;

    private static Logger logger() {
        return VeltoPlugin.get().getLogger();
    }

    public static void init(File dataFolder) {
        file = new File(dataFolder, "warps.yml");
        data = YamlConfiguration.loadConfiguration(file);
        initialized = true;
    }

    // Re-reads warps.yml from disk, picking up any external edits.
    public static void reload() {
        if (file == null) return;
        data = YamlConfiguration.loadConfiguration(file);
    }

    public static void setWarp(String name, Location location) {
        if (!initialized) return;
        if (location.getWorld() == null) {
            logger().warning("[Velto] Cannot set warp '" + name + "': world is null (unloaded?).");
            return;
        }
        String base = "warps." + name + ".";
        data.set(base + "world", location.getWorld().getName());
        data.set(base + "x", location.getX());
        data.set(base + "y", location.getY());
        data.set(base + "z", location.getZ());
        data.set(base + "yaw", (double) location.getYaw());
        data.set(base + "pitch", (double) location.getPitch());
        save();
    }

    public static Location getWarp(String name) {
        if (!initialized) return null;
        ConfigurationSection sec = data.getConfigurationSection("warps." + name);
        if (sec == null) return null;

        String worldName = sec.getString("world");
        if (worldName == null || worldName.isBlank()) {
            logger().warning("[Velto] Warp '" + name + "' has no world set.");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger().warning("[Velto] Warp '" + name + "' references unloaded world '" + worldName + "'.");
            return null;
        }

        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw");
        float pitch = (float) sec.getDouble("pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static boolean hasWarp(String name) {
        return initialized && data.getConfigurationSection("warps." + name) != null;
    }

    public static List<String> getWarpNames() {
        if (!initialized) return Collections.emptyList();
        ConfigurationSection sec = data.getConfigurationSection("warps");
        if (sec == null) return Collections.emptyList();
        return List.copyOf(sec.getKeys(false));
    }

    private static void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            logger().log(Level.SEVERE, "[Velto] Failed to save warps.yml", e);
        }
    }
}
