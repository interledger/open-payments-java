package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;

/**
 * Enumeration of resource types available for Interledger Open Payments access control.
 * 
 * <p>This enum defines the different types of payment resources that can be accessed
 * through the Interledger Open Payments protocol. Each resource type represents a
 * different aspect of the payment flow and can be combined with {@link AccessAction}
 * to create specific access permissions.
 * 
 * <p>Resource types are used in access grants to specify which types of operations
 * a client is authorized to perform within a wallet address scope.
 */
public enum AccessItemType {
    /**
     * Incoming payment resource type.
     * 
     * <p>Represents access to incoming payment resources, which are used to
     * receive payments from other parties. Incoming payments define the
     * destination for value transfer and include payment pointers, amounts,
     * and payment methods.
     * 
     * <p>Common operations include creating incoming payments to receive funds
     * and reading incoming payment details to track received amounts.
     */
    incomingPayment("incoming-payment"),
    
    /**
     * Outgoing payment resource type.
     * 
     * <p>Represents access to outgoing payment resources, which are used to
     * send payments to other parties. Outgoing payments define the source
     * and destination for value transfer and require user authorization.
     * 
     * <p>Common operations include creating outgoing payments to send funds,
     * completing authorized payments, and reading payment status.
     */
    outgoingPayment("outgoing-payment"),
    
    /**
     * Quote resource type.
     * 
     * <p>Represents access to quote resources, which provide cost estimates
     * for payment operations. Quotes calculate fees, exchange rates, and
     * final amounts before executing actual payments.
     * 
     * <p>Common operations include creating quotes to estimate payment costs
     * and reading quote details to understand payment economics.
     */
    quote("quote");

    /**
     * The string value used in JSON serialization.
     */
    public final String value;

    /**
     * Constructs an AccessItemType with the specified string value.
     * 
     * @param name the string representation of the resource type
     */
    AccessItemType(String name) {
        ensureNotNull(name, "name cannot be null.");
        this.value = name;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Gets the JSON string value for this access item type.
     * 
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Creates an AccessItemType from its string value.
     * 
     * @param value the string value to convert
     * @return the corresponding AccessItemType enum value
     * @throws IllegalArgumentException if the value doesn't match any known resource type
     */
    @JsonCreator
    public static AccessItemType fromValue(String value) {
        for (AccessItemType item : AccessItemType.values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown AccessItemType: '" + value + "'.");
    }
}
