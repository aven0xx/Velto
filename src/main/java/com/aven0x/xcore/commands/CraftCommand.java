// CraftCommand.java
package com.aven0x.xcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CraftCommand extends BaseCommand {
    public CraftCommand() { super("craft"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.craft")) return true;
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;

        // ✅ Ouvre une table de craft (3x3)
        player.openWorkbench(null, true);
        sendMessage(player, "opened-craft");
        return true;
    }
}
