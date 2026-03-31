package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.interledger.openpayments.exception.AOpenPaymentsException;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.model.common.ApiError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OpenPaymentsObjectMapper}.
 *
 * <p>Verifies that:
 *
 * <ul>
 *   <li>The singleton instance is correctly configured (e.g., supports {@link Instant}, ordered map
 *       keys, ignores unknown properties)
 *   <li>Serialization ({@code writeValueAsString}) and deserialization ({@code readValue}) delegate
 *       properly to the underlying {@link ObjectMapper}
 *   <li>Error deserialization via {@code readError} uses the dedicated unwrapping mapper
 *   <li>All JSON processing failures are correctly wrapped into {@link OpenPaymentsClientException}
 *       with appropriate error messages, {@link AOpenPaymentsException.Code#BAD_REQUEST} status,
 *       and original cause preserved
 * </ul>
 *
 * <p>Uses Mockito to mock Jackson's {@link ObjectMapper} instances and test error translation
 * behavior.
 */
@DisplayName("OpenPaymentsObjectMapper → configured Jackson + safe (de)serialization")
@ExtendWith(MockitoExtension.class)
public class OpenPaymentsObjectMapperTest {

    @Mock
    private ObjectMapper mapper;
    @Mock
    private ObjectMapper unwrapMapper;

    @Test
    void shouldCreateObjectMapper() {
        ObjectMapper objectMapper = OpenPaymentsObjectMapper.get();
        assertTrue(objectMapper.canSerialize(Instant.class));
        assertTrue(objectMapper.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        assertFalse(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @DisplayName("writeValueAsString(...) - serialization and safely wrapped failures")
    @Nested
    class WriteValueAsString {
        @Test
        void shouldWriteObjectAsString() throws JsonProcessingException {
            var opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            var expectedJson = "123";
            when(mapper.writeValueAsString(123)).thenReturn(expectedJson);

            var json = opMapper.writeValueAsString(123);
            assertEquals(expectedJson, json);
        }

        @Test
        void shouldThrowOpenPaymentsClientExceptionWhenCannotSerialize()
            throws JsonProcessingException {
            var opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            var errorMessage = "Something went wrong";
            var mapperException = new JsonProcessingException(errorMessage) {
            };
            when(mapper.writeValueAsString(any())).thenThrow(mapperException);

            var ex =
                assertThrows(
                    OpenPaymentsClientException.class, () -> opMapper.writeValueAsString("test-value"));

            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, ex.getCode());
            assertEquals(mapperException, ex.getCause());
            assertEquals("(JSON) Failed to serialize value: 'test-value' to JSON.", ex.getMessage());
        }
    }

    @DisplayName("readValue(...) - deserialization and safely wrapped failures")
    @Nested
    class ReadValue {
        @Test
        void shouldReadValue() throws JsonProcessingException {
            var opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            when(mapper.readValue("123", Integer.class)).thenReturn(123);

            var json = opMapper.readValue("123", Integer.class);
            assertEquals(123, json);
        }

        @Test
        void shouldThrowOpenPaymentsClientExceptionWhenCannotRead() throws JsonProcessingException {
            var opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            var errorMessage = "Something went wrong";
            var mapperException = new JsonProcessingException(errorMessage) {
            };
            when(mapper.readValue("123", Integer.class)).thenThrow(mapperException);

            var ex =
                assertThrows(
                    OpenPaymentsClientException.class, () -> opMapper.readValue("123", Integer.class));

            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, ex.getCode());
            assertEquals(mapperException, ex.getCause());
            assertEquals("Failed to deserialize response to: 'java.lang.Integer'.", ex.getMessage());
        }
    }

    @DisplayName("readError(...) - ApiError deserialization and wrapped failures")
    @Nested
    class ReadError {
        @Test
        void shouldWriteObjectAsString() throws JsonProcessingException {
            var opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            var expectedError = createApiError();
            when(unwrapMapper.readValue("Content", ApiError.class)).thenReturn(expectedError);
            var apiError = opMapper.readError("Content", 400);
            assertEquals(expectedError, apiError);
        }

        @Test
        void shouldThrowOpenPaymentsClientExceptionWhenCannotReadError()
            throws JsonProcessingException {
            var opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            var mapperException = new JsonProcessingException("Something went wrong") {
            };
            when(unwrapMapper.readValue("Content", ApiError.class)).thenThrow(mapperException);

            var ex =
                assertThrows(OpenPaymentsClientException.class, () -> opMapper.readError("Content", 400));

            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, ex.getCode());
            assertEquals(mapperException, ex.getCause());
            assertEquals("Failed to deserialize response to ApiError: 'Content'.", ex.getMessage());
        }

        private static ApiError createApiError() {
            var apiError = new ApiError();
            apiError.setCode("404");
            apiError.setDescription("description");
            return apiError;
        }
    }
}
