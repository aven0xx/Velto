package com.aven0x.Velto.listeners;

import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.List;
import java.util.stream.Collectors;

public class ChatListener implements Listener {

    // Delay by 1 tick so the client connection is fully ready to receive the packet.
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        Schedulers.get().entityDelayed(joined, () -> {
            if (!joined.isOnline()) return;
            String atJoined = "@" + joined.getName();

            List<String> othersAtNames = PlayerUtil.onlineSnapshot().stream()
                    .filter(p -> !p.equals(joined))
                    .map(p -> "@" + p.getName())
                    .collect(Collectors.toList());

            for (Player p : PlayerUtil.onlineSnapshot()) {
                p.addCustomChatCompletions(List.of(atJoined));
            }
            if (!othersAtNames.isEmpty()) {
                joined.addCustomChatCompletions(othersAtNames);
            }
        }, null, 1L);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (buffer.startsWith("/") || !buffer.startsWith("@") || buffer.contains(" ")) return;

        String partial = buffer.substring(1).toLowerCase();
        Player sender = event.getSender() instanceof Player p ? p : null;

        List<String> completions = PlayerUtil.onlineSnapshot().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(partial))
                .map(n -> "@" + n + " ")
                .collect(Collectors.toList());

        event.setCompletions(completions);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String atName = "@" + event.getPlayer().getName();
        for (Player p : PlayerUtil.onlineSnapshot()) {
            if (!p.equals(event.getPlayer())) {
                p.removeCustomChatCompletions(List.of(atName));
            }
        }
    }
}
