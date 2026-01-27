package com.aven0x.VeltoPaper;

import com.aven0x.Velto.listeners.GodListener;
import com.aven0x.VeltoPaper.managers.*;
import com.aven0x.VeltoPaper.utils.AfkPositionStorage;
import com.aven0x.VeltoPaper.utils.CommandUtil;
import com.aven0x.VeltoPaper.utils.LangUtil;
import com.aven0x.Velto.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class VeltoPaper extends JavaPlugin {

    private static VeltoPaper instance;
    private TeleportManager teleportManager;
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
        LangUtil.load();
        CommandUtil.load();

        // Setup managers
        this.teleportManager = new TeleportManager();
        this.autoMsgManager = new AutoMsgManager();
        this.autoMsgManager.start();
        new ChatManager(this);

        // Register commands
        CommandManager.registerAllCommands();

        // Register listeners
        getServer().getPluginManager().registerEvents(new GodListener(), this);

        AfkManager afkManager = new AfkManager();
        getServer().getPluginManager().registerEvents(afkManager, this);
        PlaceholderManager.init();
        AfkManager.start();
        AfkPositionStorage.init(getDataFolder());
    }

    @Override
    public void onDisable() {
        // nothing special to close now that Adventure is removed

        // Arrêter le système AFK
        AfkManager.stop();
    }

    public static VeltoPaper getInstance() {
        return instance;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}
