package org.interledger.openpayments.httpclient;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.interledger.openpayments.exception.OpenPaymentsAPIException;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.model.common.ApiError;
import org.interledger.openpayments.model.grant.Client;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.key.Ed25519Keys;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.payment.Payment;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.walletaddress.WalletAddress;
import org.interledger.openpayments.util.testing.Person;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.INTERRUPTED;
import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.IO_ERROR;
import static org.interledger.openpayments.util.testing.TestKeys.generatePrivateKey;
import static org.interledger.openpayments.util.testing.TestModels.*;
import static org.interledger.openpayments.util.testing.TestResponse.createResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OpenPaymentsHttpClient}.
 *
 * <p>Verifies correct behavior of the HTTP client wrapper, including:
 *
 * <ul>
 *   <li>Successful request sending, response deserialization, and logging via {@link HttpLogger}
 *   <li>Proper exception translation for {@link IOException} and {@link InterruptedException}
 *   <li>Correct handling and logging of various HTTP error responses (4xx, 5xx, unexpected codes)
 *   <li>End-to-end validation of all public API methods using parameterized tests and mocked {@link
 *       HttpClient}
 * </ul>
 *
 * <p>Uses Mockito for dependency mocking and Logback's {@link ListAppender} to assert log output.
 * All Open Payments client operations (wallet address lookup, grant creation, payments, quotes,
 * etc.) are tested with realistic success scenarios via {@code @MethodSource}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenPaymentsHttpClient → core client with signing, logging & error handling")
public class OpenPaymentsHttpClientTest {
  @Mock private static HttpClient httpClient;
  @Mock private static Client clientWallet;
  @Mock private HttpLogger httpLogger;
  private ListAppender<ILoggingEvent> appender;
  private static OpenPaymentsHttpClient opClient;
  private static WalletAddress walletAddress;
  private static Ed25519Keys keys;
  private static Grant grantForIncomingPayment;
  private static Grant grantForQuote;
  private static IncomingPayment incomingPayment;
  private static Quote quote;
  private static OutgoingPayment outgoingPayment;
  private static Payment payment;

  @BeforeEach
  void setUp() throws NoSuchAlgorithmException {
    var privateKey = generatePrivateKey();
    opClient =
        new OpenPaymentsHttpClient(
            clientWallet,
            privateKey,
            "keyId",
            OpenPaymentsClientOptions.DEFAULT,
            httpClient,
            httpLogger);
    grantForIncomingPayment = createGrantForIncomingPayment();
    grantForQuote = createGrantForQuote();
    walletAddress = createWalletAddress();
    keys = createKeys();
    incomingPayment = createIncomingPayment();
    quote = createQuote();
    outgoingPayment = createOutgoingPayment();
    payment = createPayment();
    Logger logger = (Logger) LoggerFactory.getLogger(OpenPaymentsHttpClient.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void cleanup() {
    if (appender != null) {
      Logger logger = (Logger) LoggerFactory.getLogger(OpenPaymentsHttpClient.class);
      logger.detachAppender(appender);
      appender.list.clear();
    }
  }

  @Nested
  @DisplayName("send(...) – low-level request execution and response processing")
  class Send {
    @Test
    void shouldSendRequestAndProcessResponseAndLog() throws IOException, InterruptedException {
      var req = createGetRequest();
      var person = new Person("John Doe", 25);
      var res = createResponse(person);
      when(httpClient.send(req, HttpResponse.BodyHandlers.ofString())).thenReturn(res);
      Person response = opClient.send(req, Person.class);
      assertEquals(person, response);
      verify(httpLogger).logRequest(req);
      verify(httpLogger).logResponse(res);
    }

    @Test
    void shouldThrowWhenIOExceptionFromHttpClient() throws IOException, InterruptedException {
      var req = createGetRequest();
      var message = "Handshake failure!";
      when(httpClient.send(req, HttpResponse.BodyHandlers.ofString()))
          .thenThrow(new IOException(message));
      var exception =
          assertThrows(OpenPaymentsClientException.class, () -> opClient.send(req, Person.class));
      assertEquals(IO_ERROR, exception.getCode());
      assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldThrowWhenRequestInterrupted() throws IOException, InterruptedException {
      var req = createGetRequest();
      var message = "Interrupted!";
      when(httpClient.send(req, HttpResponse.BodyHandlers.ofString()))
          .thenThrow(new InterruptedException(message));
      var exception =
          assertThrows(OpenPaymentsClientException.class, () -> opClient.send(req, Person.class));
      assertEquals(INTERRUPTED, exception.getCode());
      assertEquals(message, exception.getMessage());
    }

    @ParameterizedTest(name = "{index} → HTTP {0}")
    @CsvSource({
      "404, Not found,              WARN,  Client error [404]: Not found",
      "500, Internal server error,  ERROR, Server error [500]: Internal server error",
      "305, Use proxy,              WARN,  Unexpected HTTP status [305]: Use proxy",
      "102, Processing,             WARN,  Unexpected HTTP status [102]: Processing"
    })
    void shouldThrowAndLogHttpErrors(
        int code, String description, String expectedLevel, String expectedLogMessagePart)
        throws IOException, InterruptedException {
      var req = createGetRequest();
      when(httpClient.send(eq(req), eq(HttpResponse.BodyHandlers.ofString())))
          .thenReturn(createApiErrorResponse(code, description));

      var exception =
          assertThrows(OpenPaymentsAPIException.class, () -> opClient.send(req, Person.class));

      assertEquals(code, exception.getCode());

      assertNotNull(appender.list);
      assertEquals(1, appender.list.size());
      var loggedMessage = appender.list.getFirst().toString();
      var expectedFullMessage = "[" + expectedLevel + "] " + expectedLogMessagePart;
      assertEquals(expectedFullMessage, loggedMessage);
    }

    HttpRequest createGetRequest() {
      return HttpRequest.newBuilder(URI.create(TEST_URL)).GET().build();
    }
  }

  @DisplayName("public API methods – end-to-end success flows (wallet, grants, payments, quotes)")
  @Nested
  class PublicApiMethods {
    @ParameterizedTest(name = "{0}")
    @MethodSource("allClientMethods")
    <T> void shouldSendAPIRequestSuccessfully(
        String description, Supplier<T> methodCall, T expected) throws Exception {
      var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
      when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
          .thenReturn(createResponse(expected));

      T response = methodCall.get();
      assertEquals(expected, response);

      verify(httpClient).send(requestCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()));
    }

    static Stream<Arguments> allClientMethods() {
      return Stream.of(
          arg(
              "getWalletAddress",
              () -> opClient.walletAddress().get("https://wallet.example.com"),
              walletAddress),
          arg(
                  "getKeys",
                  () -> opClient.walletAddress().keys("https://wallet.example.com"),
                  keys),
          arg(
              "createGrantIncomingPayment",
              () -> opClient.auth().grant().incomingPayment(walletAddress),
              grantForIncomingPayment),
          arg(
              "createIncomingPayment",
              () ->
                  opClient.payment().createIncoming(
                      walletAddress, grantForIncomingPayment, BigDecimal.valueOf(100)),
              incomingPayment),
          arg("createGrantQuote", () -> opClient.auth().grant().quote(walletAddress), grantForQuote),
          arg(
              "createQuote",
              () -> opClient.quote().create(
                      "quote-token-123", walletAddress, incomingPayment, Optional.empty(), Optional.empty()
              ),
              quote),
          arg(
              "createGrantContinuation",
              () ->
                  opClient.auth().grant().continuation(
                      walletAddress, quote.getDebitAmount(), URI.create("https://app/cb"), "nonce123"),
              outgoingPayment),
          arg(
              "createGrantFinalize",
              () -> opClient.auth().grant().finalize(outgoingPayment, "interact-123"),
              grantForIncomingPayment),
          arg(
              "createFinalizePayment",
              () -> opClient.payment().createOutgoing(grantForIncomingPayment, walletAddress, quote),
              payment));
    }

    private static <T> Arguments arg(String name, Supplier<T> call, Object response) {
      return Arguments.of(name, call, response);
    }
  }

  HttpResponse<String> createApiErrorResponse(int code, String description) {
    ApiError error = new ApiError();
    error.setCode(String.valueOf(code));
    error.setDescription(description);
    return createResponse(new ApiErrorWrapper(error), code);
  }

  record ApiErrorWrapper(ApiError error) {}
}
