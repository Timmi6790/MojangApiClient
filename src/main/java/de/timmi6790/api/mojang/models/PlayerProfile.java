package de.timmi6790.api.mojang.models;

import java.util.UUID;

public class PlayerProfile extends PlayerInfo {
    public PlayerProfile(final UUID uuid, final String name) {
        super(uuid, name);
    }
}
