package de.timmi6790.api.mojang.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StatusTest {
    @ParameterizedTest
    @EnumSource(Status.class)
    void ofName(final Status status) {
        final Optional<Status> foundStatusOpt = Status.ofName(status.name());
        assertThat(foundStatusOpt)
                .isPresent()
                .contains(status);
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    void ofName_ignore_case(final Status status) {
        final Optional<Status> foundStatusUpperOpt = Status.ofName(status.name().toUpperCase());
        assertThat(foundStatusUpperOpt)
                .isPresent()
                .contains(status);

        final Optional<Status> foundStatusLowerOpt = Status.ofName(status.name().toLowerCase());
        assertThat(foundStatusLowerOpt)
                .isPresent()
                .contains(status);
    }

    @Test
    void ofName_incorrect() {
        final Optional<Status> statusNotFound = Status.ofName("1");
        assertThat(statusNotFound)
                .isNotPresent();
    }
}