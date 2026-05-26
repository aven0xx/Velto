package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
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

    public record Kit(String name, long cooldownSeconds, List<KitItem> items) {}

    private static final LinkedHashMap<String, Kit> kits = new LinkedHashMap<>();
    private static Logger logger;

    public static void load() {
        logger = VeltoPlugin.get().getLogger();
        File kitsFile = new File(VeltoPlugin.get().getDataFolder(), "kits.yml");

        if (!kitsFile.exists()) {
            VeltoPlugin.get().saveResource("kits.yml", false);
        }

        kits.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection == null) {
            logger.warning("[Velto] kits.yml has no 'kits' section — no kits loaded.");
            return;
        }

        for (String kitName : kitsSection.getKeys(false)) {
            ConfigurationSection kitSec = kitsSection.getConfigurationSection(kitName);
            if (kitSec == null) continue;

            long cooldown = kitSec.getLong("cooldown", 0L);
            List<KitItem> items = parseItems(kitSec.getMapList("items"), kitName);
            kits.put(kitName.toLowerCase(), new Kit(kitName, cooldown, items));
        }

        logger.info("[Velto] Loaded " + kits.size() + " kit(s).");
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

    // Returns true if the inventory had no overflow; false if items were dropped on the ground.
    public static boolean giveKit(Player player, Kit kit) {
        boolean dropped = false;
        for (KitItem kitItem : kit.items()) {
            ItemStack stack = new ItemStack(kitItem.material(), kitItem.amount());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (kitItem.displayName() != null) {
                    @SuppressWarnings("deprecation")
                    String colored = ChatColor.translateAlternateColorCodes('&', kitItem.displayName());
                    meta.setDisplayName(colored);
                }
                if (!kitItem.lore().isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
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
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                dropped = true;
            }
        }
        return !dropped;
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

    private static List<KitItem> parseItems(List<Map<?, ?>> rawItems, String kitName) {
        if (rawItems == null || rawItems.isEmpty()) return Collections.emptyList();

        List<KitItem> items = new ArrayList<>();
        for (Map<?, ?> raw : rawItems) {
            Object matObj = raw.get("material");
            if (matObj == null) continue;

            Material mat;
            try {
                mat = Material.valueOf(matObj.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("[Velto] Kit '" + kitName + "': unknown material '" + matObj + "', skipping item.");
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
        return items;
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
