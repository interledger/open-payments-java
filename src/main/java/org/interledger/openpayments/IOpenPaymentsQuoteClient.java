package org.interledger.openpayments;

import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

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
public interface IOpenPaymentsQuoteClient {
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
    Quote create(
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
    Quote get(URI quoteUri, Grant grantRequest);
}
