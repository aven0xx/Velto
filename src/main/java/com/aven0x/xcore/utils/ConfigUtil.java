package com.aven0x.xcore.utils;

import com.aven0x.xcore.Xcore;
import java.io.File;

public class ConfigUtil {
    public static void loadMessages() {
        File file = new File(Xcore.getInstance().getDataFolder(), "messages.yml");
        if (!file.exists()) {
            Xcore.getInstance().saveResource("messages.yml", false);
        }
    }

    public static void loadCommands() {
        File file = new File(Xcore.getInstance().getDataFolder(), "commands.yml");
        if (!file.exists()) {
            Xcore.getInstance().saveResource("commands.yml", false);
        }
    }
}