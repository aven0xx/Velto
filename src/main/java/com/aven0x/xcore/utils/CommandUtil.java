package com.aven0x.xcore.utils;

import com.aven0x.xcore.Xcore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class CommandUtil {
    private static FileConfiguration commands;

    public static void load() {
        File file = new File(Xcore.getInstance().getDataFolder(), "commands.yml");
        if (!file.exists()) {
            Xcore.getInstance().saveResource("commands.yml", false);
        }
        commands = YamlConfiguration.loadConfiguration(file);
    }

    public static boolean isEnabled(String commandName) {
        if (commands == null) load();
        return commands.getBoolean(commandName + ".enabled", true);
    }

    public static List<String> getAliases(String commandName) {
        if (commands == null) load();
        List<String> aliases = commands.getStringList(commandName + ".aliases");
        return aliases != null ? aliases : Collections.emptyList();
    }
}