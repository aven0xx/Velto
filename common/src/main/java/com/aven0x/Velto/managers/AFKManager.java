package com.aven0x.Velto.managers;

import com.aven0x.Velto.listeners.PlayerActiveListener;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AFKManager (Bukkit-only)
 * - Records last activity from async callbacks.
 * - Sync scan every N ticks flips AFK state.
 * - Broadcasts transitions unless the player is vanished.
 */
public final class AFKManager implements PlayerActiveListener.ActivityConsumer {

    private final Plugin plugin;
    private volatile long afkTimeoutMillis;
    private final long scanPeriodTicks;

    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();

    private int taskId = -1;

    public AFKManager(Plugin plugin, long afkTimeoutMillis, long scanPeriodTicks) {
        this.plugin = plugin;
        this.afkTimeoutMillis = afkTimeoutMillis;
        this.scanPeriodTicks = scanPeriodTicks;
    }

    /** Start the periodic scan and seed online players as active "now". */
    public void start() {
        final long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            lastActivity.put(p.getUniqueId(), now);
            afkPlayers.remove(p.getUniqueId());
        }
        this.taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::scanTick, 20L, scanPeriodTicks).getTaskId();
    }

    /** Stop scanning and clear state. */
    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        lastActivity.clear();
        afkPlayers.clear();
    }

    /** Update inactivity timeout at runtime. */
    public void setAfkTimeoutMillis(long timeoutMillis) {
        this.afkTimeoutMillis = timeoutMillis;
    }

    /** Called ASYNCHRONOUSLY by PlayerActiveListener (do NOT use Bukkit API here). */
    @Override
    public void onPlayerActivityAsync(UUID playerId, PlayerActiveListener.ActivityType type) {
        lastActivity.put(playerId, System.currentTimeMillis());
    }

    /** Returns true if the player is currently considered AFK. */
    public boolean isAFK(Player player) {
        return player != null && afkPlayers.contains(player.getUniqueId());
    }

    public boolean isAFK(UUID uuid) {
        return afkPlayers.contains(uuid);
    }

    /** Mark immediate activity (thread-safe; callable from any thread). */
    public void markActivity(UUID uuid) {
        lastActivity.put(uuid, System.currentTimeMillis());
    }

    public void markActivity(Player player) {
        if (player != null) markActivity(player.getUniqueId());
    }

    // ---------------- Internal sync scan ----------------

    /** Runs on main thread; flips AFK state and broadcasts if needed. */
    private void scanTick() {
        final long now = System.currentTimeMillis();

        for (Player p : Bukkit.getOnlinePlayers()) {
            final UUID id = p.getUniqueId();
            final long last = lastActivity.getOrDefault(id, 0L);
            final boolean timedOut = (now - last) >= afkTimeoutMillis;
            final boolean currentlyAfk = afkPlayers.contains(id);

            if (timedOut && !currentlyAfk) {
                // Enter AFK
                afkPlayers.add(id);

                if (!PlayerUtil.isVanished(p)) {
                    Bukkit.broadcastMessage("§7" + p.getName() + " is now AFK.");
                }

            } else if (!timedOut && currentlyAfk) {
                // Exit AFK
                afkPlayers.remove(id);

                if (!PlayerUtil.isVanished(p)) {
                    Bukkit.broadcastMessage("§a" + p.getName() + " is no longer AFK.");
                }
            }
        }

        // Housekeeping: drop entries for players who are no longer online.
        lastActivity.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        afkPlayers.removeIf(id -> Bukkit.getPlayer(id) == null);
    }
}
