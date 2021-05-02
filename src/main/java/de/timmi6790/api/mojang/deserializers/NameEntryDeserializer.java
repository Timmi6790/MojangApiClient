package de.timmi6790.api.mojang.deserializers;

import com.squareup.moshi.JsonReader;
import de.timmi6790.api.mojang.models.NameEntry;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class NameEntryDeserializer extends Deserializer<NameEntry> {
    @Override
    public NameEntry fromJson(final JsonReader reader) throws IOException {
        String playerName = null;
        LocalDateTime changedAt = LocalDateTime.MIN;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "name":
                    playerName = reader.nextString();
                    break;
                case "changedToAt":
                    changedAt = new Timestamp(reader.nextLong()).toLocalDateTime();
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (playerName == null) {
            throw new IOException("PlayerName not found.");
        }

        return new NameEntry(
                playerName,
                changedAt
        );
    }
}
