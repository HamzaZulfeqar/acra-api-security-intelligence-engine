# Troubleshooting ACRA v0.3.0

## The JAR does not exist after cloning

Run:

```bash
java -version
mvn -version
mvn --batch-mode --no-transfer-progress clean verify -pl extension/burp-extension -am
```

Expected output JAR:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0.jar
```

Java 21 is required by the project build.

## Burp rejects or fails to load the extension

Confirm:
- you selected **Java** as the extension type;
- you selected the shaded ACRA JAR, not a source archive;
- the JAR is `acra-burp-extension-0.3.0.jar`;
- Burp's **Output** and **Errors** tabs do not show a class-loading error.

PortSwigger's extension troubleshooting:
https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/troubleshooting

## ACRA loads but no ACRA tab appears

Open **Extensions > Installed**, select ACRA, and inspect Output/Errors.

Expected startup text:

```text
ACRA v0.3.0 initialized in passive observation mode. Active vulnerability scanning is disabled.
```

If initialization failed, unload ACRA, restart Burp if necessary, then reload the JAR.

## ACRA tab appears but Traffic is empty

This is commonly a scope issue.

ACRA defaults to:

```text
IN_SCOPE_ONLY
```

Verify the authorized target is in Burp suite scope:
- **Target > Site map > right-click target > Add to scope**, or
- configure the target scope in Burp settings.

Then generate a benign request through Burp Proxy.

## Burp Proxy sees traffic but ACRA does not

Check:
1. the target is in Burp suite scope;
2. ACRA remains loaded;
3. the request passed through Burp rather than bypassing the proxy;
4. ACRA Output/Errors contains no passive-pipeline rejection.

## The extension reports the wrong version

For stable v0.3.0 the expected startup text begins:

```text
ACRA v0.3.0 initialized
```

If it reports an older version:
- remove the loaded extension;
- verify which JAR Burp is using;
- rebuild or re-download the stable JAR;
- load the correct file.

## Release asset checksum does not match

Do not load the asset.

Official stable JAR SHA-256:

```text
95e3271328a77fe76e8c66728b4045a5f83ed782cc797382173ce87cc842f22e
```

Use the `SHA256SUMS` file attached to the GitHub Release as the authoritative release checksum set.

## Active execution appears enabled unexpectedly

Stop using the extension for that session and inspect the configuration/build source.

The stable product default is active execution disabled.

## Report a security problem

Follow `SECURITY.md`.

Do not paste real passwords, API keys, tokens, cookies, or personal data into public issues.
