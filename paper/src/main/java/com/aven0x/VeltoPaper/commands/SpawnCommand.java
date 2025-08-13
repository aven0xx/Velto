package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.VeltoPaper;
import com.aven0x.VeltoPaper.utils.ConfigUtil;
import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends BaseCommand {
    public SpawnCommand() {
        super("spawn");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.spawn")) return true;
        if (!(sender instanceof Player player)) {
            LangUtil.send((Player) sender, "only-player");
            return true;
        }

        Location spawn = ConfigUtil.getSpawn();
        if (spawn == null) {
            LangUtil.send(player, "spawn-not-set");
            return true;
        }

        VeltoPaper.getInstance().getTeleportManager().teleportAsync(player, spawn);
        LangUtil.send(player, "teleporting-spawn");
        return true;
    }
}