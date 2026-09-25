#!/usr/bin/env python3
"""Phase 3 untouched evaluation verifier."""
from __future__ import annotations
import hashlib, json, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-POLICY-EVAL-FEATURES.json"
LABELS=ROOT/"lab/ground-truth/GT-S13-POLICY-EVAL-LABELS.json"
REGISTRY=ROOT/"lab/ground-truth/POL-S13-EVAL-001.json"
RESULT=ROOT/"build/s13-policy-eval/predictions.json"
ROWS=ROOT/"build/s13-policy-eval/predictions.jsonl"
EVAL=ROOT/"build/s13-policy-eval/evaluation.json"
EVAL_ROWS=ROOT/"build/s13-policy-eval/evaluation-cases.jsonl"

def sha256(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def canonical(v): return json.dumps(v,sort_keys=True,separators=(",",":")).encode()

def metrics(rows,key):
    tp=tn=fp=fn=0
    for r in rows:
        t=r["groundTruth"]; p=r[key]
        if t=="POSITIVE" and p=="POSITIVE": tp+=1
        elif t=="NEGATIVE" and p=="NEGATIVE": tn+=1
        elif t=="NEGATIVE" and p=="POSITIVE": fp+=1
        elif t=="POSITIVE" and p=="NEGATIVE": fn+=1
        else: raise AssertionError("invalid binary label")
    precision=None if tp+fp==0 else tp/(tp+fp)
    recall=None if tp+fn==0 else tp/(tp+fn)
    f1=None if precision is None or recall is None or precision+recall==0 else 2*precision*recall/(precision+recall)
    return {"tp":tp,"tn":tn,"fp":fp,"fn":fn,"precision":precision,"recall":recall,"f1":f1}

def main():
    features=json.loads(FEATURES.read_text(encoding="utf-8"))
    labels=json.loads(LABELS.read_text(encoding="utf-8"))
    result=json.loads(RESULT.read_text(encoding="utf-8"))
    rows=[json.loads(x) for x in ROWS.read_text(encoding="utf-8").splitlines() if x.strip()]
    if labels.get("untouchedEvaluation") is not True: raise AssertionError("untouched-evaluation marker required")
    if labels.get("algorithmFreeze")!="c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd": raise AssertionError("label algorithm-freeze provenance drift")
    if labels.get("featureDatasetId")!=features.get("datasetId"): raise AssertionError("feature/label mismatch")
    if result.get("algorithmFreeze")!="c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd": raise AssertionError("prediction freeze provenance drift")
    if result.get("featureDatasetId")!=features.get("datasetId"): raise AssertionError("prediction dataset mismatch")
    if result.get("featureDatasetSha256")!=sha256(FEATURES): raise AssertionError("feature digest drift")
    if result.get("policyRegistrySha256")!=sha256(REGISTRY): raise AssertionError("registry digest drift")
    if len(rows)!=24: raise AssertionError("expected 24 evaluation predictions")
    head=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    if result.get("sourceCommit")!=head: raise AssertionError("prediction commit drift")
    by_label={x["caseId"]:x for x in labels["cases"]}
    if len(by_label)!=24: raise AssertionError("expected 24 evaluation labels")
    joined=[]
    for r in rows:
        label=by_label.get(r["caseId"])
        if label is None: raise AssertionError("unknown case")
        expected_policy="DENY" if label["groundTruth"]=="POSITIVE" else "ALLOW"
        joined.append({**r,**label,"expectedPolicyDecision":expected_policy,"policyDecisionCorrect":r["policyDecision"]==expected_policy})
    locked=metrics(joined,"lockedA7Prediction")
    generalized=metrics(joined,"generalizedG1Prediction")
    dim_correct=sum(1 for r in joined if r["inferredDimension"]==r["registeredDimension"])
    policy_correct=sum(1 for r in joined if r["policyDecisionCorrect"])
    unknown=sum(1 for r in joined if r["policyDecision"]=="UNKNOWN")
    eval_obj={
        "schemaVersion":"s13-policy-eval-evaluation-v1",
        "sourceCommit":head,
        "algorithmFreeze":"c7c66336daf050753aa0ac4fb3a1f293c5d1e2dd",
        "caseCount":len(joined),
        "dimensionAccuracy":dim_correct/len(joined),
        "policyDecisionAccuracy":policy_correct/len(joined),
        "unknownPolicyCount":unknown,
        "lockedA7":locked,
        "generalizedG1":generalized,
        "claimBoundary":[
            "untouched evaluation corpus created after algorithm freeze",
            "no algorithm tuning is permitted after this evaluation is observed",
            "results remain synthetic internal evidence, not production accuracy",
            "evaluation registry is explicit configured policy, not inferred application policy"
        ]
    }
    EVAL.write_bytes(canonical(eval_obj)+b"\n")
    EVAL_ROWS.write_text("".join(json.dumps(x,sort_keys=True,separators=(",",":"))+"\n" for x in joined),encoding="utf-8")
    for p in (RESULT,ROWS,EVAL,EVAL_ROWS):
        if p in (EVAL,EVAL_ROWS):
            p.with_suffix(p.suffix+".sha256").write_text(sha256(p)+"  "+p.name+"\n",encoding="utf-8")
        side=p.with_suffix(p.suffix+".sha256")
        if sha256(p)!=side.read_text(encoding="utf-8").split()[0]: raise AssertionError("sha mismatch")
    blob=RESULT.read_text(encoding="utf-8")+ROWS.read_text(encoding="utf-8")+EVAL.read_text(encoding="utf-8")+EVAL_ROWS.read_text(encoding="utf-8")
    for forbidden in ("Bearer ","\"Authorization\":","synthetic-cookie-secret"):
        if forbidden in blob: raise AssertionError("secret-like material leaked")
    print(f"SPRINT13_POLICY_EVAL_RESULT DIM={dim_correct}/24 POLICY={policy_correct}/24 UNKNOWN={unknown}")
    print(f"SPRINT13_POLICY_EVAL_A7 TP={locked['tp']} TN={locked['tn']} FP={locked['fp']} FN={locked['fn']} P={locked['precision']} R={locked['recall']} F1={locked['f1']}")
    print(f"SPRINT13_POLICY_EVAL_G1 TP={generalized['tp']} TN={generalized['tn']} FP={generalized['fp']} FN={generalized['fn']} P={generalized['precision']} R={generalized['recall']} F1={generalized['f1']}")
    print("SPRINT13_POLICY_EVAL_VERIFY PASS")

if __name__=="__main__": main()
