package com.nexusuniverse.resourcepacks.delivery;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PackJoinListener implements Listener {

    private static final long DELAY_TICKS = 40L; // ~2s -- gives the client a moment to finish loading in before the prompt shows

    private final JavaPlugin plugin;
    private final PackDeliveryManager delivery;

    public PackJoinListener(JavaPlugin plugin, PackDeliveryManager delivery) {
        this.plugin = plugin;
        this.delivery = delivery;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                delivery.applyOnJoin(event.getPlayer());
            }
        }, DELAY_TICKS);
    }
}
