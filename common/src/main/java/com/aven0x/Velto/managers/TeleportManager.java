package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private static TeleportManager instance;
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();

    public TeleportManager() {
        instance = this;
    }

    public static TeleportManager getInstance() {
        return instance;
    }

    // Player-initiated teleport: honours countdown and cancel-on-move.
    public void teleport(Player player, Location location) {
        teleport(player, location, null);
    }

    // Player-initiated teleport with an optional callback executed after the
    // teleport completes successfully.
    public void teleport(Player player, Location location, Runnable onComplete) {
        cancelPending(player);

        int seconds = resolveCountdown(player);

        if (seconds <= 0) {
            teleportAsync(player, location).thenAccept(success -> runCompletion(player, success, onComplete));
            return;
        }

        Location origin = player.getLocation().clone();

        BukkitRunnable runnable = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    pendingTeleports.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (ConfigUtil.isTeleportCancelOnMove() && hasMoved(player, origin)) {
                    pendingTeleports.remove(player.getUniqueId());
                    this.cancel();
                    LangUtil.send(player, "teleport-cancelled");
                    return;
                }

                if (remaining <= 0) {
                    pendingTeleports.remove(player.getUniqueId());
                    this.cancel();
                    teleportAsync(player, location).thenAccept(success -> runCompletion(player, success, onComplete));
                    return;
                }

                LangUtil.send(player, "teleport-countdown", Map.of("%seconds%", String.valueOf(remaining)));
                remaining--;
            }
        };

        pendingTeleports.put(player.getUniqueId(), runnable.runTaskTimer(VeltoPlugin.get(), 0L, 20L));
    }

    // System/admin teleport: bypasses countdown entirely (AFK zone, /tpall, etc.)
    public CompletableFuture<Boolean> teleportAsync(Player player, Location location) {
        if (ServerUtil.isPaper()) {
            try {
                Method method = player.getClass().getMethod("teleportAsync", Location.class);
                Object result = method.invoke(player, location);
                if (result instanceof CompletableFuture<?> future) {
                    return future.thenApply(Boolean.class::cast);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Velto] teleportAsync failed on Paper, falling back to sync chunk load: " + e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(bukkitTeleport(player, location));
    }

    // Cancels any pending countdown for the given player.
    public void cancelPending(Player player) {
        BukkitTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private boolean bukkitTeleport(Player player, Location location) {
        if (location.getWorld() == null) return false;
        location.getWorld().getChunkAt(location);
        return player.teleport(location);
    }

    private void runCompletion(Player player, boolean success, Runnable onComplete) {
        if (!success || onComplete == null) return;

        if (Bukkit.isPrimaryThread()) {
            if (player.isOnline()) onComplete.run();
            return;
        }

        Bukkit.getScheduler().runTask(VeltoPlugin.get(), () -> {
            if (player.isOnline()) onComplete.run();
        });
    }

    private int resolveCountdown(Player player) {
        for (Map.Entry<String, Integer> entry : ConfigUtil.getTeleportCountdownPermissions().entrySet()) {
            if (player.hasPermission(entry.getKey())) return entry.getValue();
        }
        return ConfigUtil.getTeleportCountdownDefault();
    }

    private boolean hasMoved(Player player, Location origin) {
        Location current = player.getLocation();
        return current.getBlockX() != origin.getBlockX()
                || current.getBlockY() != origin.getBlockY()
                || current.getBlockZ() != origin.getBlockZ();
    }
}
