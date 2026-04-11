package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RenameCommand extends BaseCommand {
    public RenameCommand() {
        super("rename");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.rename")) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            LangUtil.send(player, "rename-no-item");
            return true;
        }

        if (args.length == 0) {
            LangUtil.send(player, "rename-usage");
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("clear"))) {
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(null);
            item.setItemMeta(meta);
            LangUtil.send(player, "rename-cleared");
            return true;
        }

        String raw = String.join(" ", args);
        String colored = ChatColor.translateAlternateColorCodes('&', raw);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colored);
        item.setItemMeta(meta);

        LangUtil.send(player, "rename-success");
        return true;
    }
}
