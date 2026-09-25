# ACRA Quick Start

This is the shortest supported path from a clean machine to a loaded ACRA extension.

## 1. Clone the stable release

```bash
git clone --branch v0.3.0 --depth 1 https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine.git
cd acra-api-security-intelligence-engine
```

Using the release tag is preferred for reproducibility. Clone `main` only when you intentionally want later
post-`v0.3.0` changes.

## 2. Confirm prerequisites

```bash
java -version
mvn -version
git --version
```

Required:
- Java 21;
- Maven;
- Git.

## 3. Build

```bash
mvn --batch-mode --no-transfer-progress clean verify -pl extension/burp-extension -am
```

Expected JAR:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0.jar
```

## 4. Load in Burp

1. Open Burp Suite.
2. Go to **Extensions > Installed**.
3. Click **Add**.
4. Choose **Java**.
5. Select `extension/burp-extension/target/acra-burp-extension-0.3.0.jar`.
6. Click **Next**.
7. Confirm no fatal load error appears.
8. Close the dialog.

PortSwigger documents this manual-JAR flow at:
https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/loading-in-burp

## 5. Set authorized target scope

ACRA's normal default is `IN_SCOPE_ONLY`; it follows Burp's suite target scope.

Before expecting passive ACRA observations:

1. browse only an application you own or are explicitly authorized to test;
2. in **Target > Site map**, right-click the authorized target;
3. select **Add to scope**;
4. keep unrelated hosts out of scope.

PortSwigger scope guidance:
https://portswigger.net/burp/documentation/desktop/getting-started/setting-target-scope

## 6. Verify ACRA

Expected Burp extension output:

```text
ACRA v0.3.0 initialized in passive observation mode. Active vulnerability scanning is disabled.
```

Expected UI:
- an **ACRA** suite tab;
- Overview;
- Traffic;
- Contexts;
- Endpoints;
- Configuration.

Expected safe defaults:
- scope mode: `IN_SCOPE_ONLY`;
- active execution: disabled;
- active request budget: zero in the product UI;
- mutation budget: zero in the product UI;
- kill switch: engaged.

## 7. Generate one benign observation

With an authorized target in Burp scope:

1. route a benign GET request through Burp Proxy;
2. confirm the request appears in Burp Proxy history;
3. open the ACRA **Traffic** and **Contexts** views;
4. verify the observation appears without enabling active execution.

If traffic does not appear, use the troubleshooting guide.

## Next

- [Burp Installation](BURP_INSTALLATION.md)
- [First-Run Verification](FIRST_RUN_VERIFICATION.md)
- [Troubleshooting](TROUBLESHOOTING.md)
