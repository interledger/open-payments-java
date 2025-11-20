package org.interledger.openpayments.util.testing;

import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.*;
import org.interledger.openpayments.model.incomingpayment.IncomingPayment;
import org.interledger.openpayments.model.outgoingpayment.OutgoingPayment;
import org.interledger.openpayments.model.payment.Payment;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Set;

public final class TestModels {
    public static final String TEST_URL = "https://api.example.com/";
    private static final String AUTH_URL = "https://api.example.com/auth";
    private static final String WALLET_ADDRESS_URL = TEST_URL + "johndoe";
    private static final String OUTGOING_PAYMENT_URL = TEST_URL + "outgoing-payment/1";
    private static final String INCOMING_PAYMENT_URL = TEST_URL + "incoming-payment/1";

    public static WalletAddress createWalletAddress() {
        var walletAddress = new WalletAddress();
        walletAddress.setId(WALLET_ADDRESS_URL);
        walletAddress.setResourceServer(TEST_URL);
        walletAddress.setAuthServer(AUTH_URL);
        walletAddress.setAssetScale(1);
        walletAddress.setAssetCode("USD");
        return walletAddress;
    }

    public static Quote createQuote() {
        Quote quote = new Quote();
        quote.setDebitAmount(Amount.build(BigDecimal.valueOf(100), "USD"));
        return quote;
    }

    public static OutgoingPayment createOutgoingPayment() {
        OutgoingPayment outgoingPayment = new OutgoingPayment();
        var accessContinue = new AccessContinue();
        accessContinue.setUri(URI.create(OUTGOING_PAYMENT_URL));
        var accessToken = new AccessToken();
        accessToken.setToken("token");
        accessContinue.setAccess(accessToken);
        outgoingPayment.setPaymentContinue(accessContinue);
        outgoingPayment.setToken("token");
        outgoingPayment.setInteract(new InteractContinue());
        return outgoingPayment;
    }

    public static IncomingPayment createIncomingPayment() {
        IncomingPayment incomingPayment = new IncomingPayment();
        incomingPayment.setId(URI.create(INCOMING_PAYMENT_URL));
        incomingPayment.setCreatedAt(Instant.now());
        return incomingPayment;
    }

    public static Grant createGrantForQuote() {
        return createGrant(AccessItemType.quote, Set.of(AccessAction.read, AccessAction.create));
    }

    public static Grant createGrantForIncomingPayment() {
        return createGrant(AccessItemType.incomingPayment, Set.of(AccessAction.read, AccessAction.complete, AccessAction.create));
    }

    public static Grant createGrant(AccessItemType type, Set<AccessAction> actions) {
        Grant grant = new Grant();
        AccessToken access = new AccessToken();
        access.setToken("token");
        var accessItem = new AccessItem();
        accessItem.setAccessType(type);
        accessItem.setActions(actions);
        access.setAccess(Set.of(accessItem));
        grant.setAccess(access);
        grant.setAccessContinue(new AccessContinue());
        return grant;
    }

    public static Payment createPayment() {
        Payment payment = new Payment();
        payment.setCreatedAt(Instant.now());
        payment.setId(URI.create(TEST_URL));
        return payment;
    }
}
