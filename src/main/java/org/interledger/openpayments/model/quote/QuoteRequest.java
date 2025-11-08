package org.interledger.openpayments.model.quote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * Represents a request to create a quote for an Interledger payment operation.
 *
 * <p>A quote request contains the minimum required information to generate a
 * cost estimate for a payment between two parties. It specifies the sender's
 * wallet address, the receiver's incoming payment endpoint, and the payment
 * method to be used.
 *
 * <p>Quote requests are used in step 3 of the Interledger payment flow to
 * obtain pricing information before executing the actual payment. The quote
 * request must include either a debit amount or receive amount (or rely on
 * the receiver's incoming payment amount) to calculate accurate fees and
 * exchange rates.
 *
 * <p>This class represents the subset of the quote schema accepted as input
 * to create a new quote resource on the Interledger server.
 */
@Data
public class QuoteRequest {
    /**
     * The wallet address where the quote will be created.
     *
     * <p>This identifies the sender's wallet address that is requesting the
     * quote. It corresponds to the wallet that will be debited if the payment
     * is executed using the resulting quote.
     *
     * <p>The wallet address must be a valid URI pointing to an Interledger
     * wallet endpoint.
     *
     * <p>Example: "https://ilp.interledger-test.dev/alice"
     */
    @JsonProperty(value = "walletAddress", required = true)
    private URI walletAddress;

    /**
     * The receiver (incoming payment) endpoint for the quote.
     *
     * <p>This specifies the destination where the payment will be delivered
     * if executed using the resulting quote. It must be a valid incoming
     * payment URI that identifies a specific payment destination.
     *
     * <p>The receiver URI is used to determine the target wallet's capabilities,
     * supported currencies, and any existing incoming payment amount that may
     * influence the quote calculation.
     *
     * <p>Example: "https://ilp.interledger-test.dev/incoming-payments/123e4567-e89b-12d3-a456-426614174000"
     */
    @JsonProperty(value = "receiver", required = true)
    private String receiver;

    /**
     * The payment method to be used for the quote.
     *
     * <p>This specifies the protocol or mechanism that will be used to deliver
     * the payment. For Interledger payments, this is typically "ilp" indicating
     * the use of the Interledger Protocol.
     *
     * <p>The method affects the quote calculation as different payment methods
     * may have different fee structures, routing costs, or exchange rate
     * considerations.
     *
     * <p>Supported values:
     * <ul>
     *   <li>"ilp" - Interledger Protocol payment method</li>
     * </ul>
     */
    @JsonProperty(value = "method", required = true)
    private String method;

    /**
     * Creates a new QuoteRequest with the specified parameters.
     *
     * <p>This factory method provides a convenient way to construct a quote
     * request with all required fields. The resulting quote request can be
     * used to request a quote from an Interledger server.
     *
     * @param walletAddress the sender's wallet address where the quote will be created
     * @param receiver      the receiver (incoming payment) endpoint URI as a string
     * @param method        the payment method to be used (typically "ilp")
     * @return a new QuoteRequest instance with the specified parameters
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public static QuoteRequest build(URI walletAddress, String receiver, String method) {
        ensureNotNull(walletAddress, "walletAddress cannot be null.");
        ensureNotNullOrEmpty(receiver, "receiver cannot be null or empty.");
        ensureNotNullOrEmpty(method, "method cannot be null or empty.");

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.walletAddress = walletAddress;
        quoteRequest.receiver = receiver;
        quoteRequest.method = method;
        return quoteRequest;
    }
}
