package com.earthpol.namemc.api;

import java.util.UUID;

public final class NameMCFriend {
    private final UUID uuid;
    private final String name;

    public NameMCFriend(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
