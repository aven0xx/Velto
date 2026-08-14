package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.platform.Schedulers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class KitManager {

    private KitManager() {}

    public record KitItem(
            Material material,
            int amount,
            String displayName,
            List<String> lore,
            Map<Enchantment, Integer> enchantments
    ) {}

    public record Kit(String name, long cooldownSeconds, boolean oneTime, List<String> commands, List<KitItem> items) {}

    public record LoadResult(int kitsLoaded, int itemsSkipped) {}

    public enum GiveResult { SUCCESS, OVERFLOW, ERROR }

    private record ParsedItems(List<KitItem> items, int skipped) {}

    // volatile + rebuilt-then-swapped on reload, so a reader during /veltoreload never sees a
    // half-refilled map (an empty or partial kit list).
    private static volatile Map<String, Kit> kits = new LinkedHashMap<>();
    private static Logger logger;

    public static LoadResult load() {
        logger = VeltoPlugin.get().getLogger();
        File kitsFile = new File(VeltoPlugin.get().getDataFolder(), "kits.yml");

        if (!kitsFile.exists()) {
            VeltoPlugin.get().saveResource("kits.yml", false);
        }

        LinkedHashMap<String, Kit> fresh = new LinkedHashMap<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection == null) {
            logger.warning("[Velto] kits.yml has no 'kits' section — no kits loaded.");
            kits = fresh;
            return new LoadResult(0, 0);
        }

        int totalSkipped = 0;
        for (String kitName : kitsSection.getKeys(false)) {
            ConfigurationSection kitSec = kitsSection.getConfigurationSection(kitName);
            if (kitSec == null) continue;

            long cooldown = kitSec.getLong("cooldown", 0L);
            boolean oneTime = kitSec.getBoolean("one-time", false);
            List<String> commands = kitSec.getStringList("commands");
            ParsedItems parsed = parseItems(kitSec.getMapList("items"), kitName);
            totalSkipped += parsed.skipped();
            fresh.put(kitName.toLowerCase(), new Kit(kitName, cooldown, oneTime, commands, parsed.items()));
        }

        kits = fresh;   // publish the fully-built map in one volatile write
        logger.info("[Velto] Loaded " + kits.size() + " kit(s)"
                + (totalSkipped > 0 ? " (" + totalSkipped + " item(s) skipped due to invalid material/enchantment)" : "")
                + ".");
        return new LoadResult(kits.size(), totalSkipped);
    }

    public static Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public static Collection<Kit> getKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public static long getCooldownRemaining(UUID uuid, String kitName) {
        if (!UserdataManager.isInitialized()) return 0L;
        YamlConfiguration data = UserdataManager.getData(uuid);
        long lastUsed;
        synchronized (data) {
            lastUsed = data.getLong("kit-cooldowns." + kitName.toLowerCase(), 0L);
        }
        if (lastUsed == 0L) return 0L;

        Kit kit = getKit(kitName);
        if (kit == null || kit.cooldownSeconds() <= 0) return 0L;

        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000L;
        return Math.max(0L, kit.cooldownSeconds() - elapsed);
    }

    public static void setCooldown(UUID uuid, String kitName) {
        UserdataManager.set(uuid, "kit-cooldowns." + kitName.toLowerCase(), System.currentTimeMillis());
        UserdataManager.save(uuid);
    }

    // === One-time kits ===

    public static boolean hasClaimedOnce(UUID uuid, String kitName) {
        if (!UserdataManager.isInitialized()) return false;
        YamlConfiguration data = UserdataManager.getData(uuid);
        synchronized (data) {
            return data.getBoolean("kit-claimed." + kitName.toLowerCase(), false);
        }
    }

    public static void markClaimedOnce(UUID uuid, String kitName) {
        UserdataManager.set(uuid, "kit-claimed." + kitName.toLowerCase(), true);
        UserdataManager.save(uuid);
    }

    // Clears both the cooldown timestamp and the one-time-claim flag for a kit.
    public static void resetCooldown(UUID uuid, String kitName) {
        UserdataManager.set(uuid, "kit-cooldowns." + kitName.toLowerCase(), null);
        UserdataManager.set(uuid, "kit-claimed." + kitName.toLowerCase(), null);
        UserdataManager.save(uuid);
    }

    // Builds the actual items + runs any configured commands. Returns ERROR (and gives
    // nothing further) if the item delivery fails, so the caller can skip setting the
    // cooldown/claim instead of burning it on a botched delivery. Reward commands are now
    // dispatched on the global region (fire-and-forget), so a command that fails later no
    // longer forces ERROR — only item-delivery failures do.
    public static GiveResult giveKit(Player player, Kit kit) {
        boolean overflowed = false;
        try {
            for (KitItem kitItem : kit.items()) {
                ItemStack stack = buildItemStack(kitItem);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
                if (!overflow.isEmpty()) {
                    overflowed = true;
                    for (ItemStack leftover : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
            }
            for (String command : kit.commands()) {
                String parsed = command.replace("%player%", player.getName());
                // Console commands run on the global region; dispatching there makes them
                // fire-and-forget relative to giveKit (see the method note above).
                Schedulers.get().global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed));
            }
        } catch (Exception e) {
            logger.warning("[Velto] Failed to fully deliver kit '" + kit.name() + "' to " + player.getName() + ": " + e.getMessage());
            return GiveResult.ERROR;
        }
        return overflowed ? GiveResult.OVERFLOW : GiveResult.SUCCESS;
    }

    // Read-only inventory showing what a kit contains, without granting it.
    public static Inventory buildPreviewInventory(Kit kit) {
        int rows = Math.max(1, (kit.items().size() + 8) / 9);
        int size = Math.min(54, rows * 9);

        KitPreviewHolder holder = new KitPreviewHolder(kit.name());
        Inventory inv = Bukkit.createInventory(holder, size,
                ChatColor.translateAlternateColorCodes('&', "&8Kit Preview: &f" + kit.name()));
        holder.setInventory(inv);

        for (int i = 0; i < kit.items().size() && i < size; i++) {
            inv.setItem(i, buildItemStack(kit.items().get(i)));
        }
        return inv;
    }

    private static ItemStack buildItemStack(KitItem kitItem) {
        ItemStack stack = new ItemStack(kitItem.material(), kitItem.amount());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (kitItem.displayName() != null) {
                @SuppressWarnings("deprecation")
                String colored = ChatColor.translateAlternateColorCodes('&', kitItem.displayName());
                meta.setDisplayName(colored);
            }
            if (!kitItem.lore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>(kitItem.lore().size());
                for (String line : kitItem.lore()) {
                    @SuppressWarnings("deprecation")
                    String colored = ChatColor.translateAlternateColorCodes('&', line);
                    coloredLore.add(colored);
                }
                meta.setLore(coloredLore);
            }
            for (Map.Entry<Enchantment, Integer> entry : kitItem.enchantments().entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static String formatCooldown(long seconds) {
        if (seconds <= 0) return "0s";
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("s");
        return sb.toString().trim();
    }

    private static ParsedItems parseItems(List<Map<?, ?>> rawItems, String kitName) {
        if (rawItems == null || rawItems.isEmpty()) return new ParsedItems(Collections.emptyList(), 0);

        List<KitItem> items = new ArrayList<>();
        int skipped = 0;
        for (Map<?, ?> raw : rawItems) {
            Object matObj = raw.get("material");
            if (matObj == null) {
                skipped++;
                continue;
            }

            Material mat;
            try {
                mat = Material.valueOf(matObj.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("[Velto] Kit '" + kitName + "': unknown material '" + matObj + "', skipping item.");
                skipped++;
                continue;
            }

            int amount = 1;
            if (raw.containsKey("amount")) {
                amount = Math.max(1, Math.min(64, ((Number) raw.get("amount")).intValue()));
            }

            String displayName = raw.containsKey("name") ? raw.get("name").toString() : null;

            List<String> lore = new ArrayList<>();
            if (raw.get("lore") instanceof List<?> rawLore) {
                for (Object line : rawLore) lore.add(line != null ? line.toString() : "");
            }

            Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
            if (raw.get("enchantments") instanceof Map<?, ?> rawEnch) {
                for (Map.Entry<?, ?> entry : rawEnch.entrySet()) {
                    Enchantment ench = parseEnchantment(entry.getKey().toString());
                    if (ench == null) {
                        logger.warning("[Velto] Kit '" + kitName + "': unknown enchantment '" + entry.getKey() + "', skipping.");
                        continue;
                    }
                    enchantments.put(ench, ((Number) entry.getValue()).intValue());
                }
            }

            items.add(new KitItem(mat, amount, displayName, lore, enchantments));
        }
        return new ParsedItems(items, skipped);
    }

    @SuppressWarnings("deprecation")
    private static Enchantment parseEnchantment(String name) {
        // Modern Minecraft key (e.g. "sharpness" -> minecraft:sharpness)
        try {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase()));
            if (ench != null) return ench;
        } catch (Exception ignored) {}

        // Legacy Bukkit name fallback (e.g. "DAMAGE_ALL")
        return Enchantment.getByName(name.toUpperCase());
    }
}
