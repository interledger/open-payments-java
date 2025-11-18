# Integration Test Configuration Setup
This directory contains configuration files for running integration tests with the OpenPayments client.

## Configuration Files

### test-config.properties.template
This is the default file that shows the required configuration format for the local Rafiki environment. It contains:
- Documentation for each configuration property
- Example values (placeholder values that need to be replaced)
- Security warnings about not committing sensitive information

### test-config.properties (YOU MUST CREATE THIS)
This file contains the actual configuration values needed to run tests. 
It must be created manually by copying the template and filling in real values.

## Setup Instructions

1. **View the default properties file:**
   ```bash
   src/integrationTest/resources/test-config.properties
   ```

2. **Edit the configuration:**
   Open `test-config.properties` and replace the placeholder values with your actual test configuration:
   - Set up your test wallets on the Interledger test network
   - Generate Ed25519 private keys for testing
   - Configure the appropriate wallet addresses

3. **Security Notice:**
   - The `test-config.properties` file is automatically excluded from git via `.gitignore`
   - Never commit this file to version control as it contains sensitive private keys
   - Use only test keys and test wallets, never production credentials

## Required Configuration Properties

- `client.wallet.address`: The wallet address for the payment initiator
- `client.private.key`: Private key in PEM format for request signing
- `client.key.id`: Key ID (UUID) associated with the private key
- `sender.wallet.address`: Test wallet address for payment sender
- `receiver.wallet.address`: Test wallet address for payment receiver

## Usage

Once configured, the test classes will automatically load configuration from `test-config.properties`:

```java
// TestHelper automatically loads from configuration
String walletAddress = TestHelper.getClientWalletAddress();
PrivateKey privateKey = TestHelper.getPrivateKey();
String keyId = TestHelper.getClientKeyId();
```

## Troubleshooting

If you get configuration errors:
1. Ensure `test-config.properties` exists in `src/integrationTest/resources/`
2. Check that all required properties are set
3. Verify the private key is in proper PEM format
4. Confirm wallet addresses are valid URLs

## Test Network Setup

For setting up test wallets, refer to:
- [Interledger Test Network](https://interledger-test.dev)
- [Wallet Documentation](https://wallet.interledger-test.dev)