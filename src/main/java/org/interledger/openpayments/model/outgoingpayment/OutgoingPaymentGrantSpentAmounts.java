package org.interledger.openpayments.model.outgoingpayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Amount;

/**
 * The spent amounts for the current outgoing payment grant.
 *
 * <p>Returned by the {@code GET /outgoing-payment-grant} endpoint of the resource server. The
 * grant is identified by the access token used for the request. For grants created with an
 * {@code interval} (recurring), the amounts are for the current interval only.
 *
 * <p>Both amounts are {@code null} when nothing has been spent against the grant yet.
 */
@Data
public class OutgoingPaymentGrantSpentAmounts {
    /**
     * The total amount successfully received (by all receivers) using the current
     * outgoing payment grant, or {@code null} if nothing has been received yet.
     */
    @JsonProperty("spentReceiveAmount")
    private Amount spentReceiveAmount;

    /**
     * The total amount successfully deducted from the sender's account using the current
     * outgoing payment grant, or {@code null} if nothing has been deducted yet.
     */
    @JsonProperty("spentDebitAmount")
    private Amount spentDebitAmount;
}
