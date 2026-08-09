package com.nexusuniverse.resourcepacks;

import com.nexusuniverse.resourcepacks.config.NexusResourcePacksConfig;
import com.nexusuniverse.resourcepacks.delivery.PackDeliveryManager;
import com.nexusuniverse.resourcepacks.delivery.PackJoinListener;
import com.nexusuniverse.resourcepacks.delivery.PlayerPreferenceStore;
import com.nexusuniverse.resourcepacks.gui.ResourcePackMenu;
import com.nexusuniverse.resourcepacks.gui.ResourcePackMenuListener;
import com.nexusuniverse.resourcepacks.hosting.PackHostServer;
import org.bukkit.plugin.java.JavaPlugin;

public class NexusResourcePacksPlugin extends JavaPlugin {

    private PackHostServer hostServer;

    @Override
    public void onEnable() {
        NexusResourcePacksConfig config = new NexusResourcePacksConfig(this);

        hostServer = new PackHostServer(this, config);
        hostServer.start();

        PlayerPreferenceStore preferences = new PlayerPreferenceStore(this);
        PackDeliveryManager delivery = new PackDeliveryManager(config, hostServer, preferences);
        ResourcePackMenu menu = new ResourcePackMenu(this, delivery, config);

        getCommand("resourcepack").setExecutor(new ResourcePackCommand(delivery, menu, config, hostServer));

        getServer().getPluginManager().registerEvents(new PackJoinListener(this, delivery, menu, config), this);
        getServer().getPluginManager().registerEvents(new ResourcePackMenuListener(delivery, menu, config), this);

        getLogger().info("NexusResourcePacks enabled with " + config.packs().size()
                + " pack(s) configured. Drop new pack zips into " + config.packsFolder().getAbsolutePath()
                + " and run /resourcepack reload to add them. Reminder: Resource Pack Manager's own autoHost "
                + "should be set to false for any pack it merges, so it isn't also trying to deliver on top of this plugin.");
    }

    @Override
    public void onDisable() {
        if (hostServer != null) hostServer.stop();
    }
}
