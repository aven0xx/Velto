package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NightCommand extends BaseCommand {
    public NightCommand() {
        super("night");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.timeset")) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "no-permission");
            }
            return true;
        }

        World world = args.length > 0
                ? sender.getServer().getWorld(args[0])
                : sender instanceof Player ? ((Player) sender).getWorld() : null;

        if (world == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-world");
            }
            return true;
        }

        world.setTime(13000);

        if (sender instanceof Player player) {
            LangUtil.send(player, "time-set-night");
        }

        return true;
    }
}