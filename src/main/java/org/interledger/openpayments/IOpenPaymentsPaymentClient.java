package org.interledger.openpayments;

import org.interledger.openpayments.model.IResourceServer;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.incomingpayment.IncomingPaymentsResult;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPaymentsResult;
import org.interledger.openpayments.model.payment.Payment;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.math.BigDecimal;
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
public interface IOpenPaymentsPaymentClient {
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
    IncomingPayment incomingPayment(WalletAddress receiver, Grant ipGrant, BigDecimal amount);

    /**
     * Retrieves the current status of an incoming payment.
     * <p>This method is used to monitor payment completion and track the received amount.
     * It can be called multiple times to poll for payment status updates until the
     * payment is marked as completed.
     *
     * @param incomingPaymentUri the incoming payment URI to retrieve all the payment details
     * @param grant the access grant that provides permission to read the payment
     * @return current payment status including completion state and received amount
     * @see IncomingPayment
     * @see Grant
     */
    IncomingPayment getIncomingPayment(URI incomingPaymentUri, Grant grant);

    /**
     * Retrieves incoming payment data for a specified wallet address.
     *
     * @param resourceServer The resource server instance used to access payment data.
     * @param walletAddressUri The URI representing the wallet address for which incoming payments are to be retrieved.
     * @param grant The authorization grant used to ensure valid access to the resource server.
     * @return An instance of IncomingPaymentsResult containing details of the retrieved incoming payments.
     */
    IncomingPaymentsResult getIncomingPayments(
            IResourceServer resourceServer,
            URI walletAddressUri,
            Grant grant
    );

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
    Payment finalizePayment(Grant finalized, WalletAddress sender, Quote quote);

    /**
     * Finalizes and processes an incoming payment using the provided grant, sender's wallet address,
     * associated incoming payment, and debit amount. This method completes the payment transaction
     * and ensures the outgoing funds match the specified debit amount and the relevant incoming payment details.
     *
     * @param finalized the grant object representing the authorization to finalize the payment.
     *                  This grant holds the necessary credentials and permissions for proceeding with the payment.
     * @param sender the wallet address of the sender initiating the payment.
     *               It identifies the source account responsible for the debit transaction.
     * @param incomingPayment the incoming payment object containing details of the payment to be finalized.
     *                        This includes information about the expected amount and associated metadata.
     * @param debitAmount the amount to be debited from the sender’s wallet.
     *                    This specifies the transfer value that fulfills the terms of the payment.
     * @return a Payment object representing the finalized payment transaction.
     *         This object encapsulates details such as the payment status, identifier, and additional metadata.
     */
    Payment finalizePayment(
            Grant finalized,
            WalletAddress sender,
            IncomingPayment incomingPayment, Amount debitAmount
    );

    /**
     * Completes the process for incoming payment creation.
     *
     * @param incomingPaymentToComplete the incoming payment that needs to be completed
     * @param grant the grant associated with completing the incoming payment
     * @return the completed incoming payment object
     */
    IncomingPayment incomingPaymentComplete(IncomingPayment incomingPaymentToComplete, Grant grant);

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

    /**
     * Retrieves a list of outgoing payments for the specified wallet address.
     *
     * @param resourceServer the resource server to use for fetching outgoing payment details
     * @param walletAddressUri the URI representing the wallet address for which the outgoing payments will be retrieved
     * @param grantRequest the grant request containing authentication and access details required for the operation
     * @return an OutgoingPaymentsResult object containing the details of outgoing payments for the specified wallet address
     */
    OutgoingPaymentsResult getOutgoingPayments(
            IResourceServer resourceServer,
            URI walletAddressUri,
            Grant grantRequest
    );
}
