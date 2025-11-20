package org.interledger.openpayments.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
