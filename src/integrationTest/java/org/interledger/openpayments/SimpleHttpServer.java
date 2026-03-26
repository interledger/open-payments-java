package org.interledger.openpayments;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A very small HTTP server that listens on a {@link ServerSocket} and handles basic HTTP GET requests.
 * <p>
 * It is primarily a testing utility used to mock external API endpoints in unit or integration
 * tests. It allows developers to verify that an outgoing request was made and to provide a controlled
 * plain-text response.
 * </p>
 * <p>
 * This class is intended for lightweight testing scenarios. It does not implement the full HTTP specification.
 * It accepts connections, reads the first request line, and if it is a GET request, responds with a 200 OK and a
 * plain text body. Non-GET requests receive a 405 Method Not Allowed response.
 * </p>
 */
@Slf4j
public class SimpleHttpServer {

    private final int requestedPort;
    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String responseBody = "OK";

    @Getter
    private volatile String lastRequestLine = null;

    /**
     * Create a server bound to the provided port. Use 0 to select a random available port.
     *
     * @param port the port to bind to, or 0 for any available port
     */
    public SimpleHttpServer(int port) {
        this.requestedPort = port;
    }

    /**
     * Set the plain text response body returned for successful GET requests.
     *
     * @param body response body
     * @return this server (for chaining)
     */
    public SimpleHttpServer setResponseBody(String body) {
        this.responseBody = Objects.requireNonNull(body);
        return this;
    }

    /**
     * Start the server. Calling multiple times has no additional effect.
     *
     * @throws IOException if the server socket cannot be opened
     */
    public synchronized void start() {
        if (running.get()) return;

        try {
            this.serverSocket = new ServerSocket(this.requestedPort);
        } catch (IOException err) {
            log.error("Failed to open server socket on port {}", this.requestedPort, err);
            throw new RuntimeException(err);
        }
        this.running.set(true);

        this.acceptThread = new Thread(this::acceptLoop, "simple-http-server-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    /**
     * Main loop that accepts incoming connections and handles them.
     */
    private void acceptLoop() {
        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try (Socket socket = serverSocket.accept()) {
                    this.handleConnection(socket);
                } catch (IOException e) {
                    if (running.get()) {
                        // If still running, log to stderr and continue; otherwise, it's likely due to shutdown.
                        System.err.println("[SimpleHttpServer] Connection handling error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                System.err.println("[SimpleHttpServer] Accept loop terminated unexpectedly: " + e.getMessage());
            }
        }
    }

    private void handleConnection(Socket socket) throws IOException {
        socket.setSoTimeout(5000);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));

        // Read the request line, e.g., "GET /path HTTP/1.1"
        String requestLine = reader.readLine();
        if (requestLine.contains("favicon.ico")) {
            return;
        }

        this.lastRequestLine = requestLine;

        // Drain remaining headers (until empty line)
        if (requestLine != null) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // ignore headers for this simple server
            }
        }

        if (requestLine == null || !requestLine.startsWith("GET ")) {
            this.writeResponse(out, 405, "Method Not Allowed", "Only GET is supported\n");
            return;
        }

        // Basic 200 OK response with the configured body
        writeResponse(out, 200, "OK", responseBody + "\n");
    }

    private void writeResponse(OutputStream out, int status, String reason, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: " + bytes.length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    /**
     * Shutdown the server, closing the server socket and stopping the accept loop.
     * This method is idempotent and may be called multiple times safely.
     */
    public synchronized void shutdown() {
        if (!running.getAndSet(false)) {
            return; // already stopped
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // this will interrupt the accept()
            }
        } catch (IOException e) {
            // ignore errors on close
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
            try {
                acceptThread.join(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @return the actual bound port, or -1 if the server is not started.
     */
    public int getPort() {
        ServerSocket ss = this.serverSocket;
        return (ss != null && ss.isBound()) ? ss.getLocalPort() : -1;
    }
}
