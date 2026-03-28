package com.aven0x.VeltoPaper.listeners;

import com.aven0x.VeltoPaper.managers.BackManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BackListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        BackManager.saveLocation(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (BackManager.isBacking(event.getPlayer().getUniqueId())) {
            BackManager.clearBacking(event.getPlayer().getUniqueId());
            return;
        }
        BackManager.saveLocation(event.getPlayer());
    }
}
