package com.aven0x.VeltoBukkit.managers;

import com.aven0x.VeltoBukkit.commands.*;
import com.aven0x.VeltoBukkit.utils.CommandUtil;
import com.aven0x.VeltoBukkit.utils.DynamicCommandRegistrar;

import static com.aven0x.VeltoBukkit.commands.GamemodeCommands.*;

public class CommandManager {

    public static void registerAllCommands() {
        register("spawn", new SpawnCommand());
        register("setspawn", new SetSpawnCommand());
        register("time", new TimeCommand());
        register("day", new DayCommand());
        register("night", new NightCommand());
        register("craft", new CraftCommand());
        register("list", new ListCommand());
        register("notiftest", new NotifTestCommand());
        register("rename", new RenameCommand());
        register("feed", new FeedCommand());
        register("heal", new HealCommand());
        register("broadcast", new BroadcastCommand());
        register("weather", new WeatherCommand());
        register("kill", new KillCommand());
        register("speed", new SpeedCommand());
        register("god", new GodCommand());
        register("killall", new KillAllCommand());
        register("gamemode", new GamemodeCommand());
        register("gmc", new GmcCommand());
        register("gms", new GmsCommand());
        register("gma", new GmaCommand());
        register("gmsp", new GmspCommand());
        register("itemlore", new ItemLoreCommand());
        register("veltoreload", new ReloadCommand());
        register("afk", new AfkCommand());
    }

    private static void register(String name, BaseCommand command) {
        if (!CommandUtil.isEnabled(name)) return;

        for (String alias : CommandUtil.getAliases(name)) {
            DynamicCommandRegistrar.registerAlias(alias, command);
        }
    }
}
