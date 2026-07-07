package com.aven0x.Velto.integrations;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.managers.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

public final class VaultHook {

    private VaultHook() {}

    private static boolean registered = false;

    // Registers or unregisters Velto as the server's Vault economy provider to
    // match the current economy.yml settings. Safe to call repeatedly (e.g. on
    // every /veltoreload) — only touches Vault classes when actually needed, so
    // servers without Vault installed never load them.
    public static void refresh() {
        boolean shouldRegister = EconomyManager.isEnabled()
                && EconomyManager.isVaultEnabled()
                && Bukkit.getPluginManager().isPluginEnabled("Vault");

        if (shouldRegister && !registered) {
            try {
                Bukkit.getServicesManager().register(
                        Economy.class,
                        new VaultEconomyProvider(),
                        VeltoPlugin.get(),
                        ServicePriority.Normal
                );
                registered = true;
                Bukkit.getLogger().info("[Velto] Registered as the server's Vault economy provider.");
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[Velto] Failed to register Vault economy provider: " + t.getMessage());
            }
        } else if (!shouldRegister && registered) {
            Bukkit.getServicesManager().unregisterAll(VeltoPlugin.get());
            registered = false;
            Bukkit.getLogger().info("[Velto] Unregistered Vault economy provider.");
        }
    }
}
