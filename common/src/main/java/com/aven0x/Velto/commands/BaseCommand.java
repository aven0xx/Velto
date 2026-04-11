package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand {

    protected final String name;

    public BaseCommand(String name) {
        this.name = name;
    }

    /**
     * Execute the command. Return true if handled, false to show usage.
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    /**
     * Provide tab completion suggestions.
     */
    public List<String> complete(CommandSender sender, String label, String[] args) {
        return Collections.emptyList();
    }

    protected boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return false;
        }
        return true;
    }

    protected boolean hasPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "no-permission");
            } else {
                sender.sendMessage("§cYou do not have permission.");
            }
            return false;
        }
        return true;
    }

    public String getName() {
        return name;
    }
}
