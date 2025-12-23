package org.interledger.openpayments.model.quote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.util.UtilGlobal;

import java.net.URI;
import java.util.Optional;

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
    public static final String ILP_METHOD = "ilp";
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
     * The amount that will be debited from the sender's account.
     *
     * <p>This is the total amount that will be deducted from the sender,
     * including the base payment amount plus any fees, currency conversion
     * costs, or other charges. This represents the "total cost" of the
     * payment to the sender.
     *
     * <p>The debit amount is calculated based on the quote request parameters
     * and current exchange rates/fees at the time of quote creation.
     */
    @JsonProperty(value = "debitAmount")
    private Amount debitAmount;

    /**
     * The amount that will be received by the recipient.
     *
     * <p>This is the amount that will actually be credited to the recipient's
     * account after all fees, currency conversions, and other deductions.
     * This represents the "net amount" that the recipient will receive.
     *
     * <p>The receive amount may be different from the debit amount due to
     * fees, currency exchange rates, or other factors in the payment routing.
     */
    @JsonProperty(value = "receiveAmount")
    private Amount receiveAmount;

    /**
     * Creates a new QuoteRequest with the specified parameters.
     *
     * <p>This factory method provides a convenient way to construct a quote
     * request with all required fields. The resulting quote request can be
     * used to request a quote from an Interledger server.
     *
     * @param senderWalletAddress the sender's wallet address where the quote will be created
     * @param receiverIncPayId      the receiver (incoming payment) endpoint URI as a string
     * @param method        the payment method to be used (typically "ilp")
     * @return a new QuoteRequest instance with the specified parameters
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public static QuoteRequest build(
            URI senderWalletAddress,
            String receiverIncPayId,
            Optional<Amount> debitAmount,
            Optional<Amount> receiveAmount,
            String method
    ) {
        ensureNotNull(debitAmount, "debitAmount cannot be null.");
        ensureNotNull(receiveAmount, "receiveAmount cannot be null.");
        ensureNotNull(senderWalletAddress, "walletAddress cannot be null.");
        ensureNotNullOrEmpty(receiverIncPayId, "receiver cannot be null or empty.");
        ensureNotNullOrEmpty(method, "method cannot be null or empty.");

        if (debitAmount.isPresent() && receiveAmount.isPresent()) {
            throw new OpenPaymentsClientException(
                    "Either debitAmount or receiveAmount must be null, both should not be present."
            );
        }

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.walletAddress = senderWalletAddress;
        quoteRequest.receiver = receiverIncPayId;

        if (UtilGlobal.isNullOrEmpty(method)) {
            method = ILP_METHOD;
        }
        quoteRequest.method = method;
        quoteRequest.receiveAmount = receiveAmount.orElse(null);
        quoteRequest.debitAmount = debitAmount.orElse(null);
        return quoteRequest;
    }
}
