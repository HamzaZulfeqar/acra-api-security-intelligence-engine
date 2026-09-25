#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/"docs/research/FINAL_EVIDENCE_MANIFEST.json"
REGISTRY=ROOT/"docs/research/EXPERIMENT_REGISTRY.md"
CLAIMS=ROOT/"docs/research/FINAL_CLAIM_BOUNDARY.md"
OUT=ROOT/"build/s13-final-freeze"

def run(*args: str) -> str:
    return subprocess.check_output(args,cwd=ROOT,text=True).strip()

def sha256(path: Path) -> str:
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(1024*1024),b""):
            h.update(chunk)
    return h.hexdigest()

def main() -> None:
    manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
    if manifest.get("schemaVersion")!="acra-final-research-freeze-v1":
        raise AssertionError("unexpected final-freeze manifest schema")
    if manifest.get("freezeState") not in {"FINAL_CANDIDATE","FROZEN_COMPLETE"}:
        raise AssertionError("invalid freeze state")
    phase8=manifest.get("phase8",{})
    if phase8.get("status")!="NOT_PERFORMED":
        raise AssertionError("Phase 8 must remain NOT_PERFORMED unless a separately registered external experiment exists")
    if "No external-target" not in phase8.get("claimBoundary",""):
        raise AssertionError("external-target claim boundary missing")

    head=run("git","rev-parse","HEAD")
    registry=REGISTRY.read_text(encoding="utf-8")
    experiments=manifest["canonicalExperiments"]
    if len(experiments)!=8:
        raise AssertionError(f"expected 8 canonical completed Sprint 13 experiments, got {len(experiments)}")

    ancestry=[]
    for exp in experiments:
        commit=exp["measuredHead"]
        run("git","cat-file","-e",f"{commit}^{{commit}}")
        rc=subprocess.run(["git","merge-base","--is-ancestor",commit,head],cwd=ROOT,check=False)
        if rc.returncode!=0:
            raise AssertionError(f"canonical measured commit is not in final branch ancestry: {exp['id']} {commit}")
        if exp["id"] not in registry:
            raise AssertionError(f"experiment missing from registry: {exp['id']}")
        ancestry.append({"id":exp["id"],"runId":exp["runId"],"measuredHead":commit,"ancestor":True})

    for rel in manifest["requiredProtocols"]+[manifest["finalClaimBoundaryFile"],manifest["reproducibilityFile"],manifest["finalFreezeFile"]]:
        if not (ROOT/rel).is_file():
            raise AssertionError(f"required freeze document missing: {rel}")

    claim_text=CLAIMS.read_text(encoding="utf-8")
    if "**NOT PERFORMED.**" not in claim_text:
        raise AssertionError("final claim boundary does not explicitly preserve Phase 8 NOT PERFORMED")

    json_paths=[]
    for path in sorted((ROOT/"lab/ground-truth").glob("*.json")):
        if "S13" not in path.name and "POL-S13" not in path.name:
            continue
        json.loads(path.read_text(encoding="utf-8"))
        json_paths.append(path)

    # Preserve the real-Burp implementation measured after the Phase 7 development freeze.
    phase7_freeze="4bb7b952fb5cd0692efc6153d51b6885a7d38f9d"
    phase7_paths=[
        "extension/burp-extension/src/main/java/io/acra/burp/ACRAExtension.java",
        "extension/burp-extension/src/main/java/io/acra/burp/runtime/BurpRuntimeProbe.java",
        "extension/burp-extension/src/main/java/io/acra/burp/traffic/AcraHttpHandler.java",
        "scripts/verify-sprint13-burp-runtime-dev.py",
        "scripts/verify-sprint13-burp-runtime-dev.sh",
    ]
    diff=subprocess.run(["git","diff","--exit-code",phase7_freeze,"--",*phase7_paths],cwd=ROOT,check=False)
    if diff.returncode!=0:
        raise AssertionError("Phase 7 frozen implementation changed after its development freeze")

    tracked=run("git","ls-files").splitlines()
    evidence_files=[]
    for rel in tracked:
        if (
            rel.startswith("docs/research/")
            or rel.startswith("docs/testing/")
            or rel.startswith("lab/ground-truth/")
            or rel.startswith("scripts/verify-sprint13-")
            or rel.startswith("scripts/run-sprint13-")
            or rel.startswith(".github/workflows/sprint13-")
        ):
            p=ROOT/rel
            if p.is_file():
                evidence_files.append({"path":rel,"sha256":sha256(p),"bytes":p.stat().st_size})

    OUT.mkdir(parents=True,exist_ok=True)
    inventory={
        "schemaVersion":"acra-final-evidence-hashes-v1",
        "sourceCommit":head,
        "freezeId":manifest["freezeId"],
        "freezeState":manifest["freezeState"],
        "phase8Status":"NOT_PERFORMED",
        "canonicalExperiments":ancestry,
        "trackedEvidenceFileCount":len(evidence_files),
        "groundTruthJsonCount":len(json_paths),
        "files":evidence_files,
    }
    (OUT/"evidence-hashes.json").write_text(json.dumps(inventory,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
    summary={
        "schemaVersion":"acra-final-freeze-summary-v1",
        "sourceCommit":head,
        "freezeId":manifest["freezeId"],
        "freezeState":manifest["freezeState"],
        "completedCanonicalExperiments":len(experiments),
        "phase8":"NOT_PERFORMED",
        "canonicalHistoryIntegrity":"PASS",
        "phase7ImplementationFreeze":"PASS",
        "researchJsonParse":"PASS",
        "claimBoundary":"PASS",
    }
    (OUT/"freeze-summary.json").write_text(json.dumps(summary,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
    print(f"SPRINT13_FINAL_FREEZE_HISTORY PASS experiments={len(experiments)}")
    print(f"SPRINT13_FINAL_FREEZE_JSON PASS files={len(json_paths)}")
    print("SPRINT13_FINAL_FREEZE_PHASE7_LOCK PASS")
    print("SPRINT13_FINAL_FREEZE_PHASE8_BOUNDARY PASS status=NOT_PERFORMED")
    print(f"SPRINT13_FINAL_FREEZE_HASH_INVENTORY PASS files={len(evidence_files)}")
    print(f"SPRINT13_FINAL_FREEZE_VERIFY PASS state={manifest['freezeState']}")

if __name__=="__main__":
    main()
