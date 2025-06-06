package com.aven0x.Velto;

import com.aven0x.Velto.listeners.GodListener;
import com.aven0x.Velto.manager.AutoMsgManager;
import com.aven0x.Velto.manager.CommandManager;
import com.aven0x.Velto.manager.TeleportManager;
import com.aven0x.Velto.utils.CommandUtil;
import com.aven0x.Velto.utils.NotificationUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public class Velto extends JavaPlugin {

    private static Velto instance;
    private TeleportManager teleportManager;
    private BukkitAudiences adventure;
    private AutoMsgManager autoMsgManager;

    @Override
    public void onEnable() {
        instance = this;

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

    public static Velto getInstance() {
        return instance;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }
}
