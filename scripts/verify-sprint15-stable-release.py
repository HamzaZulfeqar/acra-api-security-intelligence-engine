#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
VERSION=(ROOT/"VERSION").read_text(encoding="utf-8").strip()
OUT=ROOT/"build/release"
STAGE=OUT/f"acra-{VERSION}"
BUNDLE=OUT/f"acra-{VERSION}.zip"

def sha256(path: Path) -> str:
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(1024*1024),b""):
            h.update(chunk)
    return h.hexdigest()

def main() -> None:
    if VERSION!="0.3.0":
        raise AssertionError(f"stable verification requires 0.3.0, got {VERSION}")

    decision=json.loads((ROOT/"release/promotion-decision.json").read_text(encoding="utf-8"))
    if decision["state"] not in {"READY_FOR_PROMOTION","PUBLISHED"}:
        raise AssertionError("promotion decision is neither READY_FOR_PROMOTION nor PUBLISHED")
    if decision["licenseDecision"]["spdxIdentifier"]!="Apache-2.0":
        raise AssertionError("unexpected license")
    if decision["versionDecision"]["targetVersion"]!="0.3.0":
        raise AssertionError("unexpected stable target")

    for required in [ROOT/"LICENSE",ROOT/"NOTICE",OUT/"release-manifest.json",OUT/"SHA256SUMS",BUNDLE]:
        if not required.is_file():
            raise AssertionError(f"required stable-release file missing: {required}")

    license_text=(ROOT/"LICENSE").read_text(encoding="utf-8")
    if "Apache License" not in license_text or "Version 2.0, January 2004" not in license_text or "END OF TERMS AND CONDITIONS" not in license_text:
        raise AssertionError("LICENSE is not the expected Apache-2.0 text")
    notice=(ROOT/"NOTICE").read_text(encoding="utf-8")
    if "ACRA" not in notice or "Hamza Zulfiqar" not in notice:
        raise AssertionError("NOTICE attribution incomplete")

    built=ROOT/"extension/burp-extension/target/acra-burp-extension-0.3.0.jar"
    packaged=STAGE/"acra-burp-extension-0.3.0.jar"
    if not built.is_file() or not packaged.is_file() or sha256(built)!=sha256(packaged):
        raise AssertionError("stable packaged JAR identity mismatch")

    manifest=json.loads((OUT/"release-manifest.json").read_text(encoding="utf-8"))
    assert manifest["version"]=="0.3.0"
    assert manifest["releaseStatus"]=="STABLE"
    assert manifest["license"]["spdx"]=="Apache-2.0"
    assert manifest["publicReleaseEligible"] is True
    assert manifest["externalTargetValidation"]=="NOT_PERFORMED_BEFORE_SPRINT13_FREEZE"

    sums={}
    for line in (OUT/"SHA256SUMS").read_text(encoding="utf-8").splitlines():
        digest,name=line.split("  ",1)
        sums[name]=digest
    for p in [packaged,OUT/"release-manifest.json",BUNDLE]:
        if sums.get(p.name)!=sha256(p):
            raise AssertionError(f"checksum mismatch: {p.name}")

    required_entries={
        "acra-0.3.0/acra-burp-extension-0.3.0.jar",
        "acra-0.3.0/README.md",
        "acra-0.3.0/LICENSE",
        "acra-0.3.0/NOTICE",
        "acra-0.3.0/SECURITY.md",
        "acra-0.3.0/CHANGELOG.md",
        "acra-0.3.0/docs/release/RELEASE_0.3.0.md",
        "acra-0.3.0/docs/research/FINAL_CLAIM_BOUNDARY.md",
        "acra-0.3.0/docs/research/FINAL_EVIDENCE_MANIFEST.json",
        "acra-0.3.0/docs/research/FINAL_REPRODUCIBILITY.md",
        "acra-0.3.0/release/promotion-decision.json",
        "acra-0.3.0/release-manifest.json",
    }
    with zipfile.ZipFile(BUNDLE) as z:
        names=set(z.namelist())
        missing=required_entries-names
        if missing:
            raise AssertionError(f"stable bundle missing: {sorted(missing)}")
        for info in z.infolist():
            if info.date_time!=(1980,1,1,0,0,0):
                raise AssertionError(f"non-deterministic ZIP timestamp: {info.filename}")
            if info.filename.startswith("/") or ".." in Path(info.filename).parts:
                raise AssertionError(f"unsafe ZIP path: {info.filename}")

    print("SPRINT15_STABLE_RELEASE_MANIFEST PASS")
    print("SPRINT15_STABLE_JAR_IDENTITY PASS")
    print("SPRINT15_STABLE_LICENSE_NOTICE PASS")
    print("SPRINT15_STABLE_CHECKSUMS PASS")
    print("SPRINT15_STABLE_ZIP PASS")
    print("SPRINT15_STABLE_RELEASE_VERIFY PASS")

if __name__=="__main__":
    main()
