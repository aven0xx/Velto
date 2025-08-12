package com.aven0x.VeltoBukkit;

import com.aven0x.Velto.listeners.GodListener;
import com.aven0x.VeltoBukkit.managers.AutoMsgManager;
import com.aven0x.VeltoBukkit.managers.CommandManager;
import com.aven0x.VeltoBukkit.managers.TeleportManager;
import com.aven0x.VeltoBukkit.managers.ChatManager;
import com.aven0x.VeltoBukkit.utils.CommandUtil;
import com.aven0x.VeltoBukkit.utils.NotificationUtil;
import com.aven0x.Velto.utils.ServerUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class VeltoBukkit extends JavaPlugin {

    private static VeltoBukkit instance;
    private TeleportManager teleportManager;
    private BukkitAudiences adventure;
    private AutoMsgManager autoMsgManager;

    @Override
    public void onEnable() {
        instance = this;

        // Detect server type (Spigot vs Paper)
        if (ServerUtil.isPaper()) {
            Bukkit.getLogger().info("[Velto] has been enabled");
            Bukkit.getLogger().info("[Velto] Paper detected. All features enabled.");
        } else {
            Bukkit.getLogger().info("[Velto] has been enabled");
            Bukkit.getLogger().warning("[Velto] Spigot detected. Some features (like /anvil) are disabled.");
        }

        // Load config.yml if not already created
        saveDefaultConfig();

        // Load custom configs
        NotificationUtil.load();
        CommandUtil.load();

        // Setup Adventure + managers
        this.adventure = BukkitAudiences.create(this);
        this.teleportManager = new TeleportManager();
        this.autoMsgManager = new AutoMsgManager();
        this.autoMsgManager.start();
        new ChatManager(this);

        // Register commands
        CommandManager.registerAllCommands();

        // Register listeners
        getServer().getPluginManager().registerEvents(new GodListener(), this);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
        }
    }

    public static VeltoBukkit getInstance() {
        return instance;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }
}
