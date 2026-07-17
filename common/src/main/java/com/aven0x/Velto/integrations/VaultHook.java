package com.aven0x.Velto.integrations;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.managers.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

public final class VaultHook {

    private VaultHook() {}

    private static boolean registered = false;

    // Whether Velto is currently registered as the Vault economy provider. Reflects the
    // real runtime state — false whenever Vault is missing, disabled, or registration failed.
    public static boolean isActive() {
        return registered;
    }

    // Registers or unregisters Velto as the server's Vault economy provider to match the
    // current economy.yml settings. Safe to call repeatedly (e.g. on every /veltoreload).
    //
    // Vault classes (Economy, VaultEconomyProvider, ServicePriority) are only referenced
    // inside the registration branch, which is reached only when the Vault plugin is
    // actually present — so servers without Vault never load those classes.
    public static void refresh() {
        boolean economyEnabled = EconomyManager.isEnabled();
        boolean vaultRequested = EconomyManager.isVaultEnabled();
        boolean vaultPresent = Bukkit.getPluginManager().isPluginEnabled("Vault");
        boolean shouldRegister = economyEnabled && vaultRequested && vaultPresent;

        if (shouldRegister) {
            if (!registered) {
                try {
                    Bukkit.getServicesManager().register(
                            Economy.class,
                            new VaultEconomyProvider(),
                            VeltoPlugin.get(),
                            ServicePriority.Normal
                    );
                    registered = true;
                    Bukkit.getLogger().info("[Velto] Vault found — registered Velto as the economy provider.");
                } catch (Throwable t) {
                    // Registration failed — stay unregistered (fall back to disabled) and report it.
                    registered = false;
                    Bukkit.getLogger().severe("[Velto] Failed to register the Vault economy provider: " + t.getMessage());
                }
            }
            return;
        }

        // Not registering. Tear down any previous registration first...
        if (registered) {
            Bukkit.getServicesManager().unregisterAll(VeltoPlugin.get());
            registered = false;
            Bukkit.getLogger().info("[Velto] Vault economy integration disabled — unregistered the economy provider.");
        }

        // ...then give the admin feedback for the easy-to-miss case: they asked for Vault in
        // economy.yml, but it isn't installed. Fall back to disabled and say so out loud
        // instead of failing silently.
        if (economyEnabled && vaultRequested && !vaultPresent) {
            Bukkit.getLogger().warning("[Velto] economy.yml has vault.enabled: true, but the Vault plugin was "
                    + "not found on this server — the Vault integration has been disabled. Install Vault to "
                    + "enable it, or set vault.enabled: false in economy.yml to hide this warning.");
        }
    }
}
