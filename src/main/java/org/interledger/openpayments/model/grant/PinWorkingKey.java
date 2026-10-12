package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * The PIN Working Key (PWK) used to encrypt the PIN block of a {@link CardAuthorization}
 * (Open Payments v1.4.0+).
 */
@Data
public class PinWorkingKey {
    /**
     * The encrypted key material.
     */
    @JsonProperty(value = "key", required = true)
    private String key;

    /**
     * Key Check Value of the key.
     */
    @JsonProperty(value = "kvc", required = true)
    private String kvc;

    /**
     * Initialization vector.
     */
    @JsonProperty(value = "iv", required = true)
    private String iv;

    /**
     * Creates a new PIN Working Key.
     *
     * @param key the encrypted key material
     * @param kvc the Key Check Value of the key
     * @param iv  the initialization vector
     * @return a new PIN Working Key
     */
    public static PinWorkingKey build(String key, String kvc, String iv) {
        ensureNotNullOrEmpty(key, "key cannot be null or empty.");
        ensureNotNullOrEmpty(kvc, "kvc cannot be null or empty.");
        ensureNotNullOrEmpty(iv, "iv cannot be null or empty.");

        var pwk = new PinWorkingKey();
        pwk.key = key;
        pwk.kvc = kvc;
        pwk.iv = iv;
        return pwk;
    }
}
