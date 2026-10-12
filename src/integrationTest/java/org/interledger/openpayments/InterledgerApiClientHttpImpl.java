package org.interledger.openpayments;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.CardAuthorization;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.grant.PinWorkingKey;
import org.interledger.openpayments.model.key.Ed25519PublicKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Implementation of the Interledger API Client using HTTP for communication.
 * This class handles operations related to retrieving wallet information and
 * facilitating payment transactions by communicating with an Interledger-enabled
 * HTTP server.
 * <p>
 * Class Fields:
 * - client: The HTTP client used to send requests to the Interledger API server.
 * - httpServer: The HTTP server endpoint associated with the API client.
 * - log: Logger instance for capturing log messages and debugging information.
 * <p>
 * Methods:
 * - setUp(): Sets up the necessary configuration before each test case execution.
 * - getClientWallet(): Retrieves and validates client wallet information.
 * - getReceiverWallet(): Fetches and ensures proper receiver wallet information is returned.
 * - getSenderWallet(): Retrieves and validates sender wallet details.
 * - getGrantRequest(): Automates the payment process between sender and receiver wallets.
 */
@Slf4j
class InterledgerApiClientHttpImpl {
    private IOpenPaymentsClient client;
    private SimpleHttpServer httpServer;

    @BeforeEach
    public void setUp() {
        client = OpenPaymentsHttpClient.defaultClient(
            TestHelper.getClientWalletAddress(),
            TestConfiguration.getClientPrivateKey(),
            TestHelper.getClientKeyId()
        );
    }

    @AfterEach
    public void tearDown() {
        if (httpServer != null) {
            httpServer.shutdown();
        }
    }

    /**
     * Validates the retrieval of the client wallet information using the specified
     * client wallet address from the test configuration.
     * <p>
     * This test asserts that the wallet details obtained by the client correspond
     * to the expected client wallet attributes, which include:
     * <p>
     * - Wallet ID matches the client wallet address.
     * - Public name matches the expected identifier.
     * - Asset code and scale match the correct currency attributes.
     * - Auth server URL is associated with the wallet as expected.
     * - Resource server URL is linked to the wallet as expected.
     * <p>
     * The test ensures the wallet address is not null and that all critical parameters
     * conform to predefined configurations to validate the overall functionality and
     * correctness of client wallet retrieval.
     */
    @Test
    public void getClientWallet() {
        var walletAdd = this.client.walletAddress().get(TestHelper.getClientWalletAddress());
        assertNotNull(walletAdd);

        Assertions.assertEquals(TestHelper.getClientWalletAddress(), walletAdd.getId());
        Assertions.assertEquals("test", walletAdd.getPublicName());
        Assertions.assertEquals("USD", walletAdd.getAssetCode());
        Assertions.assertEquals(2, walletAdd.getAssetScale());
        // Auth and Resource server with tenant:
        Assertions.assertEquals("https://auth.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", walletAdd.getAuthServer());
        Assertions.assertEquals("https://ilp.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", walletAdd.getResourceServer());
    }

    /**
     * Tests the retrieval of Ed25519 keys for a client wallet address.
     * <p>
     * This test verifies the correct fetching and validation of Ed25519 keys using the
     * client wallet address obtained from the test configuration. The following assertions
     * are performed as part of the validation process:
     * <p>
     * - Ensures the retrieved key collection is not null.
     * - Confirms that at least one key exists in the collection.
     * - Validates the algorithm ('alg') and curve ('crv') of the first key to ensure they
     * match the expected values ("EdDSA" for the algorithm and "Ed25519" for the curve).
     * <p>
     * The test relies on {@link TestHelper#getClientWalletAddress()} to fetch the client wallet
     * address required for the retrieval process.
     * <p>
     * Assertions:
     * - {@code assertNotNull(keys)}: Validates that the key collection is not null.
     * - {@code assertNotNull(firstKey)}: Ensures that the first key in the collection is not null.
     * - {@code Assertions.assertEquals("EdDSA", firstKey.getAlg())}: Validates the algorithm.
     * - {@code Assertions.assertEquals("Ed25519", firstKey.getCrv())}: Validates the curve type.
     */
    @Test
    public void getKeys() {
        var keys = this.client.walletAddress().keys(TestHelper.getClientWalletAddress());
        assertNotNull(keys);
        var firstKey = (Ed25519PublicKey) keys.getKeys().toArray()[0];
        assertNotNull(firstKey);

        Assertions.assertEquals("EdDSA", firstKey.getAlg());
        Assertions.assertEquals("Ed25519", firstKey.getCrv());
    }

    /**
     * Validates the retrieval of the receiver wallet information using the specified
     * receiver wallet address from the test configuration.
     * <p>
     * This test ensures that the wallet details fetched by the client match the
     * expected receiver wallet configuration, including:
     * <p>
     * - Wallet ID matches the receiver wallet address.
     * - Public name matches the expected identifier.
     * - Asset code and scale correspond to the predefined currency attributes.
     * - Auth server URL is correctly associated with the wallet.
     * - Resource server URL is properly linked to the wallet.
     * <p>
     * The test asserts that the wallet address obtained is not null and that all
     * critical parameters adhere to predefined configurations, verifying the
     * functionality and correctness of receiver wallet retrieval.
     */
    // Get receiver wallet data
    @Test
    public void getReceiverWallet() {
        var wallet = this.client.walletAddress().get(TestHelper.getReceiverWalletAddress());
        assertNotNull(wallet);

        Assertions.assertEquals(TestHelper.getReceiverWalletAddress(), wallet.getId());
        Assertions.assertEquals("test", wallet.getPublicName());
        Assertions.assertEquals("USD", wallet.getAssetCode());
        Assertions.assertEquals(2, wallet.getAssetScale());
        Assertions.assertEquals("https://auth.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getAuthServer());
        Assertions.assertEquals("https://ilp.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getResourceServer());
    }

    /**
     * Validates the retrieval of the sender wallet information using the given
     * sender wallet address from the configuration.
     * <p>
     * This test ensures that the wallet details fetched by the client match the
     * expected sender wallet configuration, including:
     * - Wallet ID matches the sender wallet address.
     * - Public name matches the expected identifier.
     * - Asset code and scale align with the designated currency attributes.
     * - Auth server URL is correctly associated with the wallet.
     * - Resource server URL is correctly linked to the wallet.
     * <p>
     * The test asserts that the wallet address obtained is not null, and that all
     * key parameters conform to the predefined values in the test configuration.
     */
    @Test
    public void getSenderWallet() {
        var wallet = client.walletAddress().get(TestHelper.getSenderWalletAddress());
        assertNotNull(wallet);

        Assertions.assertEquals(TestHelper.getSenderWalletAddress(), wallet.getId());
        Assertions.assertEquals("test", wallet.getPublicName());
        Assertions.assertEquals("USD", wallet.getAssetCode());
        Assertions.assertEquals(2, wallet.getAssetScale());
        Assertions.assertEquals("https://auth.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getAuthServer());
        Assertions.assertEquals("https://ilp.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getResourceServer());
    }

    /**
     * Automates the entire process of facilitating and finalizing a payment transaction
     * between a sender and receiver wallet in a structured sequence of steps.
     * <p>
     * This method covers the following tasks:
     * <p>
     * 1. Receiver Wallet Discovery:
     * Obtains and validates the receiver's wallet information, including supported currencies,
     * authorization server, and resource server endpoints.
     * <p>
     * 2. Receiver Grant Request:
     * Creates an access grant for the receiver allowing operations on their wallet to manage incoming payments.
     * <p>
     * 3. Incoming Payment Creation:
     * Defines an incoming payment request with specified amount and currency on the receiver's wallet.
     * <p>
     * 4. Sender Wallet Discovery:
     * Acquires and verifies information about the sender's wallet, including name and supported assets.
     * <p>
     * 5. Sender Quote Request Grant:
     * Establishes permissions for accessing quotes on the sender's wallet for transaction calculations.
     * <p>
     * 6. Quote Generation:
     * Calculates the payment details for the transaction, including sender's cost and receiver's receipt,
     * factoring in applicable fees.
     * <p>
     * 7. Payment Continuation and User Interaction:
     * Constructs a redirect URL for user interaction to approve the payment in their wallet.
     * Await input from the user with the required parameters from the redirect.
     * <p>
     * 8. Payment Status Check (Before Finalization):
     * Verifies the payment status at this stage before proceeding with the finalization process.
     * <p>
     * 9. Payment Finalization:
     * Finalizes the payment transaction upon successful user approval and authorization.
     * This step includes the execution of the payment using the finalized grant.
     * <p>
     * 10. Payment Status Monitoring:
     * Monitors the payment until its completion status is confirmed or a timeout is reached.
     *
     */
    /**
     * Card (EMV) authorization grant (Open Payments v1.4.0).
     * Skipped unless {@code card.tlv.data} is configured in test-config.properties.
     */
    @Test
    public void openPaymentsCardAuthorizationGrant() {
        var tlvData = TestConfiguration.getCardTlvData();
        assumeTrue(tlvData != null, "card.tlv.data not configured - skipping card authorization test");

        var requestId = Optional.ofNullable(TestConfiguration.getCardRequestId())
            .orElse(UUID.randomUUID().toString());
        var pinBlock = TestConfiguration.getCardPinBlock();
        var cardAuthorization = pinBlock == null
            ? CardAuthorization.build(tlvData, requestId)
            : CardAuthorization.build(tlvData, requestId, pinBlock, PinWorkingKey.build(
                TestConfiguration.getCardPwkKey(),
                TestConfiguration.getCardPwkKvc(),
                TestConfiguration.getCardPwkIv()));

        // STEP 1: Create an incoming payment on the receiver's wallet (the merchant).
        var receiverWallet = client.walletAddress().get(TestHelper.getReceiverWalletAddress());
        var ipGrant = this.client.auth().grant().incomingPayment(receiverWallet);
        var incomingPayment = this.client.payment().createIncoming(receiverWallet, ipGrant, BigDecimal.valueOf(2.50));
        assertNotNull(incomingPayment);

        // STEP 2: Request a non-interactive outgoing payment grant authorized by the card.
        var senderWallet = this.client.walletAddress().get(TestHelper.getSenderWalletAddress());
        var grant = this.client.auth().grant().cardAuthorization(
            senderWallet,
            incomingPayment.getId(),
            incomingPayment.getIncomingAmount(),
            cardAuthorization
        );
        assertNotNull(grant);
        assertNotNull(grant.getAccess());

        var result = grant.getAccess().getAccess().iterator().next().getCardAuthorization();
        assertNotNull(result);
        log.info("Card authorization result (TLV): " + result.getTlvData());
    }

    @Test
    public void openPaymentsCreateOutgoingFromQuote() throws InterruptedException {
        // STEP 0: RECEIVER WALLET DISCOVERY
        // Get the receiver's wallet information including supported currencies,
        // authorization server, and resource server endpoints
        log.info("********");
        log.info("STEP 0: Get receiver wallet information");
        var receiverWallet = client.walletAddress().get(TestHelper.getReceiverWalletAddress());
        assertNotNull(receiverWallet);
        log.info("Receiver wallet discovered: " + receiverWallet.getPublicName() + " (" + receiverWallet.getAssetCode() + ")");

        // STEP 1: RECEIVER GRANT REQUEST
        // Create an access grant that allows ILF to create incoming payment requests
        // on behalf of the receiver. This grant provides permissions to read, complete, and create
        // incoming payments on the receiver's wallet.
        log.info("********");
        log.info("STEP 1: Create pending grant for receiver");
        var grantRequest = this.client.auth().grant().incomingPayment(receiverWallet);
        assertNotNull(grantRequest);
        log.info("Receiver grant created with token: " + grantRequest.getAccess().getToken().substring(0, 10) + "...");

        // STEP 2: INCOMING PAYMENT CREATION
        // Create an incoming payment request for €12.34 on the receiver's wallet.
        // This establishes the payment destination and amount that the sender will pay to.
        log.info("********");
        log.info("STEP 2: Create incoming payment request");
        var incomingPayment = this.client.payment().createIncoming(receiverWallet, grantRequest, BigDecimal.valueOf(11.25));
        assertNotNull(incomingPayment);
        log.info("Incoming payment created: " + incomingPayment.getId() + " for " +
            incomingPayment.getIncomingAmount().getValue() + " " + incomingPayment.getIncomingAmount().getAssetCode());

        // STEP 2.5: SENDER WALLET DISCOVERY  
        // In a real scenario, the user would enter their wallet address at this point.
        // We simulate this by getting the sender's wallet information.
        log.info("********");
        log.info("STEP 2.5: Get sender wallet information (user enters their wallet)");
        var senderWallet = this.client.walletAddress().get(TestHelper.getSenderWalletAddress());
        assertNotNull(senderWallet);
        log.info("Sender wallet discovered: " + senderWallet.getPublicName() + " (" + senderWallet.getAssetCode() + ")");

        // STEP 3: SENDER QUOTE REQUEST GRANT
        // Create an access grant that allows ILF to request quotes on the sender's wallet.
        // This is needed to calculate transaction fees and exchange rates.
        log.info("********");
        log.info("STEP 3: Create quote request grant for sender");
        var quoteRequest = this.client.auth().grant().quote(senderWallet);
        assertNotNull(quoteRequest);
        log.info("Quote request grant created for sender wallet");

        // STEP 4: QUOTE GENERATION
        // Generate a quote that calculates the exact amount the sender needs to pay
        // including any transaction fees. The quote links the sender to the incoming payment.
        log.info("********");
        log.info("STEP 4: Generate payment quote");
        var quote = this.client.quote().create(
            quoteRequest.getAccess().getToken(),
            senderWallet,
            incomingPayment,
            Optional.empty(),
            Optional.empty()
        );
        assertNotNull(quote);
        log.info("Quote generated - Sender pays: " + quote.getDebitAmount().getValue() + " " +
            quote.getDebitAmount().getAssetCode() +
            ", Receiver gets: " + quote.getReceiveAmount().getValue() + " " +
            quote.getReceiveAmount().getAssetCode()
        );

        // STEP 5: PAYMENT CONTINUATION & USER INTERACTION
        // Create a redirect URL that the user must visit to authorize the payment in their wallet.
        // This implements the interactive authorization flow required by Open Payments.
        log.info("********");
        log.info("STEP 5: Create user interaction redirect");
        int port = 8888;
        this.httpServer = new SimpleHttpServer(port);
        this.httpServer.start();

        var urlToOpen = "http://localhost:%d?paymentId=1234".formatted(port);
        // Create outing payment from quote:
        var opContinueInteract = this.client.auth().grant().continuation(
            senderWallet,
            quote.getDebitAmount(),
            URI.create(urlToOpen),
            "test"
        );

        final String interactReference = performInteractionAndContinue(opContinueInteract.getInteract().getRedirect().toString());

        // STEP 5.5: CHECK PAYMENT STATUS BEFORE FINALIZATION
        // Verify the payment state before attempting to finalize it
        log.info("********");
        log.info("STEP 5.5: Check incoming payment status before finalization");
        var payment = this.client.payment().getIncoming(incomingPayment.getId(), grantRequest);
        assertNotNull(payment);
        assertFalse(payment.isCompleted()); // Should not be completed yet
        log.info("Payment status before finalization: completed=" + payment.isCompleted());

        // STEP 6: PAYMENT FINALIZATION
        // If user approved the payment (provided interact_ref), finalize the transaction
        log.info("********");
        log.info("STEP 6: Finalize payment transaction");

        if (interactReference != null && !interactReference.isBlank()) {
            log.info("User approved payment - finalizing with reference: " + interactReference);
            Grant finalized = this.client.auth().grant().finalize(opContinueInteract, interactReference);
            assertNotNull(finalized);
            // STEP 6A: Finalize the grant using the interact reference
            // This confirms the user's authorization and provides final access token
            log.info("Grant finalized successfully");

            if (finalized.getAccess().getToken() != null) {
                // STEP 6B: Execute the actual payment using the finalized grant
                // This transfers the funds from sender to receiver
                var outPayment = this.client.payment().createOutgoing(finalized, senderWallet, quote);
                assertNotNull(outPayment);
                assertFalse(outPayment.getFailed());
                log.info("Payment executed successfully: " + outPayment.getId());
            } else {
                log.info("********");
                log.error("Cannot execute payment - grant finalization failed: " + opContinueInteract.getInteract().getRedirect());
            }
        } else {
            log.info("********");
            log.error("Payment DECLINED - user did not provide interact_ref");
        }

        // STEP 7: PAYMENT STATUS MONITORING
        // Poll the payment status until completion or timeout (10 attempts)
        log.info("********");
        log.info("STEP 7: Monitor payment completion status");
        int count = 0;
        while (!payment.isCompleted() && count < 10) { // wait at least 10s ...
            count++;

            payment = this.client.payment().getIncoming(incomingPayment.getId(), grantRequest);
            assertNotNull(payment);
            log.info("********");
            log.info("Payment status check #" + count + " - Completed: " + payment.isCompleted());

            if (!payment.isCompleted()) {
                sleep(1000); // Wait 1 second before the next check
            }
        }

        if (payment.isCompleted()) {
            log.info("SUCCESS: Payment completed successfully!");
            log.info("Final payment amount: " + payment.getReceivedAmount().getValue() +
                " " + payment.getReceivedAmount().getAssetCode());
        } else {
            log.warn("Quote: Payment did not complete within timeout period");
        }
    }

    @Test
    public void openPaymentsCreateOutgoingFromIncomingPayment() throws InterruptedException {
        // STEP 0: RECEIVER WALLET DISCOVERY
        // Get the receiver's wallet information including supported currencies,
        // authorization server, and resource server endpoints
        log.info("********");
        log.info("STEP 0: Get receiver wallet information");
        var receiverWallet = client.walletAddress().get(TestHelper.getReceiverWalletAddress());
        assertNotNull(receiverWallet);
        log.info("Receiver wallet discovered: " + receiverWallet.getPublicName() + " (" + receiverWallet.getAssetCode() + ")");

        // STEP 1: RECEIVER GRANT REQUEST
        // Create an access grant that allows ILF to create incoming payment requests
        // on behalf of the receiver. This grant provides permissions to read, complete, and create
        // incoming payments on the receiver's wallet.
        log.info("********");
        log.info("STEP 1: Create pending grant for receiver");
        var ipGrant = this.client.auth().grant().incomingPayment(receiverWallet);
        assertNotNull(ipGrant);
        log.info("Receiver grant created with token: " + ipGrant.getAccess().getToken().substring(0, 10) + "...");

        // STEP 2: INCOMING PAYMENT CREATION
        // Create an incoming payment request for €12.34 on the receiver's wallet.
        // This establishes the payment destination and amount that the sender will pay to.
        log.info("********");
        log.info("STEP 2: Create incoming payment request");
        var amount = BigDecimal.valueOf(12.25);
        var incomingPayment = this.client.payment().createIncoming(receiverWallet, ipGrant, amount);
        assertNotNull(incomingPayment);
        log.info("Incoming payment created: " + incomingPayment.getId() + " for " +
            incomingPayment.getIncomingAmount().getValue() + " " + incomingPayment.getIncomingAmount().getAssetCode());

        // STEP 2.5: SENDER WALLET DISCOVERY
        // In a real scenario, the user would enter their wallet address at this point.
        // We simulate this by getting the sender's wallet information.
        log.info("********");
        log.info("STEP 2.5: Get sender wallet information (user enters their wallet)");
        var senderWallet = this.client.walletAddress().get(TestHelper.getSenderWalletAddress());
        assertNotNull(senderWallet);
        log.info("Sender wallet discovered: " + senderWallet.getPublicName() + " (" + senderWallet.getAssetCode() + ")");

        // STEP 3: PAYMENT CONTINUATION & USER INTERACTION
        // Create a redirect URL that the user must visit to authorize the payment in their wallet.
        // This implements the interactive authorization flow required by Open Payments.
        log.info("********");
        log.info("STEP 3: Create user interaction redirect");
        int port = 8888;
        this.httpServer = new SimpleHttpServer(port);
        this.httpServer.start();

        var urlToOpen = "http://localhost:%d?paymentId=1234".formatted(port);
        // Create outing payment from quote:
        var opContinueInteract = this.client.auth().grant().continuation(
            senderWallet,
            incomingPayment.getIncomingAmount(),
            //quote.getDebitAmount(),
            URI.create(urlToOpen),
            "test"
        );

        String interactReference = performInteractionAndContinue(opContinueInteract.getInteract().getRedirect().toString());

        // STEP 4: CHECK PAYMENT STATUS BEFORE FINALIZATION
        // Verify the payment state before attempting to finalize it
        log.info("********");
        log.info("STEP 4: Check incoming payment status before finalization");
        var payment = this.client.payment().getIncoming(incomingPayment.getId(), ipGrant);
        assertNotNull(payment);
        assertFalse(payment.isCompleted()); // Should not be completed yet
        log.info("Payment status before finalization: completed=" + payment.isCompleted());

        // STEP 5: PAYMENT FINALIZATION
        // If user approved the payment (provided interact_ref), finalize the transaction
        log.info("********");
        log.info("STEP 5: Finalize payment transaction");

        if (interactReference != null && !interactReference.isBlank()) {
            log.info("User approved payment - finalizing with reference: " + interactReference);
            var finalized = this.client.auth().grant().finalize(opContinueInteract, interactReference);
            assertNotNull(finalized);
            // STEP 6A: Finalize the grant using the interact reference
            // This confirms the user's authorization and provides final access token
            log.info("Grant finalized successfully");

            if (finalized.getAccess().getToken() != null) {
                // 2x incoming payments.
                // STEP 6B: Execute the actual payment using the finalized grant
                // This transfers the funds from sender to receiver
                var createdPayment_1Of2 = this.client.payment().createOutgoing(
                    finalized,
                    senderWallet,
                    payment,
                    Amount.build(BigDecimal.valueOf(10.00), payment.getIncomingAmount().getAssetCode())
                );
                assertNotNull(createdPayment_1Of2);
                assertFalse(createdPayment_1Of2.getFailed());
                log.info("Payment (1 of 2) executed successfully: " + createdPayment_1Of2.getId());

                var createdPayment_2Of2 = this.client.payment().createOutgoing(
                    finalized,
                    senderWallet,
                    payment,
                    Amount.build(BigDecimal.valueOf(2.25), payment.getIncomingAmount().getAssetCode())
                );
                assertNotNull(createdPayment_2Of2);
                assertFalse(createdPayment_2Of2.getFailed());
                log.info("Payment (2 of 2) executed successfully: " + createdPayment_2Of2.getId());

                // STEP 6C: Get the spent amounts for the outgoing payment grant.
                // Amounts are null until payments have been processed.
                var spentAmounts = this.client.payment().getGrantSpentAmounts(senderWallet, finalized);
                assertNotNull(spentAmounts);
                log.info("Grant spent amounts: debit=" + spentAmounts.getSpentDebitAmount()
                    + ", receive=" + spentAmounts.getSpentReceiveAmount());
            } else {
                log.info("********");
                log.error("Cannot execute payment - grant finalization failed: " + opContinueInteract.getInteract().getRedirect());
            }
        } else {
            log.info("********");
            log.error("Payment DECLINED - user did not provide interact_ref");
        }

        // STEP 7: COMPLETE THE INCOMING PAYMENT
        log.info("********");
        log.info("STEP 7: Complete the incoming payment");
        var incomingPaymentComplete = this.client.payment().incomingPaymentComplete(incomingPayment, ipGrant);
        assertNotNull(incomingPaymentComplete);

        // STEP 8: PAYMENT STATUS MONITORING
        // Poll the payment status until completion or timeout (15 attempts)
        log.info("********");
        log.info("STEP 8: Monitor payment completion status");
        int count = 0;
        while (!payment.isCompleted() && count < 15) { // wait at least 10s ...
            count++;

            payment = this.client.payment().getIncoming(incomingPayment.getId(), ipGrant);
            assertNotNull(payment);
            log.info("********");
            log.info("Payment status check #" + count + " - Completed: " + payment.isCompleted());

            if (!payment.isCompleted()) {
                sleep(1000); // Wait 1 second before the next check
            }
        }

        if (payment.isCompleted()) {
            log.info("SUCCESS: Payment completed successfully!");
            log.info("Final payment amount: " + payment.getReceivedAmount().getValue() +
                " " + payment.getReceivedAmount().getAssetCode());
        } else {
            log.warn("Incoming: Payment did not complete within timeout period");
        }
    }

    /**
     * Automates the User Interaction phase of the GNAP / Open Payments grant flow.
     * This method performs the following steps:
     * <p>1. Launches a headless Chrome instance to navigate to the wallet's interaction URL. </p>
     * <p>2. Detects if a login is required and authenticates using test credentials.</p>
     * <p>3. Waits for and clicks the "Accept/Consent" button to authorize the payment.</p>
     * <p>4. Captures the 'interact_ref' from the resulting redirect URL query parameters.</p>
     *
     * @param redirect The interaction URL provided by the AS (Authorization Server).
     * @return The 'interact_ref' string required to finalize the grant.
     * @throws RuntimeException If the login fails, the consent button is missing,
     *                          or the interact_ref is not captured within the timeout.
     */
    public String performInteractionAndContinue(
        String redirect
    ) {
        ChromeOptions options = new ChromeOptions();

        WebDriverManager.chromedriver().setup();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            driver.get(redirect);
            if (Objects.requireNonNull(driver.getCurrentUrl()).contains("/auth/login")) {
                String email = TestConfiguration.getTestWalletEmail();
                String password = TestConfiguration.getTestWalletPassword();

                WebElement emailInput = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[type='email'], input[name='email']")));
                emailInput.clear();
                emailInput.sendKeys(email);

                WebElement passInput = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[type='password'], input[name='password']")));
                passInput.clear();
                passInput.sendKeys(password);

                WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[type='submit'], button[aria-label='login']")));
                submitBtn.click();

                wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Accept"));
            }

            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[aria-label='accept']")
            ));
            btn.click();

            String interactRef = null;
            for (int i = 0; i < 100; i++) {
                String query = URI.create(driver.getCurrentUrl()).getQuery();
                if (query != null && query.contains("interact_ref=")) {
                    interactRef = Arrays.stream(query.split("&"))
                        .filter(param -> param.startsWith("interact_ref="))
                        .map(param -> param.split("=")[1])
                        .findFirst()
                        .orElse(null);
                    break;
                }
                Thread.sleep(100);
            }

            if (interactRef == null) throw new RuntimeException("interact_ref not found in URL");

            return interactRef;

        } catch (Exception e) {
            throw new RuntimeException("Failed during browser interaction: " + e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }
}
