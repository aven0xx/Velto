package com.aven0x.xcore.utils;

import com.aven0x.xcore.Xcore;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageUtil {

    private static FileConfiguration messages;

    public static void load() {
        File file = new File(Xcore.getInstance().getDataFolder(), "messages.yml");
        if (!file.exists()) {
            Xcore.getInstance().saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static String get(String path) {
        if (messages == null) {
            load(); // Sécurité au cas où ce n’est pas encore chargé
        }
        return color(messages.getString(path, path));
    }
}
