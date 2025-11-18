package org.interledger.openpayments;

import lombok.extern.slf4j.Slf4j;
import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.util.UtilGlobal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.math.BigDecimal;
import java.net.URI;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Implementation of the Interledger API Client using HTTP for communication.
 * This class handles operations related to retrieving wallet information and
 * facilitating payment transactions by communicating with an Interledger-enabled
 * HTTP server.
 *
 * Class Fields:
 * - client: The HTTP client used to send requests to the Interledger API server.
 * - httpServer: The HTTP server endpoint associated with the API client.
 * - log: Logger instance for capturing log messages and debugging information.
 *
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

    /**
     * Validates the retrieval of the client wallet information using the specified
     * client wallet address from the test configuration.
     *
     * This test asserts that the wallet details obtained by the client correspond
     * to the expected client wallet attributes, which include:
     *
     * - Wallet ID matches the client wallet address.
     * - Public name matches the expected identifier.
     * - Asset code and scale match the correct currency attributes.
     * - Auth server URL is associated with the wallet as expected.
     * - Resource server URL is linked to the wallet as expected.
     *
     * The test ensures the wallet address is not null and that all critical parameters
     * conform to predefined configurations to validate the overall functionality and
     * correctness of client wallet retrieval.
     */
    @Test
    public void getClientWallet() {
        var walletAdd = this.client.getWalletAddress(TestHelper.getClientWalletAddress());
        assertNotNull(walletAdd);

        Assertions.assertEquals(TestHelper.getClientWalletAddress(), walletAdd.getId());
        Assertions.assertEquals("f25232d1", walletAdd.getPublicName());
        Assertions.assertEquals("EUR", walletAdd.getAssetCode());
        Assertions.assertEquals(2, walletAdd.getAssetScale());
        // Auth and Resource server with tenant:
        Assertions.assertEquals("https://auth.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", walletAdd.getAuthServer());
        Assertions.assertEquals("https://ilp.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", walletAdd.getResourceServer());
    }

    /**
     * Validates the retrieval of the receiver wallet information using the specified
     * receiver wallet address from the test configuration.
     *
     * This test ensures that the wallet details fetched by the client match the
     * expected receiver wallet configuration, including:
     *
     * - Wallet ID matches the receiver wallet address.
     * - Public name matches the expected identifier.
     * - Asset code and scale correspond to the predefined currency attributes.
     * - Auth server URL is correctly associated with the wallet.
     * - Resource server URL is properly linked to the wallet.
     *
     * The test asserts that the wallet address obtained is not null and that all
     * critical parameters adhere to predefined configurations, verifying the
     * functionality and correctness of receiver wallet retrieval.
     */
    // Get receiver wallet data
    @Test
    public void getReceiverWallet() {
        var wallet = this.client.getWalletAddress(TestHelper.getReceiverWalletAddress());
        assertNotNull(wallet);

        Assertions.assertEquals(TestHelper.getReceiverWalletAddress(), wallet.getId());
        Assertions.assertEquals("1fa02483", wallet.getPublicName());
        Assertions.assertEquals("EUR", wallet.getAssetCode());
        Assertions.assertEquals(2, wallet.getAssetScale());
        Assertions.assertEquals("https://auth.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getAuthServer());
        Assertions.assertEquals("https://ilp.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getResourceServer());
    }

    /**
     * Validates the retrieval of the sender wallet information using the given
     * sender wallet address from the configuration.
     *
     * This test ensures that the wallet details fetched by the client match the
     * expected sender wallet configuration, including:
     * - Wallet ID matches the sender wallet address.
     * - Public name matches the expected identifier.
     * - Asset code and scale align with the designated currency attributes.
     * - Auth server URL is correctly associated with the wallet.
     * - Resource server URL is correctly linked to the wallet.
     *
     * The test asserts that the wallet address obtained is not null, and that all
     * key parameters conform to the predefined values in the test configuration.
     */
    @Test
    public void getSenderWallet() {
        var wallet = client.getWalletAddress(TestHelper.getSenderWalletAddress());
        assertNotNull(wallet);

        Assertions.assertEquals(TestHelper.getSenderWalletAddress(), wallet.getId());
        Assertions.assertEquals("f25232d1", wallet.getPublicName());
        Assertions.assertEquals("EUR", wallet.getAssetCode());
        Assertions.assertEquals(2, wallet.getAssetScale());
        Assertions.assertEquals("https://auth.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getAuthServer());
        Assertions.assertEquals("https://ilp.interledger-test.dev/f537937b-7016-481b-b655-9f0d1014822c", wallet.getResourceServer());
    }

    /**
     * Automates the entire process of facilitating and finalizing a payment transaction
     * between a sender and receiver wallet in a structured sequence of steps.
     *
     * This method covers the following tasks:
     *
     * 1. Receiver Wallet Discovery:
     *    Obtains and validates the receiver's wallet information, including supported currencies,
     *    authorization server, and resource server endpoints.
     *
     * 2. Receiver Grant Request:
     *    Creates an access grant for the receiver allowing operations on their wallet to manage incoming payments.
     *
     * 3. Incoming Payment Creation:
     *    Defines an incoming payment request with specified amount and currency on the receiver's wallet.
     *
     * 4. Sender Wallet Discovery:
     *    Acquires and verifies information about the sender's wallet, including name and supported assets.
     *
     * 5. Sender Quote Request Grant:
     *    Establishes permissions for accessing quotes on the sender's wallet for transaction calculations.
     *
     * 6. Quote Generation:
     *    Calculates the payment details for the transaction, including sender's cost and receiver's receipt,
     *    factoring in applicable fees.
     *
     * 7. Payment Continuation and User Interaction:
     *    Constructs a redirect URL for user interaction to approve the payment in their wallet.
     *    Await input from the user with the required parameters from the redirect.
     *
     * 8. Payment Status Check (Before Finalization):
     *    Verifies the payment status at this stage before proceeding with the finalization process.
     *
     * 9. Payment Finalization:
     *    Finalizes the payment transaction upon successful user approval and authorization.
     *    This step includes the execution of the payment using the finalized grant.
     *
     * 10. Payment Status Monitoring:
     *     Monitors the payment until its completion status is confirmed or a timeout is reached.
     *
     */
    @Test
    public void getGrantRequest() throws InterruptedException {
        // STEP 0: RECEIVER WALLET DISCOVERY
        // Get the receiver's wallet information including supported currencies,
        // authorization server, and resource server endpoints
        log.info("********");
        log.info("STEP 0: Get receiver wallet information");
        var receiverWallet = client.getWalletAddress(TestHelper.getReceiverWalletAddress());
        assertNotNull(receiverWallet);
        log.info("Receiver wallet discovered: " + receiverWallet.getPublicName() + " (" + receiverWallet.getAssetCode() + ")");

        // STEP 1: RECEIVER GRANT REQUEST
        // Create an access grant that allows ILF to create incoming payment requests
        // on behalf of the receiver. This grant provides permissions to read, complete, and create
        // incoming payments on the receiver's wallet.
        log.info("********");
        log.info("STEP 1: Create pending grant for receiver");
        var grantRequest = this.client.createGrantIncomingPayment(receiverWallet);
        assertNotNull(grantRequest);
        log.info("Receiver grant created with token: " + grantRequest.getAccess().getToken().substring(0, 10) + "...");

        // STEP 2: INCOMING PAYMENT CREATION
        // Create an incoming payment request for €12.34 on the receiver's wallet.
        // This establishes the payment destination and amount that the sender will pay to.
        log.info("********");
        log.info("STEP 2: Create incoming payment request");
        var incomingPayment = this.client.createIncomingPayment(receiverWallet, grantRequest, BigDecimal.valueOf(12.34));
        assertNotNull(incomingPayment);
        log.info("Incoming payment created: " + incomingPayment.getId() + " for " +
                incomingPayment.getIncomingAmount().getAmount() + " " + incomingPayment.getIncomingAmount().getAssetCode());

        // STEP 2.5: SENDER WALLET DISCOVERY  
        // In a real scenario, the user would enter their wallet address at this point.
        // We simulate this by getting the sender's wallet information.
        log.info("********");
        log.info("STEP 2.5: Get sender wallet information (user enters their wallet)");
        var senderWallet = this.client.getWalletAddress(TestHelper.getSenderWalletAddress());
        assertNotNull(senderWallet);
        log.info("Sender wallet discovered: " + senderWallet.getPublicName() + " (" + senderWallet.getAssetCode() + ")");

        // STEP 3: SENDER QUOTE REQUEST GRANT
        // Create an access grant that allows ILF to request quotes on the sender's wallet.
        // This is needed to calculate transaction fees and exchange rates.
        log.info("********");
        log.info("STEP 3: Create quote request grant for sender");
        var quoteRequest = this.client.createGrantQuote(senderWallet);
        assertNotNull(quoteRequest);
        log.info("Quote request grant created for sender wallet");

        // STEP 4: QUOTE GENERATION
        // Generate a quote that calculates the exact amount the sender needs to pay
        // including any transaction fees. The quote links the sender to the incoming payment.
        log.info("********");
        log.info("STEP 4: Generate payment quote");
        var quote = this.client.createQuote(quoteRequest.getAccess().getToken(), senderWallet, incomingPayment);
        assertNotNull(quote);
        log.info("Quote generated - Sender pays: " + quote.getDebitAmount().getAmount() + " " +
                quote.getDebitAmount().getAssetCode() +
                ", Receiver gets: " + quote.getReceiveAmount().getAmount() + " " +
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
        var continueInteract = this.client.createGrantContinuation(senderWallet, quote,
                URI.create(urlToOpen),
                "test"
        );

        log.info("********");
        log.info("USER INTERACTION REQUIRED:");
        log.info(String.format("%n-> CLICK ON LINK!!!: %n%s%n-----%n", continueInteract.getInteract().getRedirect()));
        log.info("After clicking, copy the 'interact_ref' parameter from the return URL");
        log.info("********");

        int waitLocalCount = 0;
        while (waitLocalCount < 60) {
            if (!UtilGlobal.isNullOrEmpty(this.httpServer.getLastRequestLine())) break;

            waitLocalCount++;
            sleep(1000);
        }

        var lastLine = this.httpServer.getLastRequestLine();
        final String interactReference;
        // The redirect URL will contain query parameters when the user returns:

        // MANUAL STEP: User clicks redirect link and authorizes payment in their wallet
        // The wallet redirects back with an interact_ref parameter that we need to capture
        if (UtilGlobal.isNullOrEmpty(lastLine)) {
            interactReference = JOptionPane.showInputDialog("Enter 'Interact Reference:' query parameter from return URL:");
            log.info("You entered: " + interactReference);
        } else {
            //GET /?paymentId=1234&hash=1uI013KNT6iU7Qtgd8U1G1E4d3bMAiCI85kgllUwXYQ=&interact_ref=003064ec-5b08-4acf-90ec-d8e4779fc7ef HTTP/1.1
            interactReference = lastLine.substring(lastLine.indexOf("interact_ref=") + 13, lastLine.indexOf("interact_ref=") + 13 + 36);
        }
        this.httpServer.shutdown();


        // STEP 5.5: CHECK PAYMENT STATUS BEFORE FINALIZATION
        // Verify the payment state before attempting to finalize it
        log.info("********");
        log.info("STEP 5.5: Check incoming payment status before finalization");
        var payment = this.client.getIncomingPayment(incomingPayment, grantRequest);
        assertNotNull(payment);
        assertFalse(payment.isCompleted()); // Should not be completed yet
        log.info("Payment status before finalization: completed=" + payment.isCompleted());

        // STEP 6: PAYMENT FINALIZATION
        // If user approved the payment (provided interact_ref), finalize the transaction
        log.info("********");
        log.info("STEP 6: Finalize payment transaction");

        if (interactReference != null && !interactReference.isBlank()) {
            log.info("User approved payment - finalizing with reference: " + interactReference);
            Grant finalized = this.client.createGrantFinalize(continueInteract, interactReference);
            assertNotNull(finalized);
            // STEP 6A: Finalize the grant using the interact reference
            // This confirms the user's authorization and provides final access token
            log.info("Grant finalized successfully");

            if (finalized.getAccess().getToken() != null) {
                // STEP 6B: Execute the actual payment using the finalized grant
                // This transfers the funds from sender to receiver
                var finalizedPayment = this.client.createFinalizePayment(finalized, senderWallet, quote);
                assertNotNull(finalizedPayment);
                assertFalse(finalizedPayment.getFailed());
                log.info("Payment executed successfully: " + finalizedPayment.getId());
            } else {
                log.info("********");
                log.error("Cannot execute payment - grant finalization failed: " + continueInteract.getInteract().getRedirect());
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

            payment = this.client.getIncomingPayment(incomingPayment, grantRequest);
            assertNotNull(payment);
            log.info("********");
            log.info("Payment status check #" + count + " - Completed: " + payment.isCompleted());

            if (!payment.isCompleted()) {
                sleep(1000); // Wait 1 second before the next check
            }
        }

        if (payment.isCompleted()) {
            log.info("SUCCESS: Payment completed successfully!");
            log.info("Final payment amount: " + payment.getReceivedAmount().getAmount() +
                    " " + payment.getReceivedAmount().getAssetCode());
        } else {
            log.warn("Payment did not complete within timeout period");
        }
    }
}