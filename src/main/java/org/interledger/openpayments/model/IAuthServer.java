package org.interledger.openpayments.model;

/**
 * Represents an authentication server responsible for providing authentication services.
 */
public interface IAuthServer {
    /**
     * Retrieves the authentication server associated with this instance.
     *
     * @return the authentication server as a non-null, non-empty string
     */
    String getAuthServer();
}
