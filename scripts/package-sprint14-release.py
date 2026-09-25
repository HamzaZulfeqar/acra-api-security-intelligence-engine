#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
VERSION=(ROOT/"VERSION").read_text(encoding="utf-8").strip()
SPRINT13_FREEZE="d6797222d31984a45fbc80674193d0388757b24c"
OUT=ROOT/"build/release"
STAGE=OUT/f"acra-{VERSION}"

NS={"m":"http://maven.apache.org/POM/4.0.0"}

def sha256(path: Path) -> str:
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(1024*1024),b""):
            h.update(chunk)
    return h.hexdigest()

def root_version(path: Path) -> str:
    root=ET.parse(path).getroot()
    value=root.findtext("m:version",namespaces=NS)
    if value:
        return value.strip()
    parent=root.find("m:parent",NS)
    if parent is None:
        raise AssertionError(f"no Maven version found: {path}")
    value=parent.findtext("m:version",namespaces=NS)
    if not value:
        raise AssertionError(f"no parent Maven version found: {path}")
    return value.strip()

def git(*args: str) -> str:
    return subprocess.check_output(["git",*args],cwd=ROOT,text=True).strip()

def copy(rel: str) -> Path:
    src=ROOT/rel
    if not src.is_file():
        raise AssertionError(f"required release file missing: {rel}")
    dst=STAGE/rel
    dst.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(src,dst)
    return dst

def main() -> None:
    versions={
        "VERSION":VERSION,
        "pom.xml":root_version(ROOT/"pom.xml"),
        "core/pom.xml":root_version(ROOT/"core/pom.xml"),
        "extension/burp-extension/pom.xml":root_version(ROOT/"extension/burp-extension/pom.xml"),
    }
    if len(set(versions.values()))!=1:
        raise AssertionError(f"version mismatch: {versions}")

    jar=ROOT/f"extension/burp-extension/target/acra-burp-extension-{VERSION}.jar"
    if not jar.is_file():
        raise AssertionError(f"built shaded extension JAR missing: {jar}")

    if OUT.exists():
        shutil.rmtree(OUT)
    STAGE.mkdir(parents=True)

    jar_dst=STAGE/jar.name
    shutil.copyfile(jar,jar_dst)

    docs=[
        "README.md",
        "LICENSE",
        "SECURITY.md",
        "CHANGELOG.md",
        f"docs/release/RELEASE_CANDIDATE_{VERSION}.md",
        "docs/research/FINAL_CLAIM_BOUNDARY.md",
        "docs/research/FINAL_EVIDENCE_MANIFEST.json",
        "docs/research/FINAL_REPRODUCIBILITY.md",
    ]
    for rel in docs:
        copy(rel)

    source_commit=git("rev-parse","HEAD")
    manifest={
        "schemaVersion":"acra-release-candidate-manifest-v1",
        "product":"ACRA API Access Control & Routing Auditor",
        "version":VERSION,
        "releaseStatus":"RELEASE_CANDIDATE",
        "sourceCommit":source_commit,
        "sprint13FrozenHead":SPRINT13_FREEZE,
        "javaRelease":21,
        "montoyaApi":"2026.7",
        "validatedBurpBoundary":"Community Edition 2026.7.3 controlled localhost",
        "activeExecutionDefault":"DISABLED",
        "automaticIssuePublication":"NOT_WIRED",
        "externalTargetValidation":"NOT_PERFORMED_BEFORE_SPRINT13_FREEZE",
        "licenseStatus":"UNRESOLVED_NO_LICENSE_SELECTED",
        "publicReleaseEligible":False,
        "files":[],
    }
    for path in sorted(p for p in STAGE.rglob("*") if p.is_file()):
        rel=path.relative_to(STAGE).as_posix()
        manifest["files"].append({"path":rel,"sha256":sha256(path),"bytes":path.stat().st_size})

    manifest_path=STAGE/"release-manifest.json"
    manifest_path.write_text(json.dumps(manifest,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")

    bundle=OUT/f"acra-{VERSION}-release-candidate.zip"
    with zipfile.ZipFile(bundle,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for path in sorted(p for p in STAGE.rglob("*") if p.is_file()):
            rel=(Path(STAGE.name)/path.relative_to(STAGE)).as_posix()
            info=zipfile.ZipInfo(rel,date_time=(1980,1,1,0,0,0))
            info.compress_type=zipfile.ZIP_DEFLATED
            info.external_attr=(0o100644 & 0xFFFF)<<16
            z.writestr(info,path.read_bytes(),compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)

    top_manifest=OUT/"release-manifest.json"
    shutil.copyfile(manifest_path,top_manifest)

    sums=[]
    for path in [jar_dst,top_manifest,bundle]:
        sums.append(f"{sha256(path)}  {path.name}")
    (OUT/"SHA256SUMS").write_text("\n".join(sums)+"\n",encoding="utf-8")

    print(f"SPRINT14_PACKAGE PASS version={VERSION}")
    print(f"SPRINT14_PACKAGE_JAR {jar_dst.name} sha256={sha256(jar_dst)}")
    print(f"SPRINT14_PACKAGE_BUNDLE {bundle.name} sha256={sha256(bundle)}")
    print("SPRINT14_PUBLIC_RELEASE_ELIGIBLE false reason=LICENSE_UNRESOLVED")

if __name__=="__main__":
    main()
