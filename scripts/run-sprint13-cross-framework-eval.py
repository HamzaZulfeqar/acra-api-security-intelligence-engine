#!/usr/bin/env python3
from __future__ import annotations
import hashlib, importlib.util, json, subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-XFRAME-EVAL-FEATURES.json"
REGISTRY=ROOT/"lab/ground-truth/POL-S13-XFRAME-EVAL-001.json"
NORM=ROOT/"scripts/sprint13_cross_framework_normalization.py"
DIM=ROOT/"scripts/sprint13_dimension_inference.py"
POL=ROOT/"scripts/sprint13_policy_semantics.py"
GOV=ROOT/"scripts/sprint13_uncertainty_governance.py"
S12=ROOT/"scripts/run-sprint12-ablation.py"
OUT=ROOT/"build/s13-xframe-eval"; RESULT=OUT/"predictions.json"; ROWS=OUT/"predictions.jsonl"
FREEZE="4011b9c05b99b14da66733aea47de43256060990"
def load(p,n):
    s=importlib.util.spec_from_file_location(n,p); m=importlib.util.module_from_spec(s); assert s.loader is not None; s.loader.exec_module(m); return m
N=load(NORM,"xn");D=load(DIM,"xd");P=load(POL,"xp");G=load(GOV,"xg");A7=load(S12,"xa")
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def head(): return subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
def stack(c,o,reg,norm):
    source={k:v for k,v in c.items() if k not in ("observation","frameworkStyle")}
    if norm: source=N.normalize_case(source); o=N.normalize_observation(o)
    visible=D.observable_case(source); inf=D.infer_dimension(visible,o)
    downstream=dict(visible); downstream["dimension"]=inf["dimension"]
    locked=A7.predict(downstream,o,set(A7.VARIANTS[-1][2])); policy=P.evaluate_policy(reg,inf["dimension"],visible,o)
    gov=G.govern(registry=reg,dimension=inf["dimension"],dimension_confidence=inf["confidence"],case=visible,observation=o,locked_prediction=locked,policy_result=policy)
    return inf,gov
def main():
    data=json.loads(FEATURES.read_text()); reg=P.load_policy_registry(REGISTRY)
    if data.get("createdAfterNormalizationFreeze")!=FREEZE: raise AssertionError("freeze provenance")
    rows=[]
    for c in sorted(data["cases"],key=lambda x:x["caseId"]):
        if any(k in c for k in ("dimension","groundTruth","expectedDisposition")): raise AssertionError("label leak")
        ri,rg=stack(c,c["observation"],reg,False); ni,ng=stack(c,c["observation"],reg,True)
        rows.append({"caseId":c["caseId"],"frameworkStyle":c["frameworkStyle"],"rawDimension":ri["dimension"],"rawDisposition":rg["disposition"],"normalizedDimension":ni["dimension"],"normalizedDisposition":ng["disposition"]})
    OUT.mkdir(parents=True,exist_ok=True)
    artifact={"schemaVersion":"s13-xframe-eval-predictions-v1","sourceCommit":head(),"normalizationFreeze":FREEZE,"featureDatasetId":data["datasetId"],"featureDatasetSha256":sha(FEATURES),"policyRegistrySha256":sha(REGISTRY),"normalizerSha256":sha(NORM),"caseCount":len(rows)}
    RESULT.write_text(json.dumps(artifact,sort_keys=True,separators=(",",":"))+"\n"); ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in rows))
    for p in (RESULT,ROWS): p.with_suffix(p.suffix+".sha256").write_text(sha(p)+"  "+p.name+"\n")
    print(f"SPRINT13_XFRAME_EVAL_PREDICTION PASS cases={len(rows)}")
if __name__=="__main__": main()
