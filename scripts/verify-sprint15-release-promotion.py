#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DECISION=ROOT/"release/promotion-decision.json"
LICENSE=ROOT/"LICENSE"
VERSION=ROOT/"VERSION"
SPRINT14_HEAD="88f2a75f190f6e8853609db2dc9d73929434ad0d"
SPRINT13_FREEZE="d6797222d31984a45fbc80674193d0388757b24c"
NS={"m":"http://maven.apache.org/POM/4.0.0"}

def run(*args: str) -> str:
    return subprocess.check_output(args,cwd=ROOT,text=True).strip()

def pom_version(path: Path) -> str:
    root=ET.parse(path).getroot()
    direct=root.findtext("m:version",namespaces=NS)
    if direct:
        return direct.strip()
    parent=root.find("m:parent",NS)
    if parent is None:
        raise AssertionError(f"missing Maven version: {path}")
    value=parent.findtext("m:version",namespaces=NS)
    if not value:
        raise AssertionError(f"missing Maven parent version: {path}")
    return value.strip()

def main() -> None:
    d=json.loads(DECISION.read_text(encoding="utf-8"))
    if d.get("schemaVersion")!="acra-release-promotion-v1":
        raise AssertionError("unexpected promotion decision schema")
    if d["sourceCandidate"]["sprint14Head"]!=SPRINT14_HEAD:
        raise AssertionError("promotion source candidate drift")
    if d["immutableBoundaries"]["sprint13FrozenHead"]!=SPRINT13_FREEZE:
        raise AssertionError("Sprint 13 freeze boundary drift")
    if d["immutableBoundaries"]["externalTargetValidation"]!="NOT_PERFORMED":
        raise AssertionError("unsupported external validation claim introduced")
    if d["immutableBoundaries"]["productionAccuracyClaim"]!="NOT_SUPPORTED":
        raise AssertionError("unsupported production accuracy claim introduced")

    # Frozen research evidence must remain unchanged.
    frozen=["docs/research","lab/ground-truth"]
    if subprocess.run(["git","diff","--exit-code",SPRINT13_FREEZE,"--",*frozen],cwd=ROOT,check=False).returncode!=0:
        raise AssertionError("frozen Sprint 13 research changed")

    state=d["state"]
    license_state=d["licenseDecision"]["status"]
    version_state=d["versionDecision"]["status"]
    license_text=LICENSE.read_text(encoding="utf-8")
    source_version=VERSION.read_text(encoding="utf-8").strip()

    if state=="PREPARED_BLOCKED":
        if d["publicReleaseAuthorized"] is not False:
            raise AssertionError("blocked state cannot authorize public release")
        if license_state!="UNRESOLVED":
            raise AssertionError("prepared blocked state expects unresolved license")
        if version_state!="UNRESOLVED":
            raise AssertionError("prepared blocked state expects unresolved version decision")
        if "No license has been selected yet" not in license_text:
            raise AssertionError("LICENSE no longer matches unresolved promotion state")
        if source_version!="0.3.0-rc1":
            raise AssertionError("blocked promotion source version must remain 0.3.0-rc1")
        if len(d.get("blockers",[]))<2:
            raise AssertionError("promotion blockers not recorded")
        print("SPRINT15_PROMOTION_STATE PASS state=PREPARED_BLOCKED")
        print("SPRINT15_LICENSE_GATE BLOCKED_EXPECTED status=UNRESOLVED")
        print("SPRINT15_VERSION_DECISION_GATE BLOCKED_EXPECTED status=UNRESOLVED")
    elif state=="READY_FOR_PROMOTION":
        if d["publicReleaseAuthorized"] is not True:
            raise AssertionError("READY_FOR_PROMOTION requires explicit authorization")
        if license_state!="SELECTED" or not d["licenseDecision"].get("spdxIdentifier"):
            raise AssertionError("READY_FOR_PROMOTION requires selected SPDX license")
        if "No license has been selected yet" in license_text:
            raise AssertionError("unresolved LICENSE notice still present")
        if version_state!="SELECTED":
            raise AssertionError("READY_FOR_PROMOTION requires version selection")
        target=d["versionDecision"].get("targetVersion")
        if target not in {"0.3.0-rc1","0.3.0"}:
            raise AssertionError(f"unsupported target version: {target}")
        versions=[
            source_version,
            pom_version(ROOT/"pom.xml"),
            pom_version(ROOT/"core/pom.xml"),
            pom_version(ROOT/"extension/burp-extension/pom.xml"),
        ]
        if len(set(versions))!=1 or versions[0]!=target:
            raise AssertionError(f"promotion version contract mismatch: {versions}, target={target}")
        if d.get("blockers"):
            raise AssertionError("READY_FOR_PROMOTION must have no blockers")
        print(f"SPRINT15_PROMOTION_STATE PASS state=READY_FOR_PROMOTION target={target}")
        print(f"SPRINT15_LICENSE_GATE PASS spdx={d['licenseDecision']['spdxIdentifier']}")
        print("SPRINT15_VERSION_DECISION_GATE PASS")
    else:
        raise AssertionError(f"unsupported pre-publication promotion state: {state}")

    # Sprint 14 source must remain an ancestor of promotion work.
    head=run("git","rev-parse","HEAD")
    if subprocess.run(["git","merge-base","--is-ancestor",SPRINT14_HEAD,head],cwd=ROOT,check=False).returncode!=0:
        raise AssertionError("Sprint 14 hardened head is not an ancestor")

    print("SPRINT15_SPRINT14_ANCESTRY PASS")
    print("SPRINT15_FROZEN_RESEARCH_LOCK PASS")
    print("SPRINT15_PROMOTION_PREFLIGHT PASS")

if __name__=="__main__":
    main()
