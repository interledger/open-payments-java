package org.interledger.openpayments;

import org.interledger.openpayments.model.key.Ed25519Keys;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

/**
 * Interledger Open Payments Client.
 * IOpenPaymentsClient is an interface that defines methods for interacting
 * with Open Payments services. It provides functionality for retrieving
 * wallet information associated with a specific URL.
 * Implementing classes represent the client-side interaction mechanism,
 * enabling secure and streamlined communication with Open Payments-enabled
 * servers.
 */
public interface IOpenPaymentsWalletAddressClient {
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
}
