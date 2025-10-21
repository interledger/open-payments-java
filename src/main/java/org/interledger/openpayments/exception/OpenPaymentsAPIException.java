package org.interledger.openpayments.exception;

import lombok.Getter;
import org.interledger.openpayments.model.ApiError;

import java.io.Serial;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;

/**
 * OpenPaymentsAPIException is a custom exception class that represents errors specific to
 * the Open Payments API. It extends AOpenPaymentsException and includes additional details
 * such as HTTP response headers and body for enhanced error context.
 * This exception can be used to handle and convey information about errors encountered when
 * interacting with the Open Payments API, such as response status codes, error messages,
 * headers, and response body content.
 */
@Getter
public class OpenPaymentsAPIException extends AOpenPaymentsException {
    @Serial
    private static final long serialVersionUID = 1L;
    private HttpHeaders responseHeaders = null;
    private String responseBody = null;

    /**
     * Constructs a new OpenPaymentsAPIException with the specified error message, cause, HTTP status code,
     * response headers, and response body.
     * @param message         the error message describing the exception
     * @param throwable       the underlying cause of the exception
     * @param code            the HTTP status code associated with the exception
     * @param responseHeaders the HTTP response headers returned by the server
     * @param responseBody    the HTTP response body as a string
     */
    public OpenPaymentsAPIException(
            String message,
            Throwable throwable,
            int code,
            HttpHeaders responseHeaders,
            String responseBody
    ) {
        super(code, message, throwable);
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    /**
     * Constructs a new OpenPaymentsAPIException with the specified error message, HTTP status code,
     * response headers, and response body.
     *
     * @param message         the error message describing the exception
     * @param code            the HTTP status code associated with the exception
     * @param responseHeaders the HTTP response headers returned by the server
     * @param responseBody    the HTTP response body as a string
     */
    public OpenPaymentsAPIException(
            String message,
            int code,
            HttpHeaders responseHeaders,
            String responseBody
    ) {
        this(message, null, code, responseHeaders, responseBody);
    }

    /**
     * Constructs a new OpenPaymentsAPIException with the specified error message, cause, HTTP status code,
     * and response headers.
     *
     * @param message         the error message describing the exception
     * @param throwable       the underlying cause of the exception
     * @param code            the HTTP status code associated with the exception
     * @param responseHeaders the HTTP response headers returned by the server
     */
    public OpenPaymentsAPIException(String message, Throwable throwable, int code, HttpHeaders responseHeaders) {
        this(message, throwable, code, responseHeaders, null);
    }

    /**
     * Constructs a new OpenPaymentsAPIException with the specified HTTP status code,
     * response headers, and response body.
     *
     * @param code            the HTTP status code associated with this exception
     * @param responseHeaders the HTTP response headers returned by the server
     * @param responseBody    the HTTP response body as a string
     */
    public OpenPaymentsAPIException(int code, HttpHeaders responseHeaders, String responseBody) {
        this(null, null, code, responseHeaders, responseBody);
    }

    public OpenPaymentsAPIException(int code, String message) {
        super(code, message);
    }

    public OpenPaymentsAPIException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Constructs a new InterledgerClientException with an HTTP status code, error message,
     * response headers, and response body.
     *
     * @param code            the HTTP status code associated with this exception
     * @param message         the error message describing the exception
     * @param responseHeaders the HTTP response headers returned by the server
     * @param responseBody    the HTTP response body as a string
     */
    public OpenPaymentsAPIException(
            int code,
            String message,
            HttpHeaders responseHeaders,
            String responseBody
    ) {
        this(code, message);
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    /**
     * Constructs an {@code OpenPaymentsAPIException} based on the provided {@code ApiError} object
     * and the {@code HttpResponse} received.
     * The method formats a detailed exception message using the error details and the HTTP status code 
     * and retrieves the response body. If the body is null, it substitutes with a placeholder string.
     * @param error    the {@code ApiError} object containing the error data, such as a code and description
     * @param response the {@code HttpResponse} object containing the status code, headers, and body of the server response
     * @return an instance of {@code OpenPaymentsAPIException} containing the constructed exception details
     */
    public static OpenPaymentsAPIException getApiException(ApiError error, HttpResponse<String> response) {
        final String message = formatExceptionMessage(error, response.statusCode());
        final String body;
        if (response.body() == null) {
            body = "[no body]";
        } else {
            body = response.body();
        }

        return new OpenPaymentsAPIException(response.statusCode(), message, response.headers(), body);
    }

    /**
     * Formats an exception message based on the provided {@code ApiError} object and HTTP status code.
     * If the {@code ApiError} properties (code or description) are null or blank, default placeholders are used.
     *
     * @param error      the {@code ApiError} object containing the error code and description
     * @param statusCode the HTTP status code associated with the error
     * @return a formatted string representing the exception message in the format:
     * "[{statusCode}] ({code}) {description}"
     */
    private static String formatExceptionMessage(ApiError error, int statusCode) {
        final String code = error.code == null || error.code.isBlank() ? ">no error code<" : error.code;
        final String description = error.description == null || error.description.isBlank() ? ">no error description<" : error.description;

        return "[" + statusCode + "] (" + code + ") " + description;
    }
}
