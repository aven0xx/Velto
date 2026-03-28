package com.aven0x.VeltoPaper.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BackManager {

    private static final Map<UUID, Location> lastLocations = new HashMap<>();
    private static final Set<UUID> backing = new HashSet<>();

    public static void saveLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public static Location getLastLocation(Player player) {
        return lastLocations.get(player.getUniqueId());
    }

    public static void markBacking(UUID uuid) {
        backing.add(uuid);
    }

    public static boolean isBacking(UUID uuid) {
        return backing.contains(uuid);
    }

    public static void clearBacking(UUID uuid) {
        backing.remove(uuid);
    }
}
