package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;

/**
 * Information about the subject for which the client is requesting information.
 *
 * <p>A subject is used in a grant request to verify that a user has access to a specific wallet
 * address, without requesting an access token for payments. The authorization server interacts
 * with the user through the identity provider (IdP) to confirm they control the wallet address
 * listed in {@code sub_ids}.
 *
 * <p>The same structure is returned in a grant continuation response once the interaction has
 * completed successfully.
 *
 * <p>Example:
 * <pre>
 * {
 *   "sub_ids": [
 *     { "id": "https://ilp.interledger-test.dev/alice", "format": "uri" }
 *   ]
 * }
 * </pre>
 *
 * @see <a href="https://openpayments.dev/guides/verify-wallet-address-ownership/">Verify ownership of a wallet address</a>
 */
@Data
public class Subject {
    /**
     * The only subject identifier format supported by Open Payments.
     */
    public static final String FORMAT_URI = "uri";

    /**
     * A list of subject identifiers. The specification allows exactly one entry.
     */
    @JsonProperty(value = "sub_ids", required = true)
    private List<SubjectId> subIds;

    /**
     * Creates a subject for the given wallet address, using the {@code uri} format.
     *
     * @param walletAddress the wallet address the user must prove access to
     * @return a new subject
     */
    public static Subject walletAddress(URI walletAddress) {
        ensureNotNull(walletAddress, "walletAddress cannot be null.");

        var subjectId = new SubjectId();
        subjectId.setId(walletAddress.toString());
        subjectId.setFormat(FORMAT_URI);

        var subject = new Subject();
        subject.subIds = new ArrayList<>();
        subject.subIds.add(subjectId);
        return subject;
    }

    /**
     * A single subject identifier.
     */
    @Data
    public static class SubjectId {
        /**
         * Specific identifier for the subject, e.g. a wallet address URL.
         */
        @JsonProperty(value = "id", required = true)
        private String id;

        /**
         * The format of the subject identifier. The only allowed value is {@code uri}.
         */
        @JsonProperty(value = "format", required = true)
        private String format;
    }
}
