package com.aven0x.xcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : sender instanceof Player ? (Player) sender : null;
        boolean self = args.length == 0;

        if (!hasPermission(sender, self ? selfPerm : otherPerm)) return true;
        if (target == null || !target.isOnline()) {
            sendMessage(sender, "invalid-player");
            return true;
        }

        target.setGameMode(mode);
        sendMessage(sender, "gamemode-set");
        return true;
    }
}
