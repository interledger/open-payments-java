package org.interledger.openpayments.model.grant;

import lombok.Data;
import lombok.ToString;

import java.net.URI;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * Represents a wallet address for Interledger Open Payments operations.
 *
 * <p>A wallet address is a URL that identifies a specific wallet or account
 * within the Interledger network. It serves as the primary identifier for
 * sending and receiving payments through the Open Payments protocol.
 *
 * <p>Wallet addresses are typically hosted by Rafiki instances and follow
 * the URI format. They are used to discover wallet capabilities, create
 * payment resources, and identify parties in payment transactions.
 *
 * <p>Example wallet addresses:
 * <ul>
 *   <li>"https://ilp.interledger-test.dev/alice"</li>
 *   <li>"https://ilp.interledger-test.dev/bob"</li>
 *   <li>"https://wallet.example.com/user123"</li>
 * </ul>
 *
 * <p>This class is immutable and thread-safe. The wallet address is validated
 * during construction to ensure it's a valid URI.
 */
@ToString
@Data
public class Client {
    /**
     * The URI representing the wallet address.
     *
     * <p>This is the actual wallet address URI that identifies the wallet
     * within the Interledger network. It's used for payment operations,
     * resource discovery, and wallet identification.
     *
     * <p>The URI must be valid and typically follows the HTTPS scheme
     * for security purposes.
     */
    private final URI paymentPointer;

    /**
     * Constructs a new WalletAddress from a string URI.
     *
     * <p>This constructor parses the provided address string into a URI
     * and validates that it's not null. The string should be a valid
     * URI format.
     *
     * @param address the wallet address as a string (e.g., "https://ilp.interledger-test.dev/alice")
     * @throws IllegalArgumentException if the address is null
     * @throws IllegalArgumentException if the address is not a valid URI format
     */
    public Client(String address) {
        ensureNotNullOrEmpty(address, "address cannot be null or empty.");
        this.paymentPointer = URI.create(address);
    }

    /**
     * Constructs a new WalletAddress from a URI object.
     *
     * <p>This constructor accepts a pre-parsed URI object and validates
     * that it's not null. This is useful when you already have a URI
     * object from other operations.
     *
     * @param address the wallet address as a URI object
     * @throws IllegalArgumentException if the address is null
     */
    public Client(URI address) {
        ensureNotNull(address, "address cannot be null.");
        this.paymentPointer = address;
    }
}
