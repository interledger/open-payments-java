package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * A grant request that is pending user interaction.
 *
 * <p>Returned by the authorization server when a grant request requires the user to interact
 * (e.g. to approve an outgoing payment or to prove access to a wallet address). The client
 * redirects the user to {@code interact.redirect}. Once the interaction finishes, the client
 * continues the grant using the {@code continue} details and the {@code interact_ref} it
 * received.
 *
 * <p>Example:
 * <pre>
 * {
 *   "interact": {
 *     "redirect": "https://auth.interledger-test.dev/4CF492MLVMSW9MKMXKHQ",
 *     "finish": "4105340a-05eb-4290-8739-f9e2b463bfa7"
 *   },
 *   "continue": {
 *     "access_token": { "value": "33OMUKMKSKU80UPRY5NM" },
 *     "uri": "https://auth.interledger-test.dev/continue/4CF492MLVMSW9MKMXKHQ",
 *     "wait": 30
 *   }
 * }
 * </pre>
 */
@Data
public class PendingGrant {
    /**
     * Interaction details: where to redirect the user, and the value that secures the callback.
     *
     * @see InteractContinue
     */
    @JsonProperty(value = "interact", required = true)
    private InteractContinue interact;

    /**
     * Details for continuing the grant request once the interaction has finished.
     *
     * @see AccessContinue
     */
    @JsonProperty(value = "continue", required = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AccessContinue grantContinue;
}
