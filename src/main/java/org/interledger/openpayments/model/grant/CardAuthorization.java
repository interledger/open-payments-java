package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * Card (EMV) authorization data on an outgoing-payment access item (Open Payments v1.4.0+).
 *
 * <p>Its presence marks a card authorization. In a grant request it carries the card
 * credentials; in a grant response it carries the authorization result.
 *
 * <p>Example (grant request with PIN):
 * <pre>
 * {
 *   "tlvData": "9F26089F27019F1002...",
 *   "pinBlock": "23e412341234",
 *   "pwk": { "key": "A1B2...", "kvc": "A3F1C2", "iv": "0000..." },
 *   "requestId": "018f8d2e-3c1a-7b6e-9f0a-2c4d6e8f0a1b"
 * }
 * </pre>
 */
@Data
public class CardAuthorization {
    /**
     * Card data on a request, the authorization result on a response, as EMV TLV.
     */
    @JsonProperty(value = "tlvData", required = true)
    private String tlvData;

    /**
     * The PIN block, encrypted under the PIN Working Key (PWK). Omitted when no PIN was captured.
     * When present, {@link #pwk} is required.
     */
    @JsonProperty("pinBlock")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String pinBlock;

    /**
     * The PIN Working Key used to encrypt the PIN block.
     */
    @JsonProperty("pwk")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PinWorkingKey pwk;

    /**
     * Client-generated identifier for the request. Replaying it MUST return the same value
     * as the one given in the original grant request.
     */
    @JsonProperty(value = "requestId", required = true)
    private String requestId;

    /**
     * Creates card authorization data for a no-CVM (no PIN) card payment.
     *
     * @param tlvData   the EMV TLV card data
     * @param requestId a client-generated identifier for the request
     * @return a new card authorization
     */
    public static CardAuthorization build(String tlvData, String requestId) {
        ensureNotNullOrEmpty(tlvData, "tlvData cannot be null or empty.");
        ensureNotNullOrEmpty(requestId, "requestId cannot be null or empty.");

        var cardAuthorization = new CardAuthorization();
        cardAuthorization.tlvData = tlvData;
        cardAuthorization.requestId = requestId;
        return cardAuthorization;
    }

    /**
     * Creates card authorization data for a card payment with an encrypted PIN.
     *
     * @param tlvData   the EMV TLV card data
     * @param requestId a client-generated identifier for the request
     * @param pinBlock  the PIN block, encrypted under the PIN Working Key
     * @param pwk       the PIN Working Key used to encrypt the PIN block
     * @return a new card authorization
     */
    public static CardAuthorization build(String tlvData, String requestId, String pinBlock, PinWorkingKey pwk) {
        ensureNotNullOrEmpty(pinBlock, "pinBlock cannot be null or empty.");
        ensureNotNull(pwk, "pwk is required when pinBlock is present.");

        var cardAuthorization = build(tlvData, requestId);
        cardAuthorization.pinBlock = pinBlock;
        cardAuthorization.pwk = pwk;
        return cardAuthorization;
    }
}
