package com.aven0x.Velto.listeners;

import com.aven0x.Velto.manager.GodManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public class GodListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && GodManager.isGod(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && GodManager.isGod(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && GodManager.isGod(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player && GodManager.isGod(player)) {
            if (event.getNewEffect() != null && isNegativeEffect(event.getNewEffect().getType())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isNegativeEffect(PotionEffectType type) {
        return type == PotionEffectType.POISON ||
                type == PotionEffectType.WITHER ||
                type == PotionEffectType.BLINDNESS ||
                type == PotionEffectType.NAUSEA ||            // confusion
                type == PotionEffectType.HUNGER ||
                type == PotionEffectType.SLOWNESS ||          // slow
                type == PotionEffectType.MINING_FATIGUE ||    // slow_digging
                type == PotionEffectType.UNLUCK ||
                type == PotionEffectType.WEAKNESS ||
                type == PotionEffectType.LEVITATION ||        // optional
                type == PotionEffectType.BAD_OMEN ||
                type == PotionEffectType.DARKNESS;
    }
}
