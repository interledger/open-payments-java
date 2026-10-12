package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.interledger.openpayments.model.common.Amount;

import java.net.URI;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;

/**
 * Represents access limits for Interledger payment operations.
 * 
 * <p>This class defines the constraints and limits that can be applied to
 * access grants for payment operations. Limits help control the scope and
 * scale of operations that can be performed using an access token, providing
 * security and risk management for payment flows.
 * 
 * <p>All fields are optional and will be excluded from JSON serialization
 * when null, allowing for flexible limit configurations based on specific
 * use cases and security requirements.
 */
@Data
public class Limits {
    /**
     * The specific receiver (incoming payment) URI that this limit applies to.
     * 
     * <p>When specified, this limit restricts operations to only work with
     * the specified incoming payment endpoint. This provides fine-grained
     * control over which payment destinations can be accessed.
     * 
     * <p>Example: "https://ilp.interledger-test.dev/incoming-payments/123e4567-e89b-12d3-a456-426614174000"
     * 
     * @see <a href="https://openpayments.guide/">Open Payments specification</a>
     */
    @JsonProperty("receiver")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private URI receiver;

    /**
     * The maximum amount that can be received through this access grant.
     * 
     * <p>This field sets an upper limit on the total amount that can be
     * received in the specified asset. It helps prevent unauthorized or
     * excessive payment amounts and provides spending control.
     * 
     * <p>When set, any payment operation that would exceed this limit
     * will be rejected by the authorization server.
     */
    @JsonProperty("receiveAmount")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Amount receiveAmount;

    /**
     * The maximum amount that can be debited (sent) through this access grant.
     * 
     * <p>This field sets an upper limit on the total amount that can be
     * debited from the account in the specified asset. It provides spending
     * control and helps prevent unauthorized large transactions.
     * 
     * <p>When set, any payment operation that would exceed this limit
     * will be rejected by the authorization server.
     */
    @JsonProperty("debitAmount")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Amount debitAmount;

    /**
     * ISO8601 repeating interval specification for recurring payment limits.
     * 
     * <p>This field defines the time interval for recurring payment operations
     * (e.g. a grant for future payments). It follows the ISO8601 repeating
     * interval format. Amount limits apply per interval, and the spent amounts
     * returned for the grant reflect the current interval only.
     *
     * <p>Example formats:
     * <ul>
     *   <li>R12/2023-01-01T00:00:00Z/P1M - 12 monthly intervals starting January 1, 2023</li>
     *   <li>R/2017-03-01T13:00:00Z/2018-05-11T15:30:00Z - unbounded repetition of a fixed interval</li>
     *   <li>R-1/P1Y2M10DT2H30M/2022-05-11T15:30:00Z - unbounded repetition ending at a date</li>
     * </ul>
     *
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601#Repeating_intervals">ISO8601 Repeating Intervals</a>
     */
    @JsonProperty("interval")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String interval;

    /**
     * Creates limits with a maximum debit amount.
     *
     * @param debitAmount the maximum amount that can be debited
     * @return new limits
     */
    public static Limits debit(Amount debitAmount) {
        return debit(debitAmount, null);
    }

    /**
     * Creates limits with a maximum debit amount per interval.
     *
     * @param debitAmount the maximum amount that can be debited per interval
     * @param interval    the ISO8601 repeating interval, or null for a one-off limit
     * @return new limits
     */
    public static Limits debit(Amount debitAmount, String interval) {
        ensureNotNull(debitAmount, "debitAmount cannot be null.");
        var limits = new Limits();
        limits.debitAmount = debitAmount;
        limits.interval = interval;
        return limits;
    }

    /**
     * Creates limits with a maximum receive amount.
     *
     * @param receiveAmount the maximum amount that can be received
     * @return new limits
     */
    public static Limits receive(Amount receiveAmount) {
        return receive(receiveAmount, null);
    }

    /**
     * Creates limits with a maximum receive amount per interval.
     *
     * @param receiveAmount the maximum amount that can be received per interval
     * @param interval      the ISO8601 repeating interval, or null for a one-off limit
     * @return new limits
     */
    public static Limits receive(Amount receiveAmount, String interval) {
        ensureNotNull(receiveAmount, "receiveAmount cannot be null.");
        var limits = new Limits();
        limits.receiveAmount = receiveAmount;
        limits.interval = interval;
        return limits;
    }
}
