package org.interledger.openpayments.exception;

import org.interledger.openpayments.model.common.ApiError;
import org.junit.jupiter.api.Test;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import static org.interledger.openpayments.util.testing.TestResponse.createResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OpenPaymentsAPIExceptionTest {

    private final HttpHeaders headers = createHeaders();

    @Test
    void shouldCreateExceptionWithCodeAndMessage() {
        var message = "I'm a teapot";
        var code = 418;
        var exception = new OpenPaymentsAPIException(code, message);
        assertEquals(message, exception.getMessage());
        assertEquals(code, exception.getCode());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithAllParams() {
        var message = "I'm a teapot";
        var code = 418;
        var body = "response body";
        var exception = new OpenPaymentsAPIException(code, message, headers, body);
        assertEquals(message, exception.getMessage());
        assertEquals(code, exception.getCode());
        assertEquals(headers, exception.getResponseHeaders());
        assertEquals(body, exception.getResponseBody());
    }

    @Test
    void shouldGetApiException() {
        var response = createResponse("body", 404, headers);
        var apiError = createApiError("404", "Not found");
        var exception = OpenPaymentsAPIException.getApiException(apiError, response);
        assertEquals("body", exception.getResponseBody());
        assertEquals(headers, exception.getResponseHeaders());
    }

    @Test
    void shouldGetApiExceptionWhenBodyIsNull() {
        var response = createResponse(null, 404);
        var apiError = createApiError("404", "Not found");
        var exception = OpenPaymentsAPIException.getApiException(apiError, response);
        assertEquals("[no body]", exception.getResponseBody());
    }

    @Test
    void shouldGetApiExceptionWhenApiCodeAndDescriptionArePresent() {
        var response = createResponse(null, 404);
        var apiError = createApiError("404", "Not found");
        var exception = OpenPaymentsAPIException.getApiException(apiError, response);
        assertEquals("[404] (404) Not found", exception.getMessage());
    }

    @Test
    void shouldGetApiExceptionWhenApiCodeAndDescriptionAreNull() {
        var response = createResponse(null, 404);
        var apiError = createApiError(null, null);
        var exception = OpenPaymentsAPIException.getApiException(apiError, response);
        assertEquals("[404] (>no error code<) >no error description<", exception.getMessage());
    }

    ApiError createApiError(String code, String description) {
        ApiError apiError = new ApiError();
        apiError.setDescription(description);
        apiError.setCode(code);
        return apiError;
    }

    HttpHeaders createHeaders() {
        return HttpHeaders.of(
                Map.of("Accept", List.of("application/json")),
                (a, b) -> true);
    }
}
