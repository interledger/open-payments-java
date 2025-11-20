package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Set;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OrderedSetSerializerTest {
    @Mock
    private JsonGenerator gen;
    @Mock
    private SerializerProvider provider;

    @Test
    void shouldSerializeSetInLexicographicalOrder() throws IOException {
        OrderedSetSerializer<String> serializer = new OrderedSetSerializer<>();

        Set<String> input = Set.of("banana", "apple", "cherry");

        serializer.serialize(input, gen, provider);

        verify(gen).writeStartArray();
        verify(gen).writeString("apple");
        verify(gen).writeString("banana");
        verify(gen).writeString("cherry");
        verify(gen).writeEndArray();
    }

    @Test
    void shouldSerializeEmptySet() throws IOException {
        OrderedSetSerializer<String> serializer = new OrderedSetSerializer<>();

        Set<String> input = Set.of();

        serializer.serialize(input, gen, provider);
        verify(gen).writeStartArray();
        verify(gen).writeEndArray();
    }
}
