package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AlertCommand extends BaseCommand {

    // The notification types LangUtil.sendGlobalRaw understands — single source of truth for
    // both validation and tab-completion.
    private static final List<String> TYPES = List.of("chat", "actionbar", "bossbar", "title");

    // Suggested durations (in ticks; 20 ticks = 1 second) — just hints, any positive int works.
    private static final List<String> COMMON_DURATIONS = List.of("40", "60", "100", "200");

    public AlertCommand() {
        super("alert");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.alert");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {

        if (!hasPermission(sender, "velto.alert")) {
            return true;
        }

        // /alert <type> <ticks> <message...>
        if (args.length < 3) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "alert-usage");
            }
            return true;
        }

        String type = args[0].toLowerCase();

        if (!isValidType(type)) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "alert-invalid-type");
            }
            return true;
        }

        int ticks;
        try {
            ticks = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "alert-invalid-duration");
            }
            return true;
        }

        if (ticks < 1) ticks = 1;

        String rawMessage = joinFrom(args, 2);

        LangUtil.sendGlobalRaw(rawMessage, type, ticks);

        Bukkit.getLogger().info("[ALERT] " + rawMessage);

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        // /alert <type> <ticks> <message...>
        // First arg (type): use <= 1 with the empty-token fallback, matching the other
        // first-argument completers (FeedCommand/MsgCommand/BalanceCommand) so TAB on a bare
        // "/alert " still lists the types regardless of how the platform splits a trailing space.
        if (args.length <= 1) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            return TYPES.stream().filter(t -> t.startsWith(typed)).toList();
        }
        // Second arg (ticks): == 2 matches KitCommand's second-argument completion.
        if (args.length == 2) {
            String typed = args[1];
            return COMMON_DURATIONS.stream().filter(d -> d.startsWith(typed)).toList();
        }
        // Third argument onward is the free-text message — nothing to suggest.
        return List.of();
    }

    private static boolean isValidType(String type) {
        return TYPES.contains(type);
    }

    private static String joinFrom(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
