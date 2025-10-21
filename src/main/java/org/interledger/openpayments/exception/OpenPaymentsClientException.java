package org.interledger.openpayments.exception;

import lombok.Getter;

import java.io.Serial;
import java.net.http.HttpHeaders;

/**
 * OpenPaymentsClientException is a custom exception class that extends AOpenPaymentsException.
 * It represents client-related exceptions in the Open Payments domain, typically used when
 * there are bad requests or invalid inputs from the client side.
 * The exception supports additional context such as HTTP response headers and response body,
 * which can be helpful for debugging and tracking the details of the error.
 */
@Getter
public class OpenPaymentsClientException extends AOpenPaymentsException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Represents the HTTP response headers associated with the exception.
     * These headers provide additional contextual information about the
     * HTTP response, such as metadata or status-related details, which can
     * be useful for debugging or analyzing the error.
     * The response headers are immutable and may be left null if not applicable.
     */
    private final HttpHeaders responseHeaders = null;
    /**
     * Represents the HTTP response body associated with the exception.
     * This variable contains the response payload returned from the server
     * during the error scenario, typically provided as a detailed message or
     * content explaining the nature of the error.
     * It is immutable and may be left null if not applicable or unavailable.
     */
    private final String responseBody = null;

    /**
     * Constructs a new OpenPaymentsClientException with the specified message.
     * This exception is typically used to indicate client-related errors, such as
     * invalid input or bad requests, in the Open Payments domain.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public OpenPaymentsClientException(String message) {
        super(Code.BAD_REQUEST, message);
    }

    /**
     * Constructs a new OpenPaymentsClientException with the specified error code and message.
     *
     * @param code    the error code associated with the client-related exception
     * @param message the detailed message explaining the reason for the exception
     */
    public OpenPaymentsClientException(int code, String message) {
        super(code, message);
    }

    /**
     * Constructs a new OpenPaymentsClientException with the specified error code, message, and cause.
     * This exception represents client-related errors such as bad requests or invalid inputs
     * in the Open Payments domain.
     *
     * @param code    the error code associated with the client-related exception
     * @param message the detailed message explaining the reason for the exception
     * @param cause   the underlying cause of the exception, which can be null
     */
    public OpenPaymentsClientException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
