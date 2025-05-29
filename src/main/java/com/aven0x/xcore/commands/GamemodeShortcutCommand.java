package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public abstract class GamemodeShortcutCommand extends BaseCommand {
    private final GameMode mode;
    private final String selfPerm;
    private final String otherPerm;

    public GamemodeShortcutCommand(String name, GameMode mode, String selfPerm, String otherPerm) {
        super(name);
        this.mode = mode;
        this.selfPerm = selfPerm;
        this.otherPerm = otherPerm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
