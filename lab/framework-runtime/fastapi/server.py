from fastapi import FastAPI, Header
from pydantic import BaseModel
from typing import Any

app = FastAPI(title="ACRA Phase 6B FastAPI Runtime")

def actor(x_user_id: str|None, x_tenant_id: str|None, x_role: str|None):
    return {"sub": x_user_id or "", "tenant_id": x_tenant_id or "", "role": x_role or ""}

@app.get("/health")
def health(): return {"status":"ok","framework":"fastapi"}

@app.get("/runtime/fastapi/assets/{asset_id}")
def asset(asset_id: str):
    owner = "alice" if asset_id == "asset-red" else "bob"
    tenant = "red" if asset_id == "asset-red" else "blue"
    return {"id":asset_id,"owner_id":owner,"tenant_id":tenant}

@app.get("/runtime/fastapi/workspaces/{tenant}/reports/{report_id}")
def report(tenant: str, report_id: str):
    return {"id":report_id,"tenant_id":tenant}

@app.get("/runtime/fastapi/management/audit")
def audit(x_role: str|None=Header(default=None)):
    return {"area":"audit","role":x_role or "","required_role":"security-admin"}

@app.post("/runtime/fastapi/workflows/release/resources/{resource_id}/transition")
async def workflow(resource_id: str, body: dict[str,Any], x_role: str|None=Header(default=None)):
    return {**body,"resource_id":resource_id,"state":body.get("to_state"),"role":x_role or ""}

@app.get("/runtime/fastapi/routing/audit")
def routing(x_role: str|None=Header(default=None)):
    return {"area":"audit","role":x_role or "","route_form":"canonical"}

@app.patch("/runtime/fastapi/accounts/{account}/preferences")
async def preferences(account: str, body: dict[str,Any]):
    return {"id":account,"applied_properties":sorted(body.keys())}

@app.post("/runtime/fastapi/assets/bulk-read")
async def bulk(body: dict[str,Any]):
    items=[]
    for asset_id in body.get("resource_ids",[]):
        owner="alice" if asset_id=="asset-red" else "bob"
        tenant="red" if asset_id=="asset-red" else "blue"
        items.append({"resource_id":asset_id,"decision":"ALLOW","owner_id":owner,"tenant_id":tenant})
    return {"items":items}

@app.get("/runtime/fastapi/references/{alias}")
def reference(alias: str):
    asset_id="asset-red" if alias=="ref-red" else "asset-blue"
    owner="alice" if asset_id=="asset-red" else "bob"
    tenant="red" if asset_id=="asset-red" else "blue"
    return {"alias":alias,"resolved_resource_id":asset_id,"resource":{"id":asset_id,"owner_id":owner,"tenant_id":tenant}}
