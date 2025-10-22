package org.interledger.openpayments.model.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.metadata.MetaData;

import java.net.URI;
import java.time.Instant;

/**
 * Represents a completed or in-progress payment in the Interledger Open Payments protocol.
 * 
 * <p>This class contains the comprehensive information about a payment transaction
 * that has been executed or is in the process of being executed. It includes
 * details about the payment amounts, status, timing, and associated metadata.
 * 
 * <p>A payment is the result of successfully executing an outgoing payment request
 * against a specific quote. It tracks the actual amounts that were sent and received,
 * as well as any grant spending information and failure status.
 * 
 * <p>The payment represents the final step in the Interledger payment flow:
 * <ol>
 *   <li>Create incoming payment (receiver)</li>
 *   <li>Create quote (sender)</li>
 *   <li>Create outgoing payment (sender)</li>
 *   <li>Execute payment - this class represents the result</li>
 * </ol>
 */
@ToString
@Data
public class Payment {
    /**
     * Unique identifier for this payment.
     * 
     * <p>This URI uniquely identifies the payment transaction within the
     * Interledger network. It can be used to query the payment status,
     * reference the payment in other operations, or for audit purposes.
     * 
     * <p>Example: "https://ilp.interledger-test.dev/payments/123e4567-e89b-12d3-a456-426614174000"
     */
    @JsonProperty(value = "id", required = true)
    private URI id;

    /**
     * The wallet address from which this payment was sent.
     * 
     * <p>This identifies the sender's wallet address that initiated and
     * executed this payment. It corresponds to the wallet that was debited
     * for the payment amount.
     * 
     * <p>Example: "https://ilp.interledger-test.dev/alice"
     */
    @JsonProperty(value = "walletAddress", required = true)
    private URI walletAddress;

    /**
     * Optional reference to the quote that was used for this payment.
     * 
     * <p>This URI identifies the specific quote that was used to determine
     * the payment amounts and fees. The quote provides the cost estimate
     * that was used when creating the payment.
     * 
     * <p>If present, this can be used to reconcile the actual payment amounts
     * with the originally quoted amounts.
     * 
     * <p>Example: "https://ilp.interledger-test.dev/quotes/456e7890-e89b-12d3-a456-426614174000"
     */
    @JsonProperty(value = "quoteId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private URI quoteId;

    /**
     * Indicates whether this payment has failed.
     * 
     * <p>This field is set to true if the payment execution failed for any
     * reason. When a payment fails, the amounts may be partial or zero,
     * and the metadata may contain additional error information.
     * 
     * <p>A failed payment may have already sent some amount before failing,
     * so clients should check the sent and received amounts even for failed
     * payments.
     */
    @JsonProperty("failed")
    private Boolean failed;

    /**
     * The receiver (incoming payment) endpoint that received this payment.
     * 
     * <p>This URI identifies the destination where the payment was delivered.
     * It corresponds to the incoming payment that was created by the receiver
     * to accept this payment.
     * 
     * <p>Example: "https://ilp.interledger-test.dev/incoming-payments/789e0123-e89b-12d3-a456-426614174000"
     */
    @JsonProperty(value = "receiver", required = true)
    private URI receiver;

    /**
     * The amount that was actually received by the receiver.
     * 
     * <p>This represents the actual amount that was successfully delivered
     * to the receiver's account. This may be different from the originally
     * intended amount due to partial payments or payment failures.
     * 
     * <p>For successful payments, this should match the quote's receive amount.
     * For failed payments, this may be less than expected.
     */
    @JsonProperty(value = "receiveAmount", required = true)
    private Amount receivedAmount;

    /**
     * The amount that was debited from the sender's account.
     * 
     * <p>This represents the total amount that was debited from the sender's
     * wallet to execute this payment. It includes the payment amount plus
     * any fees or exchange rate costs.
     * 
     * <p>For successful payments, this should match the quote's debit amount.
     * For failed payments, this may be less than expected.
     */
    @JsonProperty(value = "debitAmount", required = true)
    private Amount debitAmount;

    /**
     * The amount that was actually sent through the Interledger network.
     * 
     * <p>This represents the amount that was transmitted through the
     * Interledger protocol to deliver the payment. It may be different
     * from the debit amount due to local wallet fees or processing costs.
     * 
     * <p>The sent amount is typically between the debit amount and the
     * received amount, accounting for network fees and exchange rates.
     */
    @JsonProperty(value = "sentAmount", required = true)
    private Amount sentAmount;

    /**
     * The amount spent from the grant's debit limit for this payment.
     * 
     * <p>This field tracks how much of the access grant's debit limit was
     * consumed by this payment. It's used for grant management and to
     * ensure that payments don't exceed the authorized limits.
     * 
     * <p>This information is important for tracking grant usage and for
     * determining how much of the grant's debit authorization remains.
     */
    @JsonProperty(value = "grantSpentDebitAmount")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Amount grantSpentDebitAmount;

    /**
     * The amount spent from the grant's receive limit for this payment.
     * 
     * <p>This field tracks how much of the access grant's receive limit was
     * consumed by this payment. It's used for grant management and to
     * ensure that payments don't exceed the authorized limits.
     * 
     * <p>This information is important for tracking grant usage and for
     * determining how much of the grant's receive authorization remains.
     */
    @JsonProperty(value = "grantSpentReceiveAmount")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Amount grantSpentReceiveAmount;

    /**
     * Timestamp when this payment was created.
     * 
     * <p>This indicates when the payment was first initiated and created
     * in the system. It marks the beginning of the payment execution
     * process.
     */
    @JsonProperty(value = "createdAt", required = true)
    private Instant createdAt;

    /**
     * Timestamp when this payment was last updated.
     * 
     * <p>This indicates when the payment information was last modified,
     * such as when the payment status changed, amounts were updated,
     * or when the payment was completed or failed.
     */
    @JsonProperty(value = "updatedAt", required = true)
    private Instant updatedAt;

    /**
     * Optional metadata associated with this payment.
     * 
     * <p>This can include additional context, references, or descriptive
     * information about the payment purpose, invoice numbers, or other
     * business-related data that was provided when creating the payment.
     * 
     * @see MetaData
     */
    @JsonProperty("metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetaData metadata;
}
