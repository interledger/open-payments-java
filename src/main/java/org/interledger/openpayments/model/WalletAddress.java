package org.interledger.openpayments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * A wallet address is a secure, sharable identifier for an Open Payments-enabled account.
 * Each wallet address acts a service endpoint into the APIs, allowing clients to interact with the underlying account.
 */
@Data
public class WalletAddress {
    @JsonProperty("id")
    private String id;

    @JsonProperty("publicName")
    private String publicName;

    @JsonProperty("assetCode")
    private String assetCode;

    @JsonProperty("assetScale")
    private Integer assetScale;

    @JsonProperty("authServer")
    private String authServer;

    @JsonProperty("resourceServer")
    private String resourceServer;
}
