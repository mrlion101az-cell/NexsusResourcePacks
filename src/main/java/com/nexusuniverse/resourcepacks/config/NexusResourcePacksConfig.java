package com.nexusuniverse.resourcepacks.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NexusResourcePacksConfig {

    /** Reserved id meaning "no pack" -- never a valid key under packs: in config.yml, and never a valid dropped-file id either. */
    public static final String NONE_ID = "none";

    private final JavaPlugin plugin;

    public NexusResourcePacksConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        packsFolder().mkdirs();
    }

    /** The folder admins drop pack zips into to have them picked up automatically -- no config.yml editing needed. Created on startup if it doesn't exist. */
    public File packsFolder() {
        String path = plugin.getConfig().getString("packs-folder", "packs");
        return new File(plugin.getDataFolder(), path);
    }

    /**
     * Every pack currently available, combining two sources:
     * 1. Explicit entries under packs: in config.yml -- for packs that live somewhere
     *    fixed outside the packs folder, like Resource Pack Manager's merge output.
     * 2. Any .zip file sitting directly in packsFolder() -- dropped there by an admin
     *    (upload via FTP/SFTP/your host's file manager, same as any other server file),
     *    auto-registered using the filename as its id and display name. Add a new one
     *    just by dropping the file in and running /resourcepack reload; remove one by
     *    deleting the file and reloading the same way.
     * Explicit config entries win on an id collision (and the dropped file is skipped
     * with a warning) so a folder file can never silently shadow a deliberately configured pack.
     */
    public List<PackDefinition> packs() {
        Map<String, PackDefinition> byId = new LinkedHashMap<>();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("packs");
        if (section != null) {
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
                byId.put(id.toLowerCase(), new PackDefinition(id, displayName, new File(plugin.getDataFolder(), sourcePath)));
            }
        }

        File[] dropped = packsFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (dropped != null) {
            for (File file : dropped) {
                String nameNoExt = file.getName().substring(0, file.getName().length() - 4);
                String id = sanitizeId(nameNoExt);
                if (NONE_ID.equalsIgnoreCase(id)) id = id + "-pack"; // dodge the reserved id from an unlucky filename like "none.zip"

                if (byId.containsKey(id.toLowerCase())) {
                    // Only warn if it's actually a different file fighting for the same id -- an explicit
                    // config entry that happens to already point at this exact dropped file is fine.
                    PackDefinition existing = byId.get(id.toLowerCase());
                    if (!existing.sourceFile().equals(file)) {
                        plugin.getLogger().warning("Dropped pack file \"" + file.getName() + "\" would use the id \"" + id
                                + "\", but that id is already taken by an explicit entry in config.yml -- skipping the dropped file. Rename it to disambiguate.");
                    }
                    continue;
                }

                String displayName = displayNameOverride(id);
                if (displayName == null) displayName = prettify(nameNoExt);
                byId.put(id.toLowerCase(), new PackDefinition(id, displayName, file));
            }
        }

        return new ArrayList<>(byId.values());
    }

    private String displayNameOverride(String id) {
        return plugin.getConfig().getString("display-name-overrides." + id, null);
    }

    private static String sanitizeId(String rawName) {
        String id = rawName.toLowerCase().trim().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return id.isBlank() ? "pack" : id;
    }

    private static String prettify(String rawName) {
        String spaced = rawName.replaceAll("[_\\-]+", " ").trim();
        StringBuilder sb = new StringBuilder();
        for (String word : spaced.split(" ")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.length() > 1 ? word.substring(1) : "");
        }
        return sb.isEmpty() ? rawName : sb.toString();
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
        packsFolder().mkdirs();
    }
}
