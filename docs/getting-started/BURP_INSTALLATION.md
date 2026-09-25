# Installing ACRA in Burp Suite

ACRA v0.3.0 is a Java Burp extension built against Montoya API 2026.7.

## Option A — build the JAR yourself

```bash
git clone --branch v0.3.0 --depth 1 https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine.git
cd acra-api-security-intelligence-engine
mvn --batch-mode --no-transfer-progress clean verify -pl extension/burp-extension -am
```

Use:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0.jar
```

## Option B — use the published release asset

Open:

https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine/releases/tag/v0.3.0

Download:
- `acra-burp-extension-0.3.0.jar`;
- `SHA256SUMS`.

Verify the JAR before loading it.

Published JAR SHA-256:

```text
95e3271328a77fe76e8c66728b4045a5f83ed782cc797382173ce87cc842f22e
```

## Load the Java extension

PortSwigger's current manual-loading procedure for Java extensions is:

1. go to **Extensions > Installed**;
2. click **Add**;
3. select **Java** as the extension type;
4. click **Select file**;
5. choose the ACRA JAR;
6. optionally configure output/error destinations;
7. click **Next**;
8. inspect the **Output** and **Errors** tabs;
9. click **Close**.

Official reference:
https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/loading-in-burp

## Expected successful load

Burp should list the extension as:

```text
ACRA
```

Expected output:

```text
ACRA v0.3.0 initialized in passive observation mode. Active vulnerability scanning is disabled.
```

An **ACRA** suite tab should appear.

## Scope is required

ACRA's safe default is `IN_SCOPE_ONLY`.

A loaded extension can therefore look empty until Burp considers the target in scope.

Add only explicitly authorized targets to Burp scope. PortSwigger documents this at:
https://portswigger.net/burp/documentation/desktop/getting-started/setting-target-scope

## Safety

Loading ACRA does not enable active execution. The stable release defaults to passive observation and keeps active
execution disabled.

Use ACRA only on systems you own or are explicitly authorized to assess.
