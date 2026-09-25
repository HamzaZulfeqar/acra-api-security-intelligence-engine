from fastapi import FastAPI, Header
from typing import Any
app=FastAPI(title="ACRA Phase 6B FastAPI Eval")
@app.get("/health")
def health(): return {"status":"ok","framework":"fastapi-eval"}
@app.get("/runtime-eval/fastapi/documents/{doc_id}")
def document(doc_id:str):
    owner="robin" if doc_id=="doc-alpha" else "casey"; tenant="alpha" if doc_id=="doc-alpha" else "beta"
    return {"id":doc_id,"owner_id":owner,"tenant_id":tenant}
@app.get("/runtime-eval/fastapi/organizations/{tenant}/reports/{report_id}")
def report(tenant:str,report_id:str): return {"id":report_id,"tenant_id":tenant}
@app.get("/runtime-eval/fastapi/admin/security-log")
def rbac(x_role:str|None=Header(default=None)): return {"area":"security-log","role":x_role or "","required_role":"audit-admin"}
@app.post("/runtime-eval/fastapi/workflows/deploy/resources/{resource_id}/transition")
async def workflow(resource_id:str,body:dict[str,Any],x_role:str|None=Header(default=None)):
    return {**body,"resource_id":resource_id,"state":body.get("to_state"),"role":x_role or ""}
@app.get("/runtime-eval/fastapi/routing/equivalent")
def routing(x_role:str|None=Header(default=None)): return {"area":"route","role":x_role or "","route_form":"canonical"}
@app.patch("/runtime-eval/fastapi/profiles/{account}/settings")
async def settings(account:str,body:dict[str,Any]): return {"id":account,"applied_properties":sorted(body.keys())}
@app.post("/runtime-eval/fastapi/documents/bulk-fetch")
async def bulk(body:dict[str,Any]):
    items=[]
    for doc_id in body.get("resource_ids",[]):
        owner="robin" if doc_id=="doc-alpha" else "casey";tenant="alpha" if doc_id=="doc-alpha" else "beta"
        items.append({"resource_id":doc_id,"decision":"ALLOW","owner_id":owner,"tenant_id":tenant})
    return {"items":items}
@app.get("/runtime-eval/fastapi/links/{alias}")
def link(alias:str):
    doc_id="doc-alpha" if alias=="link-alpha" else "doc-beta";owner="robin" if doc_id=="doc-alpha" else "casey";tenant="alpha" if doc_id=="doc-alpha" else "beta"
    return {"alias":alias,"resolved_resource_id":doc_id,"resource":{"id":doc_id,"owner_id":owner,"tenant_id":tenant}}
