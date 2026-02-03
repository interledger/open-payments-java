package org.interledger.openpayments.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link OpenPaymentsClientException}.
 *
 * <p>Verifies correct construction of client-side exceptions with:
 *
 * <ul>
 *   <li>message only (defaults to 400)
 *   <li>explicit status code and message
 *   <li>inclusion of a cause throwable
 * </ul>
 *
 * <p>Ensures proper initialization of message, HTTP status code, and cause in all scenarios.
 */
@DisplayName("OpenPaymentsClientException → client-side errors & request validation")
public class OpenPaymentsClientExceptionTest {
    @Test
    void shouldCreateExceptionWithMessage() {
        var message = "Something went wrong";
        var exception = new OpenPaymentsClientException(message);
        assertEquals(message, exception.getMessage());
        assertEquals(400, exception.getCode());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithCodeAndMessage() {
        var message = "I'm a teapot";
        var code = 418;
        var exception = new OpenPaymentsClientException(code, message);
        assertEquals(message, exception.getMessage());
        assertEquals(code, exception.getCode());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithCodeMessageAndCause() {
        var message = "I'm a teapot";
        var code = 418;
        var cause = new IllegalStateException();
        var exception = new OpenPaymentsClientException(code, message, cause);
        assertEquals(message, exception.getMessage());
        assertEquals(code, exception.getCode());
        assertEquals(cause, exception.getCause());
    }
}
