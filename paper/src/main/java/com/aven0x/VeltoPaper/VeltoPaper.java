package com.aven0x.VeltoPaper;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.listeners.BackListener;
import com.aven0x.Velto.listeners.ChatListener;
import com.aven0x.Velto.listeners.GodListener;
import com.aven0x.Velto.listeners.KitPreviewListener;
import com.aven0x.Velto.listeners.UserdataListener;
import com.aven0x.Velto.integrations.VaultHook;
import com.aven0x.Velto.managers.AfkManager;
import com.aven0x.Velto.managers.AutoMsgManager;
import com.aven0x.Velto.managers.EconomyManager;
import com.aven0x.Velto.managers.KitManager;
import com.aven0x.Velto.managers.PlaceholderManager;
import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.managers.UserdataManager;
import com.aven0x.Velto.managers.WarpManager;
import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.utils.AfkPositionStorage;
import com.aven0x.Velto.utils.CommandUtil;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.VeltoPaper.managers.ChatManager;
import com.aven0x.VeltoPaper.managers.CommandManager;
import com.aven0x.VeltoPaper.platform.FoliaScheduler;
import com.aven0x.VeltoPaper.utils.DynamicCommandRegistrar;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class VeltoPaper extends JavaPlugin {

    private AutoMsgManager autoMsgManager;

    @Override
    public void onEnable() {
        VeltoPlugin.set(this);
        Schedulers.set(new FoliaScheduler(this));

        Bukkit.getLogger().info("[Velto] has been enabled");
        Bukkit.getLogger().info("[Velto] Paper detected. All features enabled.");

        // Load config.yml if not already created
        saveDefaultConfig();
        ConfigUtil.refreshCache();

        // Load custom configs
        LangUtil.load();
        CommandUtil.load();
        KitManager.load();
        WarpManager.init(getDataFolder());
        EconomyManager.load();
        VaultHook.refresh();

        // Setup managers
        new TeleportManager();
        autoMsgManager = new AutoMsgManager();
        autoMsgManager.start();
        new ChatManager(this);

        // Register commands via Paper's native lifecycle API (no reflection)
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            DynamicCommandRegistrar.setRegistrar(event.registrar());
            CommandManager.registerAllCommands();
        });

        // Register listeners
        getServer().getPluginManager().registerEvents(new GodListener(), this);
        getServer().getPluginManager().registerEvents(new BackListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new KitPreviewListener(), this);

        AfkManager afkManager = new AfkManager();
        getServer().getPluginManager().registerEvents(afkManager, this);
        PlaceholderManager.init();
        AfkManager.start();
        AfkPositionStorage.init(getDataFolder());
        UserdataManager.init(getDataFolder());
        UserdataManager.startAutosave(ConfigUtil.getUserdataAutosaveIntervalTicks());
        getServer().getPluginManager().registerEvents(new UserdataListener(), this);
    }

    @Override
    public void onDisable() {
        AfkManager.stop();
        if (autoMsgManager != null) {
            autoMsgManager.stop();
            autoMsgManager = null;
        }
        TeleportManager tm = TeleportManager.getInstance();
        if (tm != null) tm.cancelAll();
        UserdataManager.stopAutosave();
        // Stop in-flight async writers before the exclusive final flush (report F-83).
        Schedulers.cancelAllQuietly();
        UserdataManager.saveAll();
    }

    public static VeltoPaper getInstance() {
        return (VeltoPaper) VeltoPlugin.get();
    }

    public TeleportManager getTeleportManager() {
        return TeleportManager.getInstance();
    }
}
