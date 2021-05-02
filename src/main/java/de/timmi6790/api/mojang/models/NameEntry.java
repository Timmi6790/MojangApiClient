package de.timmi6790.api.mojang.models;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Data
public class NameEntry {
    private final String name;
    private final LocalDateTime changedAt;

    public boolean isOriginalName() {
        return this.changedAt == LocalDateTime.MIN;
    }

    public String getFormattedTime() {
        if (this.isOriginalName()) {
            return "Original";
        }

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss O");
        return this.changedAt.atZone(ZoneOffset.UTC).format(formatter);
    }
}
