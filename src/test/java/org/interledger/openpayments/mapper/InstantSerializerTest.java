package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.mockito.Mockito.*;

public class InstantSerializerTest {

    private final InstantSerializer serializer = new InstantSerializer();

    @Test
    void shouldWriteNullWhenValueIsNull() throws IOException {
        Instant instant = Instant.parse("2025-12-31T23:59:59.999999999Z");
        JsonGenerator gen = mock(JsonGenerator.class);
        SerializerProvider provider = mock(SerializerProvider.class);

        serializer.serialize(instant, gen, provider);

        verify(gen).writeString("2025-12-31T23:59:59.999999999Z");
        verifyNoMoreInteractions(gen);
    }

    @Test
    void shouldWriteInstantWhenIsNotNull() throws IOException {
        JsonGenerator gen = mock(JsonGenerator.class);
        SerializerProvider provider = mock(SerializerProvider.class);

        serializer.serialize(null, gen, provider);

        verify(gen).writeNull();
        verifyNoMoreInteractions(gen);
    }
}
