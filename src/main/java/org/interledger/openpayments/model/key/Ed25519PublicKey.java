package org.interledger.openpayments.model.key;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an Ed25519 public key used for cryptographic operations.
 *
 * <p>This class models an Ed25519 public key, including its associated
 * metadata and attributes. It adheres to the JSON Web Key (JWK) standard
 * for representing cryptographic keys in a JSON-based data structure.
 *
 * <p>The defined properties include the key's unique identifier, algorithm,
 * usage, and specific cryptographic attributes such as curve and key value.
 *
 * Key attributes:
 * - `kid`: A unique identifier for the key.
 * - `alg`: The cryptographic algorithm associated with the key.
 * - `use`: The intended use of the key (e.g., "sig" for signing, "enc" for encryption).
 * - `kty`: The key type (e.g., "OKP" for Octet Key Pair).
 * - `crv`: The cryptographic curve used (e.g., "Ed25519").
 * - `x`: The public key value encoded as a base64url-encoded string.
 *
 * <p>Instances of this class are serialized to and deserialized from JSON
 * using Jackson annotations, ensuring compatibility with structured data
 * exchanged in JSON format.
 */
@Data
public class Ed25519PublicKey {
    /**
     * A unique identifier for the cryptographic key.
     *
     * <p>This property serves as a globally unique identifier (often as a UUID or
     * similar format) for the Ed25519 public key. It is required for distinguishing
     * the key within systems that manage multiple cryptographic keys, such as in
     * JSON Web Key (JWK) contexts.
     *
     * <p>The "kid" (Key ID) is particularly useful in systems that rely on key
     * rotation or support multiple keys for signing or encryption, allowing for
     * efficient key lookup and management.
     *
     * <p>In serialized JSON representations, this field is represented as a
     * required string under the "kid" property.
     */
    @JsonProperty(value = "kid", required = true)
    private String kid;

    /**
     * The cryptographic algorithm associated with the Ed25519 public key.
     *
     * <p>This field specifies the algorithm to be used with the key, for example,
     * "EdDSA" for Ed25519-based digital signatures. It is a required property
     * in the context of JSON Web Key (JWK) standards and ensures that systems
     * interacting with the key can correctly interpret and apply the intended
     * cryptographic operations.
     *
     * <p>In serialized JSON representations, this field is represented as a
     * required string under the "alg" property.
     */
    @JsonProperty(value = "alg", required = true)
    private String alg;

    /**
     * Specifies the intended use of the Ed25519 public key.
     *
     * <p>This field indicates how the key is to be utilized in cryptographic operations.
     * Typical values include:
     * - "sig" for signature generation or validation.
     * - "enc" for encryption or decryption purposes.
     *
     * <p>The value is optional and, if null, the key's usage is not explicitly declared.
     *
     * <p>Serialized as the "use" property in JSON representations, aligning with the
     * JSON Web Key (JWK) standard.
     */
    @JsonProperty(value = "use")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String use;

    /**
     * The key type identifier for the Ed25519 public key.
     *
     * <p>This field specifies the general type of the key, as defined in the
     * JSON Web Key (JWK) specification. For Ed25519 public keys, the value
     * is typically set to "OKP" (Octet Key Pair).
     *
     * <p>The "kty" property is essential for identifying the key type and
     * ensuring compatibility with cryptographic operations and data formats
     * that conform to the JWK standard.
     *
     * <p>In serialized JSON representations, this field is represented as a
     * required string under the "kty" property.
     */
    @JsonProperty(value = "kty", required = true)
    private String kty;

    /**
     * The cryptographic curve associated with the Ed25519 public key.
     *
     * <p>This field specifies the curve used in the context of an Octet Key Pair (OKP),
     * with the default and expected value being "Ed25519" for EdDSA signature keys.
     * The curve parameter ensures compatibility with cryptographic operations
     * conforming to the JSON Web Key (JWK) specification.
     *
     * <p>In serialized JSON representations, this field is represented as a required
     * string under the "crv" property.
     */
    @JsonProperty(value = "crv", required = true)
    private String crv;

    /**
     * The public key value encoded as a base64url-encoded string.
     *
     * <p>This field represents the Ed25519 public key in its raw form, encoded as
     * a string for compatibility with JSON Web Key (JWK) specifications. It serves
     * as the core cryptographic material required for verifying signatures or
     * cryptographic operations associated with the Ed25519 algorithm.
     *
     * <p>The key is serialized as a required string in JSON under the "x" property,
     * adhering to the JWK standard for public key representation.
     */
    @JsonProperty(value = "x", required = true)
    private String x;
}
