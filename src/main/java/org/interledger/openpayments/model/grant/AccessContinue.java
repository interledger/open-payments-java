package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

/**
 * Represents the response from a GNAP access continuation request.
 * 
 * <p>This class contains the information returned by the authorization server
 * when a client continues an access request that was previously initiated.
 * It includes the access token, the continuation URI, and optionally a wait
 * time for polling-based interactions.
 * 
 * <p>Access continuation is used in the Grant Negotiation and Authorization
 * Protocol (GNAP) to allow clients to continue a previously started access
 * request without having to restart the entire authorization flow.
 */
@Data
public class AccessContinue {
    /**
     * The access token granted by the authorization server.
     * 
     * <p>This token contains the actual access credentials that the client
     * can use to access protected resources. It includes the token value,
     * expiration information, and the scope of access granted.
     * 
     * @see AccessToken
     */
    @JsonProperty(value = "access_token", required = true)
    private AccessToken access;

    /**
     * The URI for continuing the access request.
     *
     * <p>This URI is used by the client to continue the access request
     * in future interactions. It may be used for token refresh, access
     * continuation, or other follow-up operations as defined by the
     * authorization server.
     *
     * <p>Example: "<a href="https://ilp.interledger-test.dev/gnap/continue/12345">Continue GNAP</a>"
     */
    @JsonProperty(value = "uri", required = true)
    private URI uri;

    /**
     * Optional wait time in seconds for polling-based interactions.
     * 
     * <p>When present, this field indicates the recommended wait time
     * in seconds before the client should poll the continuation URI
     * again. This is used to prevent excessive polling and reduce
     * server load in polling-based authorization flows.
     * 
     * <p>If not present, the client should use a reasonable default
     * wait time or follow other interaction methods.
     */
    @JsonProperty(value = "wait")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer wait;
}
