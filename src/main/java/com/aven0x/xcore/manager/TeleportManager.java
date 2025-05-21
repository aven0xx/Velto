package com.aven0x.xcore.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportManager {
    public void teleportAsync(Player player, Location location) {
        Bukkit.getScheduler().runTaskAsynchronously(
                com.aven0x.xcore.Xcore.getInstance(),
                () -> Bukkit.getScheduler().runTask(
                        com.aven0x.xcore.Xcore.getInstance(),
                        () -> player.teleport(location)
                )
        );
    }
}