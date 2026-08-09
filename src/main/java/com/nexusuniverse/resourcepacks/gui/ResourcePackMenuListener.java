package com.nexusuniverse.resourcepacks.gui;

import com.nexusuniverse.resourcepacks.config.PackDefinition;
import com.nexusuniverse.resourcepacks.config.NexusResourcePacksConfig;
import com.nexusuniverse.resourcepacks.delivery.PackDeliveryManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ResourcePackMenuListener implements Listener {

    private final PackDeliveryManager delivery;
    private final ResourcePackMenu menu;
    private final NexusResourcePacksConfig config;

    public ResourcePackMenuListener(PackDeliveryManager delivery, ResourcePackMenu menu, NexusResourcePacksConfig config) {
        this.delivery = delivery;
        this.menu = menu;
        this.config = config;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ResourcePackMenu.Holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String packId = clicked.getItemMeta().getPersistentDataContainer().get(menu.packIdKey, PersistentDataType.STRING);
        if (packId == null) return; // filler / header item, not a real button

        if (NexusResourcePacksConfig.NONE_ID.equalsIgnoreCase(packId)) {
            delivery.setPreference(player, NexusResourcePacksConfig.NONE_ID);
            player.sendMessage("§7Playing without a resource pack. Run /resourcepack any time to change your mind.");
        } else {
            PackDefinition def = config.findPack(packId);
            delivery.setPreference(player, packId);
            player.sendMessage("§a" + (def != null ? def.displayName() : packId) + " selected -- downloading now.");
        }
        player.closeInventory();
    }
}
