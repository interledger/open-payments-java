package org.interledger.openpayments.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a pagination object for handling paginated data responses.
 *
 * This class provides the necessary information regarding the current
 * state of a paginated query, including cursor positions and pagination state.
 *
 * Key properties:
 * - startCursor: Marks the starting point of the current page in the dataset.
 * - endCursor: Marks the endpoint of the current page in the dataset.
 * - hasNextPage: Indicates whether there are subsequent pages in the dataset.
 * - hasPreviousPage: Indicates whether there are preceding pages in the dataset.
 *
 * Instances of this class are typically used in systems where data is structured
 * into discrete pages, allowing efficient navigation and retrieval of data chunks.
 */
@Data
public class Pagination {
    /**
     * Represents the starting cursor of the current page in a paginated dataset.
     *
     * This cursor acts as a reference point, denoting the position at which
     * the current page begins. It is commonly used in data pagination systems
     * to fetch or navigate through specific segments of a dataset.
     *
     * The value is typically updated as pages are navigated to enable
     * seamless tracking and retrieval of data.
     */
    @JsonProperty("startCursor")
    public String startCursor;

    /**
     * Represents the cursor indicating the ending position of the current page
     * in a paginated dataset.
     *
     * The value of this property is typically used to identify the last item
     * in the current page and serves as a reference for fetching subsequent
     * pages in a pagination process. It is essential for determining the state
     * and continuity of paginated data retrieval.
     */
    @JsonProperty("endCursor")
    public String endCursor;

    /**
     * Indicates whether there are additional pages of data available after the current page.
     *
     * This property is used in pagination systems to determine if the current page is the
     * last one or if more pages can be fetched. A value of {@code true} signifies that
     * further pages exist, while {@code false} indicates the current page is the final one.
     *
     * It is commonly used in conjunction with cursor-based pagination to enable seamless
     * navigation through datasets that are too large to be retrieved in a single response.
     */
    @JsonProperty(value = "hasNextPage", required = true)
    private boolean hasNextPage;

    /**
     * Indicates whether there are previous pages of data available before the current page
     * in a paginated dataset.
     *
     * This property is utilized in pagination systems to determine if the current page
     * has preceding pages. A value of {@code true} signifies that earlier pages exist,
     * while {@code false} indicates that the current page is the first one in the dataset.
     *
     * It is commonly used in cursor-based pagination to enable backward navigation
     * through the dataset.
     */
    @JsonProperty(value = "hasPreviousPage", required = true)
    private boolean hasPreviousPage;
}
