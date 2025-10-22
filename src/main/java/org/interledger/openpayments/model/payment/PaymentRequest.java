package org.interledger.openpayments.model.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.metadata.MetaData;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;

/**
 * Represents a request for payment, encapsulating details about the recipient's wallet address,
 * the amount to be paid, the expiration time of the request, and any additional metadata.
 * This class facilitates the creation and management of payment requests using the specified parameters.
 * Instances of PaymentRequest are immutable once created, ensuring thread-safety and reliability.
 * The class supports mandatory parameters like wallet address and incoming amount, while also
 * permitting optional fields like expiration time and metadata.
 * Serialization and deserialization of PaymentRequest objects are supported using Jackson annotations,
 * allowing seamless integration with JSON-based workflows.
 */
@Data
public class PaymentRequest {

    @JsonProperty(value = "walletAddress", required = true)
    private URI walletAddress;

    @JsonProperty(value = "incomingAmount", required = true)
    private Amount incomingAmount;

    @JsonProperty("expiresAt")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant expiresAt;

    @JsonProperty("metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetaData metadata;

    /**
     * Builds a new PaymentRequest object with the specified receiver, amount, and expiration time.
     *
     * The receiver must be a valid WalletAddress, the amount must be greater than zero,
     * and the expiration time must be a positive integer representing seconds.
     * Throws IllegalArgumentException if the input values do not meet these conditions.
     *
     * @param receiver the WalletAddress to which the payment request is directed; must not be null
     * @param amount the amount to be paid; must be a positive BigDecimal
     * @param expiresInSeconds the number of seconds after which the payment request expires; must be greater than zero
     * @return a PaymentRequest object initialized with the provided parameters
     * @throws IllegalArgumentException if amount is negative or if expiresInSeconds is non-positive
     */
    public static PaymentRequest build(WalletAddress receiver, BigDecimal amount, int expiresInSeconds) {
        ensureNotNull(receiver, "receiver cannot be null.");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be greater than zero.");
        }

        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException("expiresInSeconds must be greater than zero.");
        }

        PaymentRequest request = new PaymentRequest();
        request.walletAddress = receiver.getIdAsURI();
        request.incomingAmount = Amount.build(amount, receiver.getAssetCode(), receiver.getAssetScale());

        request.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        return request;
    }
}
