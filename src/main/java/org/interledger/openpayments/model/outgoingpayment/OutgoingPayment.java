package org.interledger.openpayments.model.outgoingpayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.AccessContinue;
import org.interledger.openpayments.model.grant.InteractContinue;

import java.net.URI;
import java.time.Instant;

/**
 * Represents the response from creating an outgoing payment request.
 * 
 * <p>This class contains the information returned by the authorization server
 * when a client requests permission to create an outgoing payment. It includes
 * continuation information for the access grant process and either interaction
 * details or an access token, depending on the authorization flow.
 * 
 * <p>The outgoing payment response is used in the Interledger Open Payments
 * protocol to manage the authorization flow for sending payments. It provides
 * the necessary information for the client to complete the payment authorization
 * and proceed with the actual payment execution.
 * 
 * <p>The response will contain either:
 * <ul>
 *   <li>Interaction information if user consent is required</li>
 *   <li>An access token if authorization is granted immediately</li>
 * </ul>
 */
@Data
public class OutgoingPayment {
    /**
     * Unique identifier for this outgoing payment.
     *
     * <p>This URI uniquely identifies the outgoing payment resource and is used
     * by senders to reference this payment destination when creating quotes
     * and incoming payments.
     *
     * <p>Example: "<a href="https://ilp.interledger-test.dev/outgoing-payments/123e4567-e89b-12d3-a456-426614174000">
     *     Outgoing Payment</a>"
     */
    @JsonProperty(value = "id", required = false)
    private URI id;

    /**
     * The wallet address where this outgoing payment is hosted.
     *
     * <p>This identifies the wallet or account that will receive the funds.
     * It corresponds to the base wallet address used to create this payment.
     */
    @JsonProperty(value = "walletAddress", required = false)
    private URI walletAddress;

    /**
     * Reference to the quote associated with the outgoing payment.
     *
     * This variable holds the URI identifying the quote that determines payment
     * details such as amounts, fees, and terms. The quote provides a context
     * for the payment, allowing the sender to understand the cost and conditions
     * of the transaction before it is executed.
     *
     * The quote is optional, meaning some outgoing payments may not rely on
     * pre-negotiated quotes. If present, the provided URI should resolve to
     * a valid and accessible quote resource.
     *
     * The value may be serialized and deserialized as part of the outgoing
     * payment object in data exchange formats like JSON.
     */
    @JsonProperty(value = "quoteId", required = false)
    private URI quoteId;

    /**
     * Represents the URI providing information about a failed outgoing payment.
     *
     * <p>This field is used to indicate the reason or details associated with a failure
     * during the processing of an outgoing payment. It typically contains a URI that
     * points to a resource or document providing further context about the failure, such
     * as an error description or diagnostic information.
     *
     * <p>The inclusion of this field is optional and its presence depends on whether
     * the payment has encountered a failure during its lifecycle. If this field is absent,
     * no failure has been recorded for the outgoing payment.
     */
    @JsonProperty(value = "failed", required = false)
    private Boolean failed;

    /**
     * Represents the URI of the outgoing payment receiver.
     *
     * <p>This field specifies the payment destination, which is the location where
     * the funds will be sent as part of the outgoing payment. It denotes the
     * receiver's outgoing payment endpoint as a URI.
     *
     * <p>If specified, the receiver URI helps identify the target resource that
     * will process the payment. This can be used to ensure that funds are
     * transferred to the intended recipient and facilitate communication between
     * payment systems.
     *
     * <p>This field is optional and may not always be required, depending on the
     * context of the payment and the associated business logic.
     */
    @JsonProperty(value = "receiver", required = false)
    private URI receiver;

    /**
     * The actual amount received so far for this payment.
     *
     * <p>This tracks the cumulative amount that has been successfully received
     * for this outgoing payment. It starts at zero and increases as payments
     * are received from senders.
     */
    @JsonProperty(value = "receivedAmount", required = false)
    private Amount receivedAmount;

    /**
     * Represents the monetary amount that will be debited from the sender's account
     * as part of an outgoing payment transaction.
     *
     * <p>This field corresponds to the amount deducted from the sender's balance
     * to facilitate the transfer. The `debitAmount` value must be consistent with
     * the payment parameters such as `receivedAmount` or `quoteId` to ensure the
     * integrity of the transaction.
     *
     * <ul>
     *   <li>Optional: This field is not mandatory and may be null depending on the
     *       context.</li>
     *   <li>Format: The amount is represented using the {@link Amount} class, which
     *       encapsulates details such as the asset code, scale, and value in the
     *       Interledger Protocol format.</li>
     * </ul>
     *
     * <p>Processing systems are expected to handle `null` values for `debitAmount`
     * appropriately and ensure that payment integrity rules are applied during
     * transaction execution.
     *
     * @see Amount
     * @see OutgoingPayment
     */
    @JsonProperty(value = "debitAmount", required = false)
    private Amount debitAmount;

    /**
     * Represents the amount of funds sent in a monetary transaction.
     *
     * This field uses the {@link Amount} class to encapsulate the value,
     * including details such as the asset code (e.g., currency), asset scale
     * (precision), and the actual amount value represented as a string to
     * maintain precision.
     *
     * The `sentAmount` may be null or omitted if no funds were sent in the
     * transaction or if the information is unavailable at the time of processing.
     *
     * This field is annotated with `@JsonProperty` to control serialization and
     * deserialization behavior when converting this object to and from JSON.
     */
    @JsonProperty(value = "sentAmount", required = false)
    private Amount sentAmount;

    /**
     * Timestamp when this outgoing payment was created.
     *
     * <p>This is set when the outgoing payment resource is first created and
     * remains immutable throughout the payment's lifecycle.
     */
    @JsonProperty(value = "createdAt", required = false)
    private Instant createdAt;

    /**
     * The continuation information for the access grant process.
     * 
     * <p>This field contains the information needed to continue the access
     * grant process after the initial request. It includes the access token
     * and continuation URI that the client can use to manage the grant.
     * 
     * <p>The payment continuation is always provided and allows the client
     * to track and manage the authorization status of the payment request.
     * 
     * @see AccessContinue
     */
    @JsonProperty(value = "continue", required = true)
    private AccessContinue paymentContinue;

    /**
     * Optional interaction information if user consent is required.
     * 
     * <p>This field is present when the authorization server requires user
     * interaction to complete the payment authorization. It contains the
     * information needed to continue the grant process after the user has
     * completed the required interaction.
     * 
     * <p>Note: Either this field or the access token field will be present,
     * but not both. This field is used when user consent is required.
     * 
     * @see InteractContinue
     */
    @JsonProperty(value = "interact")
    private InteractContinue interact;

    /**
     * Optional access token if authorization is granted immediately.
     * 
     * <p>This field contains the access token that can be used immediately
     * to access the payment resources. It is present when the authorization
     * server can grant access without requiring user interaction.
     * 
     * <p>Note: Either this field or the interact field will be present,
     * but not both. This field is used when immediate authorization is granted.
     * 
     * <p>Example: "OS9M2PMHKUR64TB8N6BW7OZB8CDFONP219RP1LT0"
     */
    @JsonProperty(value = "access_token")
    private String token;
}
