package com.aven0x.xcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class AnvilCommand extends BaseCommand {
    public AnvilCommand() { super("anvil"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.anvil")) return true;
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;
        player.openInventory(player.getServer().createInventory(null, InventoryType.ANVIL));
        sendMessage(player, "opened-anvil");
        return true;
    }
}
