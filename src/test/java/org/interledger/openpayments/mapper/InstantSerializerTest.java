package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InstantSerializer}.
 *
 * <p>Verifies correct JSON serialization behavior when serializing {@link Instant} values using
 * Jackson:
 *
 * <ul>
 *   <li>Serializes a non-null {@link Instant} as an ISO-8601 string
 *   <li>Writes {@code null} when the input value is {@code null}
 * </ul>
 */
@DisplayName("InstantSerializer → serializes Instant as ISO-8601 (nanos)")
public class InstantSerializerTest {

    private final InstantSerializer serializer = new InstantSerializer();

    @Test
    void shouldWriteInstantWhenIsNotNull() throws IOException {
        var instant = Instant.parse("2025-12-31T23:59:59.999999999Z");
        var gen = mock(JsonGenerator.class);
        var provider = mock(SerializerProvider.class);

        serializer.serialize(instant, gen, provider);

        verify(gen).writeString("2025-12-31T23:59:59.999999999Z");
        verifyNoMoreInteractions(gen);
    }

    @Test
    void shouldWriteNullWhenValueIsNull() throws IOException {
        var gen = mock(JsonGenerator.class);
        var provider = mock(SerializerProvider.class);

        serializer.serialize(null, gen, provider);

        verify(gen).writeNull();
        verifyNoMoreInteractions(gen);
    }
}
