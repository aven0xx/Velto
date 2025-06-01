package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GamemodeCommand extends BaseCommand {
    public GamemodeCommand() {
        super("gamemode");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String modeArg = args.length > 0 ? args[0] : null;
        String targetArg = args.length > 1 ? args[1] : null;

        GameMode mode = parseGameMode(label, modeArg);
        if (mode == null) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-gamemode");
            }
            return true;
        }

        Player target = targetArg != null ? Bukkit.getPlayer(targetArg)
                : sender instanceof Player ? (Player) sender : null;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-player");
            } else {
                sender.sendMessage("Player not found.");
            }
            return true;
        }

        boolean self = sender.equals(target);

        String permission = self ? "xcore.gamemode" : "xcore.gamemode.others";
        if (!hasPermission(sender, permission)) return true;

        target.setGameMode(mode);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target%", target.getName());
        placeholders.put("%gamemode%", mode.name());

        if (sender instanceof Player player) {
            NotificationUtil.send(player, self ? "gamemode-set-self" : "gamemode-set-other", placeholders);
        }

        if (!self && target.isOnline()) {
            NotificationUtil.send(target, "gamemode-updated-by-other", placeholders);
        }

        Bukkit.getLogger().info(sender.getName() + " set gamemode of " + target.getName() + " to " + mode.name());
        return true;
    }

    private GameMode parseGameMode(String label, String input) {
        String source = label.toLowerCase();
        if (source.equals("gms")) return GameMode.SURVIVAL;
        if (source.equals("gmc")) return GameMode.CREATIVE;
        if (source.equals("gma")) return GameMode.ADVENTURE;
        if (source.equals("gmsp")) return GameMode.SPECTATOR;

        if (input == null) return null;

        return switch (input.toLowerCase()) {
            case "0", "survival" -> GameMode.SURVIVAL;
            case "1", "creative" -> GameMode.CREATIVE;
            case "2", "adventure" -> GameMode.ADVENTURE;
            case "3", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}