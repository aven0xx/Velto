package com.aven0x.VeltoBukkit.managers;

import com.aven0x.Velto.managers.ChatFormatCache;
import com.aven0x.Velto.utils.AtMentionHandler;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import com.aven0x.VeltoBukkit.VeltoBukkit;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class ChatManager implements Listener {

    private final VeltoBukkit plugin;
    private final boolean papiAvailable;
    private BukkitTask refreshTask;

    public ChatManager(VeltoBukkit plugin) {
        this.plugin = plugin;
        this.papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startFormatRefreshTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String rawMessage = event.getMessage();

        if (isAtMentionMessage(rawMessage)) {
            event.setCancelled(true);
            runSync(() -> AtMentionHandler.handle(player, rawMessage));
            return;
        }

        String safeMessage = rawMessage.replace("%", "%%");
        event.setFormat(ChatFormatCache.get(player).replace("%message%", safeMessage));
    }

    private boolean isAtMentionMessage(String message) {
        if (!message.startsWith("@")) return false;
        int space = message.indexOf(' ');
        return space > 1 && !message.substring(space + 1).trim().isEmpty();
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private void startFormatRefreshTask() {
        if (refreshTask != null) return;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, ChatFormatCache::refreshAll, 20L, 20L * 60L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> ChatFormatCache.refresh(event.getPlayer()));

        if (PlayerUtil.isVanished(event.getPlayer())) {
            event.setJoinMessage(null);
            return;
        }

        String msg = ConfigUtil.getJoinMessage();
        msg = msg.replace("%player_name%", event.getPlayer().getName());

        if (papiAvailable) {
            String resolved = PlaceholderAPI.setPlaceholders(event.getPlayer(), msg);
            if (resolved != null) msg = resolved;
        }

        event.setJoinMessage(ChatFormatCache.translate(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ChatFormatCache.remove(event.getPlayer().getUniqueId());

        if (PlayerUtil.isVanished(event.getPlayer())) {
            event.setQuitMessage(null);
            return;
        }

        String msg = ConfigUtil.getQuitMessage();
        msg = msg.replace("%player_name%", event.getPlayer().getName());

        if (papiAvailable) {
            String resolved = PlaceholderAPI.setPlaceholders(event.getPlayer(), msg);
            if (resolved != null) msg = resolved;
        }

        event.setQuitMessage(ChatFormatCache.translate(msg));
    }

    public static class CC {
        public static String translate(String input) {
            return ChatFormatCache.translate(input);
        }

        public static List<String> translate(List<String> input) {
            return ChatFormatCache.translate(input);
        }
    }
}
