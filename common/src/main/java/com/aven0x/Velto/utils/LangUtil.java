package com.aven0x.Velto.utils;

import com.aven0x.Velto.VeltoPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LangUtil {

    private static FileConfiguration lang;
    private static final Map<String, ParsedMessage> cache = new HashMap<>();

    private static final class ParsedMessage {
        final String type;
        final String rawMessage;
        final String coloredMessage;    // pre-translated; null if rawMessage contains placeholders
        final int duration;
        final String rawSubtitle;
        final String coloredSubtitle;   // pre-translated; null if rawSubtitle contains placeholders
        final BarColor barColor;
        final BaseComponent[] prebuiltComponents; // pre-built for static actionbar messages

        ParsedMessage(ConfigurationSection sec) {
            type = sec.getString("type", "chat").toLowerCase();
            rawMessage = sec.getString("message", sec.getName());
            duration = sec.getInt("duration", 40);
            rawSubtitle = sec.getString("subtitle", "");

            coloredMessage = rawMessage.contains("%") ? null
                    : ChatColor.translateAlternateColorCodes('&', rawMessage);

            coloredSubtitle = rawSubtitle.contains("%") ? null
                    : ChatColor.translateAlternateColorCodes('&', rawSubtitle);

            String colorName = sec.getString("color", "BLUE").toUpperCase();
            BarColor resolved = BarColor.BLUE;
            try { resolved = BarColor.valueOf(colorName); } catch (IllegalArgumentException ignored) {}
            barColor = resolved;

            prebuiltComponents = (coloredMessage != null && "actionbar".equals(type))
                    ? TextComponent.fromLegacyText(coloredMessage)
                    : null;
        }
    }

    public static void load() {
        File file = new File(VeltoPlugin.get().getDataFolder(), "lang.yml");
        if (!file.exists()) {
            VeltoPlugin.get().saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(file);
        buildCache();
    }

    private static void buildCache() {
        cache.clear();
        for (String key : lang.getKeys(false)) {
            ConfigurationSection sec = lang.getConfigurationSection(key);
            if (sec != null) cache.put(key, new ParsedMessage(sec));
        }
    }

    // ===== Per-player send =====

    public static void send(Player player, String key) {
        send(player, key, null);
    }

    public static void send(Player player, String key, Map<String, String> placeholders) {
        if (lang == null) load();

        ParsedMessage msg = cache.get(key);
        if (msg == null) {
            player.sendMessage(ChatColor.RED + "Missing message: " + key);
            return;
        }

        String colored = resolveColored(msg.rawMessage, msg.coloredMessage, placeholders);

        switch (msg.type) {
            case "chat" -> player.sendMessage(colored);

            case "actionbar" -> {
                BaseComponent[] components = resolveComponents(msg, colored, placeholders);
                sendActionBar(player, components, msg.duration);
            }

            case "title" -> {
                String subtitle = resolveColored(msg.rawSubtitle, msg.coloredSubtitle, placeholders);
                player.sendTitle(colored, subtitle, 10, Math.max(1, msg.duration), 10);
            }

            case "bossbar" -> sendBossBar(player, colored, msg.barColor, msg.duration);

            default -> player.sendMessage(ChatColor.RED + "Invalid notification type: " + msg.type);
        }
    }

    // ===== Global send =====

    public static void sendGlobal(String key) {
        sendGlobal(key, null);
    }

    public static void sendGlobal(String key, Map<String, String> placeholders) {
        if (lang == null) load();

        ParsedMessage msg = cache.get(key);
        if (msg == null) {
            Bukkit.getLogger().warning("Missing global notification: " + key);
            return;
        }

        String colored = resolveColored(msg.rawMessage, msg.coloredMessage, placeholders);

        switch (msg.type) {
            case "chat" -> Bukkit.broadcastMessage(colored);

            case "actionbar" -> {
                BaseComponent[] components = resolveComponents(msg, colored, placeholders);
                sendGlobalActionBar(components, msg.duration);
            }

            case "title" -> {
                String subtitle = resolveColored(msg.rawSubtitle, msg.coloredSubtitle, placeholders);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(colored, subtitle, 10, Math.max(1, msg.duration), 10);
                }
            }

            case "bossbar" -> {
                BossBar bar = Bukkit.createBossBar(colored, msg.barColor, BarStyle.SOLID);
                bar.setProgress(1.0);
                bar.setVisible(true);
                for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);
                int dur = msg.duration;
                Bukkit.getScheduler().runTaskLater(VeltoPlugin.get(), () -> {
                    bar.removeAll();
                    bar.setVisible(false);
                }, Math.max(1L, dur));
            }

            default -> Bukkit.getLogger().warning("Invalid global notification type: " + msg.type);
        }
    }

    // ===== Raw global send =====

    public static void sendGlobalRaw(String rawMessage) {
        sendGlobalRaw(rawMessage, "chat", 40);
    }

    public static void sendGlobalRaw(String rawMessage, String type, int durationTicks) {
        String colored = ChatColor.translateAlternateColorCodes('&', rawMessage);

        switch (type.toLowerCase()) {
            case "chat" -> Bukkit.broadcastMessage(colored);

            case "actionbar" -> sendGlobalActionBar(TextComponent.fromLegacyText(colored), durationTicks);

            case "title" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(colored, "", 10, Math.max(1, durationTicks), 10);
                }
            }

            case "bossbar" -> {
                BossBar bar = Bukkit.createBossBar(colored, BarColor.BLUE, BarStyle.SOLID);
                bar.setProgress(1.0);
                bar.setVisible(true);
                for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);
                Bukkit.getScheduler().runTaskLater(VeltoPlugin.get(), () -> {
                    bar.removeAll();
                    bar.setVisible(false);
                }, Math.max(1L, durationTicks));
            }

            default -> Bukkit.getLogger().warning("Invalid global raw notification type: " + type);
        }
    }

    // ===== Helpers =====

    private static String resolveColored(String raw, String preColored, Map<String, String> placeholders) {
        if (placeholders != null && !placeholders.isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&', applyPlaceholders(raw, placeholders));
        }
        return preColored != null ? preColored : ChatColor.translateAlternateColorCodes('&', raw);
    }

    private static BaseComponent[] resolveComponents(ParsedMessage msg, String colored, Map<String, String> placeholders) {
        boolean isStatic = (placeholders == null || placeholders.isEmpty()) && msg.prebuiltComponents != null;
        return isStatic ? msg.prebuiltComponents : TextComponent.fromLegacyText(colored);
    }

    private static String applyPlaceholders(String raw, Map<String, String> placeholders) {
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace(e.getKey(), e.getValue());
        }
        return raw;
    }

    private static void sendActionBar(Player player, BaseComponent[] components, int durationTicks) {
        int repetitions = Math.max(1, durationTicks / 20);
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= repetitions || !player.isOnline()) { cancel(); return; }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
            }
        }.runTaskTimer(VeltoPlugin.get(), 0L, 20L);
    }

    private static void sendGlobalActionBar(BaseComponent[] components, int durationTicks) {
        int repetitions = Math.max(1, durationTicks / 20);
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= repetitions) { cancel(); return; }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
                }
            }
        }.runTaskTimer(VeltoPlugin.get(), 0L, 20L);
    }

    private static void sendBossBar(Player player, String title, BarColor color, int durationTicks) {
        BossBar bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bar.setProgress(1.0);
        bar.addPlayer(player);
        bar.setVisible(true);
        Bukkit.getScheduler().runTaskLater(VeltoPlugin.get(), () -> {
            bar.removePlayer(player);
            bar.setVisible(false);
        }, Math.max(1L, durationTicks));
    }
}
