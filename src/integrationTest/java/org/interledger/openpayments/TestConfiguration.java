package org.interledger.openpayments;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading and accessing test configuration properties.
 *
 * This class is responsible for loading configuration values from a
 * `test-config.properties` file located in the classpath. It provides methods
 * to retrieve required or optional configuration properties used in test cases.
 *
 * The configuration file must be properly set up before running tests. If any
 * required property is missing or empty, a runtime exception is thrown.
 *
 * This class is not meant to be instantiated, as all its methods are
 * static.
 */
public final class TestConfiguration {
    private static final String CONFIG_FILE = "test-config.properties";
    private static final Properties properties = new Properties();

    static {
        loadConfiguration();
    }

    private TestConfiguration() {
        // hide constructor
    }

    private static void loadConfiguration() {
        try (InputStream input = TestConfiguration.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Configuration file '" + CONFIG_FILE + "' not found in test resources. " +
                        "Please copy test-config.properties.template to test-config.properties and configure it.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test configuration", e);
        }
    }

    /**
     * Gets the client wallet address
     *
     * @return the wallet address for the payment initiator
     */
    public static String getClientWalletAddress() {
        return getRequiredProperty("client.wallet.address");
    }

    /**
     * Gets the client private key in PEM format
     *
     * @return the private key for request signing
     */
    public static String getClientPrivateKey() {
        return getRequiredProperty("client.private.key");
    }

    /**
     * Gets the client key ID
     *
     * @return the key ID for the private key
     */
    public static String getClientKeyId() {
        return getRequiredProperty("client.key.id");
    }

    /**
     * Gets the sender wallet address for tests
     *
     * @return the sender wallet address
     */
    public static String getSenderWalletAddress() {
        return getRequiredProperty("sender.wallet.address");
    }

    /**
     * Gets the receiver wallet address for tests
     *
     * @return the receiver wallet address
     */
    public static String getReceiverWalletAddress() {
        return getRequiredProperty("receiver.wallet.address");
    }

    /**
     * Gets a required property value
     *
     * @param key the property key
     * @return the property value
     * @throws RuntimeException if the property is not found or empty
     */
    private static String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Required configuration property '" + key + "' not found or empty in " + CONFIG_FILE);
        }
        return value.trim();
    }

    /**
     * Gets an optional property value
     *
     * @param key          the property key
     * @param defaultValue the default value if property is not found
     * @return the property value or default value
     */
    public static String getOptionalProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }
}