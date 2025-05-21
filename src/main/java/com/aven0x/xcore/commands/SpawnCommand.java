package com.aven0x.xcore.commands;

import com.aven0x.xcore.Xcore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand extends BaseCommand {
    public SpawnCommand() { super("spawn"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.spawn")) return true;
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;
        if (!Xcore.getInstance().getConfig().contains("spawn")) {
            sendMessage(sender, "spawn-not-set");
            return true;
        }
        Location spawn = (Location) Xcore.getInstance().getConfig().get("spawn");
        Xcore.getInstance().getTeleportManager().teleportAsync(player, spawn);
        sendMessage(sender, "teleporting-spawn");
        return true;
    }
}