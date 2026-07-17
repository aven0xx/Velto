package com.aven0x.Velto.commands;

import com.aven0x.Velto.integrations.VaultHook;
import com.aven0x.Velto.managers.AutoMsgManager;
import com.aven0x.Velto.managers.EconomyManager;
import com.aven0x.Velto.managers.KitManager;
import com.aven0x.Velto.managers.WarpManager;
import com.aven0x.Velto.utils.CommandUtil;
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
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.reload");
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

        try {
            CommandUtil.load();
            Bukkit.getLogger().info("[Velto] commands.yml reloaded.");
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to reload commands.yml: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            KitManager.LoadResult kitResult = KitManager.load();
            Bukkit.getLogger().info("[Velto] kits.yml reloaded (" + kitResult.kitsLoaded() + " kit(s), "
                    + kitResult.itemsSkipped() + " item(s) skipped).");
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to reload kits.yml: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            WarpManager.reload();
            Bukkit.getLogger().info("[Velto] warps.yml reloaded (" + WarpManager.getWarpNames().size() + " warp(s)).");
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to reload warps.yml: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            EconomyManager.load();
            Bukkit.getLogger().info("[Velto] economy.yml reloaded.");
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to reload economy.yml: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            VaultHook.refresh();
            Bukkit.getLogger().info("[Velto] Vault economy hook refreshed (active: " + VaultHook.isActive() + ").");
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to refresh Vault economy hook: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            if (AutoMsgManager.getInstance() != null) {
                AutoMsgManager.getInstance().restart();
                Bukkit.getLogger().info("[Velto] AutoMsgManager restarted with new interval.");
            }
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Velto] Failed to restart AutoMsgManager: " + t.getMessage());
            t.printStackTrace();
        }

        Bukkit.getLogger().info("[Velto] Reload complete.");

        if (sender instanceof Player player) {
            LangUtil.send(player, "reload-success");
        }

        return true;
    }
}
