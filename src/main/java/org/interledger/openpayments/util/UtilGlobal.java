package org.interledger.openpayments.util;

import lombok.NoArgsConstructor;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.exception.AOpenPaymentsException;

import java.net.URI;
import java.net.URISyntaxException;

import static java.util.Objects.isNull;

/**
 * Utility class providing static methods for string validation and condition enforcement.
 * This class includes methods to check for null or empty strings, convert strings to URI,
 * and enforce conditions by throwing custom exceptions.
 */
@NoArgsConstructor
public class UtilGlobal {

    /**
     * Checks if the given string is null or empty after trimming whitespace.
     *
     * @param str the string to check for null or emptiness
     * @return true if the string is null or empty after trimming, otherwise false
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Converts a given string representation of a URL into a URI object.
     * If the input string is null or empty, or if the string cannot be parsed
     * into a valid URI, an {@code OpenPaymentsClientException} will be thrown.
     *
     * @param url the string representation of the URL to be converted into a URI
     * @return a {@code URI} object representing the parsed URI
     * @throws OpenPaymentsClientException if the input is null, empty, or invalid as a URI
     */
    public static URI toUri(String url) {
        if (UtilGlobal.isNullOrEmpty(url)) {
            throw new OpenPaymentsClientException(
                    AOpenPaymentsException.Code.BAD_REQUEST, "URL cannot be null or empty!"
            );
        }
        try {
            return new URI(url);
        } catch (URISyntaxException err) {
            throw new OpenPaymentsClientException(
                    AOpenPaymentsException.Code.BAD_REQUEST, "URL [%s] invalid!".formatted(url), err
            );
        }
    }

    /**
     * Ensures that the provided string is neither null nor empty.
     * If the string is null or empty, an {@code OpenPaymentsClientException} is thrown with the specified error message.
     *
     * @param value the string to validate for null or emptiness
     * @param errorMessage the error message to include in the exception if the validation fails
     * @throws OpenPaymentsClientException if the provided string is null or empty
     */
    public static void ensureNotNullOrEmpty(String value, String errorMessage) {
        if (isNullOrEmpty(value)) {
            throw new OpenPaymentsClientException(errorMessage);
        }
    }

    /**
     * Ensures that the provided object is not null.
     * If the object is null, an {@code OpenPaymentsClientException} is thrown with the specified error message.
     *
     * @param value the object to validate for null
     * @param errorMessage the error message to include in the exception if the validation fails
     * @throws OpenPaymentsClientException if the provided object is null
     */
    public static void ensureNotNull(Object value, String errorMessage) {
        if (isNull(value)) {
            throw new OpenPaymentsClientException(errorMessage);
        }
    }

    /**
     * Ensures that the specified boolean value is true.
     * If the value is false, an {@code OpenPaymentsClientException} is thrown with the provided error message.
     *
     * @param value the boolean value to validate
     * @param errorMessage the error message to include in the exception if the validation fails
     * @throws OpenPaymentsClientException if the provided boolean value is false
     */
    public static void ensureTrue(boolean value, String errorMessage) {
        if (!value) {
            throw new OpenPaymentsClientException(errorMessage);
        }
    }
}
