package com.nexusuniverse.resourcepacks.gui;

import com.nexusuniverse.resourcepacks.config.NexusResourcePacksConfig;
import com.nexusuniverse.resourcepacks.config.PackDefinition;
import com.nexusuniverse.resourcepacks.delivery.PackDeliveryManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * PickPack-style pack picker: one button per pack configured under packs:
 * in config.yml, plus a "play without a pack" button. Which pack a clicked
 * button represents is tagged directly on the item via PersistentDataContainer,
 * so the listener doesn't need a separate slot->id map that could drift out
 * of sync with what's actually drawn.
 */
public class ResourcePackMenu {

    public final NamespacedKey packIdKey;

    private final PackDeliveryManager delivery;
    private final NexusResourcePacksConfig config;

    public ResourcePackMenu(JavaPlugin plugin, PackDeliveryManager delivery, NexusResourcePacksConfig config) {
        this.packIdKey = new NamespacedKey(plugin, "pack-id");
        this.delivery = delivery;
        this.config = config;
    }

    public Inventory build(Player player) {
        List<PackDefinition> packs = config.packs();
        int buttonCount = packs.size() + 1; // +1 for the "none" button
        int size = Math.min(54, Math.max(27, ((buttonCount + 8) / 9 + 1) * 9)); // pack row(s) + header row, rounded up to a full row, 27..54

        Inventory inventory = org.bukkit.Bukkit.createInventory(new Holder(), size,
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "NEXUS RESOURCE PACKS");

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), null);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        inventory.setItem(4, namedItem(Material.PAINTING,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Nexus Resource Packs",
                List.of(ChatColor.GRAY + "Pick one below, or play without one.",
                        ChatColor.GRAY + "You can change your mind any time."),
                null));

        String current = delivery.currentSelection(player);
        int slot = 9; // start of the second row

        // Guards against ever writing past the inventory's last slot -- realistically never hit
        // (54 slots comfortably fits 45+ packs), but cheap insurance against a runtime crash if it ever is.
        for (PackDefinition pack : packs) {
            if (slot >= inventory.getSize() - 1) break;
            boolean selected = pack.id().equalsIgnoreCase(current);
            inventory.setItem(slot, namedItem(
                    selected ? Material.LIME_DYE : Material.PAPER,
                    (selected ? ChatColor.GREEN + "" + ChatColor.BOLD + "\u2713 " : ChatColor.YELLOW.toString()) + pack.displayName(),
                    List.of(selected ? ChatColor.GREEN + "Currently selected" : ChatColor.GRAY + "Click to select"),
                    pack.id()));
            slot++;
        }

        if (slot < inventory.getSize()) {
            boolean noneSelected = NexusResourcePacksConfig.NONE_ID.equalsIgnoreCase(current);
            inventory.setItem(slot, namedItem(
                    noneSelected ? Material.RED_DYE : Material.BARRIER,
                    (noneSelected ? ChatColor.GREEN + "" + ChatColor.BOLD + "\u2713 " : ChatColor.RED.toString()) + "PLAY WITHOUT A PACK",
                    List.of(noneSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.GRAY + "Click to select"),
                    NexusResourcePacksConfig.NONE_ID));
        }

        return inventory;
    }

    private ItemStack namedItem(Material material, String name, List<String> lore, String taggedPackId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        if (taggedPackId != null) {
            meta.getPersistentDataContainer().set(packIdKey, PersistentDataType.STRING, taggedPackId);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Marker so the listener can recognize this inventory without depending on its exact title text. */
    public static class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
