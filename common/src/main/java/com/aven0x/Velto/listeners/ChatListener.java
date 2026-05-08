package com.aven0x.Velto.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class ChatListener implements Listener {

    // When a player joins, add their @Name to every online player's suggestions
    // and seed the joiner with every existing player's @Name.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        String atJoined = "@" + joined.getName();

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Every online player (including the joiner) gets the joiner's @Name
            p.addCustomChatCompletions(List.of(atJoined));
            // The joiner gets every other online player's @Name
            if (!p.equals(joined)) {
                joined.addCustomChatCompletions(List.of("@" + p.getName()));
            }
        }
    }

    // When a player leaves, remove their @Name from every remaining player's suggestions.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String atName = "@" + event.getPlayer().getName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(event.getPlayer())) {
                p.removeCustomChatCompletions(List.of(atName));
            }
        }
    }
}
