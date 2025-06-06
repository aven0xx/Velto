package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.NotificationUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends BaseCommand {
    public SetSpawnCommand() {
        super("setspawn");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.setspawn")) return true;
        if (!(sender instanceof Player player)) {
            NotificationUtil.send((Player) sender, "only-player");
            return true;
        }

        ConfigUtil.setSpawn(player.getLocation());
        NotificationUtil.send(player, "spawn-set");
        return true;
    }
}