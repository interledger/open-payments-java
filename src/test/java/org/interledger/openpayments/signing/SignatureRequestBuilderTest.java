package org.interledger.openpayments.signing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.httpclient.OpenPaymentsClientOptions;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.util.testing.Person;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Set;

import static org.interledger.openpayments.signing.SignatureRequestBuilder.*;
import static org.interledger.openpayments.util.testing.TestKeys.generateKeyPair;
import static org.interledger.openpayments.util.testing.TestKeys.generatePrivateKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SignatureRequestBuilderTest {

    @Nested
    class Method {
        @ParameterizedTest
        @ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "HEAD"})
        void shouldAddMethodToParameters(String value) {
            var builder = createBuilder();
            builder.method(value);
            Object method = builder.getParameters().get("@method");
            assertEquals(method, value);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowWhenMethodIsNullOrEmpty(String value) {
            var builder = createBuilder();
            OpenPaymentsClientException exception = assertThrows(OpenPaymentsClientException.class, () -> builder.method(value));
            assertEquals("Method cannot be null or empty!", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"OPTIONS", "PATCH", "RANDOM_STRING"})
        void shouldThrowWhenMethodIsNotAllowed(String value) {
            var builder = createBuilder();
            OpenPaymentsClientException exception = assertThrows(OpenPaymentsClientException.class, () -> builder.method(value));
            assertEquals(String.format("Method '%s' is not allowed. Allowed methods are: %s", value, String.join(", ", ALLOWED_METHODS)), exception.getMessage());
        }
    }

    @Nested
    class Json {
        @Test
        void shouldSetRequestBodyAsJsonAndAddHeaders() throws JsonProcessingException {
            var person = new Person("John Doe", 25);
            var builder = createBuilder();
            builder.json(person);

            assertEquals("{\"name\":\"John Doe\",\"age\":25}", builder.getBody());
            assertEquals("application/json", builder.parameters.get("Content-Type"));

            ObjectMapper mapper = new ObjectMapper();
            int expectedLength = mapper.writeValueAsString(person).getBytes(StandardCharsets.UTF_8).length;
            assertEquals(builder.parameters.get("Content-Length"), expectedLength);

            String digestHeader = builder.parameters.get("Content-Digest").toString();
            assertTrue(digestHeader.startsWith("sha-512=:"));
            assertTrue(digestHeader.endsWith(":"));

            String base64Part = digestHeader.substring("sha-512=:".length(),
                    digestHeader.length() - 1);
            assertTrue(base64Part.matches("^[A-Za-z0-9+/=]+$"));
        }
    }

    @Nested
    class Post {
        @Test
        void shouldSetMethodToPostAndBody() throws JsonProcessingException {
            var builder = createBuilder();
            var person = new Person("John Doe", 25);
            builder.POST(person);
            assertEquals("POST", builder.getMethod());
            ObjectMapper mapper = new ObjectMapper();
            assertEquals(mapper.writeValueAsString(person), builder.getBody());
        }

        @Test
        void shouldSetMethodToPost() {
            var builder = createBuilder();
            builder.POST();
            assertEquals("POST", builder.getMethod());
        }

    }

    @Nested
    class Put {
        @Test
        void shouldSetMethodToPutAndBody() throws JsonProcessingException {
            var builder = createBuilder();
            var person = new Person("John Doe", 25);
            builder.PUT(person);
            assertEquals("PUT", builder.getMethod());
            ObjectMapper mapper = new ObjectMapper();
            assertEquals(mapper.writeValueAsString(person), builder.getBody());
        }

        @Test
        void shouldSetMethodToPut() {
            var builder = createBuilder();
            builder.PUT();
            assertEquals("PUT", builder.getMethod());
        }
    }

    @Nested
    class Get {
        @Test
        void shouldSetMethodToGet() {
            var builder = createBuilder();
            builder.GET();
            assertEquals("GET", builder.getMethod());
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldSetMethodToDelete() {
            var builder = createBuilder();
            builder.DELETE();
            assertEquals("DELETE", builder.getMethod());
        }
    }

    @Nested
    class GetMethod {
        @Test
        void shouldGetMethodWhenSet() {
            var builder = createBuilder();
            builder.method("GET");
            String method = builder.getMethod();
            assertEquals("GET", method);
        }

        @Test
        void shouldThrowWhenMethodNotSet() {
            var builder = createBuilder();
            OpenPaymentsClientException exception = assertThrows(OpenPaymentsClientException.class, builder::getMethod);
            assertEquals("Parameter '@method' must be set before continuing!", exception.getMessage());
        }

    }

    @Nested
    class Target {
        @Test
        void shouldSetTargetWhenValid() {
            String value = "https://api.example.com";
            var builder = createBuilder();
            builder.target(value);
            assertEquals(value + '/', builder.getParameters().get("@target-uri"));
        }

        @Test
        void shouldThrowWhenValueIsNull() {
            var builder = createBuilder();
            assertThrows(NullPointerException.class, () -> builder.target((String) null));
            var exception = assertThrows(OpenPaymentsClientException.class, () -> builder.target((URI) null));
            assertEquals("Target URI cannot be null!", exception.getMessage());
        }

        @Test
        void shouldGetTargetWhenSet() {
            String value = "https://api.example.com";
            var builder = createBuilder().target(value);
            URI target = builder.getTarget();
            assertEquals(value + '/', target.toString());
        }

        @Test
        void shouldThrowWhenTargetNotSet() {
            var builder = createBuilder();
            var exception = assertThrows(OpenPaymentsClientException.class, builder::getTarget);
            assertEquals("Parameter '@target-uri' must be set before continuing!", exception.getMessage());
        }
    }

    @Nested
    class AccessToken {
        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowWhenTokenIsNullOrEmpty(String value) {
            var builder = createBuilder();
            var exception = assertThrows(OpenPaymentsClientException.class, () -> builder.accessToken(value));
            assertEquals("Token cannot be null or empty!", exception.getMessage());
        }

        @Test
        void shouldSetAccessTokenWhenValid() {
            String token = "token";
            var builder = createBuilder().accessToken(token);
            assertEquals("GNAP %s".formatted(token), builder.getParameters().get("Authorization"));
        }
    }

    @Nested
    class Signature {
        @Nested
        class BuildAndSignatureParams {
            @Test
            void shouldSetSignatureParams() {
                long createdInSeconds = Instant.now().getEpochSecond();
                var builder = createAndPopulateBuilder().build(createdInSeconds);
                String params = builder.parameters.get("@signature-params").toString();
                assertEquals("(\"@method\" \"content-digest\" \"content-length\" " +
                        "\"content-type\" \"@target-uri\");keyid=\"keyId\";created=" +
                        createdInSeconds, params);
            }

            @Test
            void shouldThrowWhenCreatedTimestampNotSet() {
                var exception = assertThrows(OpenPaymentsClientException.class, () -> createBuilder().build(0));
                assertEquals("Created timestamp must be set before calculating signature params!", exception.getMessage());
            }

            @Test
            void shouldGetSignatureParamsHeader() {
                long createdInSeconds = Instant.now().getEpochSecond();
                var builder = createAndPopulateBuilder().build(createdInSeconds);
                String expected = "sig1=(\"@method\" \"content-digest\" \"content-length\" " +
                        "\"content-type\" \"@target-uri\");keyid=\"keyId\";created=" +
                        createdInSeconds;
                String headers = builder.getSignatureParamsHeader();
                assertEquals(expected, headers);
            }

            @Test
            void shouldThrowWhenSignatureNotBuilt() {
                var builder = createAndPopulateBuilder();
                var exception = assertThrows(IllegalStateException.class, builder::getSignatureParamsHeader);
                assertEquals("Signature must be build before retrieval of signature params header!", exception.getMessage());
            }
        }

        @Nested
        class GetSignatureBase {
            @Test
            void shouldGetSignatureBase() throws NoSuchAlgorithmException {
                long createdInSeconds = Instant.now().getEpochSecond();
                String target = "https://api.example.com";
                Person person = new Person("John Doe", 25);
                var builder = createBuilder()
                        .POST(person)
                        .target(target)
                        .build(createdInSeconds);
                String signatureBase = builder.getSignatureBase();
                OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
                String json = mapper.writeValueAsString(person);
                String expected = "\"@method\": POST\n" +
                        "\"content-digest\": sha-512=:" + digestContentSha512(json) + ":\n" +
                        "\"content-length\": 28\n" +
                        "\"content-type\": application/json\n" +
                        "\"@target-uri\": https://api.example.com/\n" +
                        "\"@signature-params\": (\"@method\" \"content-digest\" \"content-length\" \"content-type\" \"@target-uri\");keyid=\"keyId\";created=" + createdInSeconds;
                assertEquals(expected, signatureBase);
            }

            @Test
            void shouldGetSignatureBaseWhenNoParameterWasSet() {
                long createdInSeconds = Instant.now().getEpochSecond();
                var builder = createBuilder().build(createdInSeconds);
                String signatureBase = builder.getSignatureBase();
                String expected = "\"@signature-params\": ();keyid=\"keyId\";created=" + createdInSeconds;
                assertEquals(expected, signatureBase);
            }
        }

        @Nested
        class GetSignature {
            @Test
            void shouldGenerateSignature() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
                KeyPair keyPair = generateKeyPair();
                PublicKey publicKey = keyPair.getPublic();
                PrivateKey privateKey = keyPair.getPrivate();
                OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
                var builder = spy(new SignatureRequestBuilder(privateKey, "keyId", mapper)).build();
                String signature = builder.getSignature();
                assertNotNull(signature);
                byte[] signatureBytes = Base64.getDecoder().decode(signature);

                var verifier = java.security.Signature.getInstance(SIGNATURE_ALGORITHM);
                verifier.initVerify(publicKey);
                verifier.update(builder.getSignatureBase().getBytes(StandardCharsets.UTF_8));
                assertTrue(verifier.verify(signatureBytes));
            }

            @Test
            void shouldThrowWhenCannotGenerateSignature() throws NoSuchAlgorithmException {
                // wrong algorithm will throw error
                PrivateKey privateKey = generatePrivateKey("RSA");
                OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
                var builder = spy(new SignatureRequestBuilder(privateKey, "keyId", mapper)).build();
                var exception = assertThrows(IllegalStateException.class, builder::getSignature);
                assertEquals("Failed to generate signature!", exception.getMessage());
            }
        }
    }

    @Nested
    class GetHeaders {
        @Test
        void shouldGetHeaders() throws NoSuchAlgorithmException {
            long createdInSeconds = Instant.now().getEpochSecond();
            var person = new Person("John Doe", 25);
            var builder = createBuilder().POST(person).build(createdInSeconds);
            builder.accessToken("token");
            String signature = "Signature";
            doReturn(signature).when(builder).getSignature();
            var headers = builder.getHeaders();
            LinkedHashMap<String, String> expected = new LinkedHashMap<>();
            expected.put("Accept", APPLICATION_JSON);
            expected.put("Signature-Input", builder.getSignatureParamsHeader());
            expected.put("Signature", "sig1=:" + signature + ":");
            expected.put("Authorization", "GNAP token");
            expected.put("Content-Digest", "sha-512=:" + digestContentSha512(builder.getMapper().writeValueAsString(person)) + ":");
            expected.put("Content-Type", APPLICATION_JSON);
            assertEquals(expected, headers);
        }
    }

    @Nested
    class HttpRequest {
        @Test
        void shouldGetHttpRequestWithBody() {
            var builder = createAndPopulateBuilder().build();
            String signature = "Signature";
            doReturn(signature).when(builder).getSignature();
            java.net.http.HttpRequest request = builder.getRequest(OpenPaymentsClientOptions.DEFAULT);

            assertEquals(URI.create("https://api.example.com/"), request.uri());
            assertEquals("POST", request.method());

            Set<String> expectedHeaders = Set.of("Accept", "Content-Digest", "Content-Type", "Signature", "Signature-Input");
            assertEquals(expectedHeaders, request.headers().map().keySet());

            assertEquals(OpenPaymentsClientOptions.DEFAULT.getTimeOutInSeconds(), request.timeout().get().get(ChronoUnit.SECONDS));
            assertTrue(request.bodyPublisher().get().contentLength() > 0);
        }

        @Test
        void shouldGetHttpRequestWithNoBody() {
            var builder = createBuilder().target("https://api.example.com").method("GET").build();
            String signature = "Signature";
            doReturn(signature).when(builder).getSignature();
            java.net.http.HttpRequest request = builder.getRequest(OpenPaymentsClientOptions.DEFAULT);

            assertEquals(URI.create("https://api.example.com/"), request.uri());
            assertEquals("GET", request.method());

            Set<String> expectedHeaders = Set.of("Accept", "Signature", "Signature-Input");
            assertEquals(expectedHeaders, request.headers().map().keySet());

            assertEquals(OpenPaymentsClientOptions.DEFAULT.getTimeOutInSeconds(), request.timeout().get().get(ChronoUnit.SECONDS));
            assertEquals(0, request.bodyPublisher().get().contentLength());
        }

        @Test
        void shouldGetHttpRequestWhenSignatureNotSet() {
            var builder = createAndPopulateBuilder();
            String signature = "Signature";
            doReturn(signature).when(builder).getSignature();
            java.net.http.HttpRequest request = builder.getRequest(OpenPaymentsClientOptions.DEFAULT);

            assertEquals(URI.create("https://api.example.com/"), request.uri());
            assertEquals("POST", request.method());

            Set<String> expectedHeaders = Set.of("Accept", "Content-Digest", "Content-Type", "Signature", "Signature-Input");
            assertEquals(expectedHeaders, request.headers().map().keySet());

            assertEquals(OpenPaymentsClientOptions.DEFAULT.getTimeOutInSeconds(), request.timeout().get().get(ChronoUnit.SECONDS));
            assertTrue(request.bodyPublisher().get().contentLength() > 0);
        }
    }

    SignatureRequestBuilder createBuilder() {
        OpenPaymentsObjectMapper mapper = new OpenPaymentsObjectMapper();
        return spy(new SignatureRequestBuilder(mock(PrivateKey.class), "keyId", mapper));
    }

    SignatureRequestBuilder createAndPopulateBuilder() {
        String target = "https://api.example.com";
        Person person = new Person("John Doe", 25);
        return createBuilder()
                .POST(person)
                .target(target);
    }
}
