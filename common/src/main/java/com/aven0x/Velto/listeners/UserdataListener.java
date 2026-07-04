package com.aven0x.Velto.listeners;

import com.aven0x.Velto.managers.BackManager;
import com.aven0x.Velto.managers.GodManager;
import com.aven0x.Velto.managers.MsgManager;
import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.managers.TpaManager;
import com.aven0x.Velto.managers.UserdataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class UserdataListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UserdataManager.load(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!UserdataManager.isLoaded(uuid)) {
            UserdataManager.load(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        TeleportManager manager = TeleportManager.getInstance();
        if (manager != null) {
            manager.cancelPending(event.getPlayer());
        }
        TpaManager.cleanup(uuid);
        MsgManager.cleanup(uuid);
        BackManager.cleanup(uuid);
        GodManager.cleanup(uuid);
        UserdataManager.unload(uuid);
    }
}
