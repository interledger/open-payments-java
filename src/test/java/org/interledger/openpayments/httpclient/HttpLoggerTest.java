package org.interledger.openpayments.httpclient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.interledger.openpayments.util.testing.TestResponse.createResponse;
import static org.junit.jupiter.api.Assertions.*;

public class HttpLoggerTest {
    private ListAppender<ILoggingEvent> appender;
    private static final String LOG_SPACE = "    ";
    private static final String LOG_BODY = "body: ";
    private static final String LOG_NO_BODY = "<no body>";
    private static final String TEST_URL = "https://api.example.com/";
    private Level originalLevel;

    @Nested
    class LogRequest {
        @Test
        void shouldLogRequestDetailsAndBodyWhenIsPresentWhenTraceIsEnabled() {
            setupLogging(true);
            var request = createPostRequest("payload");
            HttpLogger httpLogger = new HttpLogger();
            httpLogger.logRequest(request);
            assertNotNull(appender.list);
            assertEquals(1, appender.list.size());
            String log = appender.list.getFirst().toString();
            String expected = "[TRACE] HTTP Request:  POST " +
                    request.uri().toString() +
                    System.lineSeparator() +
                    LOG_SPACE +
                    LOG_BODY +
                    "payload";
            assertEquals(expected, log);
        }

        @Test
        void shouldLogRequestDetailsAndNoBodyWhenIsMissingWhenTraceIsEnabled() {
            setupLogging(true);
            var request = createGetRequest();
            HttpLogger httpLogger = new HttpLogger();
            httpLogger.logRequest(request);
            assertNotNull(appender.list);
            assertEquals(1, appender.list.size());
            String log = appender.list.getFirst().toString();
            String expected = "[TRACE] HTTP Request:  GET " +
                    request.uri().toString() +
                    System.lineSeparator() +
                    LOG_SPACE +
                    LOG_NO_BODY;
            assertEquals(expected, log);
        }

        @Test
        void shouldLogBasicInfoWhenTraceDisabled() {
            setupLogging(false);
            var request = createPostRequest(null);
            HttpLogger httpLogger = new HttpLogger();
            httpLogger.logRequest(request);
            assertNotNull(appender.list);
            assertEquals(1, appender.list.size());
            String log = appender.list.getFirst().toString();
            String expected = "[DEBUG] HTTP Request:  POST " +
                    request.uri().toString();
            assertEquals(expected, log);
        }

        @Test
        void shouldThrowWhenRequestIsNull() {
            HttpLogger httpLogger = new HttpLogger();
            var exception = assertThrows(OpenPaymentsClientException.class, () -> httpLogger.logRequest(null));
            assertEquals("HTTP request must not be null", exception.getMessage());
        }

        HttpRequest createPostRequest(String body) {
            return HttpRequest.newBuilder(URI.create(TEST_URL))
                    .POST(body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody())
                    .build();
        }

        HttpRequest createGetRequest() {
            return HttpRequest.newBuilder(URI.create(TEST_URL))
                    .GET()
                    .build();
        }
    }

    @Nested
    class LogHeaders {
        @Test
        void shouldLogHeaders() {
            setupLogging(true);
            var request = HttpRequest.newBuilder(URI.create(TEST_URL))
                    .GET()
                    .header("Accept", "application/json")
                    .header("Accept", "text/html")
                    .header("Authorization", "token")
                    .build();
            HttpLogger httpLogger = new HttpLogger();
            httpLogger.logRequest(request);
            assertNotNull(appender.list);
            assertEquals(1, appender.list.size());
            String log = appender.list.getFirst().toString();
            String expected = System.lineSeparator() + LOG_SPACE + "Accept: application/json, text/html" +
                    System.lineSeparator() + LOG_SPACE + "Authorization: token";
            assertTrue(log.contains(expected));
        }
    }

    @Nested
    class LogResponse {

        @Test
        void shouldLogResponseDetailsWithExistingBodyWhenTraceIsEnabled() {
            setupLogging(true);
            HttpLogger httpLogger = new HttpLogger();
            var response = createResponse("payload");
            httpLogger.logResponse(response);
            assertNotNull(appender.list);
            assertEquals(1, appender.list.size());
            String log = appender.list.getFirst().toString();
            String expected = "[TRACE] HTTP Response: 200 " +
                    TEST_URL +
                    System.lineSeparator() +
                    LOG_SPACE +
                    LOG_BODY +
                    "payload";
            assertEquals(expected, log);
        }

        @Test
        void shouldLogResponseDataWithNoBodyWhenMissingWhenTraceIsEnabled() {
            setupLogging(true);
            HttpLogger httpLogger = new HttpLogger();
            var response = createResponse(null);
            httpLogger.logResponse(response);
            assertNotNull(appender.list);
            assertEquals(1, appender.list.size());
            String log = appender.list.getFirst().toString();
            String expected = "[TRACE] HTTP Response: 200 " +
                    TEST_URL +
                    System.lineSeparator() +
                    LOG_SPACE +
                    LOG_BODY +
                    LOG_NO_BODY;
            assertEquals(expected, log);
        }

        @Test
        void shouldLogBasicInfoWhenTraceIsDisabled() {
            setupLogging(false);
            HttpLogger httpLogger = new HttpLogger();
            var response = createResponse("payload");
            httpLogger.logResponse(response);
            assertNotNull(appender.list);
            assertEquals(1, appender.list.size());
            String log = appender.list.getFirst().toString();
            String expected = "[DEBUG] HTTP Response: 200 " + TEST_URL;
            assertEquals(expected, log);
        }

        @Test
        void shouldThrowWhenResponseIsNull() {
            HttpLogger httpLogger = new HttpLogger();
            var exception = assertThrows(OpenPaymentsClientException.class, () -> httpLogger.logResponse(null));
            assertEquals("HTTP response must not be null", exception.getMessage());
        }
    }

    private void setupLogging(boolean enableTrace) {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpLogger.class);
        originalLevel = logger.getLevel();
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        if (enableTrace) {
            logger.setLevel(Level.TRACE);
        }
    }

    @AfterEach
    void cleanup() {
        if (appender != null) {
            Logger logger = (Logger) LoggerFactory.getLogger(HttpLogger.class);
            logger.detachAppender(appender);
            appender.list.clear();
            logger.setLevel(originalLevel);
        }
    }
}
