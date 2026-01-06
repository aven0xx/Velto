package com.aven0x.VeltoBukkit.managers;

import com.aven0x.VeltoBukkit.commands.*;
import com.aven0x.VeltoBukkit.utils.CommandUtil;
import com.aven0x.VeltoBukkit.utils.DynamicCommandRegistrar;

import java.util.function.Supplier;

import static com.aven0x.VeltoBukkit.commands.GamemodeCommands.*;

public class CommandManager {

    public static void registerAllCommands() {
        register("spawn", SpawnCommand::new);
        register("setspawn", SetSpawnCommand::new);
        register("time", TimeCommand::new);
        register("day", DayCommand::new);
        register("night", NightCommand::new);
        register("craft", CraftCommand::new);
        register("list", ListCommand::new);
        register("notiftest", NotifTestCommand::new);
        register("rename", RenameCommand::new);
        register("feed", FeedCommand::new);
        register("heal", HealCommand::new);
        register("alert", AlertCommand::new);
        register("weather", WeatherCommand::new);
        register("kill", KillCommand::new);
        register("speed", SpeedCommand::new);
        register("god", GodCommand::new);
        register("killall", KillAllCommand::new);
        register("gamemode", GamemodeCommand::new);
        register("gmc", GmcCommand::new);
        register("gms", GmsCommand::new);
        register("gma", GmaCommand::new);
        register("gmsp", GmspCommand::new);
        register("itemlore", ItemLoreCommand::new);
        register("veltoreload", ReloadCommand::new);
        register("afk", AfkCommand::new);
    }

    private static void register(String name, Supplier<? extends BaseCommand> factory) {
        if (!CommandUtil.isEnabled(name)) return;

        BaseCommand command = factory.get(); // only constructed if enabled

        // Register the MAIN command name dynamically
        DynamicCommandRegistrar.registerCommand(name, command);

        // Register aliases dynamically
        for (String alias : CommandUtil.getAliases(name)) {
            DynamicCommandRegistrar.registerCommand(alias, command);
        }
    }
}
