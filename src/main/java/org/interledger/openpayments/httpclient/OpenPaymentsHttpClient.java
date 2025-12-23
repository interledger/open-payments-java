package org.interledger.openpayments.httpclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.interledger.openpayments.IOpenPaymentsClient;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.model.IAuthServer;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.*;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.key.Ed25519Keys;
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
import java.util.Optional;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.INTERRUPTED;
import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.IO_ERROR;
import static org.interledger.openpayments.exception.OpenPaymentsAPIException.getApiException;
import static org.interledger.openpayments.signing.SignatureRequestBuilder.ACCEPT_HEADER;
import static org.interledger.openpayments.signing.SignatureRequestBuilder.APPLICATION_JSON;
import static org.interledger.openpayments.util.UtilGlobal.*;

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
    
    private final Client clientWallet;
    private final PrivateKey privateKey;
    private final String keyId;
    private final OpenPaymentsClientOptions options;

    private final HttpClient client;
    private final OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
    private final HttpLogger httpLogger;

    /**
     * The ResourcePath class provides a collection of constant string values
     * that represent predefined resource paths used within the application.
     * These paths typically denote endpoints or resource identifiers for
     * various operations such as payments and quotes.
     */
    private static class ResourcePath {
        public static final String INCOMING_PAYMENTS = "/incoming-payments";
        public static final String OUTGOING_PAYMENTS = "/outgoing-payments";
        public static final String QUOTES = "/quotes";
    }

    /**
     * Creates a default instance of the IOpenPaymentsClient configured with the provided client
     * address, private key, and key ID. This default client uses pre-configured options and
     * components to simplify initialization.
     *
     * @param clientAddress the address of the client wallet endpoint, used for Open Payments interactions
     * @param privateKeyPem the private key in PEM format used for signing requests
     * @param keyId the identifier for the key used during cryptographic operations
     * @return an instance of IOpenPaymentsClient, ready for use in Open Payments operations
     */
    public static IOpenPaymentsClient defaultClient(
            String clientAddress,
            String privateKeyPem,
            String keyId
    ) {
        var pvtKey = getPrivateKeyFromPem(privateKeyPem);
        return new OpenPaymentsHttpClient(
                new Client(clientAddress),
                pvtKey,
                keyId,
                OpenPaymentsClientOptions.DEFAULT,
                HttpClient.newHttpClient(),
                new HttpLogger()
        );
    }

    /**
     * Retrieves a wallet address by sending a GET request to the specified URL.
     *
     * @param url the URL endpoint from which to fetch the wallet address
     * @return a {@code WalletAddress} object representing the fetched wallet address
     * @throws OpenPaymentsClientException if the URL is invalid, the HTTP request fails, or deserialization fails
     */
    @Override
    public WalletAddress getWalletAddress(String url) {
        log.debug("getWalletAddress: {}", url);
        var urlObj = UtilGlobal.toUri(url);

        var request = HttpRequest.newBuilder(urlObj)
                .GET()
                .header(ACCEPT_HEADER.toLowerCase(), APPLICATION_JSON)
                .timeout(Duration.of(this.options.getTimeOutInSeconds(), SECONDS))
                .build();

        return this.send(request, WalletAddress.class);
    }

    /**
     * Retrieves Ed25519 keys from the provided URL.
     *
     * @param url the URL from which to fetch the Ed25519 keys. If the URL does not end with a "/",
     *            it will be appended automatically.
     * @return an instance of {@code Ed25519Keys} containing the fetched keys.
     */
    @Override
    public Ed25519Keys getKeys(String url) {
        log.debug("getKeys: {}", url);
        if (url != null && !url.endsWith("/")) url = String.format("%s/", url);
        var urlObj = UtilGlobal.toUri(String.format("%sjwks.json", url));

        var request = HttpRequest.newBuilder(urlObj)
                .GET()
                .header(ACCEPT_HEADER.toLowerCase(), APPLICATION_JSON)
                .timeout(Duration.of(this.options.getTimeOutInSeconds(), SECONDS))
                .build();

        return this.send(request, Ed25519Keys.class);
    }

    /**
     * Creates a new grant for an incoming payment, assigning the specified actions
     * to the provided receiver wallet address.
     *
     * @param receiver the wallet address of the receiver; cannot be null and must have a non-empty auth server
     * @param actions  optional access actions to be assigned; defaults to read, complete, and create if no actions are provided
     * @return the grant created for the incoming payment
     * @throws IllegalArgumentException if the receiver is null or its auth server is empty
     */
    @Override
    public Grant createGrantIncomingPayment(WalletAddress receiver, AccessAction ... actions) {
        log.debug("createGrantIncomingPayment: {}", receiver);

        ensureNotNull(receiver, "WalletAddress receiver cannot be null");
        ensureNotNullOrEmpty(receiver.getAuthServer(), "AuthServer cannot be empty");

        return this.sendGrantRequest(
                receiver,
                AccessItemType.incomingPayment,
                Set.of(AccessAction.read, AccessAction.complete, AccessAction.create),
                actions
        );
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
        log.debug("createIncomingPayment: ({},{},{})", receiver, ipGrant, amount);

        ensureNotNull(receiver, "WalletAddress receiver cannot be null");
        ensureNotNull(ipGrant, "Grant for incomingPayment cannot be null");
        ensureNotNull(amount, "BigDecimal amount cannot be null");

        var paymentRequest = PaymentRequest.build(
                receiver,
                amount,
                this.options.getTransactionExpirationInSeconds()
        );

        var resourceServer = receiver.getResourceServerAsURI();
        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(paymentRequest)
                .target(this.buildResourceUrl(resourceServer, ResourcePath.INCOMING_PAYMENTS))
                .accessToken(this.extractAccessToken(ipGrant))
                .getRequest(this.options);

        return this.send(request, IncomingPayment.class);
    }

    /**
     * Creates a grant quote for the specified sender with the given access actions.
     *
     * @param sender the wallet address of the grant recipient; must not be null
     * @param actions optional variable arguments specifying the access actions to be granted;
     *                if null or empty, default actions (read, create) will be granted
     * @return the created {@link Grant} instance containing the details of the grant quote
     * @throws NullPointerException if the sender parameter is null
     */
    @Override
    public Grant createGrantQuote(WalletAddress sender, AccessAction ... actions) {
        log.debug("createGrantQuote: {}", sender);
        ensureNotNull(sender, "WalletAddress sender cannot be null");

        return this.sendGrantRequest(
                sender,
                AccessItemType.quote,
                Set.of(AccessAction.read, AccessAction.create),
                actions
        );
    }

    /**
     * @see IOpenPaymentsClient#createQuote(String, WalletAddress, IncomingPayment, Optional, Optional)
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
    public Quote createQuote(
            String quoteToken,
            WalletAddress sender,
            IncomingPayment incomingPayment,
            Optional<Amount> debitAmount,
            Optional<Amount> receiveAmount
    ) {
        log.debug("createQuote: ({},{},{},{})", sender, incomingPayment, debitAmount, receiveAmount);

        ensureNotNullOrEmpty(quoteToken, "Quote token cannot be null or empty");
        ensureNotNull(sender, "PaymentPointer sender cannot be null");
        ensureNotNull(incomingPayment, "IncomingPayment cannot be null");

        var quoteRequest = QuoteRequest.build(
                sender.getIdAsURI(),
                incomingPayment.getId().toString(),
                debitAmount,
                receiveAmount,
                QuoteRequest.ILP_METHOD
        );

        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(quoteRequest)
                .target(this.buildResourceUrl(sender.getResourceServerAsURI(), ResourcePath.QUOTES))
                .accessToken(quoteToken)
                .getRequest(this.options);

        return this.send(request, Quote.class);
    }

    /**
     * Retrieves a Quote object from the specified URI using the provided grant for authentication.
     *
     * @param quoteUri The URI from which the quote will be fetched. Must not be null.
     * @param grant The authentication grant used to access the specified URI. Must not be null.
     * @return A Quote object representing the data retrieved from the specified URI.
     * @throws NullPointerException If either {@code quoteUri} or {@code grant} are null.
     */
    @Override
    public Quote getQuote(URI quoteUri, Grant grant) {
        log.debug("getQuote: {}", quoteUri);
        UtilGlobal.ensureNotNull(quoteUri, "quoteUri cannot be null");
        UtilGlobal.ensureNotNull(grant, "Grant cannot be null");

        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .GET()
                .target(quoteUri)
                .accessToken(this.extractAccessToken(grant))
                .getRequest(this.options);

        return this.send(request, Quote.class);
    }

    /**
     * Creates an outgoing payment grant continuation based on the provided input parameters.
     * This involves constructing an access request for an outgoing payment and sending it
     * to the authorization server of the sender's wallet.
     *
     * @param sender       The wallet address of the entity initiating the payment. Must not be null.
     * @param debitAmount  The amount to be debited for the outgoing payment. Must not be null.
     * @param returnUrl    The URL to which the user will be redirected after processing the grant. Must not be null.
     * @param nonce        A unique identifier to prevent replay attacks. Must not be null or empty.
     * @return OutgoingPayment object representing the created grant continuation.
     *         This includes details of the outgoing payment request.
     * @throws IllegalArgumentException if any of the input parameters are null or invalid.
     */
    @Override
    public OutgoingPayment createGrantContinuation(
            WalletAddress sender,
            Amount debitAmount,
            URI returnUrl,
            String nonce
    ) {
        log.debug("createGrantContinuation: ({},{},{})", sender, debitAmount, returnUrl);

        ensureNotNull(sender, "PaymentPointer sender cannot be null");
        ensureNotNull(debitAmount, "DebitAmount cannot be null");
        ensureNotNull(returnUrl, "Return URL cannot be null");
        ensureNotNullOrEmpty(nonce, "Nonce cannot be null or empty");

        var accessRequest = GrantRequest.outgoing(
            this.clientWallet,
            AccessItemType.outgoingPayment,
            Set.of(AccessAction.read, AccessAction.create),
            sender.getIdAsURI(), debitAmount
        ).redirectInteract(returnUrl, nonce);

        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(accessRequest)
                .target(sender.getAuthServer())
                .getRequest(this.options);

        return this.send(request, OutgoingPayment.class);
    }

    /**
     * Finalizes the creation of a grant by sending a signature request.
     * This method prepares and dispatches a request to finalize a grant using the provided outgoing payment and interaction reference.
     *
     * @param outgoingPayment The outgoing payment information used in the grant creation process. Must not be null.
     * @param interactRef The interaction reference used in the grant creation process. Must not be null or empty.
     * @return The finalized grant object created as a result of the request.
     * @throws IllegalArgumentException If any of the parameters are null or invalid.
     */
    @Override
    public Grant createGrantFinalize(OutgoingPayment outgoingPayment, String interactRef) {
        log.debug("createGrantFinalize: ({},{})", outgoingPayment, interactRef);

        ensureNotNull(outgoingPayment, "OutgoingPayment cannot be null");
        ensureNotNullOrEmpty(interactRef, "Interact reference cannot be null or empty");

        var ref = InteractRef.build(interactRef);
        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(ref)
                .target(outgoingPayment.getPaymentContinue().getUri())
                .accessToken(this.extractContinueAccessToken(outgoingPayment))
                .getRequest(this.options);

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
        log.debug("createFinalizePayment (Quote): ({},{})", sender, quote);

        ensureNotNull(finalized, "Grant finalized cannot be null");
        ensureNotNull(sender, "WalletAddress sender cannot be null");
        ensureNotNull(quote, "Quote cannot be null");

        var outgoingPayment = new OutgoingPaymentRequest();
        outgoingPayment.setQuoteId(quote.getId());
        outgoingPayment.setWalletAddress(sender.getIdAsURI());

        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(outgoingPayment)
                .target(this.buildResourceUrl(sender.getResourceServerAsURI(), ResourcePath.OUTGOING_PAYMENTS))
                .accessToken(this.extractAccessToken(finalized))
                .getRequest(this.options);

        return this.send(request, Payment.class);
    }

    /**
     * Creates a finalized payment by processing the provided details, including the finalized grant,
     * sender's wallet address, incoming payment details, and the debit amount.
     *
     * @param finalized the grant object representing the finalized grant; must not be null
     * @param sender the wallet address of the sender initiating the payment; must not be null
     * @param incomingPayment the incoming payment object containing payment details; must not be null
     * @param debitAmount the amount to be debited for the payment; must not be null
     * @return a Payment object representing the finalized outgoing payment
     * @throws IllegalArgumentException if any of the required parameters are null
     */
    @Override
    public Payment createFinalizePayment(
            Grant finalized,
            WalletAddress sender,
            IncomingPayment incomingPayment,
            Amount debitAmount
    ) {
        log.debug("createFinalizePayment (IncomingPayment): ({},{},{})", sender, incomingPayment, debitAmount);

        ensureNotNull(finalized, "Grant finalized cannot be null");
        ensureNotNull(sender, "WalletAddress sender cannot be null");
        ensureNotNull(incomingPayment, "IncomingPayment cannot be null");
        ensureNotNull(incomingPayment.getId(), "IncomingPayment Id cannot be null");
        ensureNotNull(debitAmount, "DebitAmount cannot be null");

        var outgoingPayment = new OutgoingPaymentRequest();
        outgoingPayment.setIncomingPayment(incomingPayment.getId());
        outgoingPayment.setDebitAmount(debitAmount);
        outgoingPayment.setWalletAddress(sender.getIdAsURI());

        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(outgoingPayment)
                .target(this.buildResourceUrl(sender.getResourceServerAsURI(), ResourcePath.OUTGOING_PAYMENTS))
                .accessToken(this.extractAccessToken(finalized))
                .getRequest(this.options);

        return this.send(request, Payment.class);
    }

    /**
     * Retrieves an incoming payment based on the provided payment details and grant.
     *
     * @param incomingPaymentUri the {@code IncomingPayment} object containing the payment details; must not be null
     * @param grant the {@code Grant} object representing the access grant; must not be null
     * @return the {@code IncomingPayment} object fetched from the server based on the given payment details
     */
    @Override
    public IncomingPayment getIncomingPayment(URI incomingPaymentUri, Grant grant) {
        log.debug("getIncomingPayment: {}", incomingPaymentUri);
        UtilGlobal.ensureNotNull(incomingPaymentUri, "incomingPaymentUri cannot be null");
        UtilGlobal.ensureNotNull(grant, "Grant cannot be null");

        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .GET()
                .target(incomingPaymentUri)
                .accessToken(this.extractAccessToken(grant))
                .getRequest(this.options);

        return this.send(request, IncomingPayment.class);
    }

    /**
     * Completes the creation process of an incoming payment.
     *
     * @param incomingPaymentToComplete the incoming payment object to complete, must not be null
     * @param ipCompleteGrant the grant containing necessary access rights to complete payment, must not be null
     * @return the completed IncomingPayment object
     * @throws IllegalArgumentException if the incoming payment or its ID is null
     */
    @Override
    public IncomingPayment createIncomingPaymentComplete(
            IncomingPayment incomingPaymentToComplete,
            Grant ipCompleteGrant
    ) {
        log.debug("createIncomingPaymentComplete: ({})", incomingPaymentToComplete);

        ensureNotNull(incomingPaymentToComplete, "IncomingPayment cannot be null");
        var inPaymentId = incomingPaymentToComplete.getId();
        ensureNotNull(inPaymentId, "IncomingPayment Id cannot be null");

        var pathForPost = URI.create(
                String.format("%s/complete", inPaymentId)
        );
        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST()
                .target(pathForPost)
                .accessToken(this.extractAccessToken(ipCompleteGrant))
                .getRequest(this.options);
        return this.send(request, IncomingPayment.class);
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
                this.handleHttpError(response);
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
     * Sends a grant request to the authorization server to request access permissions
     * for a specific type of access item and actions.
     *
     * @param auth the authorization server to which the grant request is sent
     * @param type the type of access item for which the grant is requested
     * @param defaultForEmpty the default set of access actions to use if no actions are explicitly specified
     * @param actions the specific access actions requested, can be empty or null
     * @return a {@code Grant} object representing the grant received from the authorization server
     * if the request is successful
     */
    private Grant sendGrantRequest(
            IAuthServer auth,
            AccessItemType type,
            Set<AccessAction> defaultForEmpty,
            AccessAction ... actions
    ) {
        assert defaultForEmpty != null : "defaultForEmpty must not be null";

        var perm = actions == null || actions.length == 0 ? defaultForEmpty : Set.of(actions);
        var accessRequest = GrantRequest.build(this.clientWallet, type, perm);

        var request = new SignatureRequestBuilder(this.privateKey, this.keyId, this.mapper)
                .POST(accessRequest)
                .target(auth.getAuthServer())
                .getRequest(this.options);

        return this.send(request, Grant.class);
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
        var error = mapper.readError(response.body(), statusCode);

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
     * Constructs a complete resource URI by concatenating the given base URI and path.
     * This method ensures the proper formatting of the path and combines it with the
     * base URI to form a valid URI string.
     *
     * @param baseUri the base URI that serves as the starting point for the resource URL;
     *                must not be null
     * @param path the additional path to append to the base URI; must not be null
     * @return the resulting URI formed by combining the base URI with the provided path
     * @throws IllegalArgumentException if either {@code baseUri} or {@code path} is null
     */
    private URI buildResourceUrl(URI baseUri, String path) {
        ensureNotNull(baseUri, "Base URI cannot be null");
        ensureNotNull(path, "Path cannot be null");
        var normalizedPath = path.startsWith("/") ? path : "/" + path;
        var resolved = baseUri.toString().concat(normalizedPath);
        return URI.create(resolved);
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
