package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * A custom serializer for the {@link Instant} class that serializes an {@link Instant} object
 * into a JSON string using the ISO-8601 format.
 *
 * This serializer is designed to work with JSON serialization frameworks such as Jackson. It
 * converts the {@link Instant} object to its ISO-8601 string representation and writes it as
 * a JSON string. If the {@link Instant} value is null, a JSON null value will be written.
 *
 * Note that this serializer only handles the serialization of {@link Instant} objects. Any
 * deserialization logic, if required, must be implemented separately.
 */
public class InstantSerializer extends JsonSerializer<Instant> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Serializes an {@link Instant} object into a JSON string using the ISO-8601 format.
     *
     * @param value The {@link Instant} object to be serialized. Can be null, in which case a JSON null value will be written.
     * @param gen The {@link JsonGenerator} used to write the serialized JSON output.
     * @param serializers The {@link SerializerProvider} that can provide additional serializers if necessary.
     * @throws IOException If an I/O error occurs while writing the JSON output.
     */
    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(FORMATTER.format(value));
        }
    }
}