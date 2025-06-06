package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.NotificationUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class AnvilCommand extends BaseCommand {
    public AnvilCommand() {
        super("anvil");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.anvil")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            // Console or command block trying to use this
            return true; // NotificationUtil only supports Player, so we silently ignore
        }

        player.openInventory(player.getServer().createInventory(null, InventoryType.ANVIL));
        NotificationUtil.send(player, "opened-anvil");
        return true;
    }
}
