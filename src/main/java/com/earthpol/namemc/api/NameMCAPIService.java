package com.earthpol.namemc.api;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NameMCAPIService {

    CompletableFuture<List<NameMCFriend>> getFriends(UUID profileUuid, boolean useCache);

    CompletableFuture<Boolean> areFriends(UUID profileUuid, UUID withUuid, boolean useCache);

    CompletableFuture<Integer> getServerLikes(String serverDomain, boolean useCache);

    CompletableFuture<Boolean> hasProfileLikedServer(String serverDomain, UUID profileUuid, boolean useCache);
}