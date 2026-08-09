package com.nexusuniverse.resourcepacks.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NexusResourcePacksConfig {

    /** Reserved id meaning "no pack" -- never a valid key under packs: in config.yml. */
    public static final String NONE_ID = "none";

    private final JavaPlugin plugin;

    public NexusResourcePacksConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    /**
     * Every pack currently configured under packs: in config.yml, in the order
     * they're listed there. Add a new pack by adding a new entry under packs:
     * (an id, a display-name, and a source-file pointing at its zip somewhere
     * on disk) and running /resourcepack reload -- no code changes needed.
     */
    public List<PackDefinition> packs() {
        List<PackDefinition> result = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("packs");
        if (section == null) return result;

        for (String id : section.getKeys(false)) {
            if (NONE_ID.equalsIgnoreCase(id)) {
                plugin.getLogger().warning("Skipping a pack configured with the id \"" + id
                        + "\" -- that id is reserved for \"no pack\". Rename it in config.yml.");
                continue;
            }
            String displayName = section.getString(id + ".display-name", id);
            String sourcePath = section.getString(id + ".source-file");
            if (sourcePath == null || sourcePath.isBlank()) {
                plugin.getLogger().warning("Pack \"" + id + "\" in config.yml has no source-file set -- skipping it.");
                continue;
            }
            result.add(new PackDefinition(id, displayName, new File(plugin.getDataFolder(), sourcePath)));
        }
        return result;
    }

    public PackDefinition findPack(String id) {
        for (PackDefinition pack : packs()) {
            if (pack.id().equalsIgnoreCase(id)) return pack;
        }
        return null;
    }

    public String publicBaseUrl() {
        String url = plugin.getConfig().getString("hosting.public-base-url", "http://YOUR_SERVER_IP:8091");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public int httpPort() {
        return plugin.getConfig().getInt("hosting.http-port", 8091);
    }

    public long rehashIntervalMinutes() {
        return plugin.getConfig().getLong("hosting.rehash-interval-minutes", 5);
    }

    public String promptMessage() {
        return plugin.getConfig().getString("prompt.message",
                "Nexus Universe has resource packs available! Pick one with /resourcepack, or play with default textures.");
    }

    public boolean required() {
        return plugin.getConfig().getBoolean("prompt.required", false);
    }

    /** What a player with no recorded choice yet gets sent on their very first join. NONE_ID (or blank/missing) means send nothing until they visit /resourcepack themselves. */
    public String defaultPackId() {
        String id = plugin.getConfig().getString("defaults.default-pack-id", NONE_ID);
        return (id == null || id.isBlank()) ? NONE_ID : id;
    }

    public void reload() {
        plugin.reloadConfig();
    }
}
