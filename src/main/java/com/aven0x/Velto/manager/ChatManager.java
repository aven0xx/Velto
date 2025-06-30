package com.aven0x.Velto.manager;

import com.aven0x.Velto.Velto;
import com.aven0x.Velto.utils.ConfigUtil;
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

    private final Velto plugin;
    private final boolean papiAvailable;

    public ChatManager(Velto plugin) {
        this.plugin = plugin;
        this.papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String format = ConfigUtil.getChatFormat();
        format = format.replace("%message%", event.getMessage());

        if (papiAvailable) {
            format = PlaceholderAPI.setPlaceholders(event.getPlayer(), format);
        }

        event.setFormat(CC.translate(format));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String msg = ConfigUtil.getJoinMessage();

        if (papiAvailable) {
            msg = PlaceholderAPI.setPlaceholders(event.getPlayer(), msg);
        }

        event.setJoinMessage(CC.translate(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String msg = ConfigUtil.getQuitMessage();

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