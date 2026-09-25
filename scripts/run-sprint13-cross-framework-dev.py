#!/usr/bin/env python3
from __future__ import annotations
import hashlib, importlib.util, json, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-XFRAME-DEV-FEATURES.json"
REGISTRY=ROOT/"lab/ground-truth/POL-S13-XFRAME-DEV-001.json"
NORM=ROOT/"scripts/sprint13_cross_framework_normalization.py"
DIM=ROOT/"scripts/sprint13_dimension_inference.py"
POL=ROOT/"scripts/sprint13_policy_semantics.py"
GOV=ROOT/"scripts/sprint13_uncertainty_governance.py"
S12=ROOT/"scripts/run-sprint12-ablation.py"
OUT=ROOT/"build/s13-xframe-dev"
RESULT=OUT/"predictions.json"
ROWS=OUT/"predictions.jsonl"

def load(path,name):
    spec=importlib.util.spec_from_file_location(name,path)
    mod=importlib.util.module_from_spec(spec); assert spec.loader is not None; spec.loader.exec_module(mod); return mod
N=load(NORM,"xnorm"); D=load(DIM,"xdim"); P=load(POL,"xpol"); G=load(GOV,"xgov"); A7=load(S12,"xa7")
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def canon(v): return json.dumps(v,sort_keys=True,separators=(",",":")).encode()
def head(): return subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()

def run_stack(case,obs,reg,normalize):
    source={k:v for k,v in case.items() if k not in ("observation","frameworkStyle")}
    if normalize:
        source=N.normalize_case(source); obs=N.normalize_observation(obs)
    visible=D.observable_case(source)
    inf=D.infer_dimension(visible,obs)
    downstream=dict(visible); downstream["dimension"]=inf["dimension"]
    locked=A7.predict(downstream,obs,set(A7.VARIANTS[-1][2]))
    policy=P.evaluate_policy(reg,inf["dimension"],visible,obs)
    governed=G.govern(registry=reg,dimension=inf["dimension"],dimension_confidence=inf["confidence"],case=visible,observation=obs,locked_prediction=locked,policy_result=policy)
    return inf,locked,policy,governed

def main():
    data=json.loads(FEATURES.read_text(encoding="utf-8"))
    reg=P.load_policy_registry(REGISTRY)
    rows=[]
    for c in sorted(data["cases"],key=lambda x:x["caseId"]):
        if any(k in c for k in ("dimension","groundTruth","expectedDisposition")): raise AssertionError("label leak")
        raw_inf,_,_,raw_gov=run_stack(c,c["observation"],reg,False)
        norm_inf,locked,policy,norm_gov=run_stack(c,c["observation"],reg,True)
        rows.append({
          "caseId":c["caseId"],"frameworkStyle":c["frameworkStyle"],
          "rawDimension":raw_inf["dimension"],"rawConfidence":raw_inf["confidence"],"rawDisposition":raw_gov["disposition"],
          "normalizedDimension":norm_inf["dimension"],"normalizedConfidence":norm_inf["confidence"],
          "normalizedDisposition":norm_gov["disposition"],"actionableFinding":norm_gov["actionableFinding"],
          "reviewRequired":norm_gov["reviewRequired"],"policyStatus":policy["status"],"policyDecision":policy["decision"],
          "lockedA7Prediction":locked["prediction"]
        })
    OUT.mkdir(parents=True,exist_ok=True)
    artifact={"schemaVersion":"s13-xframe-dev-predictions-v1","sourceCommit":head(),"featureDatasetId":data["datasetId"],
      "featureDatasetSha256":sha(FEATURES),"policyRegistrySha256":sha(REGISTRY),"normalizerSha256":sha(NORM),
      "frozenDimensionSha256":sha(DIM),"frozenPolicySha256":sha(POL),"frozenGovernanceSha256":sha(GOV),
      "caseCount":len(rows),"boundary":{"labelsAvailableToPredictor":False,"frameworkLabelUsedForDecision":False}}
    RESULT.write_bytes(canon(artifact)+b"\n")
    ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in rows),encoding="utf-8")
    for p in (RESULT,ROWS): p.with_suffix(p.suffix+".sha256").write_text(sha(p)+"  "+p.name+"\n",encoding="utf-8")
    print(f"SPRINT13_XFRAME_DEV_PREDICTION PASS cases={len(rows)}")

if __name__=="__main__": main()
