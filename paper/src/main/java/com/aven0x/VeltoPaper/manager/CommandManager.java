package com.aven0x.VeltoPaper.manager;

import com.aven0x.VeltoPaper.commands.*;
import com.aven0x.VeltoPaper.utils.CommandUtil;
import com.aven0x.VeltoPaper.utils.DynamicCommandRegistrar;

import static com.aven0x.VeltoPaper.commands.GamemodeCommands.*;

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

        register("feed", new FeedCommand());
        register("heal", new HealCommand());
        register("broadcast", new BroadcastCommand());
        register("weather", new WeatherCommand());
        register("kill", new KillCommand());
        register("speed", new SpeedCommand());
        register("god", new GodCommand());

        register("gamemode", new GamemodeCommand());
        register("gmc", new GmcCommand());
        register("gms", new GmsCommand());
        register("gma", new GmaCommand());
        register("gmsp", new GmspCommand());
    }

    private static void register(String name, BaseCommand command) {
        if (!CommandUtil.isEnabled(name)) return;

        for (String alias : CommandUtil.getAliases(name)) {
            DynamicCommandRegistrar.registerAlias(alias, command);
        }
    }
}
