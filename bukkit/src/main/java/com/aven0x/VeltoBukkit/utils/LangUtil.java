package com.aven0x.VeltoBukkit.utils;

import com.aven0x.VeltoBukkit.VeltoBukkit;
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
import java.util.Map;

public class LangUtil {
    private static FileConfiguration lang;

    public static void load() {
        File file = new File(VeltoBukkit.getInstance().getDataFolder(), "lang.yml");
        if (!file.exists()) {
            VeltoBukkit.getInstance().saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(file);
    }

    public static void send(Player player, String key) {
        send(player, key, null);
    }

    public static void send(Player player, String key, Map<String, String> placeholders) {
        if (lang == null) load();

        ConfigurationSection section = lang.getConfigurationSection(key);
        if (section == null) {
            player.sendMessage(ChatColor.RED + "Missing message: " + key);
            return;
        }

        String type = section.getString("type", "chat").toLowerCase();
        String rawMessage = section.getString("message", key);
        int duration = section.getInt("duration", 40); // ticks

        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                rawMessage = rawMessage.replace(e.getKey(), e.getValue());
            }
        }

        String colored = ChatColor.translateAlternateColorCodes('&', rawMessage);

        switch (type) {
            case "chat" -> player.sendMessage(colored);

            case "actionbar" -> sendActionBar(player, colored, duration);

            case "title" -> {
                String subtitleRaw = ChatColor.translateAlternateColorCodes(
                        '&', section.getString("subtitle", ""));
                if (placeholders != null) {
                    for (Map.Entry<String, String> e : placeholders.entrySet()) {
                        subtitleRaw = subtitleRaw.replace(e.getKey(), e.getValue());
                    }
                }
                // simple timing: 10t fade in/out, stay = duration
                player.sendTitle(colored, subtitleRaw, 10, Math.max(1, duration), 10);
            }

            case "bossbar" -> {
                String colorName = section.getString("color", "BLUE").toUpperCase();
                BarColor color;
                try {
                    color = BarColor.valueOf(colorName);
                } catch (IllegalArgumentException ex) {
                    color = BarColor.BLUE;
                }
                sendBossBar(player, colored, color, duration);
            }

            default -> player.sendMessage(ChatColor.RED + "Invalid notification type: " + type);
        }
    }

    public static void sendGlobal(String key) {
        sendGlobal(key, null);
    }

    public static void sendGlobal(String key, Map<String, String> placeholders) {
        if (lang == null) load();

        ConfigurationSection section = lang.getConfigurationSection(key);
        if (section == null) {
            Bukkit.getLogger().warning("Missing global notification: " + key);
            return;
        }

        String type = section.getString("type", "chat").toLowerCase();
        String rawMessage = section.getString("message", key);
        int duration = section.getInt("duration", 40);

        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                rawMessage = rawMessage.replace(e.getKey(), e.getValue());
            }
        }

        String colored = ChatColor.translateAlternateColorCodes('&', rawMessage);

        switch (type) {
            case "chat" -> Bukkit.broadcastMessage(colored);

            case "actionbar" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    sendActionBar(p, colored, duration);
                }
            }

            case "title" -> {
                String subtitleRaw = ChatColor.translateAlternateColorCodes(
                        '&', section.getString("subtitle", ""));
                if (placeholders != null) {
                    for (Map.Entry<String, String> e : placeholders.entrySet()) {
                        subtitleRaw = subtitleRaw.replace(e.getKey(), e.getValue());
                    }
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(colored, subtitleRaw, 10, Math.max(1, duration), 10);
                }
            }

            case "bossbar" -> {
                String colorName = section.getString("color", "BLUE").toUpperCase();
                BarColor color;
                try {
                    color = BarColor.valueOf(colorName);
                } catch (IllegalArgumentException ex) {
                    color = BarColor.BLUE;
                }
                // one bar for all players
                BossBar bar = Bukkit.createBossBar(colored, color, BarStyle.SOLID);
                bar.setProgress(1.0);
                bar.setVisible(true);
                for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);

                int finalDuration = duration;
                Bukkit.getScheduler().runTaskLater(VeltoBukkit.getInstance(), () -> {
                    bar.removeAll();
                    bar.setVisible(false);
                }, Math.max(1L, finalDuration));
            }

            default -> Bukkit.getLogger().warning("Invalid global notification type: " + type);
        }
    }

    public static void sendGlobalRaw(String rawMessage) {
        sendGlobalRaw(rawMessage, "chat", 40);
    }

    public static void sendGlobalRaw(String rawMessage, String type, int durationTicks) {
        String colored = ChatColor.translateAlternateColorCodes('&', rawMessage);

        switch (type.toLowerCase()) {
            case "chat" -> Bukkit.broadcastMessage(colored);

            case "actionbar" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    sendActionBar(p, colored, durationTicks);
                }
            }

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

                Bukkit.getScheduler().runTaskLater(VeltoBukkit.getInstance(), () -> {
                    bar.removeAll();
                    bar.setVisible(false);
                }, Math.max(1L, durationTicks));
            }

            default -> Bukkit.getLogger().warning("Invalid global raw notification type: " + type);
        }
    }

    // ===== Helpers =====

    private static void sendActionBar(Player player, String message, int durationTicks) {
        // Convert legacy string to BaseComponents and send to ACTION_BAR
        BaseComponent[] components = TextComponent.fromLegacyText(message);
        int interval = 20; // re-send every second to keep it visible
        int repetitions = Math.max(1, durationTicks / interval);

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= repetitions || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
            }
        }.runTaskTimer(VeltoBukkit.getInstance(), 0L, interval);
    }

    private static void sendBossBar(Player player, String title, BarColor color, int durationTicks) {
        BossBar bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bar.setProgress(1.0);
        bar.addPlayer(player);
        bar.setVisible(true);

        Bukkit.getScheduler().runTaskLater(VeltoBukkit.getInstance(), () -> {
            bar.removePlayer(player);
            bar.setVisible(false);
        }, Math.max(1L, durationTicks));
    }
}
