package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * Represents an interaction reference for the GNAP protocol.
 * 
 * <p>This class contains a reference to a completed user interaction that
 * can be used to continue the grant process. The interaction reference is
 * typically provided by the authorization server after a user has completed
 * the required interaction steps, such as authentication or consent.
 * 
 * <p>The interaction reference serves as a proof that the user interaction
 * has been completed successfully and can be used by the client to continue
 * the access grant process without requiring the user to repeat the
 * interaction.
 * 
 * <p>This is commonly used in scenarios where the client needs to continue
 * a grant request after the user has completed an interaction in a separate
 * context, such as a web browser or mobile app.
 */
@Data
public class InteractRef {

    /**
     * The interaction reference token from the authorization server.
     * 
     * <p>This reference token is provided by the authorization server after
     * the user has completed the required interaction. It serves as proof
     * that the interaction was completed successfully and can be used to
     * continue the grant process.
     * 
     * <p>The interaction reference is typically a unique, opaque identifier
     * that the authorization server can use to associate the client's
     * continuation request with the completed user interaction.
     * 
     * <p>Example: "4IFWWIKYBC2PQ6U56NL1"
     */
    @JsonProperty("interact_ref")
    String interactRef;

    /**
     * Creates a new InteractRef instance with the specified interaction reference.
     * 
     * <p>This factory method provides a convenient way to create an interaction
     * reference object with the specified reference token. The reference token
     * is typically obtained from the authorization server after the user has
     * completed the required interaction.
     * 
     * @param interactRef the interaction reference token from the authorization server
     * @return a new InteractRef instance with the specified reference
     * @throws IllegalArgumentException if interactRef is null or empty
     */
    public static InteractRef build(String interactRef) {
        ensureNotNullOrEmpty(interactRef, "interactRef cannot be null or empty.");
        InteractRef ref = new InteractRef();
        ref.interactRef = interactRef;
        return ref;
    }
}
