import os
from flask import Flask, jsonify, request

app=Flask(__name__)

@app.get("/health")
def health(): return jsonify(status="ok",framework="flask")

@app.get("/runtime/flask/assets/<asset_id>")
def asset(asset_id):
    owner="alice" if asset_id=="asset-red" else "bob"
    tenant="red" if asset_id=="asset-red" else "blue"
    return jsonify(id=asset_id,owner_id=owner,tenant_id=tenant)

@app.get("/runtime/flask/workspaces/<tenant>/reports/<report_id>")
def report(tenant,report_id): return jsonify(id=report_id,tenant_id=tenant)

@app.get("/runtime/flask/management/audit")
def audit(): return jsonify(area="audit",role=request.headers.get("X-Role",""),required_role="security-admin")

@app.post("/runtime/flask/workflows/release/resources/<resource_id>/transition")
def workflow(resource_id):
    body=request.get_json(silent=True) or {}
    return jsonify(**body,resource_id=resource_id,state=body.get("to_state"),role=request.headers.get("X-Role",""))

@app.get("/runtime/flask/routing/audit")
def routing(): return jsonify(area="audit",role=request.headers.get("X-Role",""),route_form="canonical")

@app.patch("/runtime/flask/accounts/<account>/preferences")
def preferences(account):
    body=request.get_json(silent=True) or {}
    return jsonify(id=account,applied_properties=sorted(body.keys()))

@app.post("/runtime/flask/assets/bulk-read")
def bulk():
    body=request.get_json(silent=True) or {}
    items=[]
    for asset_id in body.get("resource_ids",[]):
        owner="alice" if asset_id=="asset-red" else "bob"
        tenant="red" if asset_id=="asset-red" else "blue"
        items.append({"resource_id":asset_id,"decision":"ALLOW","owner_id":owner,"tenant_id":tenant})
    return jsonify(items=items)

@app.get("/runtime/flask/references/<alias>")
def reference(alias):
    asset_id="asset-red" if alias=="ref-red" else "asset-blue"
    owner="alice" if asset_id=="asset-red" else "bob"
    tenant="red" if asset_id=="asset-red" else "blue"
    return jsonify(alias=alias,resolved_resource_id=asset_id,resource={"id":asset_id,"owner_id":owner,"tenant_id":tenant})

if __name__=="__main__":
    app.run(host="127.0.0.1",port=int(os.environ.get("PORT","18202")),debug=False,use_reloader=False)
