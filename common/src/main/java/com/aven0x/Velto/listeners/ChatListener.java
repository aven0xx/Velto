package com.aven0x.Velto.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.List;
import java.util.stream.Collectors;

public class ChatListener implements Listener {

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        // Only handle non-command chat starting with @
        if (buffer.startsWith("/") || !buffer.startsWith("@")) return;
        // Stop once the player has typed a space — they're past the name
        String partial = buffer.substring(1);
        if (partial.contains(" ")) return;

        String lower = partial.toLowerCase();
        Player sender = event.getSender() instanceof Player p ? p : null;

        List<String> completions = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(sender))
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(lower))
                .map(n -> "@" + n + " ")
                .collect(Collectors.toList());

        event.setCompletions(completions);
    }
}
