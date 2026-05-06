package com.aven0x.VeltoPaper.managers;

import com.aven0x.Velto.commands.*;
import com.aven0x.Velto.utils.CommandUtil;
import com.aven0x.VeltoPaper.commands.AnvilCommand;
import com.aven0x.VeltoPaper.utils.DynamicCommandRegistrar;

import java.util.function.Supplier;

import static com.aven0x.Velto.commands.GamemodeCommands.*;

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
        register("sun", WeatherCommand.SunCommand::new);
        register("rain", WeatherCommand.RainCommand::new);
        register("thunder", WeatherCommand.ThunderCommand::new);
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
        register("back", BackCommand::new);
        register("anvil", AnvilCommand::new);
        register("fly", FlyCommand::new);
        register("msg", MsgCommand::new);
        register("reply", ReplyCommand::new);
        register("tpall", TpAllCommand::new);
        register("sudo", SudoCommand::new);
        register("home", HomeCommand::new);
        register("sethome", SetHomeCommand::new);
        register("delhome", DelHomeCommand::new);
        register("homes", HomesCommand::new);
    }

    private static void register(String name, Supplier<? extends BaseCommand> factory) {
        if (!CommandUtil.isEnabled(name)) return;

        BaseCommand command = factory.get();

        DynamicCommandRegistrar.registerCommand(name, command);

        for (String alias : CommandUtil.getAliases(name)) {
            DynamicCommandRegistrar.registerCommand(alias, command);
        }
    }
}
