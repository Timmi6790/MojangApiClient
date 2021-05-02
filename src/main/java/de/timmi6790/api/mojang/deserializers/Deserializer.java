package de.timmi6790.api.mojang.deserializers;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;

public abstract class Deserializer<T> extends JsonAdapter<T> {
    @Override
    public void toJson(final JsonWriter writer, final T value) {
        throw new UnsupportedOperationException();
    }
}
