#!/usr/bin/env python3
from __future__ import annotations

import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
NS={"m":"http://maven.apache.org/POM/4.0.0"}

def pom_version(path: Path) -> str:
    root=ET.parse(path).getroot()
    direct=root.findtext("m:version",namespaces=NS)
    if direct:
        return direct.strip()
    parent=root.find("m:parent",NS)
    if parent is None:
        raise AssertionError(f"missing Maven version in {path}")
    value=parent.findtext("m:version",namespaces=NS)
    if not value:
        raise AssertionError(f"missing parent Maven version in {path}")
    return value.strip()

def main() -> None:
    version=(ROOT/"VERSION").read_text(encoding="utf-8").strip()
    if version!="0.3.0":
        raise AssertionError(f"unexpected public version: {version}")

    versions=[
        version,
        pom_version(ROOT/"pom.xml"),
        pom_version(ROOT/"core/pom.xml"),
        pom_version(ROOT/"extension/burp-extension/pom.xml"),
    ]
    if len(set(versions))!=1:
        raise AssertionError(f"version mismatch: {versions}")

    required=[
        "README.md",
        "LICENSE",
        "NOTICE",
        "SECURITY.md",
        "docs/getting-started/QUICK_START.md",
        "docs/getting-started/BURP_INSTALLATION.md",
        "docs/getting-started/FIRST_RUN_VERIFICATION.md",
        "docs/getting-started/TROUBLESHOOTING.md",
        "docs/operations/GITHUB_REPOSITORY_HARDENING.md",
        ".github/CODEOWNERS",
    ]
    missing=[p for p in required if not (ROOT/p).is_file()]
    if missing:
        raise AssertionError(f"missing onboarding files: {missing}")

    readme=(ROOT/"README.md").read_text(encoding="utf-8")
    required_readme=[
        "## Quick Start",
        "git clone --branch v0.3.0 --depth 1 https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine.git",
        "extension/burp-extension/target/acra-burp-extension-0.3.0.jar",
        "Extensions > Installed",
        "IN_SCOPE_ONLY",
        "ACRA v0.3.0 initialized in passive observation mode",
    ]
    for needle in required_readme:
        if needle not in readme:
            raise AssertionError(f"README onboarding contract missing: {needle}")

    extension=(ROOT/"extension/burp-extension/src/main/java/io/acra/burp/ACRAExtension.java").read_text(encoding="utf-8")
    expected="ACRA v0.3.0 initialized in passive observation mode. Active vulnerability scanning is disabled."
    if expected not in extension:
        raise AssertionError("ACRA startup message does not match v0.3.0 onboarding contract")

    scope=(ROOT/"extension/burp-extension/src/main/java/io/acra/burp/scope/ScopeConfiguration.java").read_text(encoding="utf-8")
    if "ScopeMode.IN_SCOPE_ONLY" not in scope or "10_000,false" not in scope.replace(" ",""):
        raise AssertionError("safe default scope/active execution contract drift")

    license_text=(ROOT/"LICENSE").read_text(encoding="utf-8")
    if "Apache License" not in license_text or "Version 2.0, January 2004" not in license_text:
        raise AssertionError("Apache-2.0 license missing")
    if "Hamza Zulfiqar" not in (ROOT/"NOTICE").read_text(encoding="utf-8"):
        raise AssertionError("NOTICE attribution missing")

    decision=json.loads((ROOT/"release/promotion-decision.json").read_text(encoding="utf-8"))
    if decision.get("state")!="PUBLISHED":
        raise AssertionError("v0.3.0 release is not recorded as PUBLISHED")
    if decision.get("publication",{}).get("gitTag")!="v0.3.0":
        raise AssertionError("published tag contract drift")

    codeowners=(ROOT/".github/CODEOWNERS").read_text(encoding="utf-8").strip()
    if "@HamzaZulfeqar" not in codeowners:
        raise AssertionError("maintainer missing from CODEOWNERS")

    print("SPRINT16_VERSION_CONTRACT PASS version=0.3.0")
    print("SPRINT16_ONBOARDING_DOCS PASS")
    print("SPRINT16_STARTUP_MESSAGE PASS")
    print("SPRINT16_SAFE_DEFAULTS PASS scope=IN_SCOPE_ONLY active=false")
    print("SPRINT16_LICENSE_NOTICE PASS spdx=Apache-2.0")
    print("SPRINT16_PUBLISHED_RELEASE_STATE PASS tag=v0.3.0")
    print("SPRINT16_CODEOWNERS PASS")
    print("SPRINT16_PUBLIC_ONBOARDING_VERIFY PASS")

if __name__=="__main__":
    main()
