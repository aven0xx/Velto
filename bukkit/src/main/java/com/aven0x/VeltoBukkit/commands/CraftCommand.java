package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CraftCommand extends BaseCommand {
    public CraftCommand() {
        super("craft");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.craft")) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "no-permission");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            if (sender instanceof Player p) {
                LangUtil.send(p, "only-player");
            }
            return true;
        }

        player.openWorkbench(null, true);
        LangUtil.send(player, "opened-craft");
        return true;
    }
}
