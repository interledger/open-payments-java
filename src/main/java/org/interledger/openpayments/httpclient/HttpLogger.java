package org.interledger.openpayments.httpclient;

import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import static org.interledger.openpayments.util.UtilGlobal.ensureNotNull;

/**
 * Utility class for logging HTTP requests and responses, with support for detailed
 * logging of headers and body when trace-level logging is enabled.
 * The class makes use of SLF4J for logging and supports configurable verbosity
 * based on the logging level. Detailed logging of HTTP bodies is achieved via
 * a custom subscriber to capture streaming data.
 */
@Slf4j
public class HttpLogger {
    private static final String LOG_SPACE = "    ";
    private static final String LOG_BODY = "body: ";
    private static final String LOG_NO_BODY = "<no body>";

    /**
     * Logs an HTTP request with the appropriate detail level based on logger configuration.
     *
     * <p>Logging behavior:
     * <ul>
     *   <li><strong>DEBUG level:</strong> Logs method, URI, and basic request information</li>
     *   <li><strong>TRACE level:</strong> Logs complete request including headers and body</li>
     * </ul>
     *
     * <p>For requests with bodies, this method uses a custom subscriber to capture
     * the request body content as it's being sent, since the Java HTTP client
     * consumes the body stream during transmission.
     *
     * @param req HTTP request to log
     */
    public void logRequest(HttpRequest req) {
        ensureNotNull(req, "HTTP request must not be null");
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Request:  ").append(req.method()).append(" ").append(req.uri());

        if (log.isTraceEnabled()) {  // log request details only if the trace level is enabled
            logHeaders(req.headers(), logMessage);
            // define a consumer for how you want to log
            if (req.bodyPublisher().isPresent()) {
                Consumer<String> bodyConsumer = body -> {
                    if (body != null && !body.isBlank()) {
                        logMessage.append(System.lineSeparator()).append(LOG_SPACE).append(LOG_BODY).append(body);
                    }
                    log.trace(logMessage.toString());
                };

                req.bodyPublisher().get().subscribe(new HttpBodySubscriber(bodyConsumer));
            } else {
                logMessage.append(System.lineSeparator()).append(LOG_SPACE).append(LOG_NO_BODY);
                log.trace(logMessage.toString());
            }
        } else {
            log.debug(logMessage.toString());
        }
    }

    /**
     * Appends HTTP headers to the log message in a formatted manner.
     *
     * <p>Headers are formatted as "name: value1, value2" with proper indentation.
     * The ":status" pseudo-header is excluded since status codes are logged separately.
     *
     * @param headers    HTTP headers to log
     * @param logMessage StringBuilder to append formatted headers to
     */
    private static void logHeaders(HttpHeaders headers, StringBuilder logMessage) {
        headers.map().forEach((name, values) -> {
            if (!":status".equalsIgnoreCase(name)) { // ignore :status header (we already log this)
                logMessage.append(System.lineSeparator())
                        .append(LOG_SPACE).append(name).append(": ").append(String.join(", ", values));
            }
        });
    }

    /**
     * Logs an HTTP response with the appropriate detail level based on logger configuration.
     *
     * <p>Logging behavior:
     * <ul>
     *   <li><strong>DEBUG level:</strong> Logs status code, URI, and basic response information</li>
     *   <li><strong>TRACE level:</strong> Logs complete response including headers and body</li>
     * </ul>
     *
     * <p>This is particularly useful for debugging API errors, monitoring payment
     * status responses, and verifying server responses during the Interledger
     * payment flow.
     *
     * @param res HTTP response to log
     */
    public void logResponse(HttpResponse<String> res) {
        ensureNotNull(res, "HTTP response must not be null");
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Response: ").append(res.statusCode()).append(" ").append(res.uri());
        if (log.isTraceEnabled()) {
            logHeaders(res.headers(), logMessage);

            String body = res.body() != null ? res.body() : LOG_NO_BODY;
            logMessage.append(System.lineSeparator())
                    .append(LOG_SPACE)
                    .append(LOG_BODY)
                    .append(body);

            log.trace(logMessage.toString());
        } else {
            log.debug(logMessage.toString());
        }
    }

    /**
     * A subscriber implementation for processing HTTP body data.
     * This class is used internally by the HTTP logger to capture
     * and process the body content of HTTP requests or responses.
     * The {@code HttpBodySubscriber} consumes chunks of data in the form of
     * {@code ByteBuffer} objects, converts them to UTF-8 encoded strings,
     * and sends them to the provided consumer function for further handling,
     * such as logging.
     * This class implements the {@link Flow.Subscriber} interface
     * to manage the flow of data and handle lifecycle events such as
     * subscription establishment, receipt of data, completion, and errors.
     */
    protected static class HttpBodySubscriber implements Flow.Subscriber<ByteBuffer> {
        /**
         * Consumer function that processes the captured body content.
         */
        private final Consumer<String> consumer;

        /**
         * Creates a new body subscriber with the specified consumer.
         *
         * @param consumer function to process captured body content
         */
        public HttpBodySubscriber(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        /**
         * Called when the subscription is established. Requests all available data.
         *
         * @param subscription the subscription for controlling data flow
         */
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        /**
         * Called for each chunk of body data. Converts ByteBuffer to UTF-8 string
         * and passes it to the consumer for logging.
         *
         * @param item ByteBuffer containing body data chunk
         */
        @Override
        public void onNext(ByteBuffer item) {
            consumer.accept(new String(item.array(), StandardCharsets.UTF_8));
        }

        /**
         * Called when an error occurs during body streaming.
         * Currently, does not perform any error handling.
         *
         * @param throwable the error that occurred
         */
        @Override
        public void onError(Throwable throwable) {
            // Do nothing
        }

        /**
         * Called when the body streaming is complete.
         * Currently, it does not perform any completion handling.
         */
        @Override
        public void onComplete() {
            // Do nothing
        }
    }
}
