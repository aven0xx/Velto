package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
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
        Bukkit.getScheduler().runTaskAsynchronously(
                VeltoPlugin.get(),
                () -> Bukkit.getScheduler().runTask(
                        VeltoPlugin.get(),
                        () -> player.teleport(location)
                )
        );
    }
}
