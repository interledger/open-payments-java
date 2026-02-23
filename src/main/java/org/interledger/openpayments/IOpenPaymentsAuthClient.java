package org.interledger.openpayments;

import org.interledger.openpayments.model.IAuthServer;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.AccessAction;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.net.URI;

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
     * Creates and returns a grant to enable incoming payment permissions for the specified wallet address.
     *
     * @param receiver the wallet address that will receive the granted incoming payments
     * @param actions the actions associated with the granted permissions.
     * @return the created grant for the specified receiver and actions
     */
    Grant grantIncomingPayment(WalletAddress receiver, AccessAction... actions);
    
    /**
     * Creates a grant for quoting payments using the given wallet address.
     * This method interacts with the Open Payments-enabled server to generate
     * an access grant, allowing the client to perform a payment quote operation.
     *
     * @param sender the wallet address representing the source of the payment.
     *               It identifies the payment sender's account and is used to
     *               generate the associated grant.
     * @param actions the actions associated with the granted permissions.
     * @return a Grant object containing the access credentials, including the
     *         access token and optional continuation details, for quoting payments
     *         on behalf of the sender.
     */
    Grant grantQuote(WalletAddress sender, AccessAction... actions);

    /**
     * Creates a continuation grant for an outgoing payment request.
     * This method produces an authorization grant that can be used to continue
     * the process of an outgoing payment after the initial interaction.
     *
     * @param sender the wallet address of the payment sender. This identifies the
     *               account initiating the outgoing payment request.
     * @param debitAmount the amount to be debited from the sender's wallet account.
     * @param returnUrl the URI to which the user will be redirected after
     *                  completing the interaction required for the outgoing
     *                  payment grant.
     * @param nonce a unique identifier to prevent replay attacks and ensure
     *              request integrity. It serves as part of the grant continuation
     *              process.
     *
     * @return an OutgoingPayment object containing the details of the continuation
     *         grant. This includes the continuation information, optional user
     *         interaction details, or an access token based on the authorization
     *         flow.
     */
    OutgoingPayment grantContinuation(
            WalletAddress sender,
            Amount debitAmount,
            URI returnUrl,
            String nonce
    );

    /**
     * Finalizes the process for creating a grant associated with an outgoing payment.
     * This method completes the authorization flow for an outgoing payment by
     * using the provided outgoing payment details and interaction reference.
     *
     * @param outgoingPayment the outgoing payment for which the grant is to be finalized.
     *                        This contains the details and context of the outgoing payment
     *                        authorization process.
     * @param interactRef the interaction reference identifier used to complete the
     *                    authorization flow. This is typically provided during the
     *                    preceding steps of the authorization process.
     * @return a Grant object representing the finalized access credentials, including
     *         the access token and any continuation details for the outgoing payment.
     */
    Grant grantFinalize(OutgoingPayment outgoingPayment, String interactRef);

    /**
     * Cancel a grant.
     * @param grant the grant object that represents the grant to be canceled
     */
    void grantCancel(Grant grant);

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
