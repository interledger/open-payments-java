package org.interledger.openpayments.util.testing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

/**
 * Utility class for generating cryptographic key pairs and private keys in tests.
 *
 * <p>Provides simple, deterministic factory methods to create {@link KeyPair} and {@link
 * PrivateKey} instances (defaults to Ed25519) without external dependencies or keystores. Ideal for
 * unit and integration tests requiring signing keys.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * PrivateKey privateKey = TestKeys.generatePrivateKey();
 * KeyPair keyPair = TestKeys.generateKeyPair();                    // Ed25519
 * KeyPair rsaPair = TestKeys.generateKeyPair("RSA");               // custom algorithm
 * }</pre>
 *
 * <p>All methods throw {@link NoSuchAlgorithmException} if the requested algorithm is not
 * available.
 *
 * @since 1.0
 */
public final class TestKeys {
    public static PrivateKey generatePrivateKey() throws NoSuchAlgorithmException {
        return generatePrivateKey("Ed25519");
    }

    public static PrivateKey generatePrivateKey(String algorithm) throws NoSuchAlgorithmException {
        return generateKeyPair(algorithm).getPrivate();
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair("Ed25519");
    }

    public static KeyPair generateKeyPair(String algorithm) throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        return kpg.generateKeyPair();
    }
}
