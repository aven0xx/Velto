package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class HomeManager {

    private HomeManager() {}

    public static final String DEFAULT_HOME = "home";

    private static Logger logger() {
        return VeltoPlugin.get().getLogger();
    }

    public static void setHome(UUID uuid, String name, Location location) {
        String base = "homes." + name + ".";
        UserdataManager.set(uuid, base + "world", location.getWorld().getName());
        UserdataManager.set(uuid, base + "x", location.getX());
        UserdataManager.set(uuid, base + "y", location.getY());
        UserdataManager.set(uuid, base + "z", location.getZ());
        UserdataManager.set(uuid, base + "yaw", (double) location.getYaw());
        UserdataManager.set(uuid, base + "pitch", (double) location.getPitch());
        UserdataManager.save(uuid);
    }

    public static Location getHome(UUID uuid, String name) {
        YamlConfiguration data = UserdataManager.getData(uuid);
        ConfigurationSection sec = data.getConfigurationSection("homes." + name);
        if (sec == null) return null;

        String worldName = sec.getString("world");
        if (worldName == null || worldName.isBlank()) {
            logger().warning("[Velto] Home '" + name + "' for " + uuid + " has no world set.");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger().warning("[Velto] Home '" + name + "' for " + uuid + " references unloaded world '" + worldName + "'.");
            return null;
        }

        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw");
        float pitch = (float) sec.getDouble("pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static boolean hasHome(UUID uuid, String name) {
        return UserdataManager.getData(uuid).getConfigurationSection("homes." + name) != null;
    }

    public static void deleteHome(UUID uuid, String name) {
        UserdataManager.set(uuid, "homes." + name, null);
        UserdataManager.save(uuid);
    }

    public static List<String> getHomeNames(UUID uuid) {
        ConfigurationSection sec = UserdataManager.getData(uuid).getConfigurationSection("homes");
        if (sec == null) return Collections.emptyList();
        return List.copyOf(sec.getKeys(false));
    }
}
