package org.interledger.openpayments;

/**
 * Utility class for accessing test-related configuration values.
 *
 * This class provides static methods for retrieving important configuration properties
 * used in test cases, such as client wallet addresses and key IDs. All properties are
 * loaded from a central configuration source managed by the {@code TestConfiguration} class.
 *
 * This class is not meant to be instantiated, as all its methods are static.
 */
public final class TestHelper {
    private TestHelper() {
        super();
    }

    /**
     * Gets the client wallet address from configuration
     *
     * @return the wallet address for the payment initiator
     */
    public static String getClientWalletAddress() {
        return TestConfiguration.getClientWalletAddress();
    }

    /**
     * Gets the client key ID from configuration
     *
     * @return the key ID for the private key
     */
    public static String getClientKeyId() {
        return TestConfiguration.getClientKeyId();
    }

    /**
     * Gets the sender wallet address from configuration
     *
     * @return the sender wallet address for tests
     */
    public static String getSenderWalletAddress() {
        return TestConfiguration.getSenderWalletAddress();
    }

    /**
     * Gets the receiver wallet address from configuration
     *
     * @return the receiver wallet address for tests
     */
    public static String getReceiverWalletAddress() {
        return TestConfiguration.getReceiverWalletAddress();
    }
}
