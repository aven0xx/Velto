package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.VeltoBukkit;
import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand extends BaseCommand {
    public ReloadCommand() {
        super("veltoreload");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.reload")) {
            // Hardcoded so it still works even if LangUtil is broken
            sender.sendMessage("§cYou do not have permission to run this command.");
            return true;
        }

        Bukkit.getLogger().info("[Velto] Starting reload...");

        // 1) Reload config.yml via Bukkit API
        try {
            VeltoBukkit.getInstance().reloadConfig();
            Bukkit.getLogger().info("[Velto] config.yml reloaded");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Velto] Failed to reload config.yml: " + e.getMessage());
            e.printStackTrace();
        }

        // 2) Reload lang.yml (wrapped so command still works if LangUtil is broken)
        try {
            LangUtil.load();
            Bukkit.getLogger().info("[Velto] lang.yml reloaded.");
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to reload lang.yml: " + t.getMessage());
            t.printStackTrace();
        }

        Bukkit.getLogger().info("[Velto] Reload complete.");
        // No success message to player; confirmation is console-only as requested.
        return true;
    }
}
