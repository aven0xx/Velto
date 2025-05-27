package com.aven0x.xcore.utils;

import com.aven0x.xcore.Xcore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;

public class NotificationUtil {

    private static YamlConfiguration config;

    public static void load() {
        File file = new File(Xcore.getInstance().getDataFolder(), "lang.yml");
        if (!file.exists()) {
            Xcore.getInstance().saveResource("lang.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static void send(Player player, String key) {
        send(player, key, null);
    }

    public static void send(Player player, String key, String placeholderValue) {
        if (config == null) load();
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null) {
            player.sendMessage("Missing message: " + key);
            return;
        }

        String type = section.getString("type", "chat").toLowerCase();

        switch (type) {
            case "chat" -> {
                String message = replacePlaceholders(section.getString("message", key), placeholderValue);
                player.sendMessage(MessageUtil.color(message));
            }
            case "actionbar" -> {
                String message = replacePlaceholders(section.getString("message", key), placeholderValue);
                player.sendActionBar(MessageUtil.color(message));
            }
            case "title" -> {
                String title = replacePlaceholders(section.getString("title", ""), placeholderValue);
                String subtitle = replacePlaceholders(section.getString("subtitle", ""), placeholderValue);
                int fadeIn = section.getInt("fadein", 10);
                int stay = section.getInt("stay", 40);
                int fadeOut = section.getInt("fadeout", 10);
                player.sendTitle(
                        MessageUtil.color(title),
                        MessageUtil.color(subtitle),
                        fadeIn, stay, fadeOut
                );
            }
            case "bossbar" -> {
                String message = replacePlaceholders(section.getString("message", key), placeholderValue);
                BossBar bar = BossBar.bossBar(
                        Component.text(MessageUtil.color(message), NamedTextColor.GREEN),
                        1.0f, Color.GREEN, Overlay.PROGRESS
                );
                Xcore.getInstance().adventure().player(player).showBossBar(bar);
                Bukkit.getScheduler().runTaskLater(Xcore.getInstance(), () ->
                        Xcore.getInstance().adventure().player(player).hideBossBar(bar), 60L);
            }
            default -> player.sendMessage("Invalid notification type: " + type);
        }
    }

    private static String replacePlaceholders(String message, String target) {
        if (message == null) return "";
        return message.replace("%target%", target != null ? target : "");
    }
}
