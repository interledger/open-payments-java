package org.interledger.openpayments.model;

/**
 * Represents a resource server responsible for providing authentication services.
 */
public interface IResourceServer {
    /**
     * Retrieves the resource server associated with this instance.
     *
     * @return the resource server as a non-null, non-empty string
     */
    String getResourceServer();
}
