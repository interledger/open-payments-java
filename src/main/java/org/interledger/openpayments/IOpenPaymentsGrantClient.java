package org.interledger.openpayments;

import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.AccessAction;
import org.interledger.openpayments.model.grant.CardAuthorization;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.grant.Limits;
import org.interledger.openpayments.model.grant.PendingGrant;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.net.URI;

/**
 * Interface representing a client for interacting with Open Payments grants.
 * This client provides methods to create, finalize, and cancel grants to allow
 * actions related to incoming payments, outgoing payments, and payment quoting.
 *
 * Grants are used to manage permissions and access credentials for operations
 * within the Open Payments framework.
 */
public interface IOpenPaymentsGrantClient {
    /**
     * Creates and returns a grant to enable incoming payment permissions for the specified wallet address.
     *
     * @param receiver the wallet address that will receive the granted incoming payments
     * @param actions the actions associated with the granted permissions.
     * @return the created grant for the specified receiver and actions
     */
    Grant incomingPayment(WalletAddress receiver, AccessAction... actions);
    
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
    Grant quote(WalletAddress sender, AccessAction... actions);

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
     * @deprecated use {@link #continuation(WalletAddress, Limits, URI, String)} with
     *             {@link Limits#debit(Amount)}, which returns a {@link PendingGrant}.
     */
    @Deprecated
    OutgoingPayment continuation(
            WalletAddress sender,
            Amount debitAmount,
            URI returnUrl,
            String nonce
    );

    /**
     * Requests an interactive outgoing payment grant with the given limits.
     *
     * <p>Use this to request a grant for future (recurring) payments by setting an
     * {@code interval}, or to limit by {@code receiveAmount} or {@code receiver}, e.g.:
     * <pre>
     * var limits = Limits.debit(Amount.build(new BigDecimal("100.00"), "CAD"), "R3/2025-05-20T13:00:00Z/P1M");
     * var pending = client.auth().grant().continuation(sender, limits, returnUrl, nonce);
     * </pre>
     *
     * @param sender    the wallet address of the payment sender
     * @param limits    the limits under which outgoing payments can be created, or null for none
     * @param returnUrl the URI to which the user will be redirected after the interaction
     * @param nonce     a unique value used to secure the interaction callback
     * @return the pending grant, containing interaction and continuation details; continue it with
     *         {@link #finalize(PendingGrant, String)}
     * @throws org.interledger.openpayments.exception.OpenPaymentsClientException if the client uses
     *         directed identity, or the server did not return interaction details
     */
    PendingGrant continuation(
            WalletAddress sender,
            Limits limits,
            URI returnUrl,
            String nonce
    );

    /**
     * Requests an interactive grant to verify that the user has access to the given wallet address.
     *
     * <p>No access token for payments is requested. Redirect the user to
     * {@code pendingGrant.getInteract().getRedirect()}, then call
     * {@link #finalize(PendingGrant, String)} with the {@code interact_ref} received on the
     * return URL. The resulting {@link Grant#getSubject()} confirms the verified wallet address.
     *
     * @param walletAddress the wallet address the user must prove access to
     * @param returnUrl     the URI to which the user will be redirected after the interaction
     * @param nonce         a unique value used to secure the interaction callback
     * @return the pending grant, containing interaction and continuation details
     * @throws org.interledger.openpayments.exception.OpenPaymentsClientException if the client uses
     *         directed identity, or the server did not return interaction details
     */
    PendingGrant verifyOwnership(WalletAddress walletAddress, URI returnUrl, String nonce);

    /**
     * Requests a non-interactive outgoing payment grant that authorizes a card (EMV) payment
     * (Open Payments v1.4.0+).
     *
     * <p>The authorization result is returned as {@link CardAuthorization} data on the
     * outgoing payment access item of the granted access token.
     *
     * @param sender            the wallet address of the card holder (payment sender)
     * @param receiver          the incoming payment URI the payment will be made to
     * @param receiveAmount     the amount to be received
     * @param cardAuthorization the card credentials
     * @return the grant containing the access token and the card authorization result
     */
    Grant cardAuthorization(
            WalletAddress sender,
            URI receiver,
            Amount receiveAmount,
            CardAuthorization cardAuthorization
    );

    /**
     * Continues a pending grant after the user has completed the interaction.
     *
     * @param pendingGrant the pending grant returned by the initial grant request
     * @param interactRef  the interaction reference received on the return URL
     * @return the grant, containing an access token and/or subject information
     */
    Grant finalize(PendingGrant pendingGrant, String interactRef);

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
     * @deprecated use {@link #finalize(PendingGrant, String)} with the {@link PendingGrant}
     *             returned by {@link #continuation(WalletAddress, Limits, URI, String)}.
     */
    @Deprecated
    Grant finalize(OutgoingPayment outgoingPayment, String interactRef);

    /**
     * Cancel a grant.
     * @param grant the grant object that represents the grant to be canceled
     */
    void cancel(Grant grant);
}
