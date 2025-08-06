package com.aven0x.Velto.utils;

import com.aven0x.Velto.Velto;
import com.aven0x.Velto.commands.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.util.List;

public class DynamicCommandRegistrar {

    private static CommandMap commandMap;

    static {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerAlias(String alias, BaseCommand command) {
        if (commandMap == null) return;

        Command cmd = new BukkitCommand(alias) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return command.onCommand(sender, this, label, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return command.onTabComplete(sender, this, alias, args);
            }
        };

        commandMap.register(Velto.getInstance().getName(), cmd);
    }
}
