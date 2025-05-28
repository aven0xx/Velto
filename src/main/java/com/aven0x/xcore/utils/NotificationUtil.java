package com.aven0x.xcore.utils;

import com.aven0x.xcore.Xcore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
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

    public static void send(Player player, String key) {
        send(player, key, null);
    }

    public static void send(Player player, String key, Map<String, String> placeholders) {
        if (lang == null) load();

        ConfigurationSection section = lang.getConfigurationSection(key);
        if (section == null) {
            player.sendMessage("§cMissing message: " + key);
            return;
        }

        String type = section.getString("type", "chat").toLowerCase();
        String rawMessage = section.getString("message", key);
        int duration = section.getInt("duration", 40); // 2s default

        // Placeholder replacement
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rawMessage = rawMessage.replace(entry.getKey(), entry.getValue());
            }
        }

        // Convert & → § for legacy color code support
        String colored = rawMessage.replace('&', '§');
        Component component = LegacyComponentSerializer.legacySection().deserialize(colored);

        switch (type) {
            case "chat" -> player.sendMessage(component);

            case "actionbar" -> sendActionBar(player, component, duration);

            case "title" -> player.showTitle(Title.title(component, Component.empty()));

            case "bossbar" -> {
                BossBar bar = BossBar.bossBar(component, 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
                player.showBossBar(bar);
                Bukkit.getScheduler().runTaskLater(Xcore.getInstance(), () -> player.hideBossBar(bar), duration);
            }

            default -> player.sendMessage("§cInvalid notification type: " + type);
        }
    }

    private static void sendActionBar(Player player, Component message, int durationTicks) {
        int interval = 20;
        int repetitions = Math.max(1, durationTicks / interval);

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (!player.isOnline() || count++ >= repetitions) {
                    cancel();
                    return;
                }
                player.sendActionBar(message);
            }
        }.runTaskTimer(Xcore.getInstance(), 0L, interval);
    }
}
