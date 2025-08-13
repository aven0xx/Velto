package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.ConfigUtil;
import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand extends BaseCommand {
    public SetSpawnCommand() {
        super("setspawn");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.setspawn")) return true;
        if (!(sender instanceof Player player)) {
            LangUtil.send((Player) sender, "only-player");
            return true;
        }

        ConfigUtil.setSpawn(player.getLocation());
        LangUtil.send(player, "spawn-set");
        return true;
    }
}