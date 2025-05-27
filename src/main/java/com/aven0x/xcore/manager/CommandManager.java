package com.aven0x.xcore.manager;

import com.aven0x.xcore.commands.*;
import com.aven0x.xcore.utils.CommandUtil;

public class CommandManager {
    public static void registerAllCommands() {
        if (CommandUtil.isEnabled("spawn")) new SpawnCommand();
        if (CommandUtil.isEnabled("setspawn")) new SetSpawnCommand();
        if (CommandUtil.isEnabled("time")) new TimeCommand();
        if (CommandUtil.isEnabled("day")) new DayCommand();
        if (CommandUtil.isEnabled("night")) new NightCommand();
        if (CommandUtil.isEnabled("craft")) new CraftCommand();
        if (CommandUtil.isEnabled("anvil")) new AnvilCommand();
        if (CommandUtil.isEnabled("gamemode")) new GamemodeCommand();
        if (CommandUtil.isEnabled("gms")) new GamemodeSurvivalCommand();
        if (CommandUtil.isEnabled("gmc")) new GamemodeCreativeCommand();
        if (CommandUtil.isEnabled("gma")) new GamemodeAdventureCommand();
        if (CommandUtil.isEnabled("gmsp")) new GamemodeSpectatorCommand();
        if (CommandUtil.isEnabled("feed")) new FeedCommand();
        if (CommandUtil.isEnabled("heal")) new HealCommand();
        if (CommandUtil.isEnabled("broadcast")) new BroadcastCommand();
        if (CommandUtil.isEnabled("weather")) new WeatherCommand();
        if (CommandUtil.isEnabled("kill")) new KillCommand();
        if (CommandUtil.isEnabled("speed")) new SpeedCommand();
    }
}
