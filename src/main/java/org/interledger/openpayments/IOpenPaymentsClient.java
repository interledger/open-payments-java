package org.interledger.openpayments;

import org.interledger.openpayments.model.WalletAddress;

/**
 * Interledger Open Payments Client.
 * 
 * IOpenPaymentsClient is an interface that defines methods for interacting
 * with Open Payments services. It provides functionality for retrieving
 * wallet information associated with a specific URL.
 *
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
}
