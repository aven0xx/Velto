package com.aven0x.Velto.manager;

import com.aven0x.Velto.Velto;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportManager {
    public void teleportAsync(Player player, Location location) {
        Bukkit.getScheduler().runTaskAsynchronously(
                Velto.getInstance(),
                () -> Bukkit.getScheduler().runTask(
                        Velto.getInstance(),
                        () -> player.teleport(location)
                )
        );
    }
}