package com.aven0x.Velto.managers;

import com.aven0x.Velto.utils.ConfigUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatFormatCache {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final Map<UUID, String> cachedFormats = new ConcurrentHashMap<>();

    private ChatFormatCache() {}

    public static String get(Player player) {
        return cachedFormats.computeIfAbsent(player.getUniqueId(), ignored -> buildFallbackFormat(player));
    }

    public static void refresh(Player player) {
        cachedFormats.put(player.getUniqueId(), buildFormat(player));
    }

    public static void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public static void remove(UUID uuid) {
        cachedFormats.remove(uuid);
    }

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
        return input.stream().map(ChatFormatCache::translate).toList();
    }

    private static String buildFormat(Player player) {
        String format = resolveChatFormat(player);
        format = format.replace("%player_name%", player.getName());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String resolved = PlaceholderAPI.setPlaceholders(player, format);
            if (resolved != null) format = resolved;
        }

        return translate(format);
    }

    private static String buildFallbackFormat(Player player) {
        return translate(ConfigUtil.getChatFormat()).replace("%player_name%", player.getName());
    }

    private static String resolveChatFormat(Player player) {
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
}
