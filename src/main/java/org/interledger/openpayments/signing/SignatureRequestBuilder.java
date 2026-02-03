package org.interledger.openpayments.signing;

import lombok.Getter;
import org.interledger.openpayments.httpclient.OpenPaymentsClientOptions;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.util.UtilGlobal;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;
import static org.interledger.openpayments.util.UtilGlobal.ensureTrue;

/**
 * Builder for creating cryptographically signed HTTP requests for Interledger API communication.
 *
 * <p>This class implements the HTTP Message Signatures specification (RFC 9421) to create
 * signed HTTP requests that ensure authenticity and integrity when communicating with
 * Interledger Open Payments servers. All requests are signed using Ed25519 cryptographic
 * signatures.
 */
@Getter
public class SignatureRequestBuilder {
    /**
     * Signature base parameters
     */
    static final String METHOD = "@method";
    static final String TARGET = "@target-uri";
    static final String SIGNATURE_PARAMS = "@signature-params";

    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String CONTENT_DIGEST_HEADER = "Content-Digest";
    static final String CONTENT_LENGTH_HEADER = "Content-Length";
    static final String SIGNATURE_INPUT_HEADER = "Signature-Input";
    static final String SIGNATURE_HEADER = "Signature";
    static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String ACCEPT_HEADER = "Accept";


    public static final String APPLICATION_JSON = "application/json";

    static final String DEFAULT_SIGNATURE_ID = "sig1";
    static final String DIGEST_ALGORITHM = "SHA-512";
    static final String SIGNATURE_ALGORITHM = "Ed25519";
    static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "HEAD");
    final PrivateKey privateKey;
    final String keyId;
    private long created = 0;
    final LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
    private final OpenPaymentsObjectMapper mapper;
    private String body;

    public SignatureRequestBuilder(
        PrivateKey privateKey,
        String keyId,
        OpenPaymentsObjectMapper mapper
    ) {
        UtilGlobal.ensureNotNull(privateKey, "PrivateKey cannot be null");
        UtilGlobal.ensureNotNullOrEmpty(keyId, "KeyId cannot be null or empty");

        this.privateKey = privateKey;
        this.keyId = keyId;
        this.mapper = mapper == null ? new OpenPaymentsObjectMapper() : mapper;
    }

    /**
     * Constructs a new SignatureRequestBuilder with the specified private key and key identifier.
     *
     * @param privateKey the private key used for signing
     * @param keyId      the identifier of the key
     */
    public SignatureRequestBuilder(PrivateKey privateKey, String keyId) {
        this(privateKey, keyId, null);
    }

    /**
     * Sets the HTTP method for the request.
     *
     * @param value HTTP method (GET, POST, PUT, DELETE, HEAD)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if method is null, blank, or not allowed
     */
    public SignatureRequestBuilder method(String value) {
        UtilGlobal.ensureNotNullOrEmpty(value, "Method cannot be null or empty!");
        ensureTrue(ALLOWED_METHODS.contains(value), String.format("Method '%s' is not allowed. Allowed methods are: %s", value, String.join(", ", ALLOWED_METHODS)));

        parameters.put(METHOD, value.toUpperCase());
        return this;
    }

    /**
     * Sets the request method to POST and serializes the body as JSON.
     *
     * @param body object to serialize as JSON request body
     * @return this builder for method chaining
     * @throws IllegalArgumentException if method is already set or body serialization fails
     */
    @SuppressWarnings("MethodName")
    public SignatureRequestBuilder POST(Object body) {
        checkMethod();
        return method("POST").json(body);
    }

    /**
     * Sets the HTTP method for the request to POST without the request body.
     *
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the method is already set
     */
    @SuppressWarnings("MethodName")
    public SignatureRequestBuilder POST() {
        checkMethod();
        return method("POST");
    }

    /**
     * Sets the HTTP method for the request to PUT and serializes the body as JSON.
     *
     * @param body the object to serialize as the JSON request body
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the method is already set or if body serialization fails
     */
    @SuppressWarnings("MethodName")
    public SignatureRequestBuilder PUT(Object body) {
        checkMethod();
        return method("PUT").json(body);
    }

    /**
     * Sets the HTTP method for the request to PUT without the request body.
     *
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if the method is already set
     */
    @SuppressWarnings("MethodName")
    public SignatureRequestBuilder PUT() {
        checkMethod();
        return method("PUT");
    }

    /**
     * Sets the request method to GET.
     *
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the method is already set
     */
    @SuppressWarnings("MethodName")
    public SignatureRequestBuilder GET() {
        checkMethod();
        return method("GET");
    }

    /**
     * Set the request method to DELETE.
     *
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the method is already set
     */
    @SuppressWarnings("MethodName")
    public SignatureRequestBuilder DELETE() {
        checkMethod();
        return method("DELETE");
    }

    /**
     * Gets the configured HTTP method.
     *
     * @return the HTTP method (GET, POST, PUT, DELETE, HEAD)
     * @throws IllegalArgumentException if the method has not been set
     */
    public String getMethod() {
        checkHasParameter(METHOD);
        return parameters.get(METHOD).toString();
    }

    /**
     * Sets the target URI for the signature request.
     *
     * @param value the target URI as a string
     * @return the updated SignatureRequestBuilder instance
     */
    public SignatureRequestBuilder target(String value) {
        return target(URI.create(value));
    }

    /**
     * Sets the target URI for the request.
     *
     * <p>The URI is normalized to ensure it ends with '/' when no query parameters
     * are present, as required by the signature specification.
     *
     * @param value target URI for the HTTP request
     * @return this builder for method chaining
     * @throws IllegalArgumentException if URI is null
     */
    public SignatureRequestBuilder target(URI value) {
        parameters.put(TARGET, prepareTarget(value));
        return this;
    }

    /**
     * Sets the request body by serializing an object to JSON.
     *
     * <p>This method automatically serializes the provided object to JSON using
     * the configured ObjectMapper and then calls {@link #json(String)} to set
     * all required headers.
     *
     * @param object object to serialize as JSON request body
     * @return this builder for method chaining
     * @throws IllegalArgumentException if object serialization fails
     */
    public SignatureRequestBuilder json(Object object) {
        String value = mapper.writeValueAsString(object);
        return json(value);
    }

    /**
     * Sets the request body as JSON and calculates required headers.
     *
     * <p>This method automatically:
     * <ul>
     *   <li>Sets Content-Type to application/json</li>
     *   <li>Calculates Content-Length header</li>
     *   <li>Generates SHA-512 Content-Digest header</li>
     * </ul>
     *
     * @param json JSON string to set as the request body
     * @return this builder for method chaining
     * @throws IllegalArgumentException if JSON is null or empty
     */
    public SignatureRequestBuilder json(String json) {

        UtilGlobal.ensureNotNullOrEmpty(json, "JSON cannot be null or empty!");

        body = json;
        digest(json);
        length(json);
        parameters.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        return this;
    }

    /**
     * Sets the access token for authorization.
     *
     * <p>The token is formatted as a GNAP (Grant Negotiation and Authorization Protocol)
     * bearer token in the Authorization header.
     *
     * @param token access token for API authorization
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the token is null or empty
     */
    public SignatureRequestBuilder accessToken(String token) {
        UtilGlobal.ensureNotNullOrEmpty(token, "Token cannot be null or empty!");

        parameters.put(AUTHORIZATION_HEADER, prepareToken(token));
        return this;
    }

    /**
     * Prepares a token by appending it to a predefined prefix.
     *
     * @param token the input string token to be formatted
     * @return the formatted token with the prefix "GNAP "
     */
    private String prepareToken(String token) {
        return String.format("GNAP %s", token);
    }

    /**
     * Calculates the length of the given content in bytes using UTF-8 encoding
     * and stores it in the parameters map with a header key.
     *
     * @param content the string content whose byte length is to be calculated
     */
    private void length(String content) {
        int contentLength = content.getBytes(StandardCharsets.UTF_8).length;
        parameters.put(CONTENT_LENGTH_HEADER, contentLength);
    }

    /**
     * Computes a SHA-512 digest for the provided value and updates the
     * parameters map with the generated digest header. If the digest
     * computation fails, an exception is thrown.
     *
     * @param value the input string to be hashed and used for generating
     *              the digest header
     * @throws IllegalArgumentException if an error occurs while computing
     *                                  the digest for the provided value
     */
    private void digest(String value) {
        try {
            var digest = digestContentSha512(value);
            var digestHeader = String.format("%s=:%s:", DIGEST_ALGORITHM.toLowerCase(), digest);
            parameters.put(CONTENT_DIGEST_HEADER, digestHeader);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Failed to calculate %s digest for: '%s'.", DIGEST_ALGORITHM, value), e);
        }
    }

    /**
     * Constructs and sets the signature parameters required for a secure communication.
     * The method ensures that the required 'created' timestamp is set before proceeding.
     * It gathers the existing parameters, formats them into quoted lower-case strings,
     * and organizes these values into a standardized format. The generated signature
     * parameters are then added to the parameters map under a predefined key.
     * <p>
     * Preconditions:
     * - The 'created' timestamp must be set to a value greater than 0.
     * - The 'parameters' map and 'keyId' must be initialized prior to calling this method.
     * <p>
     * Postconditions:
     * - The 'SIGNATURE_PARAMS' key in the parameters map will be updated with a new
     * signature parameter string.
     * <p>
     * Throws:
     * - IllegalStateException if the 'created' timestamp value is not greater than 0.
     * <p>
     * Side Effects:
     * - Modifies the 'parameters' map by adding or updating the 'SIGNATURE_PARAMS' entry.
     */
    void setSignatureParams() {
        ensureTrue(created > 0, "Created timestamp must be set before calculating signature params!");

        String signatureParams = parameters.keySet().stream()
            .map(item -> "\"" + item.toLowerCase() + "\"")  // Quote each item, need to be lower case
            .collect(Collectors.joining(" "));

        String params = String.format("(%s);keyid=\"%s\";created=%d", signatureParams, keyId, created);
        parameters.put(SIGNATURE_PARAMS, params);
    }

    /**
     * Builds the signature using the current timestamp.
     *
     * <p>This method should be called after all request parameters have been set
     * and before retrieving headers or creating the HTTP request. It generates
     * the signature parameters and prepares the builder for signature calculation.
     *
     * @return this builder for method chaining
     */
    public SignatureRequestBuilder build() {
        return build(Instant.now().getEpochSecond());
    }

    /**
     * Builds the signature using a specific timestamp.
     *
     * <p>This method allows setting a custom timestamp for signature creation,
     * which is useful for testing or when you need precise control over the
     * signature timestamp for replay attack prevention.
     *
     * @param createdInSeconds Unix timestamp in seconds when the signature was created
     * @return this builder for method chaining
     */
    public SignatureRequestBuilder build(long createdInSeconds) {
        this.created = createdInSeconds;
        setSignatureParams();
        return this;
    }

    /**
     * URL must end with "/" in case no query parameters are present
     *
     * @param value URL
     * @return URL that ends with '/'
     */
    private String prepareTarget(URI value) {
        ensureNotNull(value, "Target URI cannot be null!");
        // We must fix target so it conforms to expectations
        var out = value.toString();
        if (!out.endsWith("/") && value.getQuery() == null) {
            return out + "/";
        }

        return out;
    }

    /**
     * Retrieves the target URI from the parameters after ensuring the required parameter exists.
     *
     * @return the target URI as a {@link URI} object
     */
    public URI getTarget() {
        checkHasParameter(TARGET);
        return URI.create(parameters.get(TARGET).toString());
    }

    /**
     * Retrieves the signature parameters header in the form of a formatted string.
     * The method ensures that the signature parameters have been built before retrieval
     * and constructs the header using the default signature identifier and the built parameters.
     *
     * @return a string representing the signature parameters header in the format "DEFAULT_SIGNATURE_ID=signatureParams"
     * @throws IllegalStateException if the signature parameters have not been built prior to retrieval
     */
    public String getSignatureParamsHeader() {

        checkIsBuild();
        String signatureParams = parameters.get(SIGNATURE_PARAMS).toString();

        return String.format("%s=%s", DEFAULT_SIGNATURE_ID, signatureParams);
    }

    /**
     * Validates whether the signature has been properly built by ensuring the necessary parameters are present.
     * If the required signature parameters are not found, an IllegalStateException is thrown to indicate
     * that the signature has not been built and it is not ready for retrieval.
     * Throws:
     * - IllegalStateException if the signature parameters are missing, indicating that the build process has not been completed.
     */
    private void checkIsBuild() {
        if (parameters.get(SIGNATURE_PARAMS) == null) {
            throw new IllegalStateException("Signature must be build before retrieval of signature params header!");
        }
    }

    /**
     * Checks if the specified parameter key exists in the parameters map. Throws an exception if the key is not found.
     *
     * @param key the parameter key to check for existence
     */
    private void checkHasParameter(String key) {
        ensureTrue(parameters.containsKey(key), String.format("Parameter '%s' must be set before continuing!", key));
    }

    /**
     * Validates whether the METHOD parameter in the parameters map is null.
     * If the METHOD parameter is already set, this method throws an exception with
     * an error message indicating that the method has already been set.
     * <p>
     * This function ensures that the METHOD parameter is not being redefined,
     * helping maintain consistency and preventing unintended reassignments.
     * <p>
     * Throws:
     * - IllegalStateException: If the METHOD parameter in the parameters map is already set.
     */
    private void checkMethod() {
        ensureTrue(parameters.get(METHOD) == null, String.format("Method '%s' already set!", parameters.get(METHOD)));
    }

    /**
     * Constructs and returns a string representation of the signature base by iterating over
     * the parameters map and appending key-value pairs. The keys are converted to lowercase
     * as required by certain header standards, and entries are separated by a newline character.
     *
     * @return a string representation of the signature base comprising key-value pairs from
     * the parameters map, where keys are in lowercase and pairs are separated by newlines
     */
    protected String getSignatureBase() {
        this.checkIsBuild();
        StringBuilder signatureBase = new StringBuilder();
        Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            signatureBase.append("\"")
                .append(entry.getKey().toLowerCase())   // headers need to be lower case
                .append("\": ")
                .append(entry.getValue());

            if (iterator.hasNext()) signatureBase.append(System.lineSeparator());
        }
        return signatureBase.toString();
    }

    /**
     * Generates a SHA-512 digest of the provided content and returns it as a Base64-encoded string.
     *
     * @param content the input string to be hashed; must not be null or empty
     * @return the Base64-encoded string of the SHA-512 hash of the input content
     * @throws NoSuchAlgorithmException if the SHA-512 algorithm is not available in the environment
     */
    protected static String digestContentSha512(String content) throws NoSuchAlgorithmException {
        UtilGlobal.ensureNotNullOrEmpty(content, "Content must not be null or empty!");

        MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM.toUpperCase());
        byte[] bodyBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] hashedBytes = digest.digest(bodyBytes);
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    /**
     * Generates a cryptographic signature based on the provided algorithm, private key,
     * and the signature base. The generated signature is returned as a Base64 encoded string.
     *
     * @return a Base64 encoded string representing the cryptographic signature
     * @throws IllegalStateException if the signature generation process fails due to an invalid
     *                               algorithm, key, or other unexpected issues during signing
     */
    protected String getSignature() {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(getSignatureBase().getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
            throw new IllegalStateException("Failed to generate signature!", ex);
        }
    }

    /**
     * Constructs and returns the signature header string by combining the default signature identifier
     * and the generated signature value in the predefined format.
     *
     * @return A formatted string representing the signature header.
     */
    String getSignatureHeader() {
        return String.format("%s=:%s:", DEFAULT_SIGNATURE_ID, getSignature());
    }

    /**
     * Retrieves the headers as a LinkedHashMap of key-value pairs. The headers include
     * pre-defined values such as "Accept" and may include dynamically added headers
     * like "Content-Type", "Content-Digest", and "Authorization" based on the parameters.
     * Additionally, the method populates signature-related headers.
     *
     * @return a LinkedHashMap containing the headers and their corresponding values
     */
    public LinkedHashMap<String, String> getHeaders() {
        this.checkIsBuild();

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put(ACCEPT_HEADER, APPLICATION_JSON);

        if (parameters.containsKey(CONTENT_LENGTH_HEADER)) {
            headers.put(CONTENT_TYPE_HEADER, parameters.get(CONTENT_TYPE_HEADER).toString());
        }

        if (parameters.containsKey(CONTENT_DIGEST_HEADER)) {
            headers.put(CONTENT_DIGEST_HEADER, parameters.get(CONTENT_DIGEST_HEADER).toString());
        }

        if (parameters.containsKey(AUTHORIZATION_HEADER)) {
            headers.put(AUTHORIZATION_HEADER, parameters.get(AUTHORIZATION_HEADER).toString());
        }

        headers.put(SIGNATURE_INPUT_HEADER, getSignatureParamsHeader());
        headers.put(SIGNATURE_HEADER, getSignatureHeader());
        return headers;
    }

    /**
     * Constructs and returns an {@link HttpRequest} instance configured with
     * HTTP headers, method, body, and other settings based on the provided
     * {@link OpenPaymentsClientOptions} and the current state of the builder.
     *
     * @param options the {@link OpenPaymentsClientOptions} instance containing client-specific
     *                configurations such as timeout settings and other relevant options
     * @return a fully constructed and configured {@link HttpRequest} instance
     */
    public HttpRequest getRequest(OpenPaymentsClientOptions options) {
        return getBuilder(options).build();
    }

    /**
     * Constructs and returns an {@link HttpRequest.Builder} instance configured with
     * HTTP headers, method, body, and timeout settings based on the provided
     * {@link OpenPaymentsClientOptions} and the current state of the object.
     *
     * @param options the {@link OpenPaymentsClientOptions} instance containing client-specific
     *                configurations such as timeout settings
     * @return a configured {@link HttpRequest.Builder} instance ready to build an HTTP request
     */
    public HttpRequest.Builder getBuilder(OpenPaymentsClientOptions options) {
        // build if not already
        if (parameters.get(SIGNATURE_PARAMS) == null) this.build();
        this.checkIsBuild();

        HttpRequest.Builder builder = HttpRequest.newBuilder(getTarget());
        for (Map.Entry<String, String> entry : getHeaders().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        if (body == null || body.isBlank()) {
            builder.method(getMethod(), HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(getMethod(), HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }

        builder.timeout(Duration.of(options.getTimeOutInSeconds(), SECONDS));
        return builder;
    }
}