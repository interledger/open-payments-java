package org.interledger.openpayments;

import org.interledger.openpayments.model.IAuthServer;
import org.interledger.openpayments.model.grant.Grant;

/**
 * Interledger Open Payments Client.
 * IOpenPaymentsClient is an interface that defines methods for interacting
 * with Open Payments services. It provides functionality for retrieving
 * wallet information associated with a specific URL.
 * Implementing classes represent the client-side interaction mechanism,
 * enabling secure and streamlined communication with Open Payments-enabled
 * servers.
 */
public interface IOpenPaymentsAuthClient {
    /**
     * Provides access to the Open Payments Grant Client, which allows for the
     * creation, management, and cancellation of grants used for payment-related
     * operations within the Open Payments framework.
     *
     * @return an instance of IOpenPaymentsGrantClient that enables interaction
     *         with grants, such as creating grants for incoming payments, quoting,
     *         and outgoing payment authorizations.
     */
    IOpenPaymentsGrantClient grant();

    /**
     * Creates a new grant for an incoming payment request using the specified authentication server and grant ID.
     *
     * @param authServer the authorization server to be used for creating the grant
     * @param tokenId the unique identifier for the token to be rotated.
     * @param grant The grant used to authorize the rotation request.
     * @return the rotated grant.
     */
    Grant rotateToken(IAuthServer authServer, String tokenId, Grant grant);

    /**
     * Revokes an existing token, rendering it invalid for future use.
     * Tokens are usually accessible from issued grants, see {@code grant.getAccess().getToken()}
     *
     * @param authServer The authentication server responsible for managing the token lifecycle.
     * @param tokenId The unique identifier of the token to be revoked.
     * @param grant The grant used to authorize the revocation request.
     */
    void revokeToken(IAuthServer authServer, String tokenId, Grant grant);
}
