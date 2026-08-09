package com.nexusuniverse.resourcepacks.hosting;

import com.nexusuniverse.resourcepacks.config.NexusResourcePacksConfig;
import com.nexusuniverse.resourcepacks.config.PackDefinition;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Hosts every pack currently listed under packs: in config.yml, each on its
 * own path (/pack/<id>.zip) off one shared embedded HTTP server, and keeps
 * a cached SHA-1 hash of whatever's currently on disk for each. Re-reading
 * happens per pack, so updating one pack's zip doesn't touch the others.
 *
 * Uses com.sun.net.httpserver.HttpServer, which ships with the JDK itself
 * (no extra dependency).
 */
public class PackHostServer {

    private final JavaPlugin plugin;
    private final NexusResourcePacksConfig config;

    private HttpServer server;
    private final Map<String, HostedPack> hosted = new ConcurrentHashMap<>();

    public PackHostServer(JavaPlugin plugin, NexusResourcePacksConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(config.httpPort()), 0);
            server.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "NexusResourcePacks-HTTP");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            plugin.getLogger().info("Hosting Nexus resource packs on port " + config.httpPort()
                    + " -- make sure that port is open, and that hosting.public-base-url in config.yml points at it.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't start the resource-pack HTTP server on port "
                    + config.httpPort() + " -- is something else already using it?", e);
            return;
        }

        // First hash of every pack runs off the main thread, so a large pack doesn't stall server startup.
        long intervalTicks = Math.max(1, config.rehashIntervalMinutes()) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshAll, 0L, intervalTicks);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    /** Re-registers any pack newly added to config.yml, drops any removed from it, and re-hashes changed ones. Runs off the main thread. */
    public void refreshAll() {
        var configured = config.packs();
        var configuredIds = configured.stream().map(PackDefinition::id).collect(java.util.stream.Collectors.toSet());

        // Drop hosting for packs no longer in config.yml -- also unregister the HTTP context
        // itself, not just the cache entry, so a later re-add of the same id doesn't collide
        // with a context that's still bound underneath.
        for (String id : java.util.List.copyOf(hosted.keySet())) {
            if (!configuredIds.contains(id)) {
                hosted.remove(id);
                try {
                    server.removeContext(pathFor(id));
                } catch (IllegalArgumentException ignored) {
                    // already unbound -- fine
                }
                plugin.getLogger().info("Pack \"" + id + "\" was removed from config.yml -- no longer hosted.");
            }
        }

        for (PackDefinition def : configured) {
            refreshOne(def);
        }
    }

    private void refreshOne(PackDefinition def) {
        if (!def.sourceFile().isFile()) {
            plugin.getLogger().warning("No pack file found for \"" + def.id() + "\" at " + def.sourceFile().getAbsolutePath());
            return;
        }

        long modified = def.sourceFile().lastModified();
        long size = def.sourceFile().length();
        HostedPack existing = hosted.get(def.id());
        if (existing != null && existing.modified() == modified && existing.size() == size) {
            return; // unchanged since last check
        }

        try {
            byte[] bytes = Files.readAllBytes(def.sourceFile().toPath());
            String hash = sha1Hex(bytes);
            boolean firstTime = existing == null;
            hosted.put(def.id(), new HostedPack(bytes, hash, modified, size));
            if (firstTime) {
                String path = pathFor(def.id());
                server.createContext(path, exchange -> handleRequest(exchange, def.id()));
                plugin.getLogger().info("Now hosting pack \"" + def.id() + "\" at " + path
                        + " (" + (bytes.length / 1024) + " KB, sha1 " + hash + ").");
            } else {
                plugin.getLogger().info("Pack \"" + def.id() + "\" updated (" + (bytes.length / 1024) + " KB, sha1 " + hash + ").");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read/hash pack \"" + def.id() + "\" at " + def.sourceFile().getAbsolutePath(), e);
        }
    }

    private void handleRequest(HttpExchange exchange, String packId) throws IOException {
        HostedPack pack = hosted.get(packId);
        if (pack == null) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "application/zip");
        exchange.sendResponseHeaders(200, pack.bytes().length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(pack.bytes());
        }
    }

    private static String sha1Hex(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String pathFor(String packId) {
        return "/pack/" + packId + ".zip";
    }

    public boolean isReady(String packId) {
        return hosted.containsKey(packId);
    }

    public String packUrl(String packId) {
        return config.publicBaseUrl() + pathFor(packId);
    }

    public String packHash(String packId) {
        HostedPack pack = hosted.get(packId);
        return pack != null ? pack.hash() : null;
    }

    private record HostedPack(byte[] bytes, String hash, long modified, long size) {
    }
}
