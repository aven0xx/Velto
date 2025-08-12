package com.aven0x.VeltoBukkit.managers;

import com.aven0x.VeltoBukkit.VeltoBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportManager {
    public void teleportAsync(Player player, Location location) {
        Bukkit.getScheduler().runTaskAsynchronously(
                VeltoBukkit.getInstance(),
                () -> Bukkit.getScheduler().runTask(
                        VeltoBukkit.getInstance(),
                        () -> player.teleport(location)
                )
        );
    }
}