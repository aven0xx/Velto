package com.aven0x.VeltoBukkit.manager;

import com.aven0x.VeltoBukkit.VeltoBukkit;
import com.aven0x.VeltoBukkit.utils.ConfigUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatManager implements Listener {

    private final VeltoBukkit plugin;
    private final boolean papiAvailable;

    public ChatManager(VeltoBukkit plugin) {
        this.plugin = plugin;
        this.papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String format = ConfigUtil.getChatFormat();

        // Manual replacements for placeholders
        format = format.replace("%player_name%", event.getPlayer().getName());

        // Apply PlaceholderAPI if available
        if (papiAvailable) {
            format = PlaceholderAPI.setPlaceholders(event.getPlayer(), format);
        }

        // Escape '%' characters in the message to avoid format issues
        String safeMessage = event.getMessage().replace("%", "%%");

        // Replace %message% placeholder with the safe player message
        format = format.replace("%message%", safeMessage);

        // Apply color formatting (& -> §, hex)
        format = CC.translate(format);

        // Set final formatted chat message
        event.setFormat(format);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String msg = ConfigUtil.getJoinMessage();

        // Manual fallback if PlaceholderAPI is missing
        msg = msg.replace("%player_name%", event.getPlayer().getName());

        if (papiAvailable) {
            msg = PlaceholderAPI.setPlaceholders(event.getPlayer(), msg);
        }

        event.setJoinMessage(CC.translate(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
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
            Matcher matcher = HEX_PATTERN.matcher(input);
            while (matcher.find()) {
                String color = matcher.group().substring(1); // Remove '&'
                input = input.replace("&" + color, ChatColor.of("#" + color.substring(1)).toString());
            }
            return input.replace("&", "§");
        }

        public static List<String> translate(List<String> input) {
            return input.stream().map(CC::translate).collect(Collectors.toList());
        }
    }
}