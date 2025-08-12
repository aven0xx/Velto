package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.NotificationUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class RenameCommand extends BaseCommand {
    public RenameCommand() {
        super("rename");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        // Permission
        if (!hasPermission(sender, "velto.rename")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        // Player-only (same pattern as your Anvil/Craft)
        if (!(sender instanceof Player player)) {
            return true;
        }

        // Must hold something
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            NotificationUtil.send(player, "rename-no-item");
            return true;
        }

        // Usage / reset
        if (args.length == 0) {
            NotificationUtil.send(player, "rename-usage"); // e.g., "&e/rename <name...> or /rename reset"
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("clear"))) {
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(null); // remove custom name
            item.setItemMeta(meta);
            NotificationUtil.send(player, "rename-cleared");
            return true;
        }

        // Apply &-codes
        String raw = String.join(" ", args);
        String colored = ChatColor.translateAlternateColorCodes('&', raw);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colored);
        item.setItemMeta(meta);

        NotificationUtil.send(player, "rename-success");
        return true;
    }
}
