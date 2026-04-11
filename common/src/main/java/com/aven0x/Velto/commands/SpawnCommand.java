package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand extends BaseCommand {
    public SpawnCommand() {
        super("spawn");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.spawn")) return true;
        if (!(sender instanceof Player player)) {
            return true;
        }

        Location spawn = ConfigUtil.getSpawn();
        if (spawn == null) {
            LangUtil.send(player, "spawn-not-set");
            return true;
        }

        TeleportManager.getInstance().teleportAsync(player, spawn);
        LangUtil.send(player, "teleporting-spawn");
        return true;
    }
}
