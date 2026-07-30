package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class EnderChestCommand extends BaseCommand {

    public EnderChestCommand() {
        super("enderchest");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.enderchest") || checkPermission(sender, "velto.enderchest.others");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        boolean self = args.length == 0;
        String perm = self ? "velto.enderchest" : "velto.enderchest.others";

        if (!hasPermission(sender, perm)) return true;

        // Opening a GUI requires a physical player on the receiving end, even when viewing someone else's.
        if (!isPlayer(sender)) return true;
        Player opener = (Player) sender;

        if (self) {
            opener.openInventory(opener.getEnderChest());
            LangUtil.send(opener, "opened-enderchest");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            LangUtil.send(opener, "invalid-player");
            return true;
        }

        opener.openInventory(target.getEnderChest());
        LangUtil.send(opener, "opened-enderchest-other", Map.of("%target%", target.getName()));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender.hasPermission("velto.enderchest.others")) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            return PlayerUtil.onlineSnapshot().stream()
                    .filter(player -> !PlayerUtil.isVanished(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
