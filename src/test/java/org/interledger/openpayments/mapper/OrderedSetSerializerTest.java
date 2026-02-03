package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Set;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link OrderedSetSerializer}.
 *
 * <p>Verifies that sets are serialized as JSON arrays with elements in lexicographical (natural)
 * order, regardless of insertion order. This ensures consistent and predictable JSON output, which
 * is critical for signature verification in Open Payments.
 *
 * <p>Tested scenarios:
 *
 * <ul>
 *   <li>Non-empty sets are sorted correctly (e.g., ["apple", "banana", "cherry])
 *   <li>Empty sets produce valid empty JSON array {@code []}
 * </ul>
 *
 * <p>Uses Mockito to verify exact interaction order with {@link JsonGenerator}.
 */
@DisplayName("OrderedSetSerializer → lexicographic order for signing")
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
