package org.interledger.openpayments.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.interledger.openpayments.exception.AOpenPaymentsException;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.model.common.ApiError;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @Nested
    class WriteValueAsString {
        @Test
        void shouldWriteObjectAsString() throws JsonProcessingException {
            OpenPaymentsObjectMapper opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            String expectedJson = "123";
            when(mapper.writeValueAsString(123)).thenReturn(expectedJson);

            String json = opMapper.writeValueAsString(123);
            assertEquals(expectedJson, json);
        }

        @Test
        void shouldThrowOpenPaymentsClientExceptionWhenCannotSerialize() throws JsonProcessingException {
            OpenPaymentsObjectMapper opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            String errorMessage = "Something went wrong";
            JsonProcessingException mapperException = new JsonProcessingException(errorMessage){};
            when(mapper.writeValueAsString(any())).thenThrow(mapperException);


            OpenPaymentsClientException ex = assertThrows(OpenPaymentsClientException.class,
                    () -> opMapper.writeValueAsString("test-value"));

            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, ex.getCode());
            assertEquals(mapperException, ex.getCause());
            assertEquals("(JSON) Failed to serialize value: 'test-value' to JSON.", ex.getMessage());
        }
    }

    @Nested
    class ReadValue {
        @Test
        void shouldReadValue() throws JsonProcessingException {
            OpenPaymentsObjectMapper opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            when(mapper.readValue("123", Integer.class)).thenReturn(123);

            Integer json = opMapper.readValue("123", Integer.class);
            assertEquals(123, json);
        }

        @Test
        void shouldThrowOpenPaymentsClientExceptionWhenCannotRead() throws JsonProcessingException {
            OpenPaymentsObjectMapper opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            String errorMessage = "Something went wrong";
            JsonProcessingException mapperException = new JsonProcessingException(errorMessage){};
            when(mapper.readValue("123", Integer.class)).thenThrow(mapperException);


            OpenPaymentsClientException ex = assertThrows(OpenPaymentsClientException.class,
                    () -> opMapper.readValue("123", Integer.class));

            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, ex.getCode());
            assertEquals(mapperException, ex.getCause());
            assertEquals("Failed to deserialize response to: 'java.lang.Integer'.", ex.getMessage());
        }
    }

    @Nested
    class ReadError {
        @Test
        void shouldWriteObjectAsString() throws JsonProcessingException {
            OpenPaymentsObjectMapper opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            ApiError expectedError = createApiError();
            when(unwrapMapper.readValue("Content", ApiError.class)).thenReturn(expectedError);
            ApiError apiError = opMapper.readError("Content", 400);
            assertEquals(expectedError, apiError);
        }

        @Test
        void shouldThrowOpenPaymentsClientExceptionWhenCannotReadError() throws JsonProcessingException {
            OpenPaymentsObjectMapper opMapper = new OpenPaymentsObjectMapper(mapper, unwrapMapper);
            JsonProcessingException mapperException = new JsonProcessingException("Something went wrong"){};
            when(unwrapMapper.readValue("Content", ApiError.class)).thenThrow(mapperException);

            OpenPaymentsClientException ex = assertThrows(OpenPaymentsClientException.class,
                    () -> opMapper.readError("Content", 400));

            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, ex.getCode());
            assertEquals(mapperException, ex.getCause());
            assertEquals("Failed to deserialize response to ApiError: 'Content'.", ex.getMessage());
        }

        private static ApiError createApiError() {
            ApiError apiError = new ApiError();
            apiError.setCode("404");
            apiError.setDescription("description");
            return apiError;
        }
    }
}
