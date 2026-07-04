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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LangUtil {

    private static FileConfiguration lang;
    private static final Map<String, ParsedMessage> cache = new HashMap<>();

    // One independently-styled, independently-clickable span of text.
    private static final class ParsedSegment {
        final String rawText;
        final String coloredText;        // null when rawText contains %
        final ClickEvent.Action clickAction;
        final String rawClickValue;
        final String coloredClickValue;  // null when rawClickValue contains %
        final String rawHover;
        final String coloredHover;       // null when rawHover contains %
        final BaseComponent[] prebuiltComponents; // non-null when fully static

        ParsedSegment(Map<?, ?> map) {
            Object textObj = map.get("text");
            rawText = textObj != null ? textObj.toString() : "";
            coloredText = rawText.contains("%") ? null
                    : ChatColor.translateAlternateColorCodes('&', rawText);

            Object clickObj = map.get("click");
            if (clickObj instanceof Map<?, ?> clickMap) {
                Object actionObj = clickMap.get("action");
                clickAction = parseClickAction(actionObj != null ? actionObj.toString() : "");
                Object valueObj = clickMap.get("value");
                rawClickValue = valueObj != null ? valueObj.toString() : "";
                coloredClickValue = rawClickValue.contains("%") ? null : rawClickValue;
            } else {
                clickAction = null;
                rawClickValue = null;
                coloredClickValue = null;
            }

            Object hoverObj = map.get("hover");
            rawHover = hoverObj instanceof String s ? s : null;
            coloredHover = (rawHover != null && !rawHover.contains("%"))
                    ? ChatColor.translateAlternateColorCodes('&', rawHover)
                    : null;

            boolean fullyStatic = coloredText != null
                    && (rawClickValue == null || coloredClickValue != null)
                    && (rawHover == null || coloredHover != null);
            prebuiltComponents = fullyStatic ? buildSegmentComponents(this, coloredText, null) : null;
        }
    }

    private static final class ParsedMessage {
        final String type;
        final String rawMessage;
        final String coloredMessage;
        final int duration;
        final String rawSubtitle;
        final String coloredSubtitle;
        final BarColor barColor;
        final BaseComponent[] prebuiltComponents;
        // Click / hover (legacy single-message format only)
        final ClickEvent.Action clickAction;
        final String rawClickValue;
        final String coloredClickValue;
        final String rawHover;
        final String coloredHover;
        // Segmented format — null means use the legacy fields above
        final List<ParsedSegment> segments;

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

            String hover = sec.getString("hover", null);
            rawHover = hover;
            coloredHover = (hover != null && !hover.contains("%"))
                    ? ChatColor.translateAlternateColorCodes('&', hover)
                    : null;

            List<?> segList = sec.getList("segments");
            if (segList != null && !segList.isEmpty()) {
                List<ParsedSegment> segs = new ArrayList<>(segList.size());
                for (Object item : segList) {
                    if (item instanceof Map<?, ?> segMap) segs.add(new ParsedSegment(segMap));
                }
                segments = segs.isEmpty() ? null : segs;
            } else {
                segments = null;
            }
        }
    }

    // ===== Load =====

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
                if (msg.segments != null) {
                    player.spigot().sendMessage(buildSegmentedComponents(msg, placeholders));
                } else if (msg.clickAction != null || msg.rawHover != null) {
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
                if (msg.segments != null) {
                    BaseComponent[] components = buildSegmentedComponents(msg, placeholders);
                    for (Player p : Bukkit.getOnlinePlayers()) p.spigot().sendMessage(components);
                } else if (msg.clickAction != null || msg.rawHover != null) {
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

    private static ClickEvent.Action parseClickAction(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase()) {
            case "run_command"       -> ClickEvent.Action.RUN_COMMAND;
            case "suggest_command"   -> ClickEvent.Action.SUGGEST_COMMAND;
            case "open_url"          -> ClickEvent.Action.OPEN_URL;
            case "copy_to_clipboard" -> ClickEvent.Action.COPY_TO_CLIPBOARD;
            default                  -> null;
        };
    }

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

    // Assembles components from all segments, reusing pre-built arrays when static.
    private static BaseComponent[] buildSegmentedComponents(ParsedMessage msg, Map<String, String> placeholders) {
        boolean hasPlaceholders = placeholders != null && !placeholders.isEmpty();
        List<BaseComponent> all = new ArrayList<>();
        for (ParsedSegment seg : msg.segments) {
            if (!hasPlaceholders && seg.prebuiltComponents != null) {
                all.addAll(Arrays.asList(seg.prebuiltComponents));
            } else {
                String text = hasPlaceholders
                        ? ChatColor.translateAlternateColorCodes('&', applyPlaceholders(seg.rawText, placeholders))
                        : (seg.coloredText != null ? seg.coloredText : ChatColor.translateAlternateColorCodes('&', seg.rawText));
                all.addAll(Arrays.asList(buildSegmentComponents(seg, text, hasPlaceholders ? placeholders : null)));
            }
        }
        return all.toArray(new BaseComponent[0]);
    }

    // Builds components for one segment with its own click/hover events applied.
    private static BaseComponent[] buildSegmentComponents(ParsedSegment seg, String colored, Map<String, String> placeholders) {
        BaseComponent[] comps = TextComponent.fromLegacyText(colored);

        ClickEvent click = null;
        if (seg.clickAction != null) {
            String value = (placeholders != null && !placeholders.isEmpty() && seg.rawClickValue != null)
                    ? applyPlaceholders(seg.rawClickValue, placeholders)
                    : (seg.coloredClickValue != null ? seg.coloredClickValue : seg.rawClickValue);
            if (value != null) click = new ClickEvent(seg.clickAction, value);
        }

        HoverEvent hover = null;
        if (seg.rawHover != null) {
            String hoverColored = (placeholders != null && !placeholders.isEmpty())
                    ? ChatColor.translateAlternateColorCodes('&', applyPlaceholders(seg.rawHover, placeholders))
                    : (seg.coloredHover != null ? seg.coloredHover : ChatColor.translateAlternateColorCodes('&', seg.rawHover));
            hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(hoverColored)));
        }

        for (BaseComponent c : comps) {
            if (click != null) c.setClickEvent(click);
            if (hover != null) c.setHoverEvent(hover);
        }
        return comps;
    }

    // Builds a component array with click/hover applied to every component (legacy whole-message format).
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
