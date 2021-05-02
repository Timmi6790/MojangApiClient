package de.timmi6790.api.mojang.deserializers;

import com.squareup.moshi.JsonReader;
import de.timmi6790.api.mojang.models.PlayerInfo;
import de.timmi6790.api.mojang.utilities.DeserializerUtilities;

import java.io.IOException;
import java.util.UUID;

public class PlayerInfoDeserializer extends Deserializer<PlayerInfo> {
    @Override
    public PlayerInfo fromJson(final JsonReader reader) throws IOException {
        String playerName = null;
        UUID playerUUID = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "name":
                    playerName = reader.nextString();
                    break;
                case "id":
                    playerUUID = DeserializerUtilities.convertMojangUUID(reader.nextString());
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (playerName == null || playerUUID == null) {
            throw new IOException("PlayerName or PlayerUUID not found.");
        }
        return new PlayerInfo(
                playerUUID,
                playerName
        );
    }
}
