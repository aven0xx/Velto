package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.NotificationUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HealCommand extends BaseCommand {

    public HealCommand() {
        super("heal");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player ? (Player) sender : null);
        boolean self = args.length == 0;

        String perm = self ? "velto.heal" : "velto.heal.others";
        if (!hasPermission(sender, perm)) return true;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "invalid-player");
            } else {
                sender.sendMessage("§cInvalid player.");
            }
            return true;
        }

        AttributeInstance attr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            target.setHealth(attr.getValue());
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);

        if (self) {
            NotificationUtil.send(target, "healed-self");
        } else {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "healed-other", Map.of("%target%", target.getName()));
            } else {
                sender.sendMessage("§aHealed: " + target.getName());
            }
            NotificationUtil.send(target, "healed-self");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("velto.heal.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !PlayerUtil.isVanished(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
