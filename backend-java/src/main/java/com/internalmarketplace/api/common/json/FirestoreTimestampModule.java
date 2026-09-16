package com.internalmarketplace.api.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.cloud.Timestamp;

import java.io.IOException;

/**
 * Reproduces the exact wire shape the Node backend already emits for Firestore
 * timestamps. {@code @google-cloud/firestore}'s Timestamp serializes over
 * JSON.stringify as {@code {"_seconds": N, "_nanoseconds": N}} (own instance
 * fields, no toJSON override) and the frontend's formatDate() helper
 * (frontend/src/components/ui.jsx) branches explicitly on
 * {@code value._seconds}. Without this module, Jackson would instead try to
 * reflect Timestamp's getters/serialize it as an unrelated shape and silently
 * break every rendered date in the UI.
 */
public class FirestoreTimestampModule extends SimpleModule {

    public FirestoreTimestampModule() {
        super("FirestoreTimestampModule");
        addSerializer(Timestamp.class, new TimestampSerializer());
    }

    private static final class TimestampSerializer extends JsonSerializer<Timestamp> {
        @Override
        public void serialize(Timestamp value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("_seconds", value.getSeconds());
            gen.writeNumberField("_nanoseconds", value.getNanos());
            gen.writeEndObject();
        }
    }
}
