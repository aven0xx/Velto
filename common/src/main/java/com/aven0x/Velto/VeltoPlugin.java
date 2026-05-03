package com.aven0x.Velto;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Shared plugin instance holder.
 * Call VeltoPlugin.set(this) at the very start of onEnable() in each module.
 */
public final class VeltoPlugin {

    private static JavaPlugin instance;

    private VeltoPlugin() {}

    public static void set(JavaPlugin plugin) {
        instance = plugin;
    }

    public static JavaPlugin get() {
        if (instance == null) {
            throw new IllegalStateException("VeltoPlugin has not been initialized. Call VeltoPlugin.set(plugin) in onEnable().");
        }
        return instance;
    }
}
