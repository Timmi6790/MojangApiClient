package de.timmi6790.api.mojang.deserializers;

import com.squareup.moshi.JsonReader;
import de.timmi6790.api.mojang.models.Status;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StatusMapDeserializer extends Deserializer<Map<String, Status>> {
    @Override
    public Map<String, Status> fromJson(final JsonReader reader) throws IOException {
        final Map<String, Status> statusMap = new HashMap<>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                final String serviceUrl = reader.nextName();
                final String statusName = reader.nextString();

                Status.ofName(statusName).ifPresent(status -> statusMap.put(serviceUrl, status));
            }
            reader.endObject();
        }
        reader.endArray();

        return statusMap;
    }
}
