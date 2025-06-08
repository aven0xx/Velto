package com.aven0x.Velto.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GodManager {

    private static final Set<UUID> godPlayers = new HashSet<>();

    public static boolean isGod(Player player) {
        return godPlayers.contains(player.getUniqueId());
    }

    public static void setGod(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        if (enabled) {
            godPlayers.add(uuid);
        } else {
            godPlayers.remove(uuid);
        }
    }

    public static boolean toggleGod(Player player) {
        boolean newState = !isGod(player);
        setGod(player, newState);
        return newState;
    }

    public static Set<Player> getGodPlayers() {
        Set<Player> players = new HashSet<>();
        for (UUID uuid : godPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }
}
