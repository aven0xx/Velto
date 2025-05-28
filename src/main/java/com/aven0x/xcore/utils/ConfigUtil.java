package com.aven0x.xcore.utils;

import com.aven0x.xcore.Xcore;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {

    private static final FileConfiguration config = Xcore.getInstance().getConfig();

    public static void setSpawn(Location location) {
        config.set("spawn", location);
        Xcore.getInstance().saveConfig();
    }

    public static Location getSpawn() {
        return config.contains("spawn") ? config.getLocation("spawn") : null;
    }
}

