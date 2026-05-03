package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand extends BaseCommand {
    public ReloadCommand() {
        super("veltoreload");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.reload")) {
            sender.sendMessage("§cYou do not have permission to run this command.");
            return true;
        }

        Bukkit.getLogger().info("[Velto] Starting reload...");

        try {
            ConfigUtil.reload();
            Bukkit.getLogger().info("[Velto] config.yml reloaded");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Velto] Failed to reload config.yml: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            LangUtil.load();
            Bukkit.getLogger().info("[Velto] lang.yml reloaded.");
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to reload lang.yml: " + t.getMessage());
            t.printStackTrace();
        }

        Bukkit.getLogger().info("[Velto] Reload complete.");

        if (sender instanceof Player player) {
            LangUtil.send(player, "reload-success");
        }

        return true;
    }
}
