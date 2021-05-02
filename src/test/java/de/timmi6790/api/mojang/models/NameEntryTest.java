package de.timmi6790.api.mojang.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NameEntryTest {
    @Test
    void getFormattedTime() {
        final LocalDateTime time = LocalDateTime.of(2020, 12, 20, 23, 10, 50);
        final String expectedResponse = "12/20/2020 23:10:50 GMT";

        final NameEntry nameEntry = new NameEntry("Test", time);
        assertThat(nameEntry.getFormattedTime()).isEqualTo(expectedResponse);
    }

    @Test
    void getFormattedTime_original() {
        final NameEntry nameEntry = new NameEntry("Test", LocalDateTime.MIN);
        assertThat(nameEntry.getFormattedTime()).isEqualTo("Original");
    }
}