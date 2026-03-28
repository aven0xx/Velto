package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TpAllCommand extends BaseCommand {

    public TpAllCommand() {
        super("tpall");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.tpall")) return true;

        Player target = (Player) sender;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(target)) {
                p.teleport(target.getLocation());
            }
        }

        LangUtil.send(target, "tpall-success");
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
