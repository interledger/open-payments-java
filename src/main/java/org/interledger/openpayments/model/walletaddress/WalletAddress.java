package org.interledger.openpayments.model.walletaddress;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

/**
 * A wallet address is a secure, sharable identifier for an Open Payments-enabled account.
 * Each wallet address acts a service endpoint into the APIs, allowing clients to interact with the underlying account.
 */
@Data
public class WalletAddress {
    /**
     * The unique identifier for the wallet address.
     *
     * This field represents the primary key or unique ID associated with the wallet address.
     * It is used to reference and distinguish this wallet address from others. The value
     * is typically assigned by the system and may be used as part of API calls or other
     * interactions to uniquely identify the wallet address.
     *
     * Annotations:
     * - {@code @JsonProperty("id")}: Maps this field to the "id" property in the JSON object.
     */
    @JsonProperty("id")
    private String id;

    /**
     * The public name associated with the wallet address.
     *
     * This field represents a human-readable identifier or alias for the wallet address.
     * It is intended for display purposes and may be used to provide additional context
     * or identification of the wallet address in user-facing interfaces.
     *
     * Annotations:
     * - {@code @JsonProperty("publicName")}: Maps this field to the "publicName" property in the JSON object.
     */
    @JsonProperty("publicName")
    private String publicName;

    /**
     * The currency asset code associated with the wallet address.
     *
     * This field represents the standardized code that identifies the currency
     * of the assets associated with the wallet address. It can be used to specify
     * the type of currency, such as `USD` for US Dollars or `EUR` for Euros,
     * and is typically defined according to international standards like ISO
     * 4217 or similar.
     *
     * Annotations:
     * - {@code @JsonProperty("assetCode")}: Maps this field to the "assetCode" property in the JSON object.
     */
    @JsonProperty("assetCode")
    private String assetCode;

    /**
     * The asset scale associated with the wallet address.
     *
     * This field represents the number of decimal places used to express the
     * value of the associated asset in its smallest divisible unit. The scale
     * defines the precision or granularity at which asset values can be
     * represented.
     *
     * For example, an asset scale of 2 indicates that values are expressed
     * to two decimal places (e.g., 100 would be represented as 1.00), while
     * an asset scale of 0 indicates values are represented as whole numbers
     * without fractional values.
     *
     * Annotations:
     * - {@code @JsonProperty("assetScale")}: Maps this field to the "assetScale" property in the JSON object.
     */
    @JsonProperty("assetScale")
    private Integer assetScale;

    /**
     * The authorization server associated with the wallet address.
     *
     * This field specifies the URL or endpoint of the authorization server
     * that manages access and permissions for interacting with the wallet address.
     * It is typically used in authentication and authorization flows to validate
     * client requests and ensure secure communication between parties.
     *
     * Annotations:
     * - {@code @JsonProperty("authServer")}: Maps this field to the "authServer" property in the JSON object.
     */
    @JsonProperty("authServer")
    private String authServer;

    /**
     * Represents the identification of a resource server associated with the wallet address.
     *
     * This field holds the URI or identifier of the resource server where accessed
     * resources are hosted. It is used in conjunction with authorization mechanisms
     * to define the target server for accessing protected resources.
     *
     * Annotations:
     * - {@code @JsonProperty("resourceServer")}: Maps this field to the "resourceServer" property in the JSON object.
     */
    @JsonProperty("resourceServer")
    private String resourceServer;

    /**
     * Represents the card service associated with the WalletAddress.
     * This field is typically used to specify or identify the service
     * that manages or interacts with payment cards linked to the wallet.
     */
    @JsonProperty("cardService")
    private String cardService;

    /**
     * Converts the `id` field of the WalletAddress into a URI representation.
     * @return a URI created from the value of the `id` field
     */
    @JsonIgnore
    public URI getIdAsURI() {
        return URI.create(this.id);
    }

    /**
     * Converts the `resourceServer` field of the WalletAddress into a URI representation.
     *
     * @return a URI created from the value of the `resourceServer` field
     */
    @JsonIgnore
    public URI getResourceServerAsURI() {
        return URI.create(this.resourceServer);
    }

    /**
     * Converts the `cardService` field of the WalletAddress into a URI representation.
     * @return a URI created from the value of the `cardService` field
     */
    @JsonIgnore
    public URI getCardServiceAsURI() {
        return URI.create(this.cardService);
    }
}
