package com.aven0x.Velto.utils;

import com.aven0x.Velto.platform.Schedulers;
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

    /**
     * Runs {@code action} on the region that owns {@code target} — inline when the caller
     * already owns it (always the case on Spigot's single main thread), otherwise hopped onto
     * the target's own region. Use this before reading or mutating a player the current thread
     * may not own, so the same call is safe on Paper, Spigot and Folia alike.
     */
    public static void onOwningRegion(Player target, Runnable action) {
        if (Schedulers.get().owns(target)) {
            action.run();
            return;
        }
        Schedulers.get().entity(target, action, null);
    }
}
