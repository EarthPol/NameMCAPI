package com.earthpol.namemc;

import com.earthpol.namemc.api.NameMCFriend;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixes for your compile errors:
 * - No "record" (records require Java 16+). Paper is often Java 17 but your project may target older language level.
 * - No "toList()" (Stream#toList requires Java 16+). Use Collectors.toList().
 * - CacheEntry is a normal static class with getters.
 * - No unused Class<T> parameter.
 */
final class NameMCClient {

    private final Plugin plugin;
    private final String baseUrl;
    private final long cacheTtlMs;
    private final int httpTimeoutMs;

    private final Gson gson = new Gson();
    private final HttpClient http;

    private final ExecutorService ioPool;
    private final ConcurrentHashMap<String, CacheEntry<?>> cache = new ConcurrentHashMap<String, CacheEntry<?>>();

    NameMCClient(Plugin plugin, String baseUrl, long cacheTtlMs, int httpTimeoutMs) {
        this.plugin = plugin;
        this.baseUrl = stripTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
        this.cacheTtlMs = Math.max(0L, cacheTtlMs);
        this.httpTimeoutMs = Math.max(1, httpTimeoutMs);

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.httpTimeoutMs))
                .version(HttpClient.Version.HTTP_2)
                .build();

        final AtomicInteger n = new AtomicInteger(1);
        this.ioPool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("NameMC-IO-" + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    void shutdown() {
        ioPool.shutdownNow();
        cache.clear();
    }

    CompletableFuture<List<NameMCFriend>> getFriends(UUID profileUuid, boolean useCache) {
        UUID profile = Objects.requireNonNull(profileUuid, "profileUuid");
        String path = "/profile/" + encodePath(profile.toString()) + "/friends";
        String key = "friends:" + profile;

        final Type listType = new TypeToken<List<FriendDto>>() {}.getType();

        return cached(key, useCache, new Supplier<CompletableFuture<List<NameMCFriend>>>() {
            @Override
            public CompletableFuture<List<NameMCFriend>> get() {
                return getBody(path).thenApply(body -> {
                    List<FriendDto> dtos = gson.fromJson(body, listType);
                    if (dtos == null) return java.util.Collections.emptyList();

                    java.util.List<NameMCFriend> out = new java.util.ArrayList<NameMCFriend>(dtos.size());
                    for (FriendDto d : dtos) {
                        out.add(new NameMCFriend(UUID.fromString(d.uuid), d.name));
                    }
                    return out;
                });
            }
        });
    }

    CompletableFuture<Boolean> areFriends(UUID profileUuid, UUID withUuid, boolean useCache) {
        UUID profile = Objects.requireNonNull(profileUuid, "profileUuid");
        UUID with = Objects.requireNonNull(withUuid, "withUuid");

        String path = "/profile/" + encodePath(profile.toString()) + "/friends?with=" + encodeQuery(with.toString());
        String key = "friendsWith:" + profile + ":" + with;

        return cached(key, useCache, new Supplier<CompletableFuture<Boolean>>() {
            @Override
            public CompletableFuture<Boolean> get() {
                return getJson(path).thenApply(NameMCClient::parseBoolean);
            }
        });
    }

    CompletableFuture<Integer> getServerLikes(String serverDomain, boolean useCache) {
        String server = requireServer(serverDomain);

        String path = "/server/" + encodePath(server) + "/likes";
        String key = "likes:" + server;

        return cached(key, useCache, new Supplier<CompletableFuture<Integer>>() {
            @Override
            public CompletableFuture<Integer> get() {
                return getJson(path).thenApply(json -> {
                    // handle: number OR {"likes":123} OR {"count":123}
                    if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                        return json.getAsInt();
                    }
                    if (json.isJsonObject()) {
                        if (json.getAsJsonObject().has("likes")) return json.getAsJsonObject().get("likes").getAsInt();
                        if (json.getAsJsonObject().has("count")) return json.getAsJsonObject().get("count").getAsInt();
                    }
                    throw new CompletionException(new IOException("Unexpected likes payload: " + json));
                });
            }
        });
    }

    CompletableFuture<Boolean> hasProfileLikedServer(String serverDomain, UUID profileUuid, boolean useCache) {
        String server = requireServer(serverDomain);
        UUID profile = Objects.requireNonNull(profileUuid, "profileUuid");

        String path = "/server/" + encodePath(server) + "/likes?profile=" + encodeQuery(profile.toString());
        String key = "liked:" + server + ":" + profile;

        return cached(key, useCache, new Supplier<CompletableFuture<Boolean>>() {
            @Override
            public CompletableFuture<Boolean> get() {
                return getJson(path).thenApply(NameMCClient::parseBoolean);
            }
        });
    }

    // -------------------------
    // HTTP helpers
    // -------------------------

    private CompletableFuture<String> getBody(String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = URI.create(baseUrl + path);
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofMillis(httpTimeoutMs))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = res.statusCode();

                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP " + code + " from NameMC for " + path + " body=" + truncate(res.body(), 500));
                }
                return res.body();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, ioPool);
    }

    private CompletableFuture<JsonElement> getJson(String path) {
        return getBody(path).thenApply(JsonParser::parseString);
    }

    private static boolean parseBoolean(JsonElement el) {
        if (el == null) return false;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) return el.getAsBoolean();
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) return Boolean.parseBoolean(el.getAsString());
        throw new CompletionException(new IllegalStateException("Unexpected boolean payload: " + el));
    }

    // -------------------------
    // Cache helpers (TTL + in-flight dedupe)
    // -------------------------

    private <T> CompletableFuture<T> cached(
            String key,
            boolean useCache,
            Supplier<CompletableFuture<T>> supplier
    ) {
        if (!useCache || cacheTtlMs <= 0L) return supplier.get();

        long now = System.currentTimeMillis();
        CacheEntry<?> existing = cache.get(key);

        if (existing != null && existing.getExpiresAtMs() > now) {
            @SuppressWarnings("unchecked")
            CompletableFuture<T> f = (CompletableFuture<T>) existing.getFuture();
            return f;
        }

        CompletableFuture<T> created = supplier.get().whenComplete((val, err) -> {
            if (err != null) {
                cache.remove(key); // don't keep failures cached
            }
        });

        cache.put(key, new CacheEntry<T>(created, now + cacheTtlMs));
        return created;
    }

    // -------------------------
    // CacheEntry (NO records, compatible with older language level)
    // -------------------------

    private static final class CacheEntry<T> {
        private final CompletableFuture<T> future;
        private final long expiresAtMs;

        private CacheEntry(CompletableFuture<T> future, long expiresAtMs) {
            this.future = future;
            this.expiresAtMs = expiresAtMs;
        }

        private CompletableFuture<T> getFuture() {
            return future;
        }

        private long getExpiresAtMs() {
            return expiresAtMs;
        }
    }

    // -------------------------
    // DTO
    // -------------------------

    private static final class FriendDto {
        String uuid;
        String name;
    }

    // -------------------------
    // util
    // -------------------------

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String encodePath(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeQuery(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String requireServer(String serverDomain) {
        String s = Objects.requireNonNull(serverDomain, "serverDomain").trim();
        if (s.isEmpty()) throw new IllegalArgumentException("serverDomain is empty");
        return s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    private interface Supplier<T> {
        T get();
    }
}
