package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpAllCommand extends BaseCommand {

    public TpAllCommand() {
        super("tpall");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.tpall")) return true;

        Player target = (Player) sender;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(target)) {
                TeleportManager.getInstance().teleportAsync(p, target.getLocation());
            }
        }

        LangUtil.send(target, "tpall-success");
        return true;
    }
}
