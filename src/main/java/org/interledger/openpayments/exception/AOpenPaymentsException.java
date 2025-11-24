package org.interledger.openpayments.exception;

import lombok.Getter;

/**
 * AOpenPaymentsException is an abstract base class for exceptions specific to the Open Payments
 * domain. It extends RuntimeException and serves as the parent class for custom exceptions related
 * to Open Payments. This class provides a mechanism to associate a specific error code with the
 * exception, which can help in categorizing and handling errors effectively. It also defines common
 * error codes as constants that can be reused across subclasses. Error codes: - INTERRUPTED (-101):
 * Indicates that a thread was interrupted during execution. - IO_ERROR (-100): Signifies an
 * input/output-related error. - BAD_REQUEST (400): Represents an invalid request sent by a client.
 * - UNAUTHORIZED (401): Indicates authentication failure or missing credentials. Subclasses can
 * inherit and extend this base class to define more specific exception behavior, while still
 * conforming to a common structure for error handling.
 */
@Getter
public abstract class AOpenPaymentsException extends RuntimeException {
    private final int code;

    /**
     * The Code class defines a collection of constant error codes that represent various error
     * states within the Open Payments domain. These constants are used to classify and
     * differentiate types of errors in the exception-handling mechanism of the system. Error codes
     * defined: - {@link #INTERRUPTED}: Represents a thread interruption error (-101). - {@link
     * #IO_ERROR}: Represents an input/output-related error (-100). - {@link #BAD_REQUEST}:
     * Indicates a bad request error (400). This class is intended to provide a centralized set of
     * error codes for use in exception classes and other error-handling contexts.
     */
    public static final class Code {
        public static final int INTERRUPTED = -101;
        public static final int IO_ERROR = -100;
        public static final int BAD_REQUEST = 400;
        public static final int PRE_CONDITION_FAILED = 412;
    }

    /**
     * Constructs a new AOpenPaymentsException with the specified error code, message, and cause.
     *
     * @param code the error code associated with the exception
     * @param message the detailed message explaining the reason for the exception
     * @param cause the underlying cause of the exception, which can be null
     */
    public AOpenPaymentsException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Constructs a new AOpenPaymentsException with the specified error code and message.
     *
     * @param code the error code associated with the exception
     * @param message the detail message explaining the reason for the exception
     */
    public AOpenPaymentsException(int code, String message) {
        this(code, message, null);
    }
}
