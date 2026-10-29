package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.interledger.openpayments.model.grant.Client;

import java.io.IOException;

/**
 * A custom serializer for the {@link Client} class that writes the client in the format
 * selected by {@link Client#getFormat()}:
 * <ul>
 *   <li>{@link Client.Format#WALLET_ADDRESS} - {@code {"walletAddress": "<uri>"}}</li>
 *   <li>{@link Client.Format#DIRECTED_IDENTITY} - {@code {"jwk": {...}}}</li>
 *   <li>{@link Client.Format#WALLET_ADDRESS_STRING} - {@code "<uri>"} (deprecated)</li>
 * </ul>
 *
 * <p>Note that this serializer only handles the serialization of the {@link Client} object;
 * deserialization logic, if needed, must be implemented separately.
 */
public class ClientSerializer extends JsonSerializer<Client> {

    /**
     * Serializes a {@link Client} object according to its {@link Client.Format}.
     *
     * @param client      The {@link Client} object to be serialized.
     * @param gen         The {@link JsonGenerator} used to write the serialized JSON output.
     * @param serializers The {@link SerializerProvider} that can provide additional serializers if necessary.
     * @throws IOException If an I/O error occurs while writing the JSON output.
     */
    @Override
    public void serialize(Client client, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (client == null || client.getFormat() == null) {
            gen.writeNull();
            return;
        }

        switch (client.getFormat()) {
            case DIRECTED_IDENTITY -> {
                if (client.getJwk() == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeStartObject();
                serializers.defaultSerializeField("jwk", client.getJwk(), gen);
                gen.writeEndObject();
            }
            case WALLET_ADDRESS_STRING -> {
                if (client.getPaymentPointer() == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(client.getPaymentPointer().toString());
            }
            default -> {
                if (client.getPaymentPointer() == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeStartObject();
                gen.writeStringField("walletAddress", client.getPaymentPointer().toString());
                gen.writeEndObject();
            }
        }
    }
}
