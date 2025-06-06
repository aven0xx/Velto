package com.aven0x.Velto.utils;

import com.aven0x.Velto.Velto;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {

    private static final FileConfiguration config = Velto.getInstance().getConfig();

    public static void setSpawn(Location location) {
        config.set("spawn", location);
        Velto.getInstance().saveConfig();
    }

    public static Location getSpawn() {
        return config.contains("spawn") ? config.getLocation("spawn") : null;
    }
}

