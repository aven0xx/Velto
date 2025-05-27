package com.aven0x.xcore;

import com.aven0x.xcore.manager.CommandManager;
import com.aven0x.xcore.manager.TeleportManager;
import com.aven0x.xcore.utils.ConfigUtil;
import com.aven0x.xcore.utils.MessageUtil;
import com.aven0x.xcore.utils.NotificationUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public class Xcore extends JavaPlugin {

    private static Xcore instance;
    private TeleportManager teleportManager;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        ConfigUtil.loadMessages();
        ConfigUtil.loadCommands();
        MessageUtil.load();
        NotificationUtil.load();

        this.adventure = BukkitAudiences.create(this);
        this.teleportManager = new TeleportManager();

        CommandManager.registerAllCommands();
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
        }
    }

    public static Xcore getInstance() {
        return instance;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }
}
