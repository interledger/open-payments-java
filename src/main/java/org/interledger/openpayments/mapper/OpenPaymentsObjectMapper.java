
package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.interledger.openpayments.exception.AOpenPaymentsException;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.model.common.ApiError;

import java.time.Instant;

/**
 * Provides customized {@link ObjectMapper} instances tailored for serialization
 * and deserialization in the Open Payments domain. It offers functionality to handle
 * specific JSON structures and custom data formats, ensuring consistent behavior.
 * This class uses two {@link ObjectMapper} configurations:
 * - A general-purpose mapper for standard JSON processing.
 * - A specialized unwrapping mapper for handling root value unwrapping during deserialization.
 * It includes functionality to register custom serializers and deserializers, such as for
 * {@link Instant}, thereby enabling precise handling of such types in JSON payloads.
 */
@Slf4j
public class OpenPaymentsObjectMapper {
    private final ObjectMapper mapper;
    private final ObjectMapper unwrapMapper;

    /**
     * Constructs a new instance of OpenPaymentsObjectMapper.
     * This constructor initializes two ObjectMapper instances:
     * - A standard mapper configured to serialize and deserialize objects
     *   with specific features like ordering map entries by keys,
     *   handling {@link Instant} serialization/deserialization,
     *   disabling failure on unknown properties, and excluding null fields from serialization.
     * - An unwrapMapper configured specifically to enable root value unwrapping during deserialization.
     */
    public OpenPaymentsObjectMapper() {
        this(get(), getUnwrapMapper());
    }

    // Added for testing
    OpenPaymentsObjectMapper(ObjectMapper mapper, ObjectMapper unwrapMapper) {
        this.mapper = mapper;
        this.unwrapMapper = unwrapMapper;
    }

    /**
     * Returns a pre-configured instance of {@link ObjectMapper} with specific serialization and
     * deserialization settings. This ObjectMapper is configured to:
     * - Ensure map fields are serialized in a consistent order.
     * - Support serialization and deserialization of {@link Instant} using custom modules.
     * - Ignore unknown properties during deserialization.
     * - Exclude null fields from serialization.
     *
     * @return an instance of {@link ObjectMapper} configured with specific serialization and
     * deserialization features.
     */
    public static ObjectMapper get() {
        ObjectMapper mapper = new ObjectMapper();

        // make sure map fields are always in the same order
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

        // register serializer/deserializer for java.time.Instant
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new InstantSerializer());
        module.addDeserializer(Instant.class, InstantDeserializer.INSTANT);

        mapper.registerModule(module);

        // don't fail on unknown fields
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * Creates and returns a pre-configured instance of {@link ObjectMapper} that is set to enable
     * root value unwrapping during deserialization. This configuration allows the mapper to
     * process JSON structures where the root name needs to be unwrapped to access the actual content.
     *
     * @return an instance of {@link ObjectMapper} configured to enable root value unwrapping.
     */
    private static ObjectMapper getUnwrapMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);  // Enable root value handling
        return mapper;
    }

    /**
     * Serializes the provided object into a JSON string representation.
     * This method uses an underlying {@link ObjectMapper} to perform the serialization.
     * If the serialization process fails, it throws a custom {@link OpenPaymentsClientException}.
     *
     * @param value the object to be serialized into a JSON string
     * @return the JSON string representation of the provided object
     * @throws OpenPaymentsClientException if serialization fails due to any JSON processing errors
     */
    public String writeValueAsString(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException err) {
            throw new OpenPaymentsClientException(
                    AOpenPaymentsException.Code.BAD_REQUEST,
                    String.format("(JSON) Failed to serialize value: '%s' to JSON.", value),
                    err
            );
        }
    }

    /**
     * Deserializes the provided JSON string into an instance of the specified class type.
     * This method uses an underlying {@code ObjectMapper} to perform the deserialization.
     * If deserialization fails, a custom {@code OpenPaymentsClientException} is thrown.
     *
     * @param content the JSON string to be deserialized
     * @param valueType the class type into which the JSON string should be deserialized
     * @param <T> the type of the resulting object
     * @return an instance of the specified class type deserialized from the provided JSON string
     * @throws OpenPaymentsClientException if deserialization fails due to invalid JSON structure or processing errors
     */
    public <T> T readValue(String content, Class<T> valueType)  {
        try {
            return mapper.readValue(content, valueType);
        } catch (JsonProcessingException err) {
            throw new OpenPaymentsClientException(
                    AOpenPaymentsException.Code.BAD_REQUEST,
                    String.format("Failed to deserialize response to: '%s'.", valueType.getName()),
                    err
            );
        }
    }

    /**
     * Reads the provided response content and attempts to deserialize it into an ApiError object.
     * This method handles errors related to the deserialization process and logs warnings
     * if the deserialization fails, throwing a custom exception in such cases.
     *
     * @param content the response content as a JSON string, expected to represent an ApiError object
     * @param httpResponseCode the HTTP response code associated with the response, used for logging purposes
     * @return an instance of ApiError deserialized from the provided content
     * @throws OpenPaymentsClientException if deserialization fails due to malformed JSON or unexpected structure
     */
    public ApiError readError(String content, int httpResponseCode) {
        try {
            return unwrapMapper.readValue(content, ApiError.class);
        } catch (JsonProcessingException err) {
            log.warn("Failed to deserialize response ({}): '{}' to: '{}'.",
                    httpResponseCode, content, ApiError.class.getName());

            throw new OpenPaymentsClientException(
                    AOpenPaymentsException.Code.BAD_REQUEST,
                    String.format("Failed to deserialize response to ApiError: '%s'.", content),
                    err
            );
        }
    }
}
