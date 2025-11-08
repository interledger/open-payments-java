package org.interledger.openpayments.httpclient;

import lombok.Getter;

/**
 * Configuration options for the OpenPaymentsClient.
 * Provides adjustable time settings to control connection, read timeout,
 * and transaction expiration durations.
 */
@Getter
public class OpenPaymentsClientOptions {
    /**
     * Specifies the maximum duration, in seconds, the client will wait while
     * attempting to establish a connection to the server before timing out.
     * Ensures failed connection attempts do not block operations indefinitely.
     */
    private final int connectTimeOutInSeconds;

    /**
     * Defines the maximum total duration, in seconds, the client will wait for
     * a request to complete, including connection establishment, request
     * transmission, server processing, and response reception.
     * Prevents indefinite waiting during extended network operations or server delays.
     */
    private final int timeOutInSeconds;

    /**
     * Specifies the duration, in seconds, after which a transaction will
     * be considered expired if it is not completed. This duration helps ensure
     * transactions do not remain active indefinitely without resolution.
     */
    private final int transactionExpirationInSeconds;

    /**
     * Constructs an instance of {@code OpenPaymentsClientOptions} with specified timeout
     * and transaction expiration configuration.
     *
     * @param connectTimeoutInSeconds the maximum time, in seconds, to wait for a
     *                                connection to be established before timing out.
     * @param timeoutInSeconds        the maximum time, in seconds, the client will
     *                                wait for a request to complete, including connection
     *                                setup, request transmission, and response reception.
     * @param transactionExpirationInSeconds the time, in seconds, after which a transaction
     *                                       is considered expired if not completed.
     */
    public OpenPaymentsClientOptions(
            int connectTimeoutInSeconds,
            int timeoutInSeconds,
            int transactionExpirationInSeconds
    ) {
        this.connectTimeOutInSeconds = connectTimeoutInSeconds;
        this.timeOutInSeconds = timeoutInSeconds;
        this.transactionExpirationInSeconds = transactionExpirationInSeconds;
    }

    /**
     * A default instance of {@link OpenPaymentsClientOptions} initialized with the
     * following default configuration values:
     * - Connection Timeout: 10 seconds
     * - Read Timeout: 10 seconds
     * - Transaction Expiration: 10 minutes
     * This configuration is intended as a standard setup that provides reasonable
     * timeout and expiration values, ensuring the client can function under typical
     * network conditions without requiring customization.
     */
    public static final OpenPaymentsClientOptions DEFAULT =
            new OpenPaymentsClientOptions(10, 10, 10 * 60);
}