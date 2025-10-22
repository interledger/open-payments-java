package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Amount;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * Represents a request to grant access to resources using the GNAP protocol.
 *
 * <p>This class is used to initiate an access request to the authorization server
 * in the Grant Negotiation and Authorization Protocol (GNAP). It contains the
 * client identifier, the access token request specifying what permissions are
 * being requested, and optionally the interaction methods for obtaining user
 * consent.
 *
 * <p>The grant access request is typically the first step in the GNAP flow where
 * the client requests permission to access protected resources on behalf of a
 * resource owner. The request includes:
 * <ul>
 *   <li>Client identification (wallet address)</li>
 *   <li>Access token request with desired permissions</li>
 *   <li>Optional interaction methods for user consent</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * GrantAccessRequest request = GrantAccessRequest.outgoing(
 *     clientWallet,
 *     AccessItemType.outgoingPayment,
 *     Set.of(AccessAction.create),
 *     quoteId,
 *     debitAmount
 * );
 * request.redirectInteract(returnUrl, nonce);
 * </pre>
 */
@Data
public class GrantRequest {
    /**
     * The client identifier for this access request.
     *
     * <p>This identifies the client application that is requesting access
     * to protected resources. In the context of Interledger Open Payments,
     * this is typically the wallet address of the client application.
     *
     * @see Client
     */
    @JsonProperty("client")
    private Client client;

    /**
     * The access token request specifying what permissions are being requested.
     *
     * <p>This field contains the detailed specification of what access the
     * client is requesting. It includes the types of resources, the actions
     * that can be performed, and any limits or constraints on the access.
     *
     * @see AccessToken
     */
    @JsonProperty("access_token")
    private AccessToken accessToken;

    /**
     * Optional interaction methods for obtaining user consent.
     *
     * <p>This field specifies how the client can interact with the authorization
     * server to obtain consent from the resource owner. If not present, the
     * authorization server may use its default interaction methods or deny
     * the request if interaction is required.
     *
     * @see AccessInteract
     */
    @JsonProperty("interact")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AccessInteract interact;

    private static final String REDIRECT = "redirect";

    /**
     * Constructs a new grant access request for the specified client.
     *
     * @param clientWalletAddress the client wallet address making the access request
     * @throws IllegalArgumentException if client is null
     */
    public GrantRequest(Client clientWalletAddress) {
        ensureNotNull(clientWalletAddress, "clientWalletAddress cannot be null.");
        this.client = clientWalletAddress;
    }

    /**
     * Helper method to quickly assemble a basic access request.
     *
     * <p>This method creates a grant access request with the specified client
     * wallet address, access type, and actions. It automatically creates the
     * necessary access token and access item structures.
     *
     * @param clientWallet  the client wallet address making the request
     * @param accessType    the type of access being requested (incoming/outgoing)
     * @param accessActions the set of actions to be performed
     * @return a new GrantAccessRequest instance configured with the specified parameters
     */
    public static GrantRequest build(
            Client clientWallet,
            AccessItemType accessType,
            Set<AccessAction> accessActions
    ) {
        ensureNotNull(clientWallet, "clientWallet cannot be null.");
        ensureNotNull(accessType, "accessType cannot be null.");
        ensureNotNullOrEmpty(accessActions, "accessActions cannot be null or empty.");

        GrantRequest request = new GrantRequest(clientWallet);
        request.accessToken = new AccessToken();

        AccessItem accessItem = new AccessItem();
        accessItem.setAccessType(accessType);
        accessItem.setActions(accessActions);

        request.accessToken.setAccess(new LinkedHashSet<>());
        request.accessToken.getAccess().add(accessItem);

        return request;
    }

    /**
     * Creates a grant access request for outgoing payment operations.
     *
     * <p>This method creates a specialized access request for outgoing payments,
     * including the specific identifier and debit amount limits. It's commonly
     * used when requesting permission to create outgoing payments with specific
     * constraints.
     *
     * @param clientWallet  the client wallet address making the request
     * @param accessType    the type of access being requested (typically outgoingPayment)
     * @param accessActions the set of actions to be performed (typically create)
     * @param identifier    the URI identifier for the specific resource (e.g., quote ID)
     * @param debitAmount   the maximum amount that can be debited
     * @return a new GrantAccessRequest configured for outgoing payments
     */
    public static GrantRequest outgoing(
            Client clientWallet,
            AccessItemType accessType,
            Set<AccessAction> accessActions,
            URI identifier,
            Amount debitAmount
    ) {
        GrantRequest request = build(clientWallet, accessType, accessActions);

        Optional<AccessItem> found = request.accessToken.getAccess().stream().findFirst();
        found.ifPresent(accessItem -> accessItem.accessOutgoing(identifier, debitAmount));

        return request;
    }

    /**
     * Configures this request to use redirect-based interaction.
     *
     * <p>This method sets up the request to use browser-based redirect interaction
     * for obtaining user consent. The authorization server will redirect the user
     * to the specified return URL after the interaction is complete.
     *
     * @param returnUrl the URL to return to when interaction is finished, or null to omit
     * @param nonce     a unique value to prevent replay attacks during the interaction
     * @return this GrantAccessRequest instance for method chaining
     */
    public GrantRequest redirectInteract(URI returnUrl, String nonce) {
        interact = new AccessInteract();
        interact.setStart(List.of(REDIRECT));

        if (returnUrl != null) {
            ensureNotNullOrEmpty(nonce, "nonce cannot be null or empty.");

            interact.setFinish(new InteractFinish());
            interact.getFinish().setMethod(REDIRECT);
            interact.getFinish().setUri(returnUrl);
            interact.getFinish().setNonce(nonce);
        }
        return this;
    }
}
