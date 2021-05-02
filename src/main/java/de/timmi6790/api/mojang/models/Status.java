package de.timmi6790.api.mojang.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum Status {
    GREEN("No issues"),
    YELLOW("Some issues"),
    RED("Service unavailable");

    private final String description;

    public static Optional<Status> ofName(final String name) {
        for (final Status status : Status.values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
