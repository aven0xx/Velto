package com.aven0x.Velto.managers;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

// Marker holder so KitPreviewListener can identify and lock down a kit preview
// inventory without relying on a fragile title string comparison.
public final class KitPreviewHolder implements InventoryHolder {

    private final String kitName;
    private Inventory inventory;

    public KitPreviewHolder(String kitName) {
        this.kitName = kitName;
    }

    public String getKitName() {
        return kitName;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
