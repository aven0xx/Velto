package com.aven0x.xcore.commands;

import com.aven0x.xcore.Xcore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends BaseCommand {
    public SetSpawnCommand() { super("setspawn"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.setspawn")) return true;
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;
        Xcore.getInstance().getConfig().set("spawn", player.getLocation());
        Xcore.getInstance().saveConfig();
        sendMessage(sender, "spawn-set");
        return true;
    }
}