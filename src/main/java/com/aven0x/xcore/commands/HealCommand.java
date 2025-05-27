package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand extends BaseCommand {
    public HealCommand() {
        super("heal");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : sender instanceof Player ? (Player) sender : null;
        boolean self = args.length == 0;
        String perm = self ? "xcore.heal" : "xcore.heal.others";

        if (!hasPermission(sender, perm)) return true;
        if (target == null || !target.isOnline()) {
            sendMessage(sender, "invalid-player");
            return true;
        }

        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);

        if (self) {
            sendMessage(target, "healed-self");
        } else {
            String msg = MessageUtil.get("healed-other").replace("%target%", target.getName());
            sender.sendMessage(msg);
            sendMessage(target, "healed-self");
        }

        return true;
    }
}
