package org.interledger.openpayments.model.quote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.interledger.openpayments.model.common.Amount;

import java.net.URI;
import java.time.Instant;

/**
 * Represents a quote for an Interledger payment operation.
 *
 * <p>A quote provides cost estimates and exchange rate information for a payment
 * between two parties. It calculates the exact amounts that will be debited from
 * the sender and received by the recipient, including any fees or currency
 * conversions that may apply.
 *
 * <p>Quotes are essential for providing transparency in payment costs and must
 * be created before executing outgoing payments. They have a limited lifespan
 * and expire after a specified time to ensure exchange rates and fees remain
 * current.
 *
 * <p>This is used in step 3 of the Interledger payment flow to estimate costs
 * before the sender authorizes the payment.
 */
@ToString
@Data
public class Quote {
    /**
     * Timestamp when this quote was created.
     *
     * <p>This indicates when the quote was generated and serves as the starting
     * point for calculating the quote's validity period. Quote creation time
     * is important for determining freshness of exchange rates and fees.
     */
    @JsonProperty(value = "createdAt", required = true)
    private Instant createdAt;

    /**
     * Timestamp when this quote expires.
     *
     * <p>After this time, the quote should not be used for payment operations
     * as the exchange rates, fees, or other conditions may have changed.
     * Expired quotes must be replaced with new quotes to ensure accurate
     * pricing.
     *
     * <p>If not specified, the quote may use the wallet's default expiration
     * policy or remain valid indefinitely (not recommended for production).
     */
    @JsonProperty("expiresAt")
    private Instant expiresAt;

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
    @JsonProperty(value = "debitAmount", required = true)
    private Amount debitAmount;

    /**
     * Unique identifier for this quote.
     *
     * <p>This URI uniquely identifies the quote resource and is used to
     * reference this quote when creating outgoing payments. The quote ID
     * must be provided when executing the payment to ensure the correct
     * rates and fees are applied.
     *
     * <p>Example: "https://ilp.interledger-test.dev/quotes/550e8400-e29b-41d4-a716-446655440000"
     */
    @JsonProperty(value = "id", required = true)
    private URI id;

    /**
     * The payment method that will be used for this quote.
     *
     * <p>This specifies the protocol or mechanism that will be used to
     * deliver the payment. For Interledger payments, this is typically "ilp"
     * indicating the use of the Interledger Protocol.
     *
     * <p>The method affects the quote calculation as different payment methods
     * may have different fee structures or routing costs.
     */
    @JsonProperty(value = "method", required = true)
    private String method;

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
    @JsonProperty(value = "receiveAmount", required = true)
    private Amount receiveAmount;

    /**
     * The receiver (incoming payment) URI for this quote.
     *
     * <p>This identifies the specific incoming payment resource that this
     * quote is for. It corresponds to the destination where the payment
     * will be delivered if executed using this quote.
     *
     * <p>Example: "https://ilp.interledger-test.dev/incoming-payments/123e4567-e89b-12d3-a456-426614174000"
     */
    @JsonProperty(value = "receiver", required = true)
    private URI receiver;

    /**
     * The wallet address where this quote was created.
     *
     * <p>This identifies the sender's wallet address that requested the quote.
     * It corresponds to the wallet that will be debited if the payment is
     * executed using this quote.
     *
     * <p>Example: "https://ilp.interledger-test.dev/alice"
     */
    @JsonProperty(value = "walletAddress", required = true)
    private URI walletAddress;
}
