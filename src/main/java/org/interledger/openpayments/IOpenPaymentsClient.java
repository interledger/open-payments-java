package org.interledger.openpayments;

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
     * Provides an interface for interacting with authentication and authorization mechanisms
     * in the context of Open Payments services. The returned object can be used to create,
     * revoke, rotate, or manage access grants for performing various operations such as
     * incoming payments, quoting payments, and outgoing payment continuations.
     *
     * @return an instance of {@code IOpenPaymentsAuthClient}, enabling access to methods
     *         for managing authentication and authorization in Open Payments workflows.
     */
    IOpenPaymentsAuthClient auth();

    /**
     * Provides an interface for interacting with wallet address-related operations
     * in the context of Open Payments. The returned object can be used to retrieve
     * wallet addresses linked to specific URLs as well as perform other wallet account
     * endpoint operations.
     *
     * @return an instance of {@code IOpenPaymentsWalletAddressClient}, enabling access
     *         to wallet address functionalities within the scope of Open Payments services.
     */
    IOpenPaymentsWalletAddressClient walletAddress();

    /**
     * Provides an interface for interacting with payment quoting operations
     * in the context of Open Payments. The returned object can be used to create
     * or retrieve payment quotes, enabling users to determine the payment terms,
     * fees, and other details before initiating a payment transaction.
     *
     * @return an instance of {@code IOpenPaymentsQuoteClient}, enabling access
     *         to methods for creating and retrieving payment quotes within
     *         Open Payments workflows.
     */
    IOpenPaymentsQuoteClient quote();

    /**
     * Provides an interface for interacting with payment-related operations
     * within the context of Open Payments services. The returned object can
     * be used to initiate, track, or retrieve details about payments, enabling
     * seamless and secure transaction management.
     *
     * @return an instance of {@code IOpenPaymentsPaymentClient}, enabling access
     *         to methods for managing payment operations in Open Payments workflows.
     */
    IOpenPaymentsPaymentClient payment();
}
