package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class GamemodeCommand extends BaseCommand {
    public GamemodeCommand() {
        super("gamemode");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("gmc")) {
            return handleShortcut(sender, args, GameMode.CREATIVE, "xcore.gamemode.c", "xcore.gamemode.c.others");
        } else if (label.equalsIgnoreCase("gms")) {
            return handleShortcut(sender, args, GameMode.SURVIVAL, "xcore.gamemode.s", "xcore.gamemode.s.others");
        } else if (label.equalsIgnoreCase("gma")) {
            return handleShortcut(sender, args, GameMode.ADVENTURE, "xcore.gamemode.a", "xcore.gamemode.a.others");
        } else if (label.equalsIgnoreCase("gmsp")) {
            return handleShortcut(sender, args, GameMode.SPECTATOR, "xcore.gamemode.sp", "xcore.gamemode.sp.others");
        }

        if (args.length < 1) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "gamemode-usage");
            }
            return true;
        }

        GameMode gm = switch (args[0].toLowerCase()) {
            case "0", "survival" -> GameMode.SURVIVAL;
            case "1", "creative" -> GameMode.CREATIVE;
            case "2", "adventure" -> GameMode.ADVENTURE;
            case "3", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };

        if (gm == null) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-gamemode");
            }
            return true;
        }

        Player target = args.length > 1
                ? Bukkit.getPlayer(args[1])
                : sender instanceof Player ? (Player) sender : null;

        boolean self = args.length == 1;
        String perm = "xcore.gamemode" + (self ? "" : ".others");

        if (!hasPermission(sender, perm)) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-player");
            }
            return true;
        }

        target.setGameMode(gm);
        if (sender instanceof Player player) {
            NotificationUtil.send(player, "gamemode-set");
        }

        return true;
    }

    private boolean handleShortcut(CommandSender sender, String[] args, GameMode mode, String selfPerm, String otherPerm) {
        Player target = args.length > 0
                ? Bukkit.getPlayer(args[0])
                : sender instanceof Player ? (Player) sender : null;

        boolean self = args.length == 0;

        String permission = self ? selfPerm : otherPerm;
        if (!hasPermission(sender, permission)) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-player");
            }
            return true;
        }

        target.setGameMode(mode);

        if (sender instanceof Player playerSender) {
            if (self) {
                NotificationUtil.send(playerSender, "gamemode-set");
            } else {
                NotificationUtil.send(playerSender, "gamemode-set", Map.of("%target%", target.getName()));
            }
        }

        return true;
    }
}
