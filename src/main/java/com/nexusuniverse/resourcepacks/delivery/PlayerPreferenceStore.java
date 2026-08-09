package com.nexusuniverse.resourcepacks.delivery;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Every player's own /resourcepack choice -- which pack id they picked, or
 * NexusResourcePacksConfig.NONE_ID -- kept independently of whatever
 * Minecraft's client itself remembers about this server. That's the whole
 * point, since the client's remembered per-server setting is what stops
 * vanilla's native prompt from reliably reappearing.
 */
public class PlayerPreferenceStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, String> preferences = new ConcurrentHashMap<>();

    public PlayerPreferenceStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                preferences.put(UUID.fromString(key), yaml.getString(key));
            } catch (IllegalArgumentException ignored) {
                // stray/corrupt key -- skip it rather than fail the whole load
            }
        }
    }

    /** Null means no preference has ever been recorded for this player. */
    public String get(UUID playerId) {
        return preferences.get(playerId);
    }

    public void set(UUID playerId, String packId) {
        preferences.put(playerId, packId);
        saveAsync();
    }

    private void saveAsync() {
        Map<UUID, String> snapshot = Map.copyOf(preferences);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<UUID, String> entry : snapshot.entrySet()) {
                yaml.set(entry.getKey().toString(), entry.getValue());
            }
            try {
                yaml.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save resource-pack preferences to " + file.getAbsolutePath(), e);
            }
        });
    }
}
