package com.aven0x.xcore.commands;

public class GamemodeSpectatorCommand extends GamemodeShortcutCommand {
    public GamemodeSpectatorCommand() {
        super("gmsp", org.bukkit.GameMode.SPECTATOR, "xcore.gamemode.sp", "xcore.gamemode.sp.others");
    }
}