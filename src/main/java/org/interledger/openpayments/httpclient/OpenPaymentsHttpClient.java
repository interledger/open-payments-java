package org.interledger.openpayments.httpclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.interledger.openpayments.*;
import org.interledger.openpayments.exception.OpenPaymentsAPIException;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.model.IAuthServer;
import org.interledger.openpayments.model.IResourceServer;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.*;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.incomingpayment.IncomingPaymentsResult;
import org.interledger.openpayments.model.key.Ed25519Keys;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPaymentRequest;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPaymentsResult;
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

    private IOpenPaymentsAuthClient cAuth;
    private IOpenPaymentsWalletAddressClient cWalletAddress;
    private IOpenPaymentsQuoteClient cQuote;
    private IOpenPaymentsPaymentClient cPayment;

    /**
     * Abstract base class for HTTP clients interacting with Open Payments services.
     * This class serves as a foundational structure for implementing clients that utilize
     * an {@code OpenPaymentsHttpClient} to perform HTTP operations.
     *
     * Subclasses of {@code AHttpOPBaseClient} can extend its functionality by implementing
     * specific methods for interacting with various endpoints of Open Payments.
     *
     * Responsibilities of this class include holding a reference to the provided
     * {@code OpenPaymentsHttpClient}, which is used for making HTTP requests.
     *
     * The lifecycle of the HTTP client instance must be managed externally to ensure proper
     * resource handling and cleanup.
     */
    private abstract static class AHttpOPBaseClient {
        protected OpenPaymentsHttpClient baseClient;

        public AHttpOPBaseClient(OpenPaymentsHttpClient httpClient) {
            this.baseClient = httpClient;
        }
    }

    /**
     * An implementation of {@code IOpenPaymentsWalletAddressClient} that utilizes HTTP requests
     * to interact with Open Payments services. This class provides methods to retrieve wallet
     * addresses and Ed25519 keys using the provided HTTP client.
     */
    private static class HttpOPWalletAddressClient extends AHttpOPBaseClient implements IOpenPaymentsWalletAddressClient {
        public HttpOPWalletAddressClient(OpenPaymentsHttpClient httpClient) {
            super(httpClient);
        }

        /**
         * Retrieves a wallet address by sending a GET request to the specified URL.
         *
         * @param url the URL endpoint from which to fetch the wallet address
         * @return a {@code WalletAddress} object representing the fetched wallet address
         * @throws OpenPaymentsClientException if the URL is invalid, the HTTP request fails, or deserialization fails
         */
        @Override
        public WalletAddress get(String url) {
            log.debug("get: {}", url);
            var urlObj = UtilGlobal.toUri(url);

            var request = HttpRequest.newBuilder(urlObj)
                    .GET()
                    .header(ACCEPT_HEADER.toLowerCase(), APPLICATION_JSON)
                    .timeout(Duration.of(this.baseClient.options.getTimeOutInSeconds(), SECONDS))
                    .build();

            return this.baseClient.send(request, WalletAddress.class);
        }

        /**
         * Retrieves Ed25519 keys from the provided URL.
         *
         * @param url the URL from which to fetch the Ed25519 keys. If the URL does not end with a "/",
         *            it will be appended automatically.
         * @return an instance of {@code Ed25519Keys} containing the fetched keys.
         */
        @Override
        public Ed25519Keys keys(String url) {
            log.debug("keys: {}", url);
            if (url != null && !url.endsWith("/")) url = String.format("%s/", url);
            var urlObj = UtilGlobal.toUri(String.format("%sjwks.json", url));

            var request = HttpRequest.newBuilder(urlObj)
                    .GET()
                    .header(ACCEPT_HEADER.toLowerCase(), APPLICATION_JSON)
                    .timeout(Duration.of(this.baseClient.options.getTimeOutInSeconds(), SECONDS))
                    .build();

            return this.baseClient.send(request, Ed25519Keys.class);
        }
    }

    /**
     * HttpOPQuoteClient is a client implementation for interacting with the Open Payments
     * quotes API. This class extends the {@code AHttpOPBaseClient} and implements the
     * {@code IOpenPaymentsQuoteClient} interface to provide functionality for creating
     * and retrieving payment quotes.
     *
     * This client uses an instance of {@code OpenPaymentsHttpClient} for performing
     * HTTP operations and includes methods for securely handling quote-related requests.
     */
    private static class HttpOPQuoteClient extends AHttpOPBaseClient implements IOpenPaymentsQuoteClient {
        public HttpOPQuoteClient(OpenPaymentsHttpClient httpClient) {
            super(httpClient);
        }

        /**
         * @see IOpenPaymentsQuoteClient#create(String, WalletAddress, IncomingPayment, Optional, Optional)
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
        public Quote create(
                String quoteToken,
                WalletAddress sender,
                IncomingPayment incomingPayment,
                Optional<Amount> debitAmount,
                Optional<Amount> receiveAmount
        ) {
            log.debug("create: ({},{},{},{})", sender, incomingPayment, debitAmount, receiveAmount);

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

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST(quoteRequest)
                    .target(this.baseClient.buildResourceUrl(sender.getResourceServerAsURI(), ResourcePath.QUOTES))
                    .accessToken(quoteToken)
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, Quote.class);
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
        public Quote get(URI quoteUri, Grant grant) {
            log.debug("get: {}", quoteUri);
            UtilGlobal.ensureNotNull(quoteUri, "quoteUri cannot be null");
            UtilGlobal.ensureNotNull(grant, "Grant cannot be null");

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .GET()
                    .target(quoteUri)
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, Quote.class);
        }
    }

    /**
     * The {@code HttpOPPayClient} class provides methods for interacting with the OpenPayments API
     * to manage outgoing and incoming payments, grants, and related payment entities.
     * This class extends the base HTTP client for OpenPayments and implements the payment client functionality.
     *
     * This client allows creation, retrieval, and management of payments and grants through
     * appropriate API calls. It is designed to handle both outgoing and incoming payments,
     * providing functionality to perform payment finalization, create grants, and interact
     * with payment endpoints.
     *
     * Superclasses:
     * - {@code org.interledger.openpayments.httpclient.OpenPaymentsHttpClient.AHttpOPBaseClient}
     * - {@code org.interledger.openpayments.IOpenPaymentsPaymentClient}
     *
     * Constructor:
     * {@code public HttpOPPayClient(OpenPaymentsHttpClient httpClient)}
     * - Initializes the client with the provided HTTP client instance for OpenPayments interaction.
     *
     * Primary Methods:
     * - {@code createGrantFinalize}: Finalizes the creation of a grant by sending a signature request.
     * - {@code createIncomingPayment}: Creates a new incoming payment for a specified receiver and amount, authorized by a grant.
     * - {@code createFinalizePayment}: Creates and finalizes a payment using the provided finalized grant, sender, and payment details.
     * - {@code getIncomingPayment}: Retrieves details of a specific incoming payment by its URI and grant.
     * - {@code getIncomingPayments}: Retrieves a list of incoming payments associated with a specific wallet address and resource server.
     * - {@code createIncomingPaymentComplete}: Completes the creation process of an incoming payment.
     * - {@code getOutgoingPayment}: Retrieves details of a specific outgoing payment by its URI and grant.
     * - {@code getOutgoingPayments}: Retrieves a list of outgoing payments associated with a specific wallet address.
     */
    private static class HttpOPPayClient extends AHttpOPBaseClient implements IOpenPaymentsPaymentClient {
        public HttpOPPayClient(OpenPaymentsHttpClient httpClient) {
            super(httpClient);
        }

        /**
         * @see IOpenPaymentsPaymentClient#incomingPayment(WalletAddress,Grant,BigDecimal)
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
        public IncomingPayment incomingPayment(WalletAddress receiver, Grant ipGrant, BigDecimal amount) {
            log.debug("incomingPayment: ({},{},{})", receiver, ipGrant, amount);

            ensureNotNull(receiver, "WalletAddress receiver cannot be null");
            ensureNotNull(ipGrant, "Grant for incomingPayment cannot be null");
            ensureNotNull(amount, "BigDecimal amount cannot be null");

            var paymentRequest = PaymentRequest.build(
                    receiver,
                    amount,
                    this.baseClient.options.getTransactionExpirationInSeconds()
            );

            var resourceServer = receiver.getResourceServerAsURI();
            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST(paymentRequest)
                    .target(this.baseClient.buildResourceUrl(resourceServer, ResourcePath.INCOMING_PAYMENTS))
                    .accessToken(this.baseClient.extractAccessToken(ipGrant))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, IncomingPayment.class);
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
        public Payment finalizePayment(Grant finalized, WalletAddress sender, Quote quote) {
            log.debug("finalizePayment (Quote): ({},{})", sender, quote);

            ensureNotNull(finalized, "Grant finalized cannot be null");
            ensureNotNull(sender, "WalletAddress sender cannot be null");
            ensureNotNull(quote, "Quote cannot be null");

            var outgoingPayment = new OutgoingPaymentRequest();
            outgoingPayment.setQuoteId(quote.getId());
            outgoingPayment.setWalletAddress(sender.getIdAsURI());

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST(outgoingPayment)
                    .target(this.baseClient.buildResourceUrl(sender.getResourceServerAsURI(), ResourcePath.OUTGOING_PAYMENTS))
                    .accessToken(this.baseClient.extractAccessToken(finalized))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, Payment.class);
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
        public Payment finalizePayment(
                Grant finalized,
                WalletAddress sender,
                IncomingPayment incomingPayment,
                Amount debitAmount
        ) {
            log.debug("finalizePayment (IncomingPayment): ({},{},{})", sender, incomingPayment, debitAmount);

            ensureNotNull(finalized, "Grant finalized cannot be null");
            ensureNotNull(sender, "WalletAddress sender cannot be null");
            ensureNotNull(incomingPayment, "IncomingPayment cannot be null");
            ensureNotNull(incomingPayment.getId(), "IncomingPayment Id cannot be null");
            ensureNotNull(debitAmount, "DebitAmount cannot be null");

            var outgoingPayment = new OutgoingPaymentRequest();
            outgoingPayment.setIncomingPayment(incomingPayment.getId());
            outgoingPayment.setDebitAmount(debitAmount);
            outgoingPayment.setWalletAddress(sender.getIdAsURI());

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST(outgoingPayment)
                    .target(this.baseClient.buildResourceUrl(sender.getResourceServerAsURI(), ResourcePath.OUTGOING_PAYMENTS))
                    .accessToken(this.baseClient.extractAccessToken(finalized))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, Payment.class);
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

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .GET()
                    .target(incomingPaymentUri)
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, IncomingPayment.class);
        }

        /**
         * Retrieves a list of incoming payments associated with a given wallet address.
         *
         * @param resourceServer The resource server providing the payment information.
         *                        This parameter must not be null.
         * @param walletAddressUri The URI of the wallet address for which incoming payments are being queried.
         *                         This parameter must not be null.
         * @param grant The grant object containing the necessary authorization details.
         *              This parameter must not be null.
         * @return An instance of {@code IncomingPaymentsResult} containing the list of incoming payments.
         *         Never returns null, but the result may contain an empty list if no payments are found.
         */
        @Override
        public IncomingPaymentsResult getIncomingPayments(
                IResourceServer resourceServer,
                URI walletAddressUri,
                Grant grant
        ) {
            log.debug("getIncomingPayments: {}", walletAddressUri);
            UtilGlobal.ensureNotNull(resourceServer, "ResourceServer cannot be null");
            UtilGlobal.ensureNotNull(walletAddressUri, "WalletAddressUri cannot be null");
            UtilGlobal.ensureNotNull(grant, "Grant cannot be null");

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .GET()
                    .target(String.format("%s/incoming-payments?wallet-address=%s", resourceServer.getResourceServer(), walletAddressUri))
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, IncomingPaymentsResult.class);
        }

        /**
         * Completes the creation process of an incoming payment.
         *
         * @param incomingPaymentToComplete the incoming payment object to complete, must not be null
         * @param grant the grant containing necessary access rights to complete payment, must not be null
         * @return the completed IncomingPayment object
         * @throws IllegalArgumentException if the incoming payment or its ID is null
         */
        @Override
        public IncomingPayment incomingPaymentComplete(
                IncomingPayment incomingPaymentToComplete,
                Grant grant
        ) {
            log.debug("incomingPaymentComplete: ({})", incomingPaymentToComplete);

            ensureNotNull(incomingPaymentToComplete, "IncomingPayment cannot be null");
            var inPaymentId = incomingPaymentToComplete.getId();
            ensureNotNull(inPaymentId, "IncomingPayment Id cannot be null");

            var pathForPost = URI.create(
                    String.format("%s/complete", inPaymentId)
            );
            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST()
                    .target(pathForPost)
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);
            return this.baseClient.send(request, IncomingPayment.class);
        }

        /**
         * Retrieves an outgoing payment using the specified URI and grant request.
         *
         * @param outgoingPaymentUri the URI of the outgoing payment to retrieve; must not be null
         * @param grant the grant request containing authorization details; must not be null
         * @return the {@code OutgoingPayment} object retrieved from the specified URI
         * @throws NullPointerException if {@code outgoingPaymentUri} or {@code grantRequest} is null
         */
        @Override
        public OutgoingPayment getOutgoingPayment(URI outgoingPaymentUri, Grant grant) {
            log.debug("getOutgoingPayment: {}", outgoingPaymentUri);
            UtilGlobal.ensureNotNull(outgoingPaymentUri, "OutgoingPaymentUri cannot be null");
            UtilGlobal.ensureNotNull(grant, "Grant cannot be null");

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .GET()
                    .target(outgoingPaymentUri)
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, OutgoingPayment.class);
        }

        /**
         * Retrieves outgoing payment information associated with a specified wallet address.
         *
         * @param resourceServer The resource server instance that provides endpoint details.
         * @param walletAddressUri The URI of the wallet address for which outgoing payments are being fetched.
         * @param grant The grant object containing the necessary credentials or tokens for authentication.
         * @return An {@link OutgoingPaymentsResult} object containing the details of the outgoing payments.
         * @throws IllegalArgumentException If the walletAddressUri or grant parameter is null.
         */
        @Override
        public OutgoingPaymentsResult getOutgoingPayments(
                IResourceServer resourceServer,
                URI walletAddressUri,
                Grant grant
        ) {
            log.debug("getOutgoingPayments: {}", walletAddressUri);
            UtilGlobal.ensureNotNull(resourceServer, "ResourceServer cannot be null");
            UtilGlobal.ensureNotNull(walletAddressUri, "WalletAddressUri cannot be null");
            UtilGlobal.ensureNotNull(grant, "Grant cannot be null");

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .GET()
                    .target(String.format("%s/outgoing-payments?wallet-address=%s", resourceServer.getResourceServer(), walletAddressUri))
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, OutgoingPaymentsResult.class);
        }
    }

    /**
     * Implements functionality to facilitate Open Payments (OP) authorization workflows.
     * This class extends {@code AHttpOPBaseClient} and provides methods for creating and managing
     * grants, token rotation, and revocation for the Open Payments protocol.
     * It operates as an HTTP client for connecting to authentication servers and managing wallet-based
     * transactions.
     */
    private static class HttpOPAuthClient extends AHttpOPBaseClient implements IOpenPaymentsAuthClient {
        public HttpOPAuthClient(OpenPaymentsHttpClient httpClient) {
            super(httpClient);
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
        public Grant grantIncomingPayment(WalletAddress receiver, AccessAction ... actions) {
            log.debug("grantIncomingPayment: {}", receiver);

            ensureNotNull(receiver, "WalletAddress receiver cannot be null");
            ensureNotNullOrEmpty(receiver.getAuthServer(), "AuthServer cannot be empty");

            return this.baseClient.sendGrantRequest(
                    receiver,
                    AccessItemType.incomingPayment,
                    Set.of(AccessAction.read, AccessAction.complete, AccessAction.create),
                    actions
            );
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
        public Grant grantQuote(WalletAddress sender, AccessAction ... actions) {
            log.debug("grantQuote: {}", sender);
            ensureNotNull(sender, "WalletAddress sender cannot be null");

            return this.baseClient.sendGrantRequest(
                    sender,
                    AccessItemType.quote,
                    Set.of(AccessAction.read, AccessAction.create),
                    actions
            );
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
        public OutgoingPayment grantContinuation(
                WalletAddress sender,
                Amount debitAmount,
                URI returnUrl,
                String nonce
        ) {
            log.debug("grantContinuation: ({},{},{})", sender, debitAmount, returnUrl);

            ensureNotNull(sender, "PaymentPointer sender cannot be null");
            ensureNotNull(debitAmount, "DebitAmount cannot be null");
            ensureNotNull(returnUrl, "Return URL cannot be null");
            ensureNotNullOrEmpty(nonce, "Nonce cannot be null or empty");

            var accessRequest = GrantRequest.outgoing(
                    this.baseClient.clientWallet,
                    AccessItemType.outgoingPayment,
                    Set.of(AccessAction.read, AccessAction.create),
                    sender.getIdAsURI(), debitAmount
            ).redirectInteract(returnUrl, nonce);

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST(accessRequest)
                    .target(sender.getAuthServer())
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, OutgoingPayment.class);
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
        public Grant grantFinalize(OutgoingPayment outgoingPayment, String interactRef) {
            log.debug("grantFinalize: ({},{})", outgoingPayment, interactRef);

            ensureNotNull(outgoingPayment, "OutgoingPayment cannot be null");
            ensureNotNullOrEmpty(interactRef, "Interact reference cannot be null or empty");

            var ref = InteractRef.build(interactRef);
            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST(ref)
                    .target(outgoingPayment.getPaymentContinue().getUri())
                    .accessToken(this.baseClient.extractContinueAccessToken(outgoingPayment))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, Grant.class);
        }

        /**
         * Rotates an existing token by revoking its current form and generating a new representation.
         *
         * @param authServer The authentication server to target for the token rotation operation.
         * @param tokenId The identifier of the token to be rotated; must not be null.
         * @param grant The authorization grant that provides access to authenticate the operation; must not be null.
         * @return A new Grant object representing the updated authorization after token rotation.
         * @throws OpenPaymentsClientException If the tokenId is null.
         */
        @Override
        public Grant rotateToken(IAuthServer authServer, String tokenId, Grant grant) {
            log.debug("rotateToken: ({})", tokenId);
            ensureNotNull(authServer, "AuthServer cannot be null");
            ensureNotNull(tokenId, "TokenId cannot be null");
            ensureNotNull(grant, "Grant cannot be null");

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .POST()
                    .target(String.format("%s/token/%s", authServer.getAuthServer(), tokenId))
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            return this.baseClient.send(request, Grant.class);
        }

        /**
         * Revokes an access token by communicating with the specified authentication server.
         * Tokens are usually accessible from issued grants, see {@code grant.getAccess().getToken()}
         *
         * @param authServer the authentication server to which the token revocation request is sent
         * @param tokenId the unique identifier of the token to be revoked; must not be null
         * @param grant the grant object containing the access token used for authorization during the request
         */
        @Override
        public void revokeToken(IAuthServer authServer, String tokenId, Grant grant) {
            log.debug("revokeToken: ({})", tokenId);
            ensureNotNull(authServer, "AuthServer cannot be null");
            ensureNotNull(tokenId, "TokenId cannot be null");
            ensureNotNull(grant, "Grant cannot be null");

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .DELETE()
                    .target(String.format("%s/token/%s", authServer.getAuthServer(), tokenId))
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            this.baseClient.send(request);
        }

        /**
         * Cancels a previously issued grant by sending a DELETE request to the access
         * continuation URI associated with the grant. This method ensures that the grant
         * is not null before proceeding, and uses a signature request to securely perform the operation.
         *
         * @param grant The Grant object representing the access to be canceled. This object
         *              must not be null and should contain the required URI and access token
         *              information for the cancellation.
         */
        @Override
        public void grantCancel(Grant grant) {
            log.debug("grantCancel: ({})", grant);

            ensureNotNull(grant, "Grant cannot be null");

            var request = new SignatureRequestBuilder(this.baseClient.privateKey, this.baseClient.keyId, this.baseClient.mapper)
                    .DELETE()
                    .target(grant.getAccessContinue().getUri())
                    .accessToken(this.baseClient.extractAccessToken(grant))
                    .getRequest(this.baseClient.options);

            this.baseClient.send(request);
        }
    }

    /**
     * Authenticates the client and provides an instance of the IOpenPaymentsAuthClient.
     * If the authentication client is not already initialized, it creates a new instance.
     *
     * @return an instance of IOpenPaymentsAuthClient used for managing authentication.
     */
    @Override
    public IOpenPaymentsAuthClient auth() {
        if (this.cAuth == null) this.cAuth = new HttpOPAuthClient(this);
        return this.cAuth;
    }

    /**
     * Provides access to the wallet address client interface. If the wallet address client does not
     * already exist, it initializes a new instance and returns it.
     *
     * @return An instance of {@code IOpenPaymentsWalletAddressClient} representing the wallet
     *         address client.
     */
    @Override
    public IOpenPaymentsWalletAddressClient walletAddress() {
        if (this.cWalletAddress == null) this.cWalletAddress = new HttpOPWalletAddressClient(this);
        return this.cWalletAddress;
    }

    /**
     * Provides an instance of IOpenPaymentsQuoteClient to handle quote-related operations.
     * If the instance is not already created, it initializes a new HttpOPQuoteClient.
     *
     * @return an instance of IOpenPaymentsQuoteClient for handling quotes
     */
    @Override
    public IOpenPaymentsQuoteClient quote() {
        if (this.cQuote == null) this.cQuote = new HttpOPQuoteClient(this);
        return this.cQuote;
    }

    /**
     * Provides access to the payment client, enabling interaction with the
     * payment processing system. Initializes the client if it is not already
     * instantiated.
     *
     * @return an instance of IOpenPaymentsPaymentClient representing the payment client.
     */
    @Override
    public IOpenPaymentsPaymentClient payment() {
        if (this.cPayment == null) this.cPayment = new HttpOPPayClient(this);
        return this.cPayment;
    }

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
     * Sends an HTTP request using the configured HTTP client and processes the response.
     * Logs the outgoing request before sending and throws an exception if the response status code is not 204.
     *
     * @param request the {@code HttpRequest} to be sent using the HTTP client.
     * @throws OpenPaymentsAPIException if the response status code is not 204.
     * @throws OpenPaymentsClientException if there is an I/O error during the request or if the thread is interrupted.
     */
    public void send(HttpRequest request) {
        try {
            this.httpLogger.logRequest(request);
            HttpResponse<Void> response = this.client.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() != 204) {
                throw new OpenPaymentsAPIException(
                        response.statusCode(),
                        "Unable to send! Status code: " + response.statusCode()
                );
            }
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
