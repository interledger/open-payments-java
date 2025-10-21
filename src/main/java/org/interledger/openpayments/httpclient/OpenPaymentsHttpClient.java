package org.interledger.openpayments.httpclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.interledger.openpayments.IOpenPaymentsClient;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.model.ApiError;
import org.interledger.openpayments.model.WalletAddress;
import org.interledger.openpayments.util.UtilGlobal;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.INTERRUPTED;
import static org.interledger.openpayments.exception.AOpenPaymentsException.Code.IO_ERROR;
import static org.interledger.openpayments.exception.OpenPaymentsAPIException.getApiException;
import static org.interledger.openpayments.signing.SignatureRequestBuilder.ACCEPT_HEADER;
import static org.interledger.openpayments.signing.SignatureRequestBuilder.APPLICATION_JSON;

/**
 * The OpenPaymentsHttpClient class provides an implementation of the IOpenPaymentsClient
 * interface, enabling interaction with Open Payments APIs for retrieving wallet addresses
 * and sending HTTP requests.
 * It offers functionality to manage HTTP requests and responses, process timeouts,
 * log requests and responses, and handle errors from API interactions.
 * The client is configured upon initialization using a set of options and requires
 * a private key, client wallet address, and key ID for signing and authentication.
 */
@RequiredArgsConstructor
@Slf4j
public class OpenPaymentsHttpClient implements IOpenPaymentsClient {
    private final String clientWalletAddress;
    private final PrivateKey privateKey;
    private final String keyId;
    private final OpenPaymentsClientOptions options;

    private final HttpClient client;
    private final OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
    private final HttpLogger httpLogger;

    /**
     * Retrieves a wallet address by sending a GET request to the specified URL.
     *
     * @param url the URL endpoint from which to fetch the wallet address
     * @return a {@code WalletAddress} object representing the fetched wallet address
     * @throws OpenPaymentsClientException if the URL is invalid, the HTTP request fails, or deserialization fails
     */
    @Override
    public WalletAddress getWalletAddress(String url) {
        var urlObj = UtilGlobal.toUri(url);

        var request = HttpRequest.newBuilder(urlObj)
                .GET()
                .header(ACCEPT_HEADER.toLowerCase(), APPLICATION_JSON)
                .timeout(Duration.of(options.timeOutInSeconds, SECONDS))
                .build();

        return this.send(request, WalletAddress.class);
    }

    /**
     * Sends an HTTP request and processes the response, including deserialization into the specified type.
     * The method logs the request and response, handles HTTP errors, and attempts to deserialize
     * the response body into the provided response type. It throws exceptions if an error occurs
     * during the process, such as network issues or deserialization failures.
     *
     * @param request      the HTTP request to be sent
     * @param responseType the class type into which the response body should be deserialized
     * @param <T>          the generic type representing the type of the response object
     * @return an instance of the specified response type {@code T} populated with data from the HTTP response body
     * @throws OpenPaymentsClientException if an I/O error occurs, the thread is interrupted, or deserialization fails
     */
    public <T> T send(HttpRequest request, Class<T> responseType) {
        try {
            this.httpLogger.logRequest(request);
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            this.httpLogger.logResponse(response);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                handleHttpError(response);
            }

            // deserialize
            return this.mapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new OpenPaymentsClientException(IO_ERROR, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenPaymentsClientException(INTERRUPTED, e.getMessage(), e);
        }
    }

    /**
     * Handles HTTP error responses by logging the error details and throwing an exception.
     * Differentiates between client errors (4xx), server errors (5xx), and other non-2xx status codes,
     * creating a server-side exception with appropriate information.
     * @param response the HTTP response containing the error status code and body
     * @throws IOException if there is an issue processing the HTTP response
     */
    private void handleHttpError(HttpResponse<String> response) throws IOException {
        int statusCode = response.statusCode();
        ApiError error = mapper.readError(response.body(), statusCode);

        if (statusCode >= 400 && statusCode < 500) {
            // 4xx - Client errors (bad request, unauthorized, forbidden, not found, etc.)
            log.warn("Client error [{}]: {}", statusCode, error.description);
        } else if (statusCode >= 500 && statusCode < 600) {
            // 5xx - Server errors (internal server error, bad gateway, service unavailable, etc.)
            log.error("Server error [{}]: {}", statusCode, error.description);
        } else {
            // Other non-2xx status codes (1xx, 3xx, or unexpected codes)
            log.warn("Unexpected HTTP status [{}]: {}", statusCode, error.description);
        }
        // Server side exception to be handled.
        throw getApiException(error, response);
    }
}
