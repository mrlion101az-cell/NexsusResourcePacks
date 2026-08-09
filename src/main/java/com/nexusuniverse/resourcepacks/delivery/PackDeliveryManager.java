package com.nexusuniverse.resourcepacks.delivery;

import com.nexusuniverse.resourcepacks.config.NexusResourcePacksConfig;
import com.nexusuniverse.resourcepacks.config.PackDefinition;
import com.nexusuniverse.resourcepacks.hosting.PackHostServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.net.URI;

/**
 * The actual send/remove calls, on top of Paper's modern (non-deprecated)
 * Adventure resource-pack API -- Player#sendResourcePacks(ResourcePackRequest)
 * to send, Player#clearResourcePacks() to remove.
 *
 * Deliberately uses clearResourcePacks() rather than targeting a specific
 * pack by ID for removal: this plugin is meant to be the only thing sending
 * a server resource pack (RPM's own delivery should be off), so "clear
 * everything we've sent" and "clear whichever pack was active" are the
 * same operation, and it sidesteps needing a hand-picked, stable pack UUID.
 */
public class PackDeliveryManager {

    private final NexusResourcePacksConfig config;
    private final PackHostServer host;
    private final PlayerPreferenceStore preferences;

    public PackDeliveryManager(NexusResourcePacksConfig config, PackHostServer host, PlayerPreferenceStore preferences) {
        this.config = config;
        this.host = host;
        this.preferences = preferences;
    }

    /** The pack id this player currently has selected, or NexusResourcePacksConfig.NONE_ID. Falls back to config defaults if nothing's been recorded yet, or if their old choice no longer exists in config.yml. */
    public String currentSelection(Player player) {
        String stored = preferences.get(player.getUniqueId());
        if (stored == null) return config.defaultPackId();
        if (NexusResourcePacksConfig.NONE_ID.equalsIgnoreCase(stored)) return NexusResourcePacksConfig.NONE_ID;
        if (config.findPack(stored) == null) return config.defaultPackId(); // their pack was removed from config.yml since
        return stored;
    }

    /** What a fresh join should do: apply whatever this player's current effective selection is. */
    public void applyOnJoin(Player player) {
        String selection = currentSelection(player);
        if (!NexusResourcePacksConfig.NONE_ID.equalsIgnoreCase(selection)) {
            sendPack(player, selection);
        }
        // NONE_ID: nothing to remove -- a fresh connection has no server pack applied yet.
    }

    /** Called from the /resourcepack menu and from /resourcepack select <id>|none. Stores the choice AND applies it immediately. */
    public void setPreference(Player player, String packId) {
        preferences.set(player.getUniqueId(), packId);
        if (NexusResourcePacksConfig.NONE_ID.equalsIgnoreCase(packId)) {
            removePack(player);
        } else {
            sendPack(player, packId);
        }
    }

    public void sendPack(Player player, String packId) {
        PackDefinition def = config.findPack(packId);
        if (def == null) {
            player.sendMessage("§cThat pack doesn't exist (anymore) -- check /resourcepack for what's available.");
            return;
        }
        if (!host.isReady(packId)) {
            player.sendMessage("§c\"" + def.displayName() + "\" isn't ready yet -- try again in a moment.");
            return;
        }

        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(host.packUrl(packId)))
                .hash(host.packHash(packId))
                .build();

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .prompt(Component.text(config.promptMessage()))
                .required(config.required())
                .replace(true)
                .build();

        player.sendResourcePacks(request);
    }

    public void removePack(Player player) {
        player.clearResourcePacks();
    }
}
