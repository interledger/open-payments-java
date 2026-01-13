package org.interledger.openpayments;

import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.AccessAction;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.key.Ed25519Keys;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.payment.Payment;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

/**
 * Interledger Open Payments Client.
 * IOpenPaymentsClient is an interface that defines methods for interacting
 * with Open Payments services. It provides functionality for retrieving
 * wallet information associated with a specific URL.
 * Implementing classes represent the client-side interaction mechanism,
 * enabling secure and streamlined communication with Open Payments-enabled
 * servers.
 */
public interface IOpenPaymentsClient {
    /**
     * Retrieves the wallet address associated with the specified URL.
     * The wallet address provides a secure identifier and acts as a service
     * endpoint for APIs to interact with the corresponding account.
     *
     * @param url the URL associated with the desired wallet address
     * @return the wallet address corresponding to the provided URL
     */
    WalletAddress getWalletAddress(String url);

    /**
     * Retrieves Ed25519 public and private keys from the specified URL.
     *
     * @param url the URL from which the Ed25519 keys should be fetched.
     *            The URL must point to a valid resource that provides the key data.
     * @return an Ed25519Keys object containing the retrieved public and private keys.
     *         If the keys cannot be retrieved or the URL is invalid, it may throw
     *         an exception or return null based on the implementation.
     */
    Ed25519Keys getKeys(String url);

    /**
     * Creates and returns a grant to enable incoming payment permissions for the specified wallet address.
     *
     * @param receiver the wallet address that will receive the granted incoming payments
     * @param actions the actions associated with the granted permissions.
     * @return the created grant for the specified receiver and actions
     */
    Grant createGrantIncomingPayment(WalletAddress receiver, AccessAction... actions);

    /**
     * Creates an incoming payment for the specified receiver with the given grant
     * and amount. This method sets up a payment destination where senders can make
     * payments to the receiver's wallet.
     *
     * @param receiver the wallet address of the payment receiver. This identifies the
     *                 destination account for the incoming payment.
     * @param ipGrant  the grant that authorizes the creation of the incoming payment.
     *                 It provides access credentials for interacting with the receiver's
     *                 payment endpoint.
     * @param amount   the amount expected to be received for this payment. This
     *                 represents the total target amount that the sender is
     *                 expected to pay.
     * @return an IncomingPayment object representing the created payment request. It
     *         contains details such as payment identifier, current status, and
     *         associated metadata.
     */
    IncomingPayment createIncomingPayment(WalletAddress receiver, Grant ipGrant, BigDecimal amount);

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
    Grant createGrantQuote(WalletAddress sender, AccessAction... actions);

    /**
     * Creates a payment quote for a specified sender and incoming payment.
     * The payment quote provides the necessary details about a payment, such as
     * conversion rates, fees, and payment terms associated with the given
     * quote token, sender, and the incoming payment.
     *
     * @param quoteToken a unique token representing the quote request. This token
     *                   is used to identify and secure the quote creation process.
     * @param sender the wallet address of the payment sender. This identifies the
     *               account initiating the payment and provides the context for
     *               the quote transaction.
     * @param incomingPayment an object representing the pre-configured incoming
     *                        payment details for which the quote is being created.
     *                        This includes information such as payment amount,
     *                        recipient details, and associated metadata.
     * @param debitAmount the OPTIONAL amount to debit from the sender's wallet account.
     * @param receiveAmount the OPTIONAL amount to be received by the payee's wallet account.
     * @return a Quote object containing the details of the created payment quote.
     *         It includes information such as the quoted payment amount, currency
     *         details, expiration time, and any applicable terms or fees.
     */
    Quote createQuote(
            String quoteToken,
            WalletAddress sender,
            IncomingPayment incomingPayment,
            Optional<Amount> debitAmount,
            Optional<Amount> receiveAmount
    );

    /**
     * Retrieves a quote based on the provided URI and grant request.
     *
     * @param quoteUri the URI pointing to the desired quote resource
     * @param grantRequest the grant request containing necessary permissions or details for accessing the quote
     * @return the Quote object retrieved from the specified URI
     */
    Quote getQuote(URI quoteUri, Grant grantRequest);

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
    OutgoingPayment createGrantContinuation(
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
    Grant createGrantFinalize(OutgoingPayment outgoingPayment, String interactRef);

    /**
     * Cancel a grant.
     * @param grant the grant object that represents the grant to be canceled
     */
    void createGrantCancel(Grant grant);

    /**
     * Finalizes and processes an outgoing payment using the given grant, sender's wallet, and payment quote.
     * This method completes the payment transaction based on the pre-established quote and authorization grant.
     *
     * @param finalized the finalized grant object representing the authorization to perform the payment.
     *                  This grant includes the necessary access credentials and authorization details.
     * @param sender the wallet address of the payment sender. It identifies the account initiating
     *                     the payment process.
     * @param quote the payment quote object containing details of the payment, such as the quoted amount,
     *              currency, terms, and associated fees.
     * @return a Payment object representing the completed payment transaction. It provides details such as
     *         the transaction status, identifier, and associated metadata.
     */
    Payment createFinalizePayment(Grant finalized, WalletAddress sender, Quote quote);

    /**
     * Creates and finalizes a payment based on the provided grant, sender wallet address, incoming payment,
     * and debit amount. This method consolidates the required information to generate a finalized
     * payment transaction.
     *
     * @param finalized the finalized grant object containing necessary grant details
     * @param sender the wallet address initiating the payment
     * @param incomingPayment the incoming payment details received for processing
     * @param debitAmount the amount to be debited from the sender's wallet
     * @return a Payment object representing the completed and finalized payment transaction
     */
    Payment createFinalizePayment(Grant finalized, WalletAddress sender, IncomingPayment incomingPayment, Amount debitAmount);

    /**
     * Retrieves the current status of an incoming payment.
     * <p>This method is used to monitor payment completion and track the received amount.
     * It can be called multiple times to poll for payment status updates until the
     * payment is marked as completed.
     *
     * @param incomingPaymentUri the incoming payment URI to retrieve all the payment details
     * @param grantRequest the access grant that provides permission to read the payment
     * @return current payment status including completion state and received amount
     * @see IncomingPayment
     * @see Grant
     */
    IncomingPayment getIncomingPayment(URI incomingPaymentUri, Grant grantRequest);

    /**
     * Completes the process for incoming payment creation.
     *
     * @param incomingPaymentToComplete the incoming payment that needs to be completed
     * @param ipCompleteGrant the grant associated with completing the incoming payment
     * @return the completed incoming payment object
     */
    IncomingPayment createIncomingPaymentComplete(IncomingPayment incomingPaymentToComplete, Grant ipCompleteGrant);

    /**
     * Retrieves the current status of an outgoing payment.
     *
     * @param outgoingPaymentUri the outgoing payment URI to retrieve all the payment details
     * @param grantRequest the access grant that provides permission to read the payment
     * @return current payment status.
     * @see OutgoingPayment
     * @see Grant
     */
    OutgoingPayment getOutgoingPayment(URI outgoingPaymentUri, Grant grantRequest);
}
