package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNullOrEmpty;

/**
 * Enumeration of access actions available for Interledger Open Payments operations.
 *
 * <p>This enum defines the specific actions that can be requested in an access grant
 * for different types of payment resources. Access actions control what operations
 * a client can perform on payment resources like incoming payments, outgoing payments,
 * and quotes.
 *
 * <p>Actions are combined with resource types to create fine-grained access control
 * for payment operations, ensuring that clients only have the minimum necessary
 * permissions to perform their intended functions.
 */
public enum AccessAction {
    /**
     * Permission to read a specific resource.
     *
     * <p>Allows the client to retrieve information about a specific payment resource
     * identified by its URI. This is used for reading individual payments, quotes,
     * or other resources.
     */
    read("read"),

    /**
     * Permission to complete a resource operation.
     *
     * <p>Allows the client to finalize or complete a payment operation that was
     * previously initiated. This is commonly used for completing outgoing payments
     * after user authorization.
     */
    complete("complete"),

    /**
     * Permission to create a new resource.
     *
     * <p>Allows the client to create new payment resources such as incoming payments,
     * outgoing payments, or quotes. This is typically the first step in payment
     * operations.
     */
    create("create"),

    /**
     * Permission to read all resources of a specific type.
     *
     * <p>Allows the client to access all resources of a given type within the
     * wallet address scope, not just specific resources. This is broader than
     * the {@link #read} action.
     */
    read_all("read-all"),

    /**
     * Permission to list resources.
     *
     * <p>Allows the client to retrieve a list of resources of a specific type.
     * This is typically used for getting paginated lists of payments or quotes.
     */
    list("list"),

    /**
     * Permission to list all resources of a specific type.
     *
     * <p>Allows the client to retrieve comprehensive lists of all resources of a
     * specific type within the wallet address scope. This is broader than the
     * {@link #list} action.
     */
    list_all("list-all");

    /**
     * The string value used in JSON serialization.
     */
    public final String value;

    /**
     * Constructs an AccessAction with the specified string value.
     *
     * @param name the string representation of the action
     */
    AccessAction(String name) {
        ensureNotNullOrEmpty(name, "name cannot be null or empty.");
        this.value = name;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Gets the JSON string value for this access action.
     *
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Creates an AccessAction from its string value.
     *
     * @param value the string value to convert
     * @return the corresponding AccessAction enum value
     * @throws IllegalArgumentException if the value doesn't match any known action
     */
    @JsonCreator
    public static AccessAction fromValue(String value) {
        for (AccessAction item : AccessAction.values()) {
            if (item.value.equals(value)) return item;
        }
        throw new IllegalArgumentException("Unknown AccessAction: '" + value + "'.");
    }
}
