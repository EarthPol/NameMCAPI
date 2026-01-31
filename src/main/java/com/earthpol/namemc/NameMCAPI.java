package com.earthpol.namemc;

import com.earthpol.namemc.api.NameMCAPIService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NameMCAPI extends JavaPlugin implements NameMCAPIService {

    private NameMCClient client;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        FileConfiguration cfg = getConfig();
        String baseUrl = cfg.getString("namemc.base-url", "https://api.namemc.com");
        long cacheTtlMs = cfg.getLong("namemc.cache-ttl-ms", 60_000L);
        int httpTimeoutMs = cfg.getInt("namemc.http-timeout-ms", 6_000);

        this.client = new NameMCClient(this, baseUrl, cacheTtlMs, httpTimeoutMs);

        Bukkit.getServicesManager().register(NameMCAPIService.class, this, this, ServicePriority.Normal);
        getLogger().info("NameMCAPI enabled (baseUrl=" + baseUrl + ", cacheTtlMs=" + cacheTtlMs + ")");
    }

    @Override
    public void onDisable() {
        if (client != null) {
            client.shutdown();
        }
    }

    // -------------------------
    // NameMCAPIService methods
    // -------------------------

    @Override
    public java.util.concurrent.CompletableFuture<java.util.List<com.earthpol.namemc.api.NameMCFriend>> getFriends(java.util.UUID profileUuid, boolean useCache) {
        return client.getFriends(profileUuid, useCache);
    }

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> areFriends(java.util.UUID profileUuid, java.util.UUID withUuid, boolean useCache) {
        return client.areFriends(profileUuid, withUuid, useCache);
    }

    @Override
    public java.util.concurrent.CompletableFuture<Integer> getServerLikes(String serverDomain, boolean useCache) {
        return client.getServerLikes(serverDomain, useCache);
    }

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> hasProfileLikedServer(String serverDomain, java.util.UUID profileUuid, boolean useCache) {
        return client.hasProfileLikedServer(serverDomain, profileUuid, useCache);
    }
}
