package com.aven0x.Velto.utils;

import com.aven0x.Velto.VeltoPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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
        // Click / hover (chat type only)
        final ClickEvent.Action clickAction;  // null = no click event
        final String rawClickValue;           // may contain % placeholders
        final String coloredClickValue;       // pre-resolved if no placeholders, else null
        final String rawHover;               // null = no hover tooltip
        final String coloredHover;           // pre-translated if no placeholders, else null

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

            // Parse optional click event
            ConfigurationSection clickSec = sec.getConfigurationSection("click");
            if (clickSec != null) {
                clickAction = parseClickAction(clickSec.getString("action", ""));
                rawClickValue = clickSec.getString("value", "");
                coloredClickValue = rawClickValue.contains("%") ? null : rawClickValue;
            } else {
                clickAction = null;
                rawClickValue = null;
                coloredClickValue = null;
            }

            // Parse optional hover tooltip
            String hover = sec.getString("hover", null);
            rawHover = hover;
            coloredHover = (hover != null && !hover.contains("%"))
                    ? ChatColor.translateAlternateColorCodes('&', hover)
                    : null;
        }

        private static ClickEvent.Action parseClickAction(String raw) {
            return switch (raw.toLowerCase()) {
                case "run_command"      -> ClickEvent.Action.RUN_COMMAND;
                case "suggest_command"  -> ClickEvent.Action.SUGGEST_COMMAND;
                case "open_url"         -> ClickEvent.Action.OPEN_URL;
                case "copy_to_clipboard"-> ClickEvent.Action.COPY_TO_CLIPBOARD;
                default                 -> null;
            };
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
            case "chat" -> {
                if (msg.clickAction != null || msg.rawHover != null) {
                    player.spigot().sendMessage(buildInteractive(msg, colored, placeholders));
                } else {
                    player.sendMessage(colored);
                }
            }

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
            case "chat" -> {
                if (msg.clickAction != null || msg.rawHover != null) {
                    BaseComponent[] components = buildInteractive(msg, colored, placeholders);
                    for (Player p : Bukkit.getOnlinePlayers()) p.spigot().sendMessage(components);
                } else {
                    Bukkit.broadcastMessage(colored);
                }
            }

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

    // Builds a component array with click and/or hover events applied.
    private static BaseComponent[] buildInteractive(ParsedMessage msg, String colored, Map<String, String> placeholders) {
        BaseComponent[] components = TextComponent.fromLegacyText(colored);

        ClickEvent click = null;
        if (msg.clickAction != null) {
            String value = (placeholders != null && !placeholders.isEmpty() && msg.rawClickValue != null)
                    ? applyPlaceholders(msg.rawClickValue, placeholders)
                    : (msg.coloredClickValue != null ? msg.coloredClickValue : msg.rawClickValue);
            if (value != null) click = new ClickEvent(msg.clickAction, value);
        }

        HoverEvent hover = null;
        if (msg.rawHover != null) {
            String hoverColored = (placeholders != null && !placeholders.isEmpty())
                    ? ChatColor.translateAlternateColorCodes('&', applyPlaceholders(msg.rawHover, placeholders))
                    : (msg.coloredHover != null ? msg.coloredHover
                            : ChatColor.translateAlternateColorCodes('&', msg.rawHover));
            hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(hoverColored)));
        }

        for (BaseComponent c : components) {
            if (click != null) c.setClickEvent(click);
            if (hover != null) c.setHoverEvent(hover);
        }
        return components;
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
