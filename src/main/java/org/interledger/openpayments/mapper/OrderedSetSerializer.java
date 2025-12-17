package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.Set;


/**
 * A custom serializer for {@link Set} that outputs the contents of the set
 * as a JSON array. The elements of the set are ordered lexicographically
 * based on their string representation.
 *
 * @param <T> The type of elements in the set.
 */
public class OrderedSetSerializer<T> extends JsonSerializer<Set<T>> {
    /**
     * Serializes a {@link Set} into a JSON array. The elements in the set are
     * serialized in lexicographical order based on their string representation.
     *
     * @param value The {@link Set} to be serialized. Should not be null.
     * @param gen The {@link JsonGenerator} used to write the JSON output.
     * @param serializers The {@link SerializerProvider} that can be used to get serializers for
     *                     serializing objects within the set.
     * @throws IOException If an I/O error occurs during serialization.
     */
    @Override
    public void serialize(Set<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        // order item by toString() value
        List<String> list = value.stream().map(Object::toString).sorted().toList();
        for (String item : list) gen.writeString(item);

        gen.writeEndArray();
    }
}
