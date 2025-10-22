package org.interledger.openpayments.model.outgoingpayment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.metadata.MetaData;

import java.net.URI;

/**
 * Represents a request to create an outgoing payment.
 * 
 * <p>This class encapsulates the data required to initiate an outgoing payment
 * from a sender's wallet to a receiver's incoming payment. It contains references
 * to the quote, wallet address, and payment destination.
 */
@Data
public class OutgoingPaymentRequest {
    /**
     * The wallet address from which the payment will be sent.
     */
    @JsonProperty(value = "walletAddress", required = true)
    private URI walletAddress;

    /**
     * Reference to the quote that determines payment amounts and fees.
     */
    @JsonProperty("quoteId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private URI quoteId;

    /**
     * Additional metadata for the payment.
     */
    @JsonProperty("metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetaData metadata;

    /**
     * Reference to the incoming payment that will receive the funds.
     */
    @JsonProperty("incomingPayment")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private URI incomingPayment;

    /**
     * The amount to be debited from the sender's wallet.
     */
    @JsonProperty("debitAmount")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Amount debitAmount;
}