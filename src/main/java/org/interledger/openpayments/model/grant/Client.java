package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.interledger.openpayments.mapper.ClientSerializer;
import org.interledger.openpayments.model.key.Ed25519PublicKey;

import java.net.URI;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * Identifies the client instance making a grant request to the authorization server.
 *
 * <p>Open Payments (v1.3.0+) supports three forms of client identification:
 * <ul>
 *   <li>{@link Format#WALLET_ADDRESS} - an object with a {@code walletAddress} property, e.g.
 *       {@code {"walletAddress": "https://ilp.interledger-test.dev/alice"}}. This is the default
 *       when using the {@link #Client(String)} or {@link #Client(URI)} constructors.</li>
 *   <li>{@link Format#DIRECTED_IDENTITY} - an object with a {@code jwk} property containing the
 *       client's public key. The authorization server does not need to resolve a wallet address,
 *       which improves privacy. Only allowed for non-interactive grant requests
 *       (e.g. incoming payments).</li>
 *   <li>{@link Format#WALLET_ADDRESS_STRING} - the wallet address as a plain string.
 *       <strong>Deprecated</strong> by the specification and kept only for backwards compatibility
 *       with authorization servers that do not support the object form.</li>
 * </ul>
 *
 * <p>When using a wallet address, a JSON Web Key Set containing the public key used to sign the
 * request MUST be available at the wallet address + {@code /jwks.json}.
 *
 * <p>Example wallet addresses:
 * <ul>
 *   <li>"https://ilp.interledger-test.dev/alice"</li>
 *   <li>"https://ilp.interledger-test.dev/bob"</li>
 *   <li>"https://wallet.example.com/user123"</li>
 * </ul>
 *
 * <p>This class is immutable and thread-safe.
 */
@Data
@JsonSerialize(using = ClientSerializer.class)
public class Client {
    /**
     * The supported client identification formats.
     */
    public enum Format {
        /**
         * Object form: {@code {"walletAddress": "<uri>"}}.
         */
        WALLET_ADDRESS,

        /**
         * Directed identity: {@code {"jwk": {...}}}.
         */
        DIRECTED_IDENTITY,

        /**
         * Deprecated plain string form: {@code "<uri>"}.
         */
        WALLET_ADDRESS_STRING
    }

    /**
     * The URI representing the client wallet address.
     *
     * <p>Present for {@link Format#WALLET_ADDRESS} and {@link Format#WALLET_ADDRESS_STRING},
     * {@code null} for {@link Format#DIRECTED_IDENTITY}.
     */
    private final URI paymentPointer;

    /**
     * The client's public key, used for {@link Format#DIRECTED_IDENTITY}.
     */
    private final Ed25519PublicKey jwk;

    /**
     * How this client is serialized in grant requests.
     */
    private final Format format;

    /**
     * Constructs a new client identified by its wallet address (object form) from a string URI.
     *
     * @param address the wallet address as a string (e.g., "https://ilp.interledger-test.dev/alice")
     * @throws IllegalArgumentException if the address is null or empty
     * @throws IllegalArgumentException if the address is not a valid URI format
     */
    public Client(String address) {
        ensureNotNullOrEmpty(address, "paymentPointer cannot be null or empty.");
        this.paymentPointer = URI.create(address);
        this.jwk = null;
        this.format = Format.WALLET_ADDRESS;
    }

    /**
     * Constructs a new client identified by its wallet address (object form) from a URI object.
     *
     * @param address the wallet address as a URI object
     * @throws IllegalArgumentException if the address is null
     */
    public Client(URI address) {
        this(address, Format.WALLET_ADDRESS);
    }

    private Client(URI address, Format format) {
        ensureNotNull(address, "paymentPointer cannot be null.");
        this.paymentPointer = address;
        this.jwk = null;
        this.format = format;
    }

    private Client(Ed25519PublicKey jwk) {
        ensureNotNull(jwk, "jwk cannot be null.");
        this.paymentPointer = null;
        this.jwk = jwk;
        this.format = Format.DIRECTED_IDENTITY;
    }

    /**
     * Creates a client identified by its wallet address, serialized as
     * {@code {"walletAddress": "<uri>"}}.
     *
     * @param address the client wallet address
     * @return a new client
     */
    public static Client walletAddress(URI address) {
        return new Client(address, Format.WALLET_ADDRESS);
    }

    /**
     * Creates a client using directed identity, serialized as {@code {"jwk": {...}}}.
     *
     * <p>The authorization server uses the supplied public key to verify the request signature,
     * so the key MUST match the private key used to sign requests. Directed identity can only be
     * used for non-interactive grant requests (e.g. incoming payments).
     *
     * @param jwk the client's Ed25519 public key
     * @return a new client
     */
    public static Client directedIdentity(Ed25519PublicKey jwk) {
        return new Client(jwk);
    }

    /**
     * Creates a client identified by its wallet address, serialized as a plain string.
     *
     * @param address the client wallet address
     * @return a new client
     * @deprecated the plain string form is deprecated by the Open Payments specification.
     *             Use {@link #walletAddress(URI)} or {@link #directedIdentity(Ed25519PublicKey)}.
     */
    @Deprecated
    public static Client walletAddressString(URI address) {
        return new Client(address, Format.WALLET_ADDRESS_STRING);
    }
}
