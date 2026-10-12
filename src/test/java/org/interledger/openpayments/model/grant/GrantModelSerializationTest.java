package org.interledger.openpayments.model.grant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.openpayments.exception.OpenPaymentsClientException;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.key.Ed25519PublicKey;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPaymentGrantSpentAmounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the (de)serialization of the grant and payment models against the
 * Open Payments v1.4.0 specification.
 *
 * <p>Verifies that:
 *
 * <ul>
 *   <li>{@link Client} is written as a wallet address object, a directed identity (JWK) object,
 *       or the deprecated plain string
 *   <li>{@link GrantRequest} supports {@code subject}, interval limits and card authorization
 *   <li>{@link Grant}, {@link PendingGrant}, {@link OutgoingPayment} and
 *       {@link OutgoingPaymentGrantSpentAmounts} read the specification's example responses
 * </ul>
 */
@DisplayName("Grant models → Open Payments v1.4.0 (de)serialization")
public class GrantModelSerializationTest {
    private static final ObjectMapper MAPPER = OpenPaymentsObjectMapper.get();
    private static final String WALLET = "https://ilp.interledger-test.dev/alice";
    private static final String RECEIVER =
        "https://ilp.interledger-test.dev/incoming-payments/45a0d0ee-26dc-4c66-89e0-01fbf93156f7";

    private static JsonNode toJson(Object value) {
        return MAPPER.valueToTree(value);
    }

    private static JsonNode readTree(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    @DisplayName("Client – wallet address, directed identity and deprecated string forms")
    @Nested
    class ClientForms {
        @Test
        void shouldWriteWalletAddressObjectByDefault() throws Exception {
            assertEquals(readTree("{\"walletAddress\":\"" + WALLET + "\"}"), toJson(new Client(WALLET)));
            assertEquals(readTree("{\"walletAddress\":\"" + WALLET + "\"}"), toJson(new Client(URI.create(WALLET))));
            assertEquals(Client.Format.WALLET_ADDRESS, new Client(WALLET).getFormat());
        }

        @Test
        void shouldWriteDirectedIdentity() throws Exception {
            var jwk = new Ed25519PublicKey();
            jwk.setKid("key-1");
            jwk.setAlg("EdDSA");
            jwk.setKty("OKP");
            jwk.setCrv("Ed25519");
            jwk.setX("oy0L_vTygNE4IogRyn_F5GmHXdqYVjIXkWs2jky7zsI");

            var expected = readTree("{\"jwk\":{\"kid\":\"key-1\",\"alg\":\"EdDSA\",\"kty\":\"OKP\","
                + "\"crv\":\"Ed25519\",\"x\":\"oy0L_vTygNE4IogRyn_F5GmHXdqYVjIXkWs2jky7zsI\"}}");
            var client = Client.directedIdentity(jwk);

            assertEquals(expected, toJson(client));
            assertNull(client.getPaymentPointer());
        }

        @Test
        @SuppressWarnings("deprecation")
        void shouldWriteDeprecatedString() {
            var json = toJson(Client.walletAddressString(URI.create(WALLET)));
            assertTrue(json.isTextual());
            assertEquals(WALLET, json.asText());
        }

        @Test
        void shouldRejectMissingValues() {
            assertThrows(OpenPaymentsClientException.class, () -> Client.directedIdentity(null));
            assertThrows(OpenPaymentsClientException.class, () -> Client.walletAddress(null));
        }
    }

    @DisplayName("GrantRequest – subject, interval limits and card authorization")
    @Nested
    class GrantRequests {
        @Test
        void shouldWriteSubjectRequestWithoutAccessToken() throws Exception {
            var request = GrantRequest.subject(new Client(WALLET), Subject.walletAddress(URI.create(WALLET)))
                .redirectInteract(URI.create("https://paymentplatform.example/finish"), "nonce-1");

            var expected = readTree("{"
                + "\"client\":{\"walletAddress\":\"" + WALLET + "\"},"
                + "\"subject\":{\"sub_ids\":[{\"id\":\"" + WALLET + "\",\"format\":\"uri\"}]},"
                + "\"interact\":{\"start\":[\"redirect\"],\"finish\":{\"method\":\"redirect\","
                + "\"uri\":\"https://paymentplatform.example/finish\",\"nonce\":\"nonce-1\"}}"
                + "}");

            assertEquals(expected, toJson(request));
        }

        @Test
        void shouldWriteOutgoingRequestWithInterval() throws Exception {
            var debit = Amount.build(new BigDecimal("100.00"), "CAD");
            var request = GrantRequest.outgoing(
                new Client(WALLET),
                Set.of(AccessAction.create),
                URI.create(WALLET),
                Limits.debit(debit, "R3/2025-05-20T13:00:00Z/P1M"));

            var item = toJson(request).at("/access_token/access/0");
            assertEquals("outgoing-payment", item.get("type").asText());
            assertEquals(WALLET, item.get("identifier").asText());
            assertEquals("R3/2025-05-20T13:00:00Z/P1M", item.at("/limits/interval").asText());
            assertEquals("10000", item.at("/limits/debitAmount/value").asText());
            assertFalse(item.get("limits").has("receiveAmount"));
            assertFalse(item.has("cardAuthorization"));
        }

        @Test
        void shouldKeepLegacyDebitAmountOutgoingRequest() {
            var debit = Amount.build(new BigDecimal("5.00"), "USD");
            var request = GrantRequest.outgoing(
                new Client(WALLET), AccessItemType.outgoingPayment, Set.of(AccessAction.create), URI.create(WALLET), debit);

            var limits = toJson(request).at("/access_token/access/0/limits");
            assertEquals("500", limits.at("/debitAmount/value").asText());
            assertFalse(limits.has("interval"));
        }

        @Test
        void shouldWriteCardAuthorizationWithPin() throws Exception {
            var card = CardAuthorization.build(
                "9F26089F27019F1002...",
                "018f8d2e-3c1a-7b6e-9f0a-2c4d6e8f0a1b",
                "23e412341234",
                PinWorkingKey.build("A1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D4", "A3F1C2", "00000000000000000000000000000000"));

            var expected = readTree("{\"tlvData\":\"9F26089F27019F1002...\",\"pinBlock\":\"23e412341234\","
                + "\"pwk\":{\"key\":\"A1B2C3D4E5F6A1B2C3D4E5F6A1B2C3D4\",\"kvc\":\"A3F1C2\","
                + "\"iv\":\"00000000000000000000000000000000\"},"
                + "\"requestId\":\"018f8d2e-3c1a-7b6e-9f0a-2c4d6e8f0a1b\"}");

            assertEquals(expected, toJson(card));
        }

        @Test
        void shouldWriteNoCvmCardAuthorization() {
            var json = toJson(CardAuthorization.build("9F26089F27019F1002...", "req-1"));
            assertFalse(json.has("pinBlock"));
            assertFalse(json.has("pwk"));
        }

        @Test
        void shouldRequirePwkWithPinBlock() {
            assertThrows(OpenPaymentsClientException.class,
                () -> CardAuthorization.build("tlv", "req-1", "23e412341234", null));
        }
    }

    @DisplayName("Responses – spec examples deserialize into the models")
    @Nested
    class Responses {
        @Test
        void shouldReadSubjectContinuationResponse() throws Exception {
            var json = "{\"subject\":{\"sub_ids\":[{\"id\":\"" + WALLET + "\",\"format\":\"uri\"}]},"
                + "\"continue\":{\"access_token\":{\"value\":\"33OMUKMKSKU80UPRY5NM\"},"
                + "\"uri\":\"https://auth.interledger-test.dev/continue/4CF492MLVMSW9MKMXKHQ\",\"wait\":30}}";

            var grant = MAPPER.readValue(json, Grant.class);

            assertNull(grant.getAccess());
            assertEquals(WALLET, grant.getSubject().getSubIds().getFirst().getId());
            assertEquals(Subject.FORMAT_URI, grant.getSubject().getSubIds().getFirst().getFormat());
            assertEquals("33OMUKMKSKU80UPRY5NM", grant.getAccessContinue().getAccess().getToken());
        }

        @Test
        void shouldReadPendingGrant() throws Exception {
            var json = "{\"interact\":{\"redirect\":\"https://auth.interledger-test.dev/4CF492MLVMSW9MKMXKHQ\","
                + "\"finish\":\"4105340a-05eb-4290-8739-f9e2b463bfa7\"},"
                + "\"continue\":{\"access_token\":{\"value\":\"33OMUKMKSKU80UPRY5NM\"},"
                + "\"uri\":\"https://auth.interledger-test.dev/continue/4CF492MLVMSW9MKMXKHQ\",\"wait\":30}}";

            var pending = MAPPER.readValue(json, PendingGrant.class);

            assertEquals(URI.create("https://auth.interledger-test.dev/4CF492MLVMSW9MKMXKHQ"), pending.getInteract().getRedirect());
            assertEquals("4105340a-05eb-4290-8739-f9e2b463bfa7", pending.getInteract().getToken());
            assertEquals("33OMUKMKSKU80UPRY5NM", pending.getGrantContinue().getAccess().getToken());
            assertEquals(30, pending.getGrantContinue().getWait());
        }

        @Test
        void shouldReadCardAuthorizationGrant() throws Exception {
            var json = "{\"access_token\":{\"value\":\"OS9M2PMHKUR64TB8N6BW7OZB8CDFONP219RP1LT0\","
                + "\"manage\":\"https://auth.interledger-test.dev/token/dd17a202\",\"expires_in\":3600,"
                + "\"access\":[{\"type\":\"outgoing-payment\",\"actions\":[\"create\",\"read\"],"
                + "\"identifier\":\"" + WALLET + "\","
                + "\"limits\":{\"receiver\":\"" + RECEIVER + "\","
                + "\"receiveAmount\":{\"value\":\"1000\",\"assetCode\":\"USD\",\"assetScale\":2}},"
                + "\"cardAuthorization\":{\"tlvData\":\"8A023030...\","
                + "\"requestId\":\"018f8d2e-3c1a-7b6e-9f0a-2c4d6e8f0a1b\"}}]},"
                + "\"continue\":{\"access_token\":{\"value\":\"33OMUKMKSKU80UPRY5NM\"},"
                + "\"uri\":\"https://auth.interledger-test.dev/continue/4CF492MLVMSW9MKMXKHQ\"}}";

            var grant = MAPPER.readValue(json, Grant.class);
            var item = grant.getAccess().getAccess().iterator().next();

            assertEquals(URI.create(RECEIVER), item.getLimits().getReceiver());
            assertEquals("8A023030...", item.getCardAuthorization().getTlvData());
            assertEquals("018f8d2e-3c1a-7b6e-9f0a-2c4d6e8f0a1b", item.getCardAuthorization().getRequestId());
        }

        @Test
        void shouldReadGrantSpentAmounts() throws Exception {
            var json = "{\"spentReceiveAmount\":{\"value\":\"2500\",\"assetCode\":\"USD\",\"assetScale\":2},"
                + "\"spentDebitAmount\":{\"value\":\"2600\",\"assetCode\":\"USD\",\"assetScale\":2}}";

            var spent = MAPPER.readValue(json, OutgoingPaymentGrantSpentAmounts.class);

            assertEquals("2500", spent.getSpentReceiveAmount().getValue());
            assertEquals(new BigDecimal("26.00"), spent.getSpentDebitAmount().asBigDecimal());
        }

        @Test
        void shouldReadNullGrantSpentAmounts() throws Exception {
            var spent = MAPPER.readValue(
                "{\"spentReceiveAmount\":null,\"spentDebitAmount\":null}", OutgoingPaymentGrantSpentAmounts.class);

            assertNull(spent.getSpentReceiveAmount());
            assertNull(spent.getSpentDebitAmount());
        }

        @Test
        void shouldReadOutgoingPaymentWithSpentAmounts() throws Exception {
            var json = "{\"id\":\"https://ilp.interledger-test.dev/outgoing-payments/8c68d3cc\","
                + "\"walletAddress\":\"https://ilp.interledger-test.dev/alice/\",\"failed\":false,"
                + "\"receiver\":\"" + RECEIVER + "\","
                + "\"receiveAmount\":{\"value\":\"2500\",\"assetCode\":\"USD\",\"assetScale\":2},"
                + "\"debitAmount\":{\"value\":\"2600\",\"assetCode\":\"USD\",\"assetScale\":2},"
                + "\"sentAmount\":{\"value\":\"2500\",\"assetCode\":\"USD\",\"assetScale\":2},"
                + "\"grantSpentDebitAmount\":{\"value\":\"2600\",\"assetCode\":\"USD\",\"assetScale\":2},"
                + "\"createdAt\":\"2022-03-12T23:20:50.52Z\","
                + "\"metadata\":{\"description\":\"APlusVideo subscription\",\"externalRef\":\"customer: 847458475\"}}";

            var payment = MAPPER.readValue(json, OutgoingPayment.class);

            assertEquals("2500", payment.getReceivedAmount().getValue());
            assertEquals("2600", payment.getGrantSpentDebitAmount().getValue());
            assertNull(payment.getGrantSpentReceiveAmount());
            assertEquals("APlusVideo subscription", payment.getMetadata().get("description"));
        }
    }
}
