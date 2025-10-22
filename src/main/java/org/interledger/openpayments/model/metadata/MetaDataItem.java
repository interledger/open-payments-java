package org.interledger.openpayments.model.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a single key-value pair in payment metadata.
 * 
 * <p>This class provides a flexible structure for storing arbitrary metadata
 * associated with Interledger payments. Each item consists of a required key
 * and an optional value, allowing for both simple flags and descriptive
 * key-value pairs.
 * 
 * <p>Metadata items are commonly used for:
 * <ul>
 *   <li>Payment descriptions (e.g., key="description", value="Coffee purchase")</li>
 *   <li>Reference numbers (e.g., key="reference", value="REF-123456")</li>
 *   <li>Category tags (e.g., key="category", value="food")</li>
 *   <li>Boolean flags (e.g., key="urgent", value=null for simple presence)</li>
 * </ul>
 */
@Data
public class MetaDataItem {
    /**
     * The metadata key identifier.
     * 
     * <p>This is a required field that identifies the type or category of metadata.
     * Keys should be descriptive and follow a consistent naming convention within
     * an application or organization.
     * 
     * <p>Examples of common keys:
     * <ul>
     *   <li>"description" - Human-readable payment description</li>
     *   <li>"reference" - Business reference number</li>
     *   <li>"category" - Payment category or type</li>
     *   <li>"customer_id" - Customer identifier</li>
     *   <li>"merchant_id" - Merchant identifier</li>
     * </ul>
     */
    @JsonProperty(value = "key", required = true)
    private String key;

    /**
     * The metadata value.
     * 
     * <p>This is an optional field that contains the actual metadata value
     * associated with the key. When null, the metadata item represents a
     * simple flag or tag where the presence of the key is sufficient.
     * 
     * <p>Values are stored as strings but can represent various data types
     * depending on the application's interpretation of the metadata.
     */
    @JsonProperty(value = "value")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String value;
}
