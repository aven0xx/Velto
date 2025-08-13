package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AnvilCommand extends BaseCommand {
    public AnvilCommand() {
        super("anvil");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.anvil")) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "no-permission");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            // Command used by console or non-player source
            return true;
        }

        // Open the Paper anvil UI
        player.openAnvil(null, true);
        LangUtil.send(player, "opened-anvil");
        return true;
    }
}