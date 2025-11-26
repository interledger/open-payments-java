package org.interledger.openpayments.exception;

import static org.interledger.openpayments.util.testing.TestResponse.createResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import org.interledger.openpayments.model.common.ApiError;
import org.interledger.openpayments.util.testing.TestResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OpenPaymentsAPIException}.
 *
 * <p>Verifies correct construction of the exception in various scenarios, including: creation with
 * status code and message, full response data (headers/body), and proper handling of {@link
 * ApiError} objects (with/without code and description). Also ensures fallback behavior when
 * response body or error details are missing.
 *
 * <p>Uses {@link TestResponse} and mock {@link ApiError} instances to simulate real API error
 * responses.
 */
@DisplayName("OpenPaymentsAPIException → API error responses (4xx/5xx)")
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
    return HttpHeaders.of(Map.of("Accept", List.of("application/json")), (a, b) -> true);
  }
}
