#!/usr/bin/env python3
from __future__ import annotations
import json, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-BURP-EVAL-FEATURES.json"
LABELS=ROOT/"lab/ground-truth/GT-S13-BURP-EVAL-LABELS.json"

probe=Path(sys.argv[1])
output=Path(sys.argv[2])

features=json.loads(FEATURES.read_text(encoding="utf-8"))
labels=json.loads(LABELS.read_text(encoding="utf-8"))
events=[json.loads(x) for x in probe.read_text(encoding="utf-8").splitlines() if x.strip()]

raw=probe.read_text(encoding="utf-8")
for forbidden in ("Authorization","Bearer ","synthetic-cookie-secret"):
    if forbidden in raw:
        raise AssertionError(f"secret-bearing material leaked into probe: {forbidden}")

errors=[e for e in events if e.get("event")=="ERROR"]
if errors:
    raise AssertionError(f"Burp/ACRA runtime probe recorded errors: {errors}")

init=[e for e in events if e.get("event")=="INITIALIZED"]
requests=[e for e in events if e.get("event")=="REQUEST"]
responses=[e for e in events if e.get("event")=="RESPONSE"]
processed=[e for e in events if e.get("event")=="PROCESSED"]

if len(init)!=1:
    raise AssertionError(f"expected exactly one ACRA initialization, got {len(init)}")
if len(requests)!=2 or len(responses)!=2 or len(processed)!=2:
    raise AssertionError(f"expected 2 request/response/processed events, got {len(requests)}/{len(responses)}/{len(processed)}")

feature_paths={c["path"] for c in features["cases"]}
request_paths={e.get("path") for e in requests}
if request_paths != feature_paths:
    raise AssertionError(f"request path mismatch: {request_paths} != {feature_paths}")

for event in requests:
    if event.get("host")!="127.0.0.1" or event.get("port")!=18502 or event.get("secure") is not False:
        raise AssertionError(f"non-local or unexpected request evidence: {event}")
for event in responses:
    if event.get("status")!=200 or event.get("tool")!="PROXY":
        raise AssertionError(f"response was not a successful real Burp Proxy callback: {event}")

by_resource={e.get("resourceId"):e for e in processed}
results=[]
for item in labels["cases"]:
    expected=item["expected"]
    actual=by_resource.get(expected["resourceId"])
    if actual is None:
        raise AssertionError(f"missing processed context for {expected['resourceId']}")
    checks={
        "principal":actual.get("principal")==expected["principal"],
        "tenant":actual.get("tenant")==expected["tenant"],
        "resourceId":actual.get("resourceId")==expected["resourceId"],
        "action":actual.get("action")==expected["action"],
        "responseStatus":actual.get("responseStatus")==expected["responseStatus"],
        "contextResolved":actual.get("contextStatus")=="RESOLVED",
        "observationObserved":actual.get("observationStatus")=="OBSERVED",
    }
    if not all(checks.values()):
        raise AssertionError(f"context mismatch for {item['caseId']}: {checks}; actual={actual}")
    results.append({
        "caseId":item["caseId"],
        "principal":actual.get("principal"),
        "tenant":actual.get("tenant"),
        "resourceType":actual.get("resourceType"),
        "resourceId":actual.get("resourceId"),
        "action":actual.get("action"),
        "responseStatus":actual.get("responseStatus"),
        "contextStatus":actual.get("contextStatus"),
        "observationStatus":actual.get("observationStatus"),
    })

summary={
    "schemaVersion":"s13-burp-eval-v1",
    "featureDatasetId":features["datasetId"],
    "labelSetId":labels["labelSetId"],
    "realBurpDesktop":True,
    "montoyaInitialization":True,
    "proxyRequestCallbacks":len(requests),
    "proxyResponseCallbacks":len(responses),
    "pipelineProcessed":len(processed),
    "contextsCorrect":len(results),
    "cases":sorted(results,key=lambda x:x["caseId"]),
    "claimBoundary":[
        "Burp Suite Community Edition 2026.7.3 controlled localhost runtime only",
        "no external target validation",
        "no active ACRA request execution",
    ],
}
output.parent.mkdir(parents=True,exist_ok=True)
output.write_text(json.dumps(summary,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
print(f"SPRINT13_BURP_EVAL_RESULT INIT=1 REQUEST={len(requests)} RESPONSE={len(responses)} PROCESSED={len(processed)} CONTEXT={len(results)}/2")
print("SPRINT13_BURP_EVAL_VERIFY PASS")
