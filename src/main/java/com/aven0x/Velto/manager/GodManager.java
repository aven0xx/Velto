package com.aven0x.Velto.manager;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class GodManager {

    private static final Set<Player> godPlayers = new HashSet<>();

    public static boolean isGod(Player player) {
        return godPlayers.contains(player);
    }

    public static void setGod(Player player, boolean enabled) {
        if (enabled) {
            godPlayers.add(player);
        } else {
            godPlayers.remove(player);
        }
    }

    public static boolean toggleGod(Player player) {
        boolean newState = !isGod(player);
        setGod(player, newState);
        return newState;
    }

    public static Set<Player> getGodPlayers() {
        return godPlayers;
    }
}
