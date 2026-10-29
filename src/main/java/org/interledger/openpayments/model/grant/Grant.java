package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a complete access grant response from the authorization server.
 * 
 * <p>This class contains the result of a successful access grant request in
 * the Grant Negotiation and Authorization Protocol (GNAP). It includes the
 * access token that can be used to access protected resources, and optionally
 * continuation information for future interactions.
 * 
 * <p>An access grant is the successful outcome of an authorization flow where
 * the client has been granted permission to access specific resources on
 * behalf of a resource owner. The grant includes both immediate access
 * credentials and information for maintaining or extending that access.
 */
@Data
public class Grant {

    /**
     * The access token granted by the authorization server.
     * 
     * <p>This token contains the credentials that the client can use to
     * access protected resources. It includes the token value, expiration
     * information, and the scope of access that has been granted.
     * 
     * <p>The access token is typically used in the Authorization header
     * of HTTP requests to authenticate and authorize API calls.
     * 
     * @see AccessToken
     */
    @JsonProperty("access_token")
    private AccessToken access;

    /**
     * Optional continuation information for future interactions.
     * 
     * <p>When present, this field provides information that the client
     * can use to continue the access request in the future. This might
     * be used for token refresh, access extension, or other follow-up
     * operations as defined by the authorization server.
     * 
     * <p>If not present, the client should treat this as a final grant
     * with no continuation capabilities.
     * 
     * @see AccessContinue
     */
    @JsonProperty("continue")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AccessContinue accessContinue;

    /**
     * Optional subject information, returned by a grant continuation when the grant
     * request contained a {@code subject} and the user has proved access to it.
     *
     * <p>When present, the authorization server has verified that the user has access
     * to the wallet address(es) listed in {@link Subject#getSubIds()}. A subject-only
     * grant does not include an access token.
     *
     * @see Subject
     */
    @JsonProperty("subject")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Subject subject;
}
