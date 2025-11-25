package org.interledger.openpayments.httpclient;

import static org.interledger.openpayments.util.testing.TestResponse.createResponse;
import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.URI;
import java.net.http.HttpRequest;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link HttpLogger}.
 *
 * <p>Verifies correct logging behavior for HTTP requests and responses under different logging
 * levels (TRACE vs DEBUG), including:
 *
 * <ul>
 *   <li>Full request/response details with body when TRACE is enabled
 *   <li>Compact logging (method + URI only) when TRACE is disabled
 *   <li>Proper handling of missing request/response bodies ("<no body>")
 *   <li>Correct multi-value header logging (e.g., Accept headers)
 *   <li>Null-safety: throws {@link OpenPaymentsClientException} with clear message on null input
 * </ul>
 *
 * <p>Uses Logback's {@link ListAppender} to capture and assert log output.
 */
@DisplayName("HttpLogger → request/response logging (TRACE vs DEBUG)")
public class HttpLoggerTest {
  private ListAppender<ILoggingEvent> appender;
  private static final String LOG_SPACE = "    ";
  private static final String LOG_BODY = "body: ";
  private static final String LOG_NO_BODY = "<no body>";
  private static final String TEST_URL = "https://api.example.com/";
  private Level originalLevel;

  @DisplayName("logRequest(...) – method, URI, body, and headers")
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
      String expected =
          "[TRACE] HTTP Request:  POST "
              + request.uri().toString()
              + System.lineSeparator()
              + LOG_SPACE
              + LOG_BODY
              + "payload";
      assertEquals(expected, log);
    }

    @Test
    void shouldLogRequestDetailsAndNoBodyWhenIsMissingWhenTraceIsEnabled() {
      setupLogging(true);
      var request = createGetRequest();
      var httpLogger = new HttpLogger();
      httpLogger.logRequest(request);
      assertNotNull(appender.list);
      assertEquals(1, appender.list.size());
      var log = appender.list.getFirst().toString();
      var expected =
          "[TRACE] HTTP Request:  GET "
              + request.uri().toString()
              + System.lineSeparator()
              + LOG_SPACE
              + LOG_NO_BODY;
      assertEquals(expected, log);
    }

    @Test
    void shouldLogBasicInfoWhenTraceDisabled() {
      setupLogging(false);
      var request = createPostRequest(null);
      var httpLogger = new HttpLogger();
      httpLogger.logRequest(request);
      assertNotNull(appender.list);
      assertEquals(1, appender.list.size());
      var log = appender.list.getFirst().toString();
      var expected = "[DEBUG] HTTP Request:  POST " + request.uri().toString();
      assertEquals(expected, log);
    }

    @Test
    void shouldThrowWhenRequestIsNull() {
      var httpLogger = new HttpLogger();
      var exception =
          assertThrows(OpenPaymentsClientException.class, () -> httpLogger.logRequest(null));
      assertEquals("HTTP request must not be null", exception.getMessage());
    }

    HttpRequest createPostRequest(String body) {
      return HttpRequest.newBuilder(URI.create(TEST_URL))
          .POST(
              body != null
                  ? HttpRequest.BodyPublishers.ofString(body)
                  : HttpRequest.BodyPublishers.noBody())
          .build();
    }

    HttpRequest createGetRequest() {
      return HttpRequest.newBuilder(URI.create(TEST_URL)).GET().build();
    }
  }

  @DisplayName("logRequest(...) – multi-value header handling")
  @Nested
  class LogHeaders {
    @Test
    void shouldLogHeaders() {
      setupLogging(true);
      var request =
          HttpRequest.newBuilder(URI.create(TEST_URL))
              .GET()
              .header("Accept", "application/json")
              .header("Accept", "text/html")
              .header("Authorization", "token")
              .build();
      var httpLogger = new HttpLogger();
      httpLogger.logRequest(request);
      assertNotNull(appender.list);
      assertEquals(1, appender.list.size());
      var log = appender.list.getFirst().toString();
      var expected =
          System.lineSeparator()
              + LOG_SPACE
              + "Accept: application/json, text/html"
              + System.lineSeparator()
              + LOG_SPACE
              + "Authorization: token";
      assertTrue(log.contains(expected));
    }
  }

  @DisplayName("logResponse(...) – status, URI, and body")
  @Nested
  class LogResponse {

    @Test
    void shouldLogResponseDetailsWithExistingBodyWhenTraceIsEnabled() {
      setupLogging(true);
      var httpLogger = new HttpLogger();
      var response = createResponse("payload");
      httpLogger.logResponse(response);
      assertNotNull(appender.list);
      assertEquals(1, appender.list.size());
      var log = appender.list.getFirst().toString();
      var expected =
          "[TRACE] HTTP Response: 200 "
              + TEST_URL
              + System.lineSeparator()
              + LOG_SPACE
              + LOG_BODY
              + "payload";
      assertEquals(expected, log);
    }

    @Test
    void shouldLogResponseDataWithNoBodyWhenMissingWhenTraceIsEnabled() {
      setupLogging(true);
      var httpLogger = new HttpLogger();
      var response = createResponse(null);
      httpLogger.logResponse(response);
      assertNotNull(appender.list);
      assertEquals(1, appender.list.size());
      var log = appender.list.getFirst().toString();
      var expected =
          "[TRACE] HTTP Response: 200 "
              + TEST_URL
              + System.lineSeparator()
              + LOG_SPACE
              + LOG_BODY
              + LOG_NO_BODY;
      assertEquals(expected, log);
    }

    @Test
    void shouldLogBasicInfoWhenTraceIsDisabled() {
      setupLogging(false);
      var httpLogger = new HttpLogger();
      var response = createResponse("payload");
      httpLogger.logResponse(response);
      assertNotNull(appender.list);
      assertEquals(1, appender.list.size());
      var log = appender.list.getFirst().toString();
      var expected = "[DEBUG] HTTP Response: 200 " + TEST_URL;
      assertEquals(expected, log);
    }

    @Test
    void shouldThrowWhenResponseIsNull() {
      var httpLogger = new HttpLogger();
      var exception =
          assertThrows(OpenPaymentsClientException.class, () -> httpLogger.logResponse(null));
      assertEquals("HTTP response must not be null", exception.getMessage());
    }
  }

  private void setupLogging(boolean enableTrace) {
    var logger = (Logger) LoggerFactory.getLogger(HttpLogger.class);
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
      var logger = (Logger) LoggerFactory.getLogger(HttpLogger.class);
      logger.detachAppender(appender);
      appender.list.clear();
      logger.setLevel(originalLevel);
    }
  }
}
