package org.interledger.openpayments.model.outgoingpayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Pagination;

import java.util.List;

/**
 * Represents the result of an outgoing payments query, including the associated
 * pagination details and a list of outgoing payments.
 *
 * This class provides a structured representation of the data returned for outgoing
 * payments, enabling easy access to both the individual payment records as well as
 * the metadata required to handle pagination.
 *
 * Key components:
 * - `pagination`: Provides information about the current page within a paginated dataset.
 * - `result`: Contains the list of outgoing payment details for the current page.
 *
 * Instances of this class are commonly used in scenarios where the system processes
 * or retrieves large datasets of outgoing payments, and only a subset of the data
 * can be provided per request due to pagination constraints.
 */
@Data
public class OutgoingPaymentsResult {
    /**
     * Contains pagination information for handling paginated datasets.
     *
     * This variable holds an instance of the {@link Pagination} class, which provides
     * details about the current pagination state when working with datasets that
     * are divided into pages. It enables efficient navigation and data retrieval
     * by keeping track of cursor positions and determining the availability of
     * additional pages.
     *
     * Pagination details include:
     * - The starting and ending cursors of the current data page.
     * - Indicators for the presence of previous and next pages.
     *
     * This field is particularly useful in scenarios where large datasets need to be
     * fetched, displayed, or processed incrementally via a paginated approach.
     */
    @JsonProperty(value = "pagination")
    private Pagination pagination;

    @JsonProperty(value = "result")
    private List<OutgoingPayment> result;
}
