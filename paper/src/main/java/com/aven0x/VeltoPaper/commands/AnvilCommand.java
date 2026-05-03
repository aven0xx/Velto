package com.aven0x.VeltoPaper.commands;

import com.aven0x.Velto.commands.BaseCommand;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnvilCommand extends BaseCommand {
    public AnvilCommand() {
        super("anvil");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.anvil")) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true;
        }

        player.openAnvil(null, true);
        LangUtil.send(player, "opened-anvil");
        return true;
    }
}
