package org.interledger.openpayments.util.testing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

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
