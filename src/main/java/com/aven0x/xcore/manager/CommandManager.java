package com.aven0x.xcore.manager;

import com.aven0x.xcore.commands.*;

public class CommandManager {
    public static void registerAllCommands() {
        new SpawnCommand();
        new SetSpawnCommand();
        new TimeCommand();
        new DayCommand();
        new NightCommand();
        new CraftCommand();
        new AnvilCommand();
        new GamemodeCommand();
        new GamemodeSurvivalCommand();
        new GamemodeCreativeCommand();
        new GamemodeAdventureCommand();
        new GamemodeSpectatorCommand();
        new FeedCommand();
        new HealCommand();
        new BroadcastCommand();
        new WeatherCommand();
    }
}