package com.aven0x.xcore.commands;

import com.aven0x.xcore.Xcore;
import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand extends BaseCommand {
    public SpawnCommand() {
        super("spawn");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.spawn")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true; // tu peux aussi activer `only-player` ici si tu veux l’indiquer
        }

        if (!Xcore.getInstance().getConfig().contains("spawn")) {
            NotificationUtil.send(player, "spawn-not-set");
            return true;
        }

        Location spawn = (Location) Xcore.getInstance().getConfig().get("spawn");
        Xcore.getInstance().getTeleportManager().teleportAsync(player, spawn);
        NotificationUtil.send(player, "teleporting-spawn");
        return true;
    }
}