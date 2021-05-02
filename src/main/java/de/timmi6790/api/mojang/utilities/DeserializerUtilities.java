package de.timmi6790.api.mojang.utilities;

import lombok.experimental.UtilityClass;

import java.util.UUID;
import java.util.regex.Pattern;

@UtilityClass
public class DeserializerUtilities {
    private static final Pattern FULL_UUID_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    public UUID convertMojangUUID(final String mojangUUID) {
        final String uuidString = FULL_UUID_PATTERN.matcher(mojangUUID).replaceAll("$1-$2-$3-$4-$5");
        return UUID.fromString(uuidString);
    }
}
