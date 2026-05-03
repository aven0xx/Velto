package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CraftCommand extends BaseCommand {
    public CraftCommand() {
        super("craft");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.craft")) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true;
        }

        player.openWorkbench(null, true);
        LangUtil.send(player, "opened-craft");
        return true;
    }
}
