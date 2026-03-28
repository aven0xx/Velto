package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.managers.BackManager;
import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BackCommand extends BaseCommand {

    public BackCommand() {
        super("back");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.back")) return true;

        Player player = (Player) sender;
        Location last = BackManager.getLastLocation(player);

        if (last == null) {
            LangUtil.send(player, "back-no-location");
            return true;
        }

        BackManager.markBacking(player.getUniqueId());
        player.teleport(last);
        LangUtil.send(player, "back-teleported");
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
