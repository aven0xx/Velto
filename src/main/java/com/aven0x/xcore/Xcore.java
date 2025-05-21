package com.aven0x.xcore;

import com.aven0x.xcore.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;
import com.aven0x.xcore.manager.CommandManager;
import com.aven0x.xcore.manager.TeleportManager;
import com.aven0x.xcore.utils.ConfigUtil;

public class Xcore extends JavaPlugin {

    private static Xcore instance;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ConfigUtil.loadMessages();
        ConfigUtil.loadCommands();
        MessageUtil.load(); // Charge les messages depuis messages.yml


        teleportManager = new TeleportManager();

        CommandManager.registerAllCommands();
    }

    @Override
    public void onDisable() {
        // Any necessary cleanup
    }

    public static Xcore getInstance() {
        return instance;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}
