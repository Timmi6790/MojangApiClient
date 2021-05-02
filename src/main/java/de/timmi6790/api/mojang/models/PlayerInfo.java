package de.timmi6790.api.mojang.models;

import lombok.Data;

import java.util.UUID;

@Data
public class PlayerInfo {
    private final UUID uuid;
    private final String name;
}
