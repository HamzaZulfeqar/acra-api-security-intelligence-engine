#!/usr/bin/env python3
from __future__ import annotations
import hashlib, importlib.util, json, subprocess, urllib.error, urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-XRUNTIME-DEV-FEATURES.json"
REGISTRY=ROOT/"lab/ground-truth/POL-S13-XRUNTIME-DEV-001.json"
NORM=ROOT/"scripts/sprint13_cross_framework_normalization.py"
DIM=ROOT/"scripts/sprint13_dimension_inference.py"
POL=ROOT/"scripts/sprint13_policy_semantics.py"
GOV=ROOT/"scripts/sprint13_uncertainty_governance.py"
S12=ROOT/"scripts/run-sprint12-ablation.py"
OUT=ROOT/"build/s13-xruntime-dev"; RESULT=OUT/"predictions.json"; ROWS=OUT/"predictions.jsonl"
PORTS={"FASTAPI":18201,"FLASK":18202,"EXPRESS":18203,"SPRING":18204}

def load(p,n):
    s=importlib.util.spec_from_file_location(n,p);m=importlib.util.module_from_spec(s);assert s.loader is not None;s.loader.exec_module(m);return m
N=load(NORM,"rn");D=load(DIM,"rd");P=load(POL,"rp");G=load(GOV,"rg");A7=load(S12,"ra")
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def head():return subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
def headers(actor):
    return {"Content-Type":"application/json","X-User-Id":str(actor.get("sub") or actor.get("userId") or actor.get("principalId") or ""),"X-Tenant-Id":str(actor.get("tenant_id") or actor.get("tenantId") or ""),"X-Role":str(actor.get("role") or "")}
def request_live(case):
    url=f"http://127.0.0.1:{PORTS[case['frameworkStyle']]}{case['path']}"
    data=None if "requestBody" not in case else json.dumps(case["requestBody"],separators=(",",":")).encode()
    req=urllib.request.Request(url,data=data,headers=headers(case.get("actor",{})),method=case["method"])
    try:
        with urllib.request.urlopen(req,timeout=5) as resp:
            raw=resp.read().decode("utf-8");status=resp.status
    except urllib.error.HTTPError as exc:
        raw=exc.read().decode("utf-8");status=exc.code
    body=json.loads(raw) if raw else {}
    return {"status":status,"body":body}
def main():
    data=json.loads(FEATURES.read_text());reg=P.load_policy_registry(REGISTRY);rows=[]
    for c in sorted(data["cases"],key=lambda x:x["caseId"]):
        if any(k in c for k in ("dimension","groundTruth","expectedDisposition")):raise AssertionError("label leak")
        obs_raw=request_live(c)
        base={k:v for k,v in c.items() if k!="frameworkStyle"}
        base=N.normalize_case(base);obs=N.normalize_observation(obs_raw)
        visible=D.observable_case(base);inf=D.infer_dimension(visible,obs)
        downstream=dict(visible);downstream["dimension"]=inf["dimension"]
        locked=A7.predict(downstream,obs,set(A7.VARIANTS[-1][2]));policy=P.evaluate_policy(reg,inf["dimension"],visible,obs)
        gov=G.govern(registry=reg,dimension=inf["dimension"],dimension_confidence=inf["confidence"],case=visible,observation=obs,locked_prediction=locked,policy_result=policy)
        rows.append({"caseId":c["caseId"],"frameworkStyle":c["frameworkStyle"],"httpStatus":obs_raw["status"],"capturedBody":obs_raw["body"],"inferredDimension":inf["dimension"],"dimensionConfidence":inf["confidence"],"policyDecision":policy["decision"],"policyStatus":policy["status"],"disposition":gov["disposition"],"actionableFinding":gov["actionableFinding"]})
    OUT.mkdir(parents=True,exist_ok=True)
    artifact={"schemaVersion":"s13-xruntime-dev-predictions-v1","sourceCommit":head(),"featureDatasetId":data["datasetId"],"featureDatasetSha256":sha(FEATURES),"policyRegistrySha256":sha(REGISTRY),"normalizerSha256":sha(NORM),"caseCount":len(rows),"transport":{"actualLocalHttp":True,"ports":PORTS}}
    RESULT.write_text(json.dumps(artifact,sort_keys=True,separators=(",",":"))+"\n");ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in rows))
    for p in (RESULT,ROWS):p.with_suffix(p.suffix+".sha256").write_text(sha(p)+"  "+p.name+"\n")
    print(f"SPRINT13_XRUNTIME_DEV_PREDICTION PASS live_http_cases={len(rows)}")
if __name__=="__main__":main()
