package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends BaseCommand {
    public SetSpawnCommand() {
        super("setspawn");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.setspawn")) return true;
        if (!(sender instanceof Player player)) {
            return true;
        }

        ConfigUtil.setSpawn(player.getLocation());
        LangUtil.send(player, "spawn-set");
        return true;
    }
}
