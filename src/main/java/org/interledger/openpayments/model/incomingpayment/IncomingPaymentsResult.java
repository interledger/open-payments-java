package org.interledger.openpayments.model.incomingpayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Pagination;

import java.util.List;

/**
 * Represents the result of fetching incoming payments with pagination details.
 *
 * This class encapsulates a paginated list of incoming payment objects,
 * allowing callers to access the individual results and navigate through
 * paginated data using the provided pagination information.
 *
 * Key components:
 * - `pagination`: Contains details about the current page, including cursors
 *   and indicators for the presence of additional pages.
 * - `result`: A list of incoming payment objects representing the fetched data
 *   for the current page.
 *
 * This class is commonly used in APIs or services where incoming payments data
 * is retrieved in a paginated format for efficient data access and processing.
 */
@Data
public class IncomingPaymentsResult {
    /**
     * Contains pagination information for the current result set.
     *
     * This property provides details about the state of pagination, including cursors
     * to track the current position within a dataset and boolean flags to indicate the
     * presence of additional pages. It is used to facilitate navigation through large
     * datasets in a paginated format.
     *
     * Commonly utilized in APIs or data retrieval systems, this field helps ensure
     * efficient handling of dataset slices and seamless traversal across pages.
     */
    @JsonProperty(value = "pagination")
    private Pagination pagination;

    /**
     * A list of incoming payments representing the result set for the current page.
     *
     * This property holds the actual data representing incoming payments fetched
     * as a part of a paginated response. Each entry in the list corresponds to an
     * instance of an `IncomingPayment`, which contains detailed information about
     * a specific incoming payment, such as its status, received amount, and metadata.
     *
     * It is typically used in conjunction with pagination details to enable efficient
     * retrieval and processing of larger datasets, allowing consumers to navigate
     * through paginated incoming payment records.
     */
    @JsonProperty(value = "result")
    private List<IncomingPayment> result;
}
