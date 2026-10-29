# Open Payments
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/)
[![GitHub Issues](https://img.shields.io/github/issues/interledger/open-payments/interledger.svg)](https://github.com/interledger/open-payments-java/issues)

<p align="center">
  <img src="https://raw.githubusercontent.com/interledger/open-payments/main/docs/public/img/logo.svg" width="700" alt="Open Payments">
</p>

## What is Open Payments?

Open Payments is an open API standard that can be implemented by account servicing entities (e.g. banks, digital wallet providers, and mobile money providers) to facilitate interoperability in the setup and completion of payments for different use cases including:

- [Web Monetization](https://webmonetization.org)
- Tipping/Donations (low value/low friction)
- eCommerce checkout
- P2P transfers
- Subscriptions
- Invoice Payments

The Open Payments APIs are a collection of three sub-systems:

- A **wallet address server** which exposes public information about Open Payments-enabled accounts called "wallet addresses"
- A **resource server** which exposes APIs for performing functions against the underlying accounts
- A **authorisation server** which exposes APIs compliant with the [GNAP](https://datatracker.ietf.org/doc/html/draft-ietf-gnap-core-protocol) standard for getting grants to access the resource server APIs

This repository contains a [Java/JVM Open Payments SDK](https://github.com/interledger/open-payments-node/tree/main/packages/open-payments) to make requests via the Open Payments API, as well as Java types for the API.

### New to Interledger?

Never heard of Interledger before? Or would you like to learn more? Here are some excellent places to start:

- [Interledger Website](https://interledger.org/)
- [Interledger Specification](https://interledger.org/developers/rfcs/interledger-protocol/)
- [Interledger Explainer Video](https://twitter.com/Interledger/status/1567916000074678272)
- [Open Payments](https://openpayments.dev/)
- [Web monetization](https://webmonetization.org/)

## Contributing

Please read the [contribution guidelines](.github/contributing.md) before submitting contributions. All contributions must adhere to our [code of conduct](.github/code_of_conduct.md).

## Open Payments Catchup Call

Our catchup calls are open to our community. We have them every other Wednesday at 13:00 GMT, via Google Meet.

Video call link: https://meet.google.com/htd-eefo-ovn

Or dial: (DE) +49 30 300195061 and enter this PIN: 105 520 503#

More phone numbers: https://tel.meet/htd-eefo-ovn?hs=5

[Add to Google Calendar](https://calendar.google.com/calendar/event?action=TEMPLATE&tmeid=MDNjYTdhYmE5MTgwNGJhMmIxYmU0YWFkMzI2NTFmMjVfMjAyNDA1MDhUMTIwMDAwWiBjX2NqMDI3Z21oc3VqazkxZXZpMjRkOXB2bXQ0QGc&tmsrc=c_cj027gmhsujk91evi24d9pvmt4%40group.calendar.google.com&scp=ALL)

## Local Development Environment

This repository contains a Git submodule, which contains the Open Payments OpenAPI specifications.
After cloning, make sure to initialize and update it:

```bash
git submodule update --init
```

Alternatively, clone the repository with submodules in one step:

```bash
git clone --recurse-submodules git@github.com:interledger/open-payments-java.git
```

### Prerequisites

- [Java Development Kit (JDK)](https://sdkman.io/jdks/) v21+ via SdkMan
- Gradle v9.0+ 

### Environment Setup

Maven dependency (Please see [Maven Central](https://central.sonatype.com/artifact/org.interledger/open-payments) or [Maven Repository](https://mvnrepository.com/artifact/org.interledger/open-payments) for the latest version):
```xml
<dependency>
    <groupId>org.interledger</groupId>
    <artifactId>open-payments</artifactId>
    <version>1.3.0</version>
</dependency>
```

### Open Payments specification support

The SDK implements [Open Payments specification v1.4.0](https://github.com/interledger/open-payments-specifications/releases/tag/v1.4.0). New in v1.4.0:

- Card (EMV) authorization on outgoing payment grants

Added from earlier specification versions (v1.1 to v1.3):

- Spent amounts for the current outgoing payment grant (`GET /outgoing-payment-grant`)
- Wallet address ownership verification using a `subject` grant
- Outgoing payment grants with `interval` limits (future and recurring payments)
- Client identification using the `walletAddress` object form (default) or a directed identity `jwk`

**Upgrade notes:**

- Grant requests now identify the client as `{"walletAddress": "..."}` instead of a plain string.
  For authorization servers that only accept the string form, use
  `OpenPaymentsHttpClient.defaultClient(Client.walletAddressString(uri), PRIVATE_KEY_PEM, KEY_ID)`.
- A directed identity (`jwk`) client can only be used for non-interactive grants (e.g. incoming payments).
- Request targets are no longer given a trailing `/` (e.g. `POST /incoming-payments`, not `/incoming-payments/`).
- Deprecated: `continuation(sender, debitAmount, …)` and `finalize(OutgoingPayment, …)`. Use
  `continuation(sender, Limits.debit(debitAmount), …)`, which returns a `PendingGrant`, and `finalize(PendingGrant, …)`.
- Deprecated: `OutgoingPayment.getReceivedAmount()`. Use `getReceiveAmount()`.

```java
var client = OpenPaymentsHttpClient.defaultClient(CLIENT_WALLET_ADDRESS, PRIVATE_KEY_PEM, KEY_ID);
var userWallet = client.walletAddress().get(WALLET_ADDRESS);

// Request an outgoing payment grant for future payments: 100.00 CAD per month, 3 times.
var limits = Limits.debit(Amount.build(new BigDecimal("100.00"), "CAD"), "R3/2025-05-20T13:00:00Z/P1M");
var pendingPayment = client.auth().grant().continuation(userWallet, limits, RETURN_URL, NONCE);
// ...redirect the user to pendingPayment.getInteract().getRedirect(), then:
var outgoingPaymentGrant = client.auth().grant().finalize(pendingPayment, INTERACT_REF);

// Get the spent amounts for the current outgoing payment grant (current interval for recurring grants).
var spent = client.payment().getGrantSpentAmounts(userWallet, outgoingPaymentGrant);
System.out.println(spent.getSpentDebitAmount());

// Verify that the user has access to their wallet address.
var pendingVerification = client.auth().grant().verifyOwnership(userWallet, RETURN_URL, NONCE);
// ...redirect the user to pendingVerification.getInteract().getRedirect(), then:
var verified = client.auth().grant().finalize(pendingVerification, INTERACT_REF);
System.out.println(verified.getSubject().getSubIds().getFirst().getId());
```

Gradle commands:
```sh
# Clean build (Linux)
./gradlew clean build

# Run unit tests only
./gradlew test
```

### Useful commands

```sh

# 1. Run integration tests only:
./gradlew integrationTest

# 2. Publish to local Maven repository:
./gradlew clean build publish

# 3. Release to Maven Central:
# Set the GPG key for signing the artifacts.
export JRELEASER_GPG_PUBLIC_KEY="$(cat public.key.asc)"
export JRELEASER_GPG_SECRET_KEY="$(cat private.key.asc)"

# Run the tasks in order to release.
./gradlew clean build publish
./gradlew jreleaserDeploy
```
