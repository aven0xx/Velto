package com.aven0x.VeltoPaper.utils;

import com.aven0x.VeltoPaper.VeltoPaper;
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
        File file = new File(VeltoPaper.getInstance().getDataFolder(), "lang.yml");
        if (!file.exists()) {
            VeltoPaper.getInstance().saveResource("lang.yml", false);
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

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rawMessage = rawMessage.replace(entry.getKey(), entry.getValue());
            }
        }

        String colored = rawMessage.replace('&', '§');
        Component component = LegacyComponentSerializer.legacySection().deserialize(colored);

        var audience = VeltoPaper.getInstance().adventure().player(player);

        switch (type) {
            case "chat" -> audience.sendMessage(component);

            case "actionbar" -> sendActionBar(audience, component, duration);

            case "title" -> {
                String subtitleRaw = section.getString("subtitle", "");
                if (placeholders != null) {
                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                        subtitleRaw = subtitleRaw.replace(entry.getKey(), entry.getValue());
                    }
                }
                Component subtitle = LegacyComponentSerializer.legacySection().deserialize(subtitleRaw.replace('&', '§'));
                audience.showTitle(Title.title(component, subtitle));
            }

            case "bossbar" -> {
                String colorName = section.getString("color", "blue").toUpperCase();
                BossBar.Color color;
                try {
                    color = BossBar.Color.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    color = BossBar.Color.BLUE;
                }

                BossBar bar = BossBar.bossBar(component, 1f, color, BossBar.Overlay.PROGRESS);
                audience.showBossBar(bar);
                Bukkit.getScheduler().runTaskLater(VeltoPaper.getInstance(), () -> audience.hideBossBar(bar), duration);
            }

            default -> player.sendMessage("§cInvalid notification type: " + type);
        }
    }

    public static void sendGlobal(String key) {
        sendGlobal(key, null);
    }

    public static void sendGlobal(String key, Map<String, String> placeholders) {
        if (lang == null) load();

        ConfigurationSection section = lang.getConfigurationSection(key);
        if (section == null) {
            Bukkit.getLogger().warning("§cMissing global notification: " + key);
            return;
        }

        String type = section.getString("type", "chat").toLowerCase();
        String rawMessage = section.getString("message", key);
        int duration = section.getInt("duration", 40);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rawMessage = rawMessage.replace(entry.getKey(), entry.getValue());
            }
        }

        Component component = LegacyComponentSerializer.legacySection().deserialize(rawMessage.replace('&', '§'));
        var audience = VeltoPaper.getInstance().adventure().all();

        switch (type) {
            case "chat" -> audience.sendMessage(component);

            case "actionbar" -> sendActionBar(audience, component, duration);

            case "title" -> {
                String subtitleRaw = section.getString("subtitle", "");
                if (placeholders != null) {
                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                        subtitleRaw = subtitleRaw.replace(entry.getKey(), entry.getValue());
                    }
                }
                Component subtitle = LegacyComponentSerializer.legacySection().deserialize(subtitleRaw.replace('&', '§'));
                audience.showTitle(Title.title(component, subtitle));
            }

            case "bossbar" -> {
                String colorName = section.getString("color", "blue").toUpperCase();
                BossBar.Color color;
                try {
                    color = BossBar.Color.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    color = BossBar.Color.BLUE;
                }

                BossBar bar = BossBar.bossBar(component, 1f, color, BossBar.Overlay.PROGRESS);
                audience.showBossBar(bar);
                Bukkit.getScheduler().runTaskLater(VeltoPaper.getInstance(), () -> audience.hideBossBar(bar), duration);
            }

            default -> Bukkit.getLogger().warning("§cInvalid global notification type: " + type);
        }
    }

    public static void sendGlobalRaw(String rawMessage) {
        sendGlobalRaw(rawMessage, "chat", 40);
    }

    public static void sendGlobalRaw(String rawMessage, String type, int durationTicks) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(rawMessage.replace('&', '§'));
        var audience = VeltoPaper.getInstance().adventure().all();

        switch (type.toLowerCase()) {
            case "chat" -> audience.sendMessage(component);

            case "actionbar" -> sendActionBar(audience, component, durationTicks);

            case "title" -> audience.showTitle(Title.title(component, Component.empty()));

            case "bossbar" -> {
                BossBar bar = BossBar.bossBar(component, 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
                audience.showBossBar(bar);
                Bukkit.getScheduler().runTaskLater(VeltoPaper.getInstance(), () -> audience.hideBossBar(bar), durationTicks);
            }

            default -> Bukkit.getLogger().warning("§cInvalid global raw notification type: " + type);
        }
    }

    private static void sendActionBar(net.kyori.adventure.audience.Audience audience, Component message, int durationTicks) {
        int interval = 20;
        int repetitions = Math.max(1, durationTicks / interval);

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count++ >= repetitions) {
                    cancel();
                    return;
                }
                audience.sendActionBar(message);
            }
        }.runTaskTimer(VeltoPaper.getInstance(), 0L, interval);
    }
}
