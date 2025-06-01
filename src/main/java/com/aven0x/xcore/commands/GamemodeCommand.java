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
        if (args.length < 1) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-usage");
            }
            return true;
        }

        GameMode mode = parseGameMode(args[0]);
        if (mode == null) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-gamemode");
            }
            return true;
        }

        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
        boolean self = args.length == 1 || sender.equals(target);

        String permission = self ? "xcore.gamemode" : "xcore.gamemode.others";
        if (!hasPermission(sender, permission)) return true;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-player");
            }
            return true;
        }

        target.setGameMode(mode);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target%", target.getName());

        if (sender instanceof Player player) {
            NotificationUtil.send(player, self ? "gamemode-set-self" : "gamemode-set-other", placeholders);
        }

        // Always log to console
        Bukkit.getLogger().info(sender.getName() + " set gamemode of " + target.getName() + " to " + mode.name());

        return true;
    }

    private GameMode parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "0", "survival", "gms" -> GameMode.SURVIVAL;
            case "1", "creative", "gmc" -> GameMode.CREATIVE;
            case "2", "adventure", "gma" -> GameMode.ADVENTURE;
            case "3", "spectator", "gmsp" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
