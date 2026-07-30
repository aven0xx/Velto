package com.aven0x.VeltoPaper.managers;

import com.aven0x.Velto.managers.IgnoreManager;
import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.utils.AtMentionHandler;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import com.aven0x.VeltoPaper.VeltoPaper;
import io.papermc.paper.event.player.AsyncChatEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatManager implements Listener {

    private final VeltoPaper plugin;
    private final boolean papiAvailable;

    public ChatManager(VeltoPaper plugin) {
        this.plugin = plugin;
        this.papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String rawMessage = LegacyComponentSerializer.legacySection().serialize(event.message());

        if (isAtMentionMessage(rawMessage)) {
            event.setCancelled(true);
            runSync(() -> AtMentionHandler.handle(player, rawMessage));
            return;
        }

        Set<Audience> viewers = new HashSet<>(event.viewers());
        event.setCancelled(true);

        runSync(() -> {
            if (!player.isOnline()) return;

            Component formatted = LegacyComponentSerializer.legacySection()
                    .deserialize(buildChatFormat(player, rawMessage));
            for (Audience viewer : viewers) {
                // Hide this player's chat from anyone who's ignoring them.
                if (viewer instanceof Player viewingPlayer && IgnoreManager.isBlocked(player, viewingPlayer)) {
                    continue;
                }
                viewer.sendMessage(formatted);
            }
        });
    }

    private String buildChatFormat(Player player, String messageText) {
        String format = resolveChatFormat(player);
        format = format.replace("%player_name%", player.getName());

        if (papiAvailable) {
            String resolved = PlaceholderAPI.setPlaceholders(player, format);
            if (resolved != null) format = resolved;
        }

        format = format.replace("%message%", messageText);
        return CC.translate(format);
    }

    private boolean isAtMentionMessage(String message) {
        if (!message.startsWith("@")) return false;
        int space = message.indexOf(' ');
        return space > 1 && !message.substring(space + 1).trim().isEmpty();
    }

    private void runSync(Runnable runnable) {
        // Chat events fire off-tick; defer the formatting + send to the global region.
        // Conservative migration — a full event.renderer rewrite belongs to the chat phase.
        Schedulers.get().global(runnable);
    }

    private String resolveChatFormat(Player player) {
        String fallback = ConfigUtil.getChatFormat();

        List<String> priority = ConfigUtil.getChatPriority();
        if (priority == null || priority.isEmpty()) {
            return fallback;
        }

        for (String group : priority) {
            ConfigurationSection sec = ConfigUtil.getChatGroupSection(group);
            if (sec == null) continue;

            String groupFormat = sec.getString("format", "");
            if (groupFormat == null || groupFormat.isBlank()) continue;

            String perm = sec.getString("permission", "");
            if (perm != null && !perm.isBlank() && player.hasPermission(perm)) {
                return groupFormat;
            }
        }

        return fallback;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
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

        event.setJoinMessage(CC.translate(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
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

        event.setQuitMessage(CC.translate(msg));
    }

    public static class CC {
        private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

        public static String translate(String input) {
            if (input == null) return "";
            Matcher matcher = HEX_PATTERN.matcher(input);
            while (matcher.find()) {
                String token = matcher.group();
                String hex = token.substring(1);
                input = input.replace(token, ChatColor.of(hex).toString());
            }
            return input.replace("&", "§");
        }

        public static List<String> translate(List<String> input) {
            return input.stream().map(CC::translate).collect(Collectors.toList());
        }
    }
}
