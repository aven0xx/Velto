package com.aven0x.xcore.utils;

import com.aven0x.xcore.Xcore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;
import java.util.Map;

public class NotificationUtil {

    private static FileConfiguration lang;

    public static void load() {
        File file = new File(Xcore.getInstance().getDataFolder(), "lang.yml");
        if (!file.exists()) {
            Xcore.getInstance().saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(file);
    }

    public static String get(String key) {
        if (lang == null) load();
        return lang.getString(key, key);
    }

    public static void send(Player player, String key) {
        send(player, key, null);
    }

    public static void send(Player player, String key, Map<String, String> placeholders) {
        String message = get(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        show(player, message);
    }

    public static void send(CommandSender sender, String key) {
        send(sender, key, null);
    }

    public static void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = get(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        if (sender instanceof Player player) {
            show(player, message);
        } else {
            sender.sendMessage(message);
        }
    }

    private static void show(Player player, String message) {
        String type = get("notification-type").toLowerCase();
        Component component = MiniMessage.miniMessage().deserialize(message);

        switch (type) {
            case "chat" -> player.sendMessage(component);
            case "actionbar" -> Xcore.getInstance().adventure().player(player).sendActionBar(component);
            case "title" -> {
                Title title = Title.title(component, Component.empty(), Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500)));
                Xcore.getInstance().adventure().player(player).showTitle(title);
            }
            case "bossbar" -> {
                BossBar bar = BossBar.bossBar(component, 1.0f, Color.BLUE, Overlay.NOTCHED_10);
                Xcore.getInstance().adventure().player(player).showBossBar(bar);
                Bukkit.getScheduler().runTaskLater(Xcore.getInstance(), () ->
                        Xcore.getInstance().adventure().player(player).hideBossBar(bar), 60L);
            }
            default -> player.sendMessage("Invalid notification type: " + type);
        }
    }
}
