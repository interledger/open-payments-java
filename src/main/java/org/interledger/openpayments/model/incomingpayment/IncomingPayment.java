package org.interledger.openpayments.model.incomingpayment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.metadata.MetaData;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

/**
 * Represents an incoming payment request on a receiver's wallet.
 * 
 * <p>An incoming payment is created to establish a payment destination with a specific
 * amount that senders can pay to. It serves as the target for outgoing payments and
 * tracks the completion status and received amount.
 * 
 * <p>This is created in step 2 of the payment flow on the receiver's wallet and
 * referenced by senders when creating quotes and executing payments.
 */
@Data
public class IncomingPayment {
    /**
     * Unique identifier for this incoming payment.
     *
     * <p>This URI uniquely identifies the incoming payment resource and is used
     * by senders to reference this payment destination when creating quotes
     * and outgoing payments.
     *
     * <p>Example: "<a href="https://ilp.interledger-test.dev/incoming-payments/123e4567-e89b-12d3-a456-426614174000">
     *     Incoming Payment</a>"
     */
    @JsonProperty(value = "id", required = true)
    private URI id;

    /**
     * Indicates whether the incoming payment has been completed.
     * 
     * <p>A payment is considered completed when the full expected amount has been
     * received or when the payment has been explicitly marked as complete by
     * the receiver.
     */
    @JsonProperty(value = "completed", required = true)
    private boolean completed;

    /**
     * Timestamp when this incoming payment was created.
     * 
     * <p>This is set when the incoming payment resource is first created and
     * remains immutable throughout the payment's lifecycle.
     */
    @JsonProperty(value = "createdAt", required = true)
    private Instant createdAt;

    /**
     * Timestamp when this incoming payment was last updated.
     * 
     * <p>This is updated whenever the payment status changes, amounts are
     * received, or other modifications are made to the payment resource.
     */
    @JsonProperty(value = "updatedAt", required = true)
    private Instant updatedAt;

    /**
     * Optional expiration timestamp for this incoming payment.
     * 
     * <p>After this time, the payment will no longer accept incoming funds.
     * If not specified, the payment may remain open indefinitely or use
     * the wallet's default expiration policy.
     */
    @JsonProperty(value = "expiresAt")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant expiresAt;

    /**
     * The expected amount to be received for this payment.
     * 
     * <p>This represents the total amount that the receiver expects to receive.
     * It may be used by senders to determine the appropriate payment amount,
     * though senders can also send partial payments.
     */
    @JsonProperty(value = "incomingAmount")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Amount incomingAmount;

    /**
     * The payment methods supported by this incoming payment.
     * 
     * <p>This set contains the various ways senders can deliver payments to
     * this incoming payment endpoint. Each method includes the necessary
     * information for establishing a payment connection.
     * 
     * @see InterledgerMethod
     */
    @JsonProperty(value = "methods", required = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<InterledgerMethod> methods;

    /**
     * The actual amount received so far for this payment.
     * 
     * <p>This tracks the cumulative amount that has been successfully received
     * for this incoming payment. It starts at zero and increases as payments
     * are received from senders.
     */
    @JsonProperty(value = "receivedAmount", required = true)
    private Amount receivedAmount;

    /**
     * The wallet address where this incoming payment is hosted.
     * 
     * <p>This identifies the wallet or account that will receive the funds.
     * It corresponds to the base wallet address used to create this payment.
     */
    @JsonProperty(value = "walletAddress", required = true)
    private URI walletAddress;

    /**
     * Optional metadata associated with this incoming payment.
     * 
     * <p>This can include additional context, references, or descriptive
     * information about the payment purpose, invoice numbers, or other
     * business-related data.
     * @see MetaData
     */
    @JsonProperty("metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetaData metadata;
}
