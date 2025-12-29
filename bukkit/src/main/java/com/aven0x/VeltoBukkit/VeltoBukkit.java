package com.aven0x.VeltoBukkit;

import com.aven0x.Velto.listeners.GodListener;
import com.aven0x.VeltoBukkit.managers.AfkManager;
import com.aven0x.VeltoBukkit.managers.AutoMsgManager;
import com.aven0x.VeltoBukkit.managers.CommandManager;
import com.aven0x.VeltoBukkit.managers.TeleportManager;
import com.aven0x.VeltoBukkit.managers.ChatManager;
import com.aven0x.VeltoBukkit.utils.AfkPositionStorage;
import com.aven0x.VeltoBukkit.utils.CommandUtil;
import com.aven0x.VeltoBukkit.utils.LangUtil;
import com.aven0x.Velto.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class VeltoBukkit extends JavaPlugin {

    private static VeltoBukkit instance;
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
        AfkManager.start();
        AfkPositionStorage.init(getDataFolder());
    }

    @Override
    public void onDisable() {
        // nothing special to close now that Adventure is removed

        // Arrêter le système AFK
        AfkManager.stop();
    }

    public static VeltoBukkit getInstance() {
        return instance;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}
