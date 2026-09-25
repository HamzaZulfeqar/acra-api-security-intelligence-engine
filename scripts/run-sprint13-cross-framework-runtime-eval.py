#!/usr/bin/env python3
from __future__ import annotations
import hashlib,importlib.util,json,subprocess,urllib.error,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-XRUNTIME-EVAL-FEATURES.json";REGISTRY=ROOT/"lab/ground-truth/POL-S13-XRUNTIME-EVAL-001.json"
NORM=ROOT/"scripts/sprint13_cross_framework_normalization.py";DIM=ROOT/"scripts/sprint13_dimension_inference.py";POL=ROOT/"scripts/sprint13_policy_semantics.py";GOV=ROOT/"scripts/sprint13_uncertainty_governance.py";S12=ROOT/"scripts/run-sprint12-ablation.py"
OUT=ROOT/"build/s13-xruntime-eval";RESULT=OUT/"predictions.json";ROWS=OUT/"predictions.jsonl";PORTS={"FASTAPI":18301,"FLASK":18302,"EXPRESS":18303,"SPRING":18304}
def load(p,n):
 s=importlib.util.spec_from_file_location(n,p);m=importlib.util.module_from_spec(s);assert s.loader is not None;s.loader.exec_module(m);return m
N=load(NORM,"en");D=load(DIM,"ed");P=load(POL,"ep");G=load(GOV,"eg");A7=load(S12,"ea")
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def head():return subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
def hdr(a):return{"Content-Type":"application/json","X-User-Id":str(a.get("sub")or a.get("userId")or a.get("principalId")or""),"X-Tenant-Id":str(a.get("tenant_id")or a.get("tenantId")or""),"X-Role":str(a.get("role")or"")}
def live(c):
 url=f"http://127.0.0.1:{PORTS[c['frameworkStyle']]}{c['path']}";data=None if "requestBody" not in c else json.dumps(c["requestBody"],separators=(",",":")).encode()
 req=urllib.request.Request(url,data=data,headers=hdr(c.get("actor",{})),method=c["method"])
 try:
  with urllib.request.urlopen(req,timeout=5) as resp:raw=resp.read().decode();status=resp.status
 except urllib.error.HTTPError as e:raw=e.read().decode();status=e.code
 return{"status":status,"body":json.loads(raw) if raw else{}}
def main():
 data=json.loads(FEATURES.read_text());reg=P.load_policy_registry(REGISTRY)
 if data.get("createdAfterDevelopmentFreeze")!="683933836d2c2fa5ec155bc8e224be87e6958e95":raise AssertionError("freeze provenance")
 rows=[]
 for c in sorted(data["cases"],key=lambda x:x["caseId"]):
  if any(k in c for k in("dimension","groundTruth","expectedDisposition")):raise AssertionError("label leak")
  raw=live(c);base={k:v for k,v in c.items() if k!="frameworkStyle"};base=N.normalize_case(base);obs=N.normalize_observation(raw);visible=D.observable_case(base);inf=D.infer_dimension(visible,obs)
  downstream=dict(visible);downstream["dimension"]=inf["dimension"];locked=A7.predict(downstream,obs,set(A7.VARIANTS[-1][2]));policy=P.evaluate_policy(reg,inf["dimension"],visible,obs);gov=G.govern(registry=reg,dimension=inf["dimension"],dimension_confidence=inf["confidence"],case=visible,observation=obs,locked_prediction=locked,policy_result=policy)
  rows.append({"caseId":c["caseId"],"frameworkStyle":c["frameworkStyle"],"httpStatus":raw["status"],"capturedBody":raw["body"],"inferredDimension":inf["dimension"],"dimensionConfidence":inf["confidence"],"policyDecision":policy["decision"],"policyStatus":policy["status"],"disposition":gov["disposition"],"actionableFinding":gov["actionableFinding"]})
 OUT.mkdir(parents=True,exist_ok=True);artifact={"schemaVersion":"s13-xruntime-eval-predictions-v1","sourceCommit":head(),"developmentFreeze":"683933836d2c2fa5ec155bc8e224be87e6958e95","featureDatasetId":data["datasetId"],"featureDatasetSha256":sha(FEATURES),"policyRegistrySha256":sha(REGISTRY),"normalizerSha256":sha(NORM),"caseCount":len(rows),"transport":{"actualLocalHttp":True,"ports":PORTS}}
 RESULT.write_text(json.dumps(artifact,sort_keys=True,separators=(",",":"))+"\n");ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in rows))
 for p in(RESULT,ROWS):p.with_suffix(p.suffix+".sha256").write_text(sha(p)+"  "+p.name+"\n")
 print(f"SPRINT13_XRUNTIME_EVAL_PREDICTION PASS live_http_cases={len(rows)}")
if __name__=="__main__":main()
