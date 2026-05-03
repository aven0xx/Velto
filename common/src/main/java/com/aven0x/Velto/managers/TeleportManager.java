package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportManager {

    private static TeleportManager instance;

    public TeleportManager() {
        instance = this;
    }

    public static TeleportManager getInstance() {
        return instance;
    }

    public void teleportAsync(Player player, Location location) {
        if (ServerUtil.isPaper()) {
            // Paper: native teleportAsync handles chunk loading entirely off the main thread
            try {
                player.getClass().getMethod("teleportAsync", Location.class).invoke(player, location);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Velto] teleportAsync failed on Paper, falling back to sync chunk load: " + e.getMessage());
                bukkitTeleport(player, location);
            }
        } else {
            bukkitTeleport(player, location);
        }
    }

    private void bukkitTeleport(Player player, Location location) {
        location.getWorld().getChunkAt(location);
        player.teleport(location);
    }
}
