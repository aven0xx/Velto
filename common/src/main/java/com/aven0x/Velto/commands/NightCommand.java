package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NightCommand extends BaseCommand {
    public NightCommand() {
        super("night");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.timeset");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.timeset")) {
            return true;
        }

        World world = args.length > 0
                ? sender.getServer().getWorld(args[0])
                : sender instanceof Player ? ((Player) sender).getWorld() : null;

        if (world == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-world");
            } else if (args.length > 0) {
                sender.sendMessage("§cInvalid world: " + args[0]);
            } else {
                // Console has no world of its own — it must name one.
                sender.sendMessage("§cUsage: /night <world>");
            }
            return true;
        }

        world.setTime(13000);

        if (sender instanceof Player player) {
            LangUtil.send(player, "time-set-night");
        } else {
            sender.sendMessage("§aSet time to night in world §f" + world.getName() + "§a.");
        }

        return true;
    }
}
