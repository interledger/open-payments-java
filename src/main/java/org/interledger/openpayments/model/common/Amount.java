package org.interledger.openpayments.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import static org.interledger.openpayments.util.UtilGlobal.*;

/**
 * Represents a monetary amount in the Interledger Protocol format.
 *
 * <p>This class encapsulates the three components required for representing monetary
 * amounts in the Interledger network: the asset code (currency), the asset scale
 * (precision), and the amount value (as an unsigned 64-bit integer string).
 *
 * <p>The Interledger Protocol uses a specific format for amounts to ensure precision
 * and consistency across different systems and currencies. Amounts are represented
 * as scaled integers to avoid floating-point precision issues.
 *
 * <p>Example: $12.34 USD would be represented as:
 * <ul>
 *   <li>assetCode: "USD"</li>
 *   <li>assetScale: 2</li>
 *   <li>amount: "1234" (12.34 * 10^2)</li>
 * </ul>
 */
@ToString
@Data
public class Amount {
    /**
     * The default scale for monetary amounts (2 decimal places).
     *
     * <p>This is the most common scale used for traditional currencies,
     * representing cents, pence, or similar subunits.
     */
    public static final int DEFAULT_AMOUNT_SCALE = 2;

    /**
     * The asset code indicating the underlying asset.
     *
     * <p>This should be an ISO4217 currency code such as "USD", "EUR", "GBP", etc.
     * The asset code identifies the type of value being represented and must be
     * exactly 3 characters long.
     *
     * @see <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO4217 Currency Codes</a>
     */
    @JsonProperty(value = "assetCode", required = true)
    private String assetCode;

    /**
     * The scale of amounts denoted in the corresponding asset code.
     *
     * <p>This represents the number of decimal places in the amount. For example,
     * a scale of 2 means the amount is expressed in hundredths (cents for USD).
     * The scale must be between 0 and 255.
     *
     * <p>Common scales:
     * <ul>
     *   <li>0 - Whole units (e.g., Japanese Yen)</li>
     *   <li>2 - Cents/pence (e.g., USD, EUR, GBP)</li>
     *   <li>3 - Mills (e.g., some cryptocurrencies)</li>
     * </ul>
     */
    @JsonProperty(value = "assetScale", required = true)
    private int assetScale;

    /**
     * The value as an unsigned 64-bit integer amount, represented as a string.
     *
     * <p>This is the actual amount value scaled by the asset scale. For example,
     * if the human-readable amount is $12.34 and the scale is 2, this value
     * would be "1234" (12.34 * 10^2).
     *
     * <p>The value is stored as a string to avoid precision issues and to support
     * the full range of 64-bit unsigned integers.
     */
    @JsonProperty(value = "value", required = true)
    private String value;

    /**
     * Converts this InterledgerAmount to a BigDecimal representation.
     *
     * <p>This method converts the scaled integer amount back to a decimal
     * representation by moving the decimal point left by the asset scale.
     *
     * @return a BigDecimal representing the human-readable amount
     */
    public BigDecimal asBigDecimal() {
        return new BigDecimal(value).movePointLeft(assetScale);
    }

    /**
     * Builds an InterledgerAmount object by converting the provided BigDecimal amount into an Interledger
     * formatted amount (string representation) with a default scale of 2 and asset code.
     *
     * @param amount    the monetary value, must not be null
     * @param assetCode the asset code, must be a non-empty string with 3 characters (ISO4217 currency code)
     * @return InterledgerAmount instance representing the given parameters
     */
    public static Amount build(BigDecimal amount, String assetCode) {
        return build(amount, assetCode, DEFAULT_AMOUNT_SCALE);
    }

    /**
     * Builds an InterledgerAmount object by converting the provided BigDecimal amount into an Interledger
     * formatted amount (string representation) with the specified scale and asset code.
     *
     * @param amount    the monetary value, must not be null
     * @param assetCode the asset code, must be a non-empty string with 3 characters (ISO4217 currency code)
     * @param scale     the scale/precision for the amount, must be between 0-255
     * @return InterledgerAmount instance representing the given parameters
     */
    public static Amount build(BigDecimal amount, String assetCode, int scale) {
        ensureNotNull(amount, "amount cannot be null.");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be greater than zero.");
        }
        ensureNotNullOrEmpty(assetCode, "assetCode cannot be null or empty.");
        if (assetCode.length() != 3) {
            throw new IllegalArgumentException(String.format("assetCode must be 3 characters long / ISO4217 currency code, but was: '%s'.", assetCode));
        }
        var out = new Amount();
        out.assetCode = assetCode;
        out.value = toInterledgerAmount(amount, scale);
        out.assetScale = scale;
        return out;
    }

    /**
     * Converts a BigDecimal amount to an Interledger amount string representation.
     *
     * <p>This method scales the decimal amount by the specified scale and converts
     * it to a string representation suitable for Interledger protocol usage.
     * The amount is first rounded to 2 decimal places, then scaled by the power
     * of 10 corresponding to the amount scale.
     *
     * @param amount      the BigDecimal amount to convert
     * @param amountScale the scale to apply (number of decimal places)
     * @return the string representation of the scaled amount
     */
    public static String toInterledgerAmount(BigDecimal amount, int amountScale) {
        ensureNotNull(amount, "amount cannot be null.");
        ensureTrue(amountScale >= 0, "amountScale must be greater than or equal to zero.");

        BigDecimal out = amount.setScale(2, RoundingMode.HALF_UP).scaleByPowerOfTen(amountScale);

        DecimalFormat decimalFormat = new DecimalFormat("#");
        return decimalFormat.format(out);
    }

    /**
     * Converts a BigDecimal amount to an Interledger amount string using the default scale.
     *
     * <p>This is a convenience method that uses the {@link #DEFAULT_AMOUNT_SCALE}
     * to convert the amount.
     *
     * @param amount the BigDecimal amount to convert
     * @return the string representation of the scaled amount
     */
    public static String toInterledgerAmount(BigDecimal amount) {
        return toInterledgerAmount(amount, DEFAULT_AMOUNT_SCALE);
    }
}
