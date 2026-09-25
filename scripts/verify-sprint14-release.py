#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
VERSION=(ROOT/"VERSION").read_text(encoding="utf-8").strip()
OUT=ROOT/"build/release"
BUNDLE=OUT/f"acra-{VERSION}-release-candidate.zip"
MANIFEST=OUT/"release-manifest.json"
SUMS=OUT/"SHA256SUMS"
SPRINT13_FREEZE="d6797222d31984a45fbc80674193d0388757b24c"
NS={"m":"http://maven.apache.org/POM/4.0.0"}

def sha256(path: Path) -> str:
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(1024*1024),b""):
            h.update(chunk)
    return h.hexdigest()

def pom_version(path: Path) -> str:
    root=ET.parse(path).getroot()
    value=root.findtext("m:version",namespaces=NS)
    if value:
        return value.strip()
    parent=root.find("m:parent",NS)
    if parent is None:
        raise AssertionError(f"missing Maven version: {path}")
    value=parent.findtext("m:version",namespaces=NS)
    if not value:
        raise AssertionError(f"missing Maven parent version: {path}")
    return value.strip()

def main() -> None:
    versions=[
        VERSION,
        pom_version(ROOT/"pom.xml"),
        pom_version(ROOT/"core/pom.xml"),
        pom_version(ROOT/"extension/burp-extension/pom.xml"),
    ]
    if len(set(versions))!=1:
        raise AssertionError(f"version contract mismatch: {versions}")

    for path in (BUNDLE,MANIFEST,SUMS):
        if not path.is_file():
            raise AssertionError(f"release output missing: {path}")

    manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
    assert manifest["version"]==VERSION
    assert manifest["releaseStatus"]=="RELEASE_CANDIDATE"
    assert manifest["sprint13FrozenHead"]==SPRINT13_FREEZE
    assert manifest["publicReleaseEligible"] is False
    assert manifest["licenseStatus"]=="UNRESOLVED_NO_LICENSE_SELECTED"
    assert manifest["externalTargetValidation"]=="NOT_PERFORMED_BEFORE_SPRINT13_FREEZE"
    assert manifest["activeExecutionDefault"]=="DISABLED"
    assert manifest["automaticIssuePublication"]=="NOT_WIRED"

    built=ROOT/f"extension/burp-extension/target/acra-burp-extension-{VERSION}.jar"
    packaged=OUT/f"acra-{VERSION}/{built.name}"
    assert built.is_file() and packaged.is_file()
    if sha256(built)!=sha256(packaged):
        raise AssertionError("packaged extension JAR differs from Maven-built shaded JAR")

    listed={}
    for line in SUMS.read_text(encoding="utf-8").splitlines():
        digest,name=line.split("  ",1)
        listed[name]=digest
    for name in [packaged.name,MANIFEST.name,BUNDLE.name]:
        path=packaged if name==packaged.name else OUT/name
        if listed.get(name)!=sha256(path):
            raise AssertionError(f"SHA256SUMS mismatch for {name}")

    required={
        f"acra-{VERSION}/acra-burp-extension-{VERSION}.jar",
        f"acra-{VERSION}/README.md",
        f"acra-{VERSION}/LICENSE",
        f"acra-{VERSION}/SECURITY.md",
        f"acra-{VERSION}/CHANGELOG.md",
        f"acra-{VERSION}/docs/release/RELEASE_CANDIDATE_{VERSION}.md",
        f"acra-{VERSION}/docs/research/FINAL_CLAIM_BOUNDARY.md",
        f"acra-{VERSION}/docs/research/FINAL_EVIDENCE_MANIFEST.json",
        f"acra-{VERSION}/docs/research/FINAL_REPRODUCIBILITY.md",
        f"acra-{VERSION}/release-manifest.json",
    }
    with zipfile.ZipFile(BUNDLE) as z:
        names=set(z.namelist())
        missing=required-names
        if missing:
            raise AssertionError(f"release bundle missing entries: {sorted(missing)}")
        for info in z.infolist():
            if info.date_time!=(1980,1,1,0,0,0):
                raise AssertionError(f"non-deterministic ZIP timestamp: {info.filename} {info.date_time}")
            if info.filename.startswith("/") or ".." in Path(info.filename).parts:
                raise AssertionError(f"unsafe ZIP path: {info.filename}")

    license_text=(ROOT/"LICENSE").read_text(encoding="utf-8")
    if "No license has been selected yet" not in license_text:
        raise AssertionError("license state changed without Sprint 14 release-policy update")

    frozen=[
        "docs/research",
        "lab/ground-truth",
    ]
    cmd=["git","diff","--exit-code",SPRINT13_FREEZE,"--",*frozen]
    if subprocess.run(cmd,cwd=ROOT,check=False).returncode!=0:
        raise AssertionError("frozen Sprint 13 research evidence changed")

    # Freeze Sprint 13 automation surfaces as historical evidence.
    tracked=subprocess.check_output(["git","ls-files"],cwd=ROOT,text=True).splitlines()
    s13=[p for p in tracked if (
        p.startswith("scripts/run-sprint13-")
        or p.startswith("scripts/verify-sprint13-")
        or p.startswith(".github/workflows/sprint13-")
    )]
    if s13 and subprocess.run(["git","diff","--exit-code",SPRINT13_FREEZE,"--",*s13],cwd=ROOT,check=False).returncode!=0:
        raise AssertionError("Sprint 13 automation evidence changed after freeze")

    print(f"SPRINT14_VERSION_CONTRACT PASS version={VERSION}")
    print("SPRINT14_FROZEN_RESEARCH_LOCK PASS")
    print("SPRINT14_JAR_IDENTITY PASS")
    print("SPRINT14_RELEASE_MANIFEST PASS")
    print("SPRINT14_RELEASE_ZIP PASS")
    print("SPRINT14_RELEASE_CHECKSUMS PASS")
    print("SPRINT14_PUBLIC_RELEASE_BLOCKER PASS license=UNRESOLVED")
    print("SPRINT14_RELEASE_VERIFY PASS")

if __name__=="__main__":
    main()
