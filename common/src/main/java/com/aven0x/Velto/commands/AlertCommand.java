package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AlertCommand extends BaseCommand {

    public AlertCommand() {
        super("alert");
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

    private static boolean isValidType(String type) {
        return switch (type) {
            case "chat", "actionbar", "bossbar", "title" -> true;
            default -> false;
        };
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
