package org.interledger.openpayments.model;

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
    @JsonProperty("description")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String description;

    @JsonProperty(value = "code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String code;
}