package org.interledger.openpayments.httpclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.interledger.openpayments.IOpenPaymentsClient;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.model.common.ApiError;
import org.interledger.openpayments.model.grant.*;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPaymentRequest;
import org.interledger.openpayments.model.payment.Payment;
import org.interledger.openpayments.model.payment.PaymentRequest;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.quote.QuoteRequest;
import org.interledger.openpayments.model.walletaddress.WalletAddress;
import org.interledger.openpayments.signing.SignatureRequestBuilder;
import org.interledger.openpayments.util.UtilGlobal;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.INTERRUPTED;
import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.IO_ERROR;
import static org.interledger.openpayments.exception.OpenPaymentsAPIException.getApiException;
import static org.interledger.openpayments.signing.SignatureRequestBuilder.ACCEPT_HEADER;
import static org.interledger.openpayments.signing.SignatureRequestBuilder.APPLICATION_JSON;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * The OpenPaymentsHttpClient class provides an implementation of the IOpenPaymentsClient
 * interface, enabling interaction with Open Payments APIs for retrieving wallet addresses
 * and sending HTTP requests.
 * It offers functionality to manage HTTP requests and responses, process timeouts,
 * log requests and responses, and handle errors from API interactions.
 * The client is configured upon initialization using a set of options and requires
 * a private key, client wallet address, and key ID for signing and authentication.
 */
@RequiredArgsConstructor
@Slf4j
public class OpenPaymentsHttpClient implements IOpenPaymentsClient {
    private static final String ILP_METHOD = "ilp";
    
    private final Client clientWallet;
    private final PrivateKey privateKey;
    private final String keyId;
    private final OpenPaymentsClientOptions options;

    private final HttpClient client;
    private final OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
    private final HttpLogger httpLogger;

    /**
     * Retrieves a wallet address by sending a GET request to the specified URL.
     *
     * @param url the URL endpoint from which to fetch the wallet address
     * @return a {@code WalletAddress} object representing the fetched wallet address
     * @throws OpenPaymentsClientException if the URL is invalid, the HTTP request fails, or deserialization fails
     */
    @Override
    public WalletAddress getWalletAddress(String url) {
        var urlObj = UtilGlobal.toUri(url);

        var request = HttpRequest.newBuilder(urlObj)
                .GET()
                .header(ACCEPT_HEADER.toLowerCase(), APPLICATION_JSON)
                .timeout(Duration.of(options.getTimeOutInSeconds(), SECONDS))
                .build();

        return this.send(request, WalletAddress.class);
    }

    /**
     * @see IOpenPaymentsClient#createGrantIncomingPayment(WalletAddress)
     * @param receiver the wallet address for which the grant is to be created;
     *                it represents a secure, sharable identifier linked to the
     *                specific Open Payments account
     * @return Grant
     */
    @Override
    public Grant createGrantIncomingPayment(WalletAddress receiver) {
        ensureNotNull(receiver, "WalletAddress receiver cannot be null");
        log.debug("createPendingGrant: {}", receiver);

        GrantRequest accessRequest = GrantRequest.build(
                clientWallet,
                AccessItemType.incomingPayment,
                Set.of(AccessAction.read, AccessAction.complete, AccessAction.create)
        );

        HttpRequest request = new SignatureRequestBuilder(privateKey, keyId, mapper)
                .POST(accessRequest)
                .target(receiver.getAuthServer())
                .getRequest(options);
        return this.send(request, Grant.class);
    }

    /**
     * @see IOpenPaymentsClient#createIncomingPayment(WalletAddress,Grant,BigDecimal)
     * @param receiver the wallet address of the payment receiver. This identifies the
     *                 destination account for the incoming payment.
     * @param ipGrant  the grant that authorizes the creation of the incoming payment.
     *                 It provides access credentials for interacting with the receiver's
     *                 payment endpoint.
     * @param amount   the amount expected to be received for this payment. This
     *                 represents the total target amount that the sender is
     *                 expected to pay.
     * @return IncomingPayment
     */
    @Override
    public IncomingPayment createIncomingPayment(WalletAddress receiver, Grant ipGrant, BigDecimal amount) {
        ensureNotNull(receiver, "WalletAddress receiver cannot be null");
        ensureNotNull(ipGrant, "Grant for incomingPayment cannot be null");
        ensureNotNull(amount, "BigDecimal amount cannot be null");

        log.debug("createIncomingPayment: {} for: {}", receiver, amount);

        PaymentRequest paymentRequest = PaymentRequest.build(
                receiver,
                amount,
                options.getTransactionExpirationInSeconds()
        );

        HttpRequest request = new SignatureRequestBuilder(privateKey, keyId, mapper)
                .POST(paymentRequest)
                .target(buildResourceUrl(receiver.getResourceServerAsURI(), "/incoming-payments"))
                .accessToken(extractAccessToken(ipGrant))
                .getRequest(options);

        return send(request, IncomingPayment.class);
    }

    /**
     * @see IOpenPaymentsClient#createGrantQuote(WalletAddress)
     * @param sender the wallet address for which the grant is to be created;
     *                it represents a secure, sharable identifier linked to the
     *                specific Open Payments account
     * @return Grant
     */
    @Override
    public Grant createGrantQuote(WalletAddress sender) {
        ensureNotNull(sender, "WalletAddress sender cannot be null");
        log.debug("createGrantQuote: {}", sender);

        GrantRequest accessRequest = GrantRequest.build(
                this.clientWallet,
                AccessItemType.quote,
                Set.of(AccessAction.read, AccessAction.create)
        );
        HttpRequest request = new SignatureRequestBuilder(privateKey, keyId, mapper)
                .POST(accessRequest)
                .target(sender.getAuthServer())
                .getRequest(this.options);
        return send(request, Grant.class);
    }

    /**
     * @see IOpenPaymentsClient#createQuote(String, WalletAddress, IncomingPayment)
     * @param quoteToken a unique token representing the quote request. This token
     *                   is used to identify and secure the quote creation process.
     * @param sender the wallet address of the payment sender. This identifies the
     *               account initiating the payment and provides the context for
     *               the quote transaction.
     * @param incomingPayment an object representing the pre-configured incoming
     *                        payment details for which the quote is being created.
     *                        This includes information such as payment amount,
     *                        recipient details, and associated metadata.
     * @return Quote
     */
    @Override
    public Quote createQuote(String quoteToken, WalletAddress sender, IncomingPayment incomingPayment) {
        ensureNotNullOrEmpty(quoteToken, "Quote token cannot be null or empty");
        ensureNotNull(sender, "PaymentPointer sender cannot be null");
        ensureNotNull(incomingPayment, "IncomingPayment cannot be null");
        log.debug("createQuote: {} for: {}", incomingPayment, sender);

        QuoteRequest quoteRequest = QuoteRequest.build(
                sender.getIdAsURI(),
                incomingPayment.getId().toString(),
                ILP_METHOD
        );

        HttpRequest request = new SignatureRequestBuilder(privateKey, keyId, mapper)
                .POST(quoteRequest)
                .target(buildResourceUrl(sender.getResourceServerAsURI(), "/quotes"))
                .accessToken(quoteToken)
                .getRequest(options);

        return this.send(request, Quote.class);
    }

    @Override
    public OutgoingPayment createGrantContinuation(WalletAddress sender, Quote quote, URI returnUrl, String nonce) {
        ensureNotNull(sender, "PaymentPointer sender cannot be null");
        ensureNotNull(quote, "Quote cannot be null");
        ensureNotNull(returnUrl, "Return URL cannot be null");
        ensureNotNullOrEmpty(nonce, "Nonce cannot be null or empty");
        log.debug("continueGrant: {} for: {}", quote, sender);

        GrantRequest accessRequest = GrantRequest.outgoing(clientWallet,
                        AccessItemType.outgoingPayment,
                        Set.of(AccessAction.read, AccessAction.create),
                        sender.getIdAsURI(), quote.getDebitAmount())
                .redirectInteract(returnUrl, nonce);

        HttpRequest request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(accessRequest)
                .target(sender.getAuthServer())
                .getRequest(options);

        return send(request, OutgoingPayment.class);
    }

    @Override
    public Grant createGrantFinalize(OutgoingPayment outgoingPayment, String interactRef) {
        ensureNotNull(outgoingPayment, "OutgoingPayment cannot be null");
        ensureNotNullOrEmpty(interactRef, "Interact reference cannot be null or empty");
        log.debug("finalizeGrant: {} for: {}", outgoingPayment, interactRef);

        var ref = InteractRef.build(interactRef);

        HttpRequest request = new SignatureRequestBuilder(privateKey, keyId, mapper)
                .POST(ref)
                .target(outgoingPayment.getPaymentContinue().getUri())
                .accessToken(extractContinueAccessToken(outgoingPayment))
                .getRequest(options);

        return this.send(request, Grant.class);
    }

    /**
     * 
     * @param finalized the finalized grant object representing the authorization to perform the payment.
     *                  This grant includes the necessary access credentials and authorization details.
     * @param sender the wallet address of the payment sender. It identifies the account initiating
     *                     the payment process.
     * @param quote the payment quote object containing details of the payment, such as the quoted amount,
     *              currency, terms, and associated fees.
     * @return Payment
     */
    @Override
    public Payment createFinalizePayment(Grant finalized, WalletAddress sender, Quote quote) {
        ensureNotNull(finalized, "Grant finalized cannot be null");
        ensureNotNull(sender, "WalletAddress sender cannot be null");
        ensureNotNull(quote, "Quote cannot be null");

        log.debug("finalizePayment: {} for: {}, with: {}", finalized, sender, quote);

        OutgoingPaymentRequest outgoingPayment = new OutgoingPaymentRequest();
        outgoingPayment.setQuoteId(quote.getId());
        outgoingPayment.setWalletAddress(sender.getIdAsURI());

        HttpRequest request = new SignatureRequestBuilder(privateKey, keyId, mapper)
                .POST(outgoingPayment)
                .target(buildResourceUrl(sender.getResourceServerAsURI(), "/outgoing-payments"))
                .accessToken(extractAccessToken(finalized))
                .getRequest(options);

        return this.send(request, Payment.class);
    }

    /**
     * Sends an HTTP request and processes the response, including deserialization into the specified type.
     * The method logs the request and response, handles HTTP errors, and attempts to deserialize
     * the response body into the provided response type. It throws exceptions if an error occurs
     * during the process, such as network issues or deserialization failures.
     *
     * @param request      the HTTP request to be sent
     * @param responseType the class type into which the response body should be deserialized
     * @param <T>          the generic type representing the type of the response object
     * @return an instance of the specified response type {@code T} populated with data from the HTTP response body
     * @throws OpenPaymentsClientException if an I/O error occurs, the thread is interrupted, or deserialization fails
     */
    public <T> T send(HttpRequest request, Class<T> responseType) {
        try {
            this.httpLogger.logRequest(request);
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            this.httpLogger.logResponse(response);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                handleHttpError(response);
            }

            // deserialize
            return this.mapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new OpenPaymentsClientException(IO_ERROR, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenPaymentsClientException(INTERRUPTED, e.getMessage(), e);
        }
    }

    /**
     * Handles HTTP error responses by logging the error details and throwing an exception.
     * Differentiates between client errors (4xx), server errors (5xx), and other non-2xx status codes,
     * creating a server-side exception with appropriate information.
     * @param response the HTTP response containing the error status code and body
     * @throws IOException if there is an issue processing the HTTP response
     */
    private void handleHttpError(HttpResponse<String> response) throws IOException {
        int statusCode = response.statusCode();
        ApiError error = mapper.readError(response.body(), statusCode);

        if (statusCode >= 400 && statusCode < 500) {
            // 4xx - Client errors (bad request, unauthorized, forbidden, not found, etc.)
            log.warn("Client error [{}]: {}", statusCode, error.description);
        } else if (statusCode >= 500 && statusCode < 600) {
            // 5xx - Server errors (internal server error, bad gateway, service unavailable, etc.)
            log.error("Server error [{}]: {}", statusCode, error.description);
        } else {
            // Other non-2xx status codes (1xx, 3xx, or unexpected codes)
            log.warn("Unexpected HTTP status [{}]: {}", statusCode, error.description);
        }
        // Server side exception to be handled.
        throw getApiException(error, response);
    }


    /**
     * Safely builds a resource URL by appending a path to a base URI
     *
     * @param baseUri the base URI
     * @param path    the path to append (should start with / or will be prepended)
     * @return the constructed URI
     * @throws IllegalArgumentException if baseUri or path is null
     */
    private URI buildResourceUrl(URI baseUri, String path) {
        ensureNotNull(baseUri, "Base URI cannot be null");
        ensureNotNull(path, "Path cannot be null");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return baseUri.resolve(normalizedPath);
    }


    /**
     * Extracts access token from AccessGrant
     *
     * @param grant the access grant containing the token
     * @return the access token string
     * @throws IllegalArgumentException if grant, grant.access, or grant.access.token is null
     */
    private String extractAccessToken(Grant grant) {
        ensureNotNull(grant, "AccessGrant cannot be null");
        ensureNotNull(grant.getAccess(), "AccessGrant.access cannot be null");
        ensureNotNull(grant.getAccess().getToken(), "AccessGrant.access.token cannot be null");
        return grant.getAccess().getToken();
    }

    /**
     * Extracts access token from OutgoingPayment's continue access
     *
     * @param outgoingPayment the outgoing payment containing continue access
     * @return the access token string
     * @throws IllegalArgumentException if outgoingPayment, paymentContinue, access, or token is null
     */
    private String extractContinueAccessToken(OutgoingPayment outgoingPayment) {
        ensureNotNull(outgoingPayment, "OutgoingPayment cannot be null");
        ensureNotNull(outgoingPayment.getPaymentContinue(), "OutgoingPayment.paymentContinue cannot be null");
        ensureNotNull(outgoingPayment.getPaymentContinue().getAccess(), "OutgoingPayment.paymentContinue.access cannot be null");
        ensureNotNull(outgoingPayment.getPaymentContinue().getAccess().getToken(), "OutgoingPayment.paymentContinue.access.token cannot be null");
        return outgoingPayment.getPaymentContinue().getAccess().getToken();
    }
}
