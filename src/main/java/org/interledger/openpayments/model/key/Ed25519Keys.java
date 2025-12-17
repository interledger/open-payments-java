package org.interledger.openpayments.model.key;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.metadata.MetaDataItem;

import java.util.Set;

/**
 * Represents a collection of Ed25519 public keys, alongside their associated metadata.
 *
 * <p>This class is designed to hold a set of Ed25519 public keys and their metadata
 * within a flexible and extensible structure. Each key in the collection is described
 * using a {@link MetaDataItem} object, which allows the addition of descriptive or
 * operational metadata.
 *
 * <p>Common use cases for this class include:
 * - Managing a group of public keys used in cryptographic operations.
 * - Associating metadata (e.g., descriptions, tags) with each key.
 * - Facilitating the serialization and deserialization of key data in JSON format.
 *
 * <p>Serialization and deserialization of instances rely on Jackson annotations, enabling
 * their use in systems that require structured JSON data exchange.
 *
 * Key details:
 * - Each key is represented as a {@link MetaDataItem}, where the `key` property refers to the
 *   identifier of the key, and the `value` property contains optional metadata.
 * - The `keys` field is nullable, allowing for empty key collections to be represented.
 */
@Data
public class Ed25519Keys {
    /**
     * A set of metadata items representing Ed25519 public keys and their associated metadata.
     *
     * <p>This field contains a collection of {@link MetaDataItem} objects, where each item
     * represents a single Ed25519 public key paired with descriptive or operational metadata.
     * The `keys` property is nullable, allowing the representation of an empty collection
     * when no keys are present.
     *
     * <p>Features:
     * - Each element in the set is an instance of {@link MetaDataItem}, which includes a `key`
     *   field to identify the public key and an optional `value` field for associated metadata.
     * - By default, this field is excluded from JSON serialization if null, facilitated by the
     *   `@JsonInclude` annotation with `JsonInclude.Include.NON_NULL`.
     * - Serialized as "keys" in JSON using the `@JsonProperty` annotation.
     *
     * <p>Usage scenarios include:
     * - Managing a group of cryptographic keys with metadata.
     * - Providing metadata for identification, categorization, or operational purposes
     *   associated with Ed25519 public keys.
     */
    @JsonProperty(value = "keys")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<Ed25519PublicKey> keys;
}
