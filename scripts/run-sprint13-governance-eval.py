#!/usr/bin/env python3
from __future__ import annotations
import hashlib, importlib.util, json, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-GOV-EVAL-FEATURES.json"
REGISTRY=ROOT/"lab/ground-truth/POL-S13-GOV-EVAL-001.json"
S12=ROOT/"scripts/run-sprint12-ablation.py"
DIM=ROOT/"scripts/sprint13_dimension_inference.py"
POL=ROOT/"scripts/sprint13_policy_semantics.py"
GOV=ROOT/"scripts/sprint13_uncertainty_governance.py"
OUT=ROOT/"build/s13-gov-eval"
RESULT=OUT/"predictions.json"
ROWS=OUT/"predictions.jsonl"
FREEZE="11afbb80be24116c9facd0d4b0791f4a12efed3a"

def load(path,name):
    spec=importlib.util.spec_from_file_location(name,path)
    mod=importlib.util.module_from_spec(spec); assert spec.loader is not None; spec.loader.exec_module(mod); return mod
A7=load(S12,"a7_gov_eval"); D=load(DIM,"dim_gov_eval"); P=load(POL,"pol_gov_eval"); G=load(GOV,"gov_eval")
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def canon(v): return json.dumps(v,sort_keys=True,separators=(",",":")).encode()
def head(): return subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()

def main():
    data=json.loads(FEATURES.read_text(encoding="utf-8"))
    reg=P.load_policy_registry(REGISTRY)
    if data.get("createdAfterGovernanceFreeze")!=FREEZE: raise AssertionError("governance freeze provenance drift")
    if data.get("labelSeparated") is not True: raise AssertionError("labels must be separated")
    cases=data.get("cases")
    if not isinstance(cases,list) or len(cases)!=64: raise AssertionError("expected 64 eval cases")
    rows=[]
    for c in sorted(cases,key=lambda x:x["caseId"]):
        if any(k in c for k in ("dimension","groundTruth","expectedDisposition")): raise AssertionError("label leak")
        obs=c["observation"]; base={k:v for k,v in c.items() if k!="observation"}
        visible=D.observable_case(base)
        inf=D.infer_dimension(visible,obs)
        downstream=dict(visible); downstream["dimension"]=inf["dimension"]
        locked=A7.predict(downstream,obs,set(A7.VARIANTS[-1][2]))
        policy=P.evaluate_policy(reg,inf["dimension"],visible,obs)
        legacy=P.apply_policy_to_prediction(locked,policy)
        governed=G.govern(registry=reg,dimension=inf["dimension"],dimension_confidence=inf["confidence"],case=visible,observation=obs,locked_prediction=locked,policy_result=policy)
        rows.append({
            "caseId":c["caseId"],"inferredDimension":inf["dimension"],"dimensionConfidence":inf["confidence"],
            "lockedA7Prediction":locked["prediction"],"legacyG1Prediction":legacy["prediction"],
            "policyDecision":policy["decision"],"policyStatus":policy["status"],"policyId":policy["policyId"],
            "disposition":governed["disposition"],"actionableFinding":governed["actionableFinding"],
            "reviewRequired":governed["reviewRequired"],"governanceReason":governed["reason"],
            "policyHealth":governed["policyHealth"],"missingContext":governed["missingContext"],
            "observedDecision":locked["observedDecision"]
        })
    OUT.mkdir(parents=True,exist_ok=True)
    artifact={"schemaVersion":"s13-gov-eval-predictions-v1","sourceCommit":head(),"governanceFreeze":FREEZE,
      "featureDatasetId":data["datasetId"],"featureDatasetSha256":sha(FEATURES),"policyRegistrySha256":sha(REGISTRY),
      "governanceEngineSha256":sha(GOV),"lockedPolicyEngineSha256":sha(POL),"lockedDimensionEngineSha256":sha(DIM),
      "lockedA7Sha256":sha(S12),"caseCount":len(rows),
      "boundary":{"untouchedEvaluation":True,"labelsAvailableToPredictor":False,"registeredDimensionAvailableToPredictor":False}}
    RESULT.write_bytes(canon(artifact)+b"\n")
    ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in rows),encoding="utf-8")
    for p in (RESULT,ROWS): p.with_suffix(p.suffix+".sha256").write_text(sha(p)+"  "+p.name+"\n",encoding="utf-8")
    print(f"SPRINT13_GOV_EVAL_PREDICTION PASS cases={len(rows)}")

if __name__=="__main__": main()
