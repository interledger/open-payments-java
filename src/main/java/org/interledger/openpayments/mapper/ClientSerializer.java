package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.interledger.openpayments.model.grant.Client;

import java.io.IOException;

/**
 * A custom serializer for the {@link Client} class that serializes the {@code paymentPointer}
 * property of the client object as a string.
 *
 * <p>This serializer is designed to be used with JSON serialization frameworks like Jackson.
 * It converts the {@link java.net.URI} representation of the {@code paymentPointer} into its string form
 * and writes it as a JSON string.
 *
 * <p>Note that this serializer only handles the serialization of the {@link Client} object;
 * deserialization logic, if needed, must be implemented separately.
 */
public class ClientSerializer extends JsonSerializer<Client> {

    /**
     * Serializes a {@link Client} object by writing its {@code paymentPointer} property as a JSON string.
     *
     * @param client The {@link Client} object to be serialized. Must not be null.
     * @param gen The {@link JsonGenerator} used to write the serialized JSON output.
     * @param serializers The {@link SerializerProvider} that can provide additional serializers if necessary.
     * @throws IOException If an I/O error occurs while writing the JSON output.
     */
    @Override
    public void serialize(Client client, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (client == null || client.getPaymentPointer() == null) {
            gen.writeNull();
        } else {
            gen.writeString(client.getPaymentPointer().toString());
        }
    }
}
