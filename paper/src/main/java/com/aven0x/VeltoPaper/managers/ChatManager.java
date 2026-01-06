package com.aven0x.VeltoPaper.managers;

import com.aven0x.VeltoPaper.VeltoPaper;
import com.aven0x.VeltoPaper.utils.ConfigUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
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
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String format = resolveChatFormat(player);

        // Manual placeholder
        format = format.replace("%player_name%", player.getName());

        // PAPI placeholders
        if (papiAvailable) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        // Escape % to avoid String.format issues in Bukkit chat format
        String safeMessage = event.getMessage().replace("%", "%%");
        format = format.replace("%message%", safeMessage);

        // Colors
        format = CC.translate(format);

        event.setFormat(format);
    }

    /**
     * Dynamic groups:
     * - Default: messages.chat (required)
     * - Optional priority list: messages.chat-priority
     * - Optional groups: messages.chat-groups.<group>.format + .permission(optional)
     */
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
            if (perm == null || perm.isBlank() || player.hasPermission(perm)) {
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
            msg = PlaceholderAPI.setPlaceholders(event.getPlayer(), msg);
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
            msg = PlaceholderAPI.setPlaceholders(event.getPlayer(), msg);
        }

        event.setQuitMessage(CC.translate(msg));
    }

    public static class CC {
        private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

        public static String translate(String input) {
            if (input == null) return "";
            Matcher matcher = HEX_PATTERN.matcher(input);
            while (matcher.find()) {
                String token = matcher.group();  // "&#RRGGBB"
                String hex = token.substring(1); // "#RRGGBB"
                input = input.replace(token, ChatColor.of(hex).toString());
            }
            return input.replace("&", "§");
        }

        public static List<String> translate(List<String> input) {
            return input.stream().map(CC::translate).collect(Collectors.toList());
        }
    }
}
