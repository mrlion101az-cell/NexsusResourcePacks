package com.nexusuniverse.resourcepacks.delivery;

import com.nexusuniverse.resourcepacks.config.NexusResourcePacksConfig;
import com.nexusuniverse.resourcepacks.gui.ResourcePackMenu;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PackJoinListener implements Listener {

    private static final long APPLY_DELAY_TICKS = 40L;   // ~2s -- gives the client a moment to finish loading in
    private static final long MENU_DELAY_TICKS = 60L;    // ~3s -- staggered a bit after the apply, so the picker GUI doesn't pop open at the exact same moment as the client's own download-confirmation dialog

    private final JavaPlugin plugin;
    private final PackDeliveryManager delivery;
    private final ResourcePackMenu menu;
    private final NexusResourcePacksConfig config;

    public PackJoinListener(JavaPlugin plugin, PackDeliveryManager delivery, ResourcePackMenu menu, NexusResourcePacksConfig config) {
        this.plugin = plugin;
        this.delivery = delivery;
        this.menu = menu;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                delivery.applyOnJoin(event.getPlayer());
            }
        }, APPLY_DELAY_TICKS);

        if (config.showMenuOnJoin()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    event.getPlayer().openInventory(menu.build(event.getPlayer()));
                }
            }, MENU_DELAY_TICKS);
        }
    }
}
