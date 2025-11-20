package org.interledger.openpayments.util;

import org.interledger.openpayments.exception.AOpenPaymentsException;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class UtilGlobalTest {
    @Nested
    class IsNullOrEmpty {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "     "})
        void shouldReturnTrueWhenIsNullOrEmptyStringAfterTrimming(String nullOrString) {
            assertTrue(UtilGlobal.isNullOrEmpty(nullOrString));
        }

        @Test
        void shouldReturnFalseWhenIsNotNullOrEmptyStringAfterTrimming() {
            String str = " random string ";
            assertFalse(UtilGlobal.isNullOrEmpty(str));
        }
    }

    @Nested
    class ToUri {
        @ParameterizedTest
        @ValueSource(strings = {
                "https://example.com",
                "http://example.com/",
                "HTTPS://EXAMPLE.COM"
        })
        void shouldReturnUriWhenUrlIsValid(String url) {
            URI uri = UtilGlobal.toUri(url);
            assertNotNull(uri);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "     "})
        void shouldThrowOpenPaymentsClientExceptionWhenIsNullOrEmpty(String url) {
            OpenPaymentsClientException thrown = assertThrows(
                    OpenPaymentsClientException.class,
                    () -> UtilGlobal.toUri(url),
                    () -> "Expected OpenPaymentsClientException for input: '" + url + "'"
            );

            assertEquals("URL cannot be null or empty!", thrown.getMessage());
            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, thrown.getCode());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://",
                "  http://example.com  "
        })
        void shouldThrowOpenPaymentsClientExceptionWhenUrlNotValid(String url) {
            OpenPaymentsClientException thrown = assertThrows(
                    OpenPaymentsClientException.class,
                    () -> UtilGlobal.toUri(url),
                    () -> "Expected OpenPaymentsClientException for input: '" + url + "'"
            );
            assertEquals("URL [%s] invalid!".formatted(url), thrown.getMessage());
            assertEquals(AOpenPaymentsException.Code.BAD_REQUEST, thrown.getCode());
        }

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class EnsureNotNullOrEmpty {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "     "})
        void shouldThrowOpenPaymentsClientExceptionWhenStringIsNullOrEmpty(String str) {
            String errorMessage = "String [%s] is null or empty!".formatted(str);
            OpenPaymentsClientException thrown = assertThrows(
                    OpenPaymentsClientException.class,
                    () -> UtilGlobal.ensureNotNullOrEmpty(str, errorMessage),
                    () -> "Expected OpenPaymentsClientException for input: '" + str + "'"
            );

            assertEquals(errorMessage, thrown.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"some-string", "  another string   "})
        void shouldDoNothingWhenStringIsNotNullOrEmpty(String str) {
            assertDoesNotThrow(() -> UtilGlobal.ensureNotNullOrEmpty(str, ""));
        }

        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowOpenPaymentsClientExceptionWhenCollectionIsNullOrEmpty(Collection<?> items) {
            String errorMessage = "The collection is null or empty!";
            OpenPaymentsClientException thrown = assertThrows(
                    OpenPaymentsClientException.class,
                    () -> UtilGlobal.ensureNotNullOrEmpty(items, errorMessage),
                    "Expected OpenPaymentsClientException for given collection"
            );
            assertEquals(errorMessage, thrown.getMessage());
        }

        @ParameterizedTest
        @MethodSource("nonEmptyCollections")
        void shouldAcceptNonEmptyCollections(Collection<?> items) {
            assertDoesNotThrow(() -> UtilGlobal.ensureNotNullOrEmpty(items, ""));
        }

        private static Stream<Collection<?>> nonEmptyCollections() {
            return Stream.of(
                    List.of("hello"),
                    List.of(42),
                    Set.of("a"),
                    Arrays.asList(1, 2, 3),
                    Collections.singletonList("singleton"),
                    new HashSet<>(List.of("x", "y"))
            );
        }

    }

    @Nested
    class EnsureNotNull {
        @ParameterizedTest
        @NullSource
        void shouldThrowOpenPaymentsClientExceptionWhenObjectIsNull(Object value) {
            String errorMessage = "Object is null!";
            OpenPaymentsClientException thrown = assertThrows(
                    OpenPaymentsClientException.class,
                    () -> UtilGlobal.ensureNotNull(value, errorMessage),
                    "Expected OpenPaymentsClientException for null object"
            );

            assertEquals(errorMessage, thrown.getMessage());
        }

        @ParameterizedTest
        @MethodSource("validObjects")
        void shouldDoNothingWhenObjectIsNotNull(Object value) {
            assertDoesNotThrow(() -> UtilGlobal.ensureNotNull(value, ""));
        }

        static Stream<Object> validObjects() {
            return Stream.of("text", 123, List.of(), new Object(), new int[]{});
        }
    }

    @Nested
    class EnsureTrue {
        @Test
        void shouldThrowWhenEnabledIsFalse() {
            String errorMessage = "Value is false!";
            OpenPaymentsClientException thrown = assertThrows(OpenPaymentsClientException.class,
                    () -> UtilGlobal.ensureTrue(false, errorMessage));
            assertEquals(errorMessage, thrown.getMessage());
        }

        @Test
        void shouldAllowWhenEnabledIsTrue() {
            assertDoesNotThrow(() -> UtilGlobal.ensureTrue(true, ""));
        }
    }
}
