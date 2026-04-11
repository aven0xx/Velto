package com.aven0x.VeltoBukkit;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.listeners.BackListener;
import com.aven0x.Velto.listeners.GodListener;
import com.aven0x.Velto.managers.AfkManager;
import com.aven0x.Velto.managers.AutoMsgManager;
import com.aven0x.Velto.managers.PlaceholderManager;
import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.utils.AfkPositionStorage;
import com.aven0x.Velto.utils.CommandUtil;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.ServerUtil;
import com.aven0x.VeltoBukkit.managers.ChatManager;
import com.aven0x.VeltoBukkit.managers.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class VeltoBukkit extends JavaPlugin {

    @Override
    public void onEnable() {
        VeltoPlugin.set(this);

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
        new TeleportManager();
        AutoMsgManager autoMsgManager = new AutoMsgManager();
        autoMsgManager.start();
        new ChatManager(this);

        // Register commands
        CommandManager.registerAllCommands();

        // Register listeners
        getServer().getPluginManager().registerEvents(new GodListener(), this);
        getServer().getPluginManager().registerEvents(new BackListener(), this);

        AfkManager afkManager = new AfkManager();
        getServer().getPluginManager().registerEvents(afkManager, this);
        PlaceholderManager.init();
        AfkManager.start();
        AfkPositionStorage.init(getDataFolder());
    }

    @Override
    public void onDisable() {
        AfkManager.stop();
    }

    public static VeltoBukkit getInstance() {
        return (VeltoBukkit) VeltoPlugin.get();
    }

    public TeleportManager getTeleportManager() {
        return TeleportManager.getInstance();
    }
}
