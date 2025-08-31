package com.aven0x.Velto.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * PlayerActiveListener (Bukkit-only)
 * Listens to MOVE (block-to-block), CHAT (async), and COMMAND (sync).
 * Does NO storage and NO AFK logic. It immediately forwards a signal ASYNCHRONOUSLY.
 * IMPORTANT:
 * - The ActivityConsumer runs on a background thread.
 * - Do NOT call Bukkit API in the consumer; handle your own thread-safe data only.
 * - If you need Bukkit operations, hop back to main with Bukkit.getScheduler().runTask(...).
 */
public final class PlayerActiveListener implements Listener {
    public enum ActivityType { MOVE, CHAT, COMMAND }

    @FunctionalInterface
    public interface ActivityConsumer {
        /** Called ASYNCHRONOUSLY. Do NOT use Bukkit API here. */
        void onPlayerActivityAsync(UUID playerId, ActivityType type);
    }

    private final Plugin plugin;
    private ActivityConsumer sink = null;

    public PlayerActiveListener(Plugin plugin) {
        this.plugin = plugin;
        this.sink = sink;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // --- Events ---

    /** Move is sync; filter head turns; capture UUID on main thread, then offload to async. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom() == null || e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return; // ignore tiny rotations
        }
        final UUID id = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                sink.onPlayerActivityAsync(id, ActivityType.MOVE)
        );
    }

    /** Chat is already async; capture UUID and pass straight through (still async). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        final UUID id = e.getPlayer().getUniqueId();
        sink.onPlayerActivityAsync(id, ActivityType.CHAT);
    }

    /** Commands are sync; capture UUID and offload to async immediately. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        final UUID id = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                sink.onPlayerActivityAsync(id, ActivityType.COMMAND)
        );
    }
}
