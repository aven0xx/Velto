package com.aven0x.Velto.utils;

import org.bukkit.entity.Player;

public class PlayerUtil {

    /**
     * Checks if the player is vanished.
     * Works with EssentialsX, SuperVanish, and other plugins that set "vanished" metadata.
     */
    public static boolean isVanished(Player player) {
        return player.hasMetadata("vanished");
    }

    /**
     * Optional helper: Checks if the player is visible (not vanished).
     */
    public static boolean isVisible(Player player) {
        return !isVanished(player);
    }
}
