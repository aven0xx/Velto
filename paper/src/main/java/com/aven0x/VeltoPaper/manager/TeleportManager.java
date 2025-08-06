package com.aven0x.VeltoPaper.manager;

import com.aven0x.VeltoPaper.VeltoPaper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportManager {
    public void teleportAsync(Player player, Location location) {
        Bukkit.getScheduler().runTaskAsynchronously(
                VeltoPaper.getInstance(),
                () -> Bukkit.getScheduler().runTask(
                        VeltoPaper.getInstance(),
                        () -> player.teleport(location)
                )
        );
    }
}