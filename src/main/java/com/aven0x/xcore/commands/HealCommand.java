package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class HealCommand extends BaseCommand {
    public HealCommand() {
        super("heal");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player ? (Player) sender : null);
        boolean self = args.length == 0;

        String perm = self ? "xcore.heal" : "xcore.heal.others";
        if (!hasPermission(sender, perm)) return true;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "invalid-player");
            } else {
                sender.sendMessage("§cInvalid player.");
            }
            return true;
        }

        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);

        if (self) {
            NotificationUtil.send(target, "healed-self");
        } else {
            if (sender instanceof Player playerSender) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%target%", target.getName());
                NotificationUtil.send(playerSender, "healed-other", placeholders);
            } else {
                sender.sendMessage("§aHealed: " + target.getName());
            }
            NotificationUtil.send(target, "healed-self");
        }

        return true;
    }
}