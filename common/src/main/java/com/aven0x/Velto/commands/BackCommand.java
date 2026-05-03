package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.BackManager;
import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand extends BaseCommand {

    public BackCommand() {
        super("back");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.back")) return true;

        Player player = (Player) sender;
        Location last = BackManager.getLastLocation(player);

        if (last == null) {
            LangUtil.send(player, "back-no-location");
            return true;
        }

        BackManager.markBacking(player.getUniqueId());
        TeleportManager.getInstance().teleportAsync(player, last);
        LangUtil.send(player, "back-teleported");
        return true;
    }
}
