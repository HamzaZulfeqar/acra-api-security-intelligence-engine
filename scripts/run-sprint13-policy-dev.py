#!/usr/bin/env python3
"""Phase 3 development prediction: locked A7 vs configurable-policy G1."""
from __future__ import annotations
import hashlib, importlib.util, json, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-POLICY-DEV-FEATURES.json"
REGISTRY=ROOT/"lab/ground-truth/POL-S13-DEV-001.json"
S12_RUNNER=ROOT/"scripts/run-sprint12-ablation.py"
DIM_MODULE=ROOT/"scripts/sprint13_dimension_inference.py"
POLICY_MODULE=ROOT/"scripts/sprint13_policy_semantics.py"
OUT_DIR=ROOT/"build/s13-policy-dev"
RESULT=OUT_DIR/"predictions.json"
ROWS=OUT_DIR/"predictions.jsonl"

def load(path,name):
    spec=importlib.util.spec_from_file_location(name,path)
    module=importlib.util.module_from_spec(spec); assert spec.loader is not None
    spec.loader.exec_module(module); return module

S12=load(S12_RUNNER,"s12_policy_dev")
DIM=load(DIM_MODULE,"dim_policy_dev")
POL=load(POLICY_MODULE,"policy_dev")

def sha256(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def canonical(v): return json.dumps(v,sort_keys=True,separators=(",",":")).encode()
def git_head(): return subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()

def main():
    data=json.loads(FEATURES.read_text(encoding="utf-8"))
    registry=POL.load_policy_registry(REGISTRY)
    if data.get("datasetId")!="GT-S13-POLICY-DEV-FEATURES": raise AssertionError("unexpected dev corpus")
    if data.get("labelSeparated") is not True: raise AssertionError("dev labels must be separated")
    if data.get("policyRegistryId")!=registry.get("registryId"): raise AssertionError("policy registry identity mismatch")
    cases=data.get("cases")
    if not isinstance(cases,list) or len(cases)!=24: raise AssertionError("expected 24 development cases")
    rows=[]
    for case in sorted(cases,key=lambda x:x["caseId"]):
        if any(k in case for k in ("dimension","groundTruth","secureExpected","vulnerableExpected","expectedCandidate")):
            raise AssertionError("development feature leaked labels")
        observation=case.get("observation")
        if not isinstance(observation,dict): raise AssertionError("snapshot observation required")
        case_input={k:v for k,v in case.items() if k!="observation"}
        observable=DIM.observable_case(case_input)
        inference=DIM.infer_dimension(observable,observation)
        downstream=dict(observable); downstream["dimension"]=inference["dimension"]
        a7_caps=set(S12.VARIANTS[-1][2])
        locked=S12.predict(downstream,observation,a7_caps)
        policy=POL.evaluate_policy(registry,inference["dimension"],observable,observation)
        generalized=POL.apply_policy_to_prediction(locked,policy)
        rows.append({
            "caseId":case["caseId"],
            "inferredDimension":inference["dimension"],
            "dimensionConfidence":inference["confidence"],
            "dimensionScore":inference["topScore"],
            "dimensionMargin":inference["margin"],
            "lockedA7Prediction":locked["prediction"],
            "generalizedG1Prediction":generalized["prediction"],
            "observedDecision":locked["observedDecision"],
            "policyDecision":generalized["policyDecision"],
            "policyId":generalized["policyId"],
            "policyReasons":generalized["policyReasons"],
            "lockedReasons":locked["reasons"],
            "generalizedReasons":generalized["reasons"],
        })
    OUT_DIR.mkdir(parents=True,exist_ok=True)
    artifact={
        "schemaVersion":"s13-policy-dev-predictions-v1",
        "sourceCommit":git_head(),
        "featureDatasetId":data["datasetId"],
        "featureDatasetSha256":sha256(FEATURES),
        "policyRegistryId":registry["registryId"],
        "policyRegistrySha256":sha256(REGISTRY),
        "lockedSprint12RunnerSha256":sha256(S12_RUNNER),
        "lockedDimensionEngineSha256":sha256(DIM_MODULE),
        "policyEngineSha256":sha256(POLICY_MODULE),
        "caseCount":len(rows),
        "boundary":{
            "developmentOnly":True,
            "vulnerabilityLabelsAvailableToPredictor":False,
            "registeredDimensionAvailableToPredictor":False,
            "dimensionSource":"automatic-inference",
            "policySource":"explicit-configured-registry",
            "unknownPolicyFallback":"locked-A7"
        }
    }
    RESULT.write_bytes(canonical(artifact)+b"\n")
    ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in rows),encoding="utf-8")
    for p in (RESULT,ROWS):
        p.with_suffix(p.suffix+".sha256").write_text(sha256(p)+"  "+p.name+"\n",encoding="utf-8")
    print(f"SPRINT13_POLICY_DEV_PREDICTION PASS cases={len(rows)} decisive_policy={sum(1 for r in rows if r['policyDecision']!='UNKNOWN')}")

if __name__=="__main__": main()
