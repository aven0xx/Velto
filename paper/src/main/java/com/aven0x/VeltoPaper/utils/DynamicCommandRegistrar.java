package com.aven0x.VeltoPaper.utils;

import com.aven0x.Velto.commands.BaseCommand;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Paper-native command registrar using Paper's BasicCommand API and LifecycleEvents.
 * Set the registrar before calling registerCommand via setRegistrar().
 */
@SuppressWarnings("UnstableApiUsage")
public class DynamicCommandRegistrar {

    private static Commands registrar;

    public static void setRegistrar(Commands registrar) {
        DynamicCommandRegistrar.registrar = registrar;
    }

    public static void registerCommand(String alias, BaseCommand command) {
        if (registrar == null) return;

        registrar.register(alias, new BasicCommand() {
            @Override
            public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
                command.execute(source.getSender(), alias, args);
            }

            @Override
            public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
                List<String> completions = command.complete(source.getSender(), alias, args);
                return completions;
            }
        });
    }
}
