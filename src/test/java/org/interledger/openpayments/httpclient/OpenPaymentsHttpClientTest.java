package org.interledger.openpayments.httpclient;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.interledger.openpayments.exception.OpenPaymentsAPIException;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.model.common.ApiError;
import org.interledger.openpayments.model.grant.AccessContinue;
import org.interledger.openpayments.model.grant.AccessToken;
import org.interledger.openpayments.model.grant.CardAuthorization;
import org.interledger.openpayments.model.grant.Client;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.grant.Limits;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.key.Ed25519Keys;
import org.interledger.openpayments.model.key.Ed25519PublicKey;
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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;
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
import static org.mockito.Mockito.never;
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
    @Mock
    private static HttpClient httpClient;
    @Mock
    private static Client clientWallet;
    @Mock
    private HttpLogger httpLogger;
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

        @SuppressWarnings("deprecation")
        static Stream<Arguments> allClientMethods() {
            return Stream.of(
                arg(
                    "getWalletAddress",
                    () -> opClient.walletAddress().get("https://wallet.example.com"),
                    createWalletAddress()),
                arg(
                    "getKeys",
                    () -> opClient.walletAddress().keys("https://wallet.example.com"),
                    createKeys()),
                arg(
                    "createGrantIncomingPayment",
                    () -> opClient.auth().grant().incomingPayment(walletAddress),
                    createGrantForIncomingPayment()),
                arg(
                    "createIncomingPayment",
                    () ->
                        opClient.payment().createIncoming(
                            walletAddress, grantForIncomingPayment, BigDecimal.valueOf(100)),
                    createIncomingPayment()),
                arg("createGrantQuote", () -> opClient.auth().grant().quote(walletAddress), createGrantForQuote()),
                arg(
                    "createQuote",
                    () -> opClient.quote().create(
                        "quote-token-123", walletAddress, incomingPayment, Optional.empty(), Optional.empty()
                    ),
                    createQuote()),
                arg(
                    "createGrantContinuation",
                    () ->
                        opClient.auth().grant().continuation(
                            walletAddress, quote.getDebitAmount(), URI.create("https://app/cb"), "nonce123"),
                    createOutgoingPayment()),
                arg(
                    "createGrantFinalize",
                    () -> opClient.auth().grant().finalize(outgoingPayment, "interact-123"),
                    createGrantForIncomingPayment()),
                arg(
                    "createFinalizePayment",
                    () -> opClient.payment().createOutgoing(grantForIncomingPayment, walletAddress, quote),
                    createOutgoingPayment()),
                arg(
                    "createGrantContinuationWithIntervalLimits",
                    () ->
                        opClient.auth().grant().continuation(
                            walletAddress,
                            Limits.debit(quote.getDebitAmount(), "R3/2025-05-20T13:00:00Z/P1M"),
                            URI.create("https://app/cb"),
                            "nonce123"),
                    createPendingGrant()),
                arg(
                    "createGrantVerifyOwnership",
                    () -> opClient.auth().grant().verifyOwnership(walletAddress, URI.create("https://app/cb"), "nonce123"),
                    createPendingGrant()),
                arg(
                    "createGrantFinalizePendingGrant",
                    () -> opClient.auth().grant().finalize(createPendingGrant(), "interact-123"),
                    createGrantForSubject()),
                arg(
                    "createGrantCardAuthorization",
                    () ->
                        opClient.auth().grant().cardAuthorization(
                            walletAddress,
                            URI.create(TEST_URL + "incoming-payments/1"),
                            quote.getDebitAmount(),
                            CardAuthorization.build("9F26089F27019F1002", "request-1")),
                    createGrantForOutgoingPayment()),
                arg(
                    "getGrantSpentAmounts",
                    () -> opClient.payment().getGrantSpentAmounts(walletAddress, createGrantForOutgoingPayment()),
                    createGrantSpentAmounts()));
        }

        private static <T> Arguments arg(String name, Supplier<T> call, Object response) {
            return Arguments.of(name, call, response);
        }
    }

    @DisplayName("Open Payments v1.4.0 – request targets and bodies")
    @Nested
    class SpecV140Requests {
        private HttpRequest captureRequest(Object response, Runnable call) throws Exception {
            var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(createResponse(response));
            call.run();
            verify(httpClient).send(requestCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()));
            return requestCaptor.getValue();
        }

        private String body(HttpRequest request) throws Exception {
            var publisher = request.bodyPublisher().orElseThrow();
            var subscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
            publisher.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    subscriber.onNext(List.of(item));
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
            return subscriber.getBody().toCompletableFuture().get();
        }

        @Test
        void shouldGetGrantSpentAmountsFromResourceServer() throws Exception {
            var request = captureRequest(createGrantSpentAmounts(),
                () -> opClient.payment().getGrantSpentAmounts(walletAddress, createGrantForOutgoingPayment()));

            assertEquals("GET", request.method());
            assertEquals(URI.create(TEST_URL + "outgoing-payment-grant"), request.uri());
            assertEquals("GNAP token", request.headers().firstValue("Authorization").orElseThrow());
        }

        @Test
        void shouldRequestSubjectGrantForOwnershipVerification() throws Exception {
            var request = captureRequest(createPendingGrant(),
                () -> opClient.auth().grant().verifyOwnership(walletAddress, URI.create("https://app/cb"), "nonce123"));

            assertEquals("POST", request.method());
            assertEquals(URI.create(walletAddress.getAuthServer()), request.uri());

            var json = OpenPaymentsObjectMapper.get().readTree(body(request));
            assertFalse(json.has("access_token"));
            assertEquals(walletAddress.getId(), json.at("/subject/sub_ids/0/id").asText());
            assertEquals("uri", json.at("/subject/sub_ids/0/format").asText());
            assertEquals("redirect", json.at("/interact/finish/method").asText());
            assertEquals("nonce123", json.at("/interact/finish/nonce").asText());
        }

        @Test
        void shouldContinuePendingGrantWithInteractRef() throws Exception {
            var pendingGrant = createPendingGrant();
            var request = captureRequest(createGrantForSubject(),
                () -> opClient.auth().grant().finalize(pendingGrant, "interact-123"));

            assertEquals("POST", request.method());
            assertEquals(pendingGrant.getGrantContinue().getUri(), request.uri());
            assertEquals("GNAP continue-token", request.headers().firstValue("Authorization").orElseThrow());
            assertEquals("interact-123",
                OpenPaymentsObjectMapper.get().readTree(body(request)).get("interact_ref").asText());
        }

        @Test
        void shouldRequestOutgoingGrantWithInterval() throws Exception {
            var request = captureRequest(createPendingGrant(),
                () -> opClient.auth().grant().continuation(
                    walletAddress,
                    Limits.debit(quote.getDebitAmount(), "R3/2025-05-20T13:00:00Z/P1M"),
                    URI.create("https://app/cb"),
                    "nonce123"));

            var item = OpenPaymentsObjectMapper.get().readTree(body(request)).at("/access_token/access/0");
            assertEquals("outgoing-payment", item.get("type").asText());
            assertEquals(walletAddress.getId(), item.get("identifier").asText());
            assertEquals("R3/2025-05-20T13:00:00Z/P1M", item.at("/limits/interval").asText());
            assertEquals(quote.getDebitAmount().getValue(), item.at("/limits/debitAmount/value").asText());
        }

        @Test
        void shouldRequestCardAuthorizationGrantWithoutInteraction() throws Exception {
            var receiver = URI.create(TEST_URL + "incoming-payments/1");
            var request = captureRequest(createGrantForOutgoingPayment(),
                () -> opClient.auth().grant().cardAuthorization(
                    walletAddress, receiver, quote.getDebitAmount(), CardAuthorization.build("9F26089F27019F1002", "request-1")));

            var json = OpenPaymentsObjectMapper.get().readTree(body(request));
            assertFalse(json.has("interact"));
            var item = json.at("/access_token/access/0");
            assertEquals(receiver.toString(), item.at("/limits/receiver").asText());
            assertEquals(quote.getDebitAmount().getValue(), item.at("/limits/receiveAmount/value").asText());
            assertEquals("9F26089F27019F1002", item.at("/cardAuthorization/tlvData").asText());
            assertEquals("request-1", item.at("/cardAuthorization/requestId").asText());
        }
    }

    @DisplayName("PR review fixes – continuation token, error bodies, interactive grant checks")
    @Nested
    class ReviewFixes {
        private Grant grantWithContinuation() {
            var grant = createGrantForOutgoingPayment();
            var accessContinue = new AccessContinue();
            accessContinue.setUri(URI.create(TEST_URL + "auth/continue/1"));
            var continueToken = new AccessToken();
            continueToken.setToken("continue-token");
            accessContinue.setAccess(continueToken);
            grant.setAccessContinue(accessContinue);
            return grant;
        }

        private Ed25519PublicKey validJwk() {
            var jwk = new Ed25519PublicKey();
            jwk.setKid("key-1");
            jwk.setAlg("EdDSA");
            jwk.setKty("OKP");
            jwk.setCrv("Ed25519");
            jwk.setX("oy0L_vTygNE4IogRyn_F5GmHXdqYVjIXkWs2jky7zsI");
            return jwk;
        }

        @Test
        void shouldCancelGrantWithContinuationToken() throws Exception {
            var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(createResponse("", 204));

            var grant = grantWithContinuation();
            opClient.auth().grant().cancel(grant);

            verify(httpClient).send(requestCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()));
            var request = requestCaptor.getValue();
            assertEquals("DELETE", request.method());
            assertEquals(grant.getAccessContinue().getUri(), request.uri());
            assertEquals("GNAP continue-token", request.headers().firstValue("Authorization").orElseThrow());
        }

        @Test
        void shouldIncludeErrorBodyWhenNoContentRequestFails() throws Exception {
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(createApiErrorResponse(401, "invalid continuation"));

            var exception = assertThrows(OpenPaymentsAPIException.class,
                () -> opClient.auth().grant().cancel(grantWithContinuation()));

            assertEquals(401, exception.getCode());
            assertTrue(exception.getMessage().contains("invalid continuation"));
        }

        @Test
        void shouldKeepHttpStatusWhenErrorBodyIsNotAnOpenPaymentsError() throws Exception {
            var body = "{\"error\":\"Not Found\",\"message\":\"Route POST:/incoming-payments not found\",\"statusCode\":404}";
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(createResponse(body, 404));

            var exception = assertThrows(OpenPaymentsAPIException.class,
                () -> opClient.payment().createIncoming(walletAddress, grantForIncomingPayment, BigDecimal.ONE));

            assertEquals(404, exception.getCode());
            assertEquals(body, exception.getResponseBody());
            assertTrue(exception.getMessage().contains("Route POST:/incoming-payments not found"));
        }

        @Test
        void shouldKeepHttpStatusWhenErrorBodyIsEmpty() throws Exception {
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(createResponse("", 503));

            var exception = assertThrows(OpenPaymentsAPIException.class,
                () -> opClient.payment().getGrantSpentAmounts(walletAddress, createGrantForOutgoingPayment()));

            assertEquals(503, exception.getCode());
        }

        @Test
        void shouldRejectInteractiveGrantsForDirectedIdentity() throws Exception {
            var directedClient = new OpenPaymentsHttpClient(
                Client.directedIdentity(validJwk()),
                generatePrivateKey(),
                "key-1",
                OpenPaymentsClientOptions.DEFAULT,
                httpClient,
                httpLogger);
            var returnUrl = URI.create("https://app/cb");

            assertThrows(OpenPaymentsClientException.class, () -> directedClient.auth().grant().continuation(
                walletAddress, Limits.debit(quote.getDebitAmount()), returnUrl, "nonce123"));
            assertThrows(OpenPaymentsClientException.class,
                () -> directedClient.auth().grant().verifyOwnership(walletAddress, returnUrl, "nonce123"));
            verify(httpClient, never()).send(any(HttpRequest.class), any());
        }

        @Test
        void shouldFailClearlyWhenServerReturnsNoInteraction() throws Exception {
            when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(createResponse(createGrantForSubject()));

            var exception = assertThrows(OpenPaymentsClientException.class,
                () -> opClient.auth().grant().verifyOwnership(walletAddress, URI.create("https://app/cb"), "nonce123"));
            assertTrue(exception.getMessage().contains("interact.redirect"));
        }

        @Test
        void shouldRejectNullClientForDefaultClient() {
            assertThrows(OpenPaymentsClientException.class,
                () -> OpenPaymentsHttpClient.defaultClient((Client) null, "pem", "key-1"));
        }
    }

    HttpResponse<String> createApiErrorResponse(int code, String description) {
        ApiError error = new ApiError();
        error.setCode(String.valueOf(code));
        error.setDescription(description);
        return createResponse(new ApiErrorWrapper(error), code);
    }

    record ApiErrorWrapper(ApiError error) {
    }
}
