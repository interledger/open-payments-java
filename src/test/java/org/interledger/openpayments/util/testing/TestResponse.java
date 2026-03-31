package org.interledger.openpayments.util.testing;

import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable test implementation of {@link HttpResponse}<{@link String}> for use in unit tests.
 *
 * <p>Provides factory methods to easily create mock HTTP responses with custom status codes,
 * JSON/String bodies, and headers. Bodies from objects are serialized using {@link
 * OpenPaymentsObjectMapper}.
 *
 * <p>Common usage:
 *
 * <pre>{@code
 * TestResponse.createResponse(myObject);           // 200 + JSON
 * TestResponse.createResponse("error", 404);       // custom code
 * TestResponse.createResponse(obj, 201, headers);  // full control
 * }</pre>
 *
 * @param statusCode HTTP status code
 * @param body       response body as String
 * @param headers    response headers
 */
public record TestResponse(int statusCode, String body, HttpHeaders headers)
    implements HttpResponse<String> {

    public TestResponse(int statusCode, String body) {
        this(statusCode, body, HttpHeaders.of(Map.of(), (a, b) -> true));
    }

    private static final java.lang.String TEST_URL = "https://api.example.com/";

    public HttpRequest request() {
        return null;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public Optional<HttpResponse<String>> previousResponse() {
        return Optional.empty();
    }

    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    public URI uri() {
        return URI.create(TEST_URL);
    }

    public HttpClient.Version version() {
        return HttpClient.Version.HTTP_2;
    }

    public static HttpResponse<String> createResponse(Object object) {
        OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
        String body = mapper.writeValueAsString(object);
        return createResponse(body, 200);
    }

    public static HttpResponse<String> createResponse(String body) {
        return new TestResponse(200, body);
    }

    public static HttpResponse<String> createResponse(Object object, int code) {
        OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
        String body = mapper.writeValueAsString(object);
        return createResponse(body, code);
    }

    public static HttpResponse<String> createResponse(Object object, int code, HttpHeaders headers) {
        OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
        String body = mapper.writeValueAsString(object);
        return createResponse(body, code, headers);
    }

    public static HttpResponse<String> createResponse(String body, int code, HttpHeaders headers) {
        return new TestResponse(code, body, headers);
    }

    public static HttpResponse<String> createResponse(String body, int code) {
        return new TestResponse(code, body);
    }
}
