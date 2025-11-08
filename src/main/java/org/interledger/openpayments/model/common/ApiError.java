package org.interledger.openpayments.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;

/**
 * Represents an API error response that can be serialized to or deserialized from JSON.
 * This class is used to encapsulate error information in API responses.
 * The error object contains a code identifying the type of the error and
 * an optional description providing additional context or details about the error.
 * Fields with null values will not be included in the serialized JSON representation.
 * Annotations:
 * - {@code @JsonRootName("error")}: Designates the root name of this object when serialized.
 * - {@code @Data}: Automatically generates getters, setters, and other utility methods.
 * - {@code @JsonProperty}: Maps the field to a JSON property.
 * - {@code @JsonInclude}: Specifies the inclusion rule for JSON serialization.
 */
@JsonRootName("error")
@Data
public class ApiError {
    /**
     * A descriptive message providing additional context or details about the error.
     *
     * <p>This field provides an optional, human-readable explanation of the error.
     * It is intended to give more context or clarification regarding the cause or
     * nature of the error that occurred. If not specified, this field will be omitted
     * from the JSON representation.
     *
     * Annotations:
     * - {@code @JsonProperty("description")}: Maps this field to the "description" property in the JSON object.
     * - {@code @JsonInclude(JsonInclude.Include.NON_NULL)}: Ensures that the field is only included
     *   in the JSON representation if its value is not null.
     */
    @JsonProperty("description")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String description;

    /**
     * A code identifying the type of error.
     *
     * This field contains a string value that represents an error code used
     * to classify the type of error that occurred. The error code is intended
     * for programmatic use and may be used by clients to handle specific error
     * conditions or map them to predefined actions. If the value is null, this
     * field will not be included in the JSON representation.
     *
     * Annotations:
     * - {@code @JsonProperty("code")}: Maps this field to the "code" property in the JSON object.
     * - {@code @JsonInclude(JsonInclude.Include.NON_NULL)}: Ensures that the field is only included
     *   in the JSON representation if its value is not null.
     */
    @JsonProperty(value = "code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String code;
}