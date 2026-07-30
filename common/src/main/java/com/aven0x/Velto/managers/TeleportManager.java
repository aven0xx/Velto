package com.aven0x.Velto.managers;

import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.platform.VeltoTask;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private static TeleportManager instance;
    private final Map<UUID, VeltoTask> pendingTeleports = new ConcurrentHashMap<>();

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
        UUID uuid = player.getUniqueId();

        // Preserve the original 0-tick behaviour: the first countdown line shows immediately.
        // The entity scheduler clamps any delay below 1 to 1, so the repeating timer starts one
        // tick later and only handles the remaining seconds.
        LangUtil.send(player, "teleport-countdown", Map.of("%seconds%", String.valueOf(seconds)));

        int[] remaining = { seconds - 1 };
        VeltoTask[] handle = new VeltoTask[1];

        handle[0] = Schedulers.get().entityTimer(player,
                () -> {
                    if (!player.isOnline()) {
                        finish(uuid, handle[0]);
                        return;
                    }

                    if (ConfigUtil.isTeleportCancelOnMove() && hasMoved(player, origin)) {
                        finish(uuid, handle[0]);
                        LangUtil.send(player, "teleport-cancelled");
                        return;
                    }

                    if (remaining[0] <= 0) {
                        finish(uuid, handle[0]);
                        teleportAsync(player, location).thenAccept(success -> runCompletion(player, success, onComplete));
                        return;
                    }

                    LangUtil.send(player, "teleport-countdown", Map.of("%seconds%", String.valueOf(remaining[0])));
                    remaining[0]--;
                },
                () -> pendingTeleports.remove(uuid),   // retired: player vanished before a tick fired
                20L, 20L);

        VeltoTask previous = pendingTeleports.put(uuid, handle[0]);
        if (previous != null) previous.cancel();
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
        VeltoTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    // Cancels every pending countdown. Called from onDisable, since entity-scheduled
    // tasks are not covered by the scheduler's bulk cancelAll().
    public void cancelAll() {
        for (VeltoTask task : pendingTeleports.values()) {
            task.cancel();
        }
        pendingTeleports.clear();
    }

    // Removes this player's pending entry only if it still points at `task`, then cancels it.
    // The conditional remove keeps a concurrent re-teleport's newer entry intact.
    private void finish(UUID uuid, VeltoTask task) {
        if (task == null) return;
        pendingTeleports.remove(uuid, task);
        task.cancel();
    }

    private boolean bukkitTeleport(Player player, Location location) {
        if (location.getWorld() == null) return false;
        location.getWorld().getChunkAt(location);
        return player.teleport(location);
    }

    private void runCompletion(Player player, boolean success, Runnable onComplete) {
        if (!success || onComplete == null) return;

        // teleportAsync completes on an unspecified thread; only touch the player from the
        // region that owns them.
        if (Schedulers.get().owns(player)) {
            if (player.isOnline()) onComplete.run();
            return;
        }

        Schedulers.get().entity(player, () -> {
            if (player.isOnline()) onComplete.run();
        }, null);
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
