import os
from flask import Flask,jsonify,request
app=Flask(__name__)
@app.get("/health")
def health(): return jsonify(status="ok",framework="flask-eval")
@app.get("/runtime-eval/flask/documents/<doc_id>")
def document(doc_id):
    owner="robin" if doc_id=="doc-alpha" else "casey";tenant="alpha" if doc_id=="doc-alpha" else "beta"
    return jsonify(id=doc_id,owner_id=owner,tenant_id=tenant)
@app.get("/runtime-eval/flask/organizations/<tenant>/reports/<report_id>")
def report(tenant,report_id): return jsonify(id=report_id,tenant_id=tenant)
@app.get("/runtime-eval/flask/admin/security-log")
def rbac(): return jsonify(area="security-log",role=request.headers.get("X-Role",""),required_role="audit-admin")
@app.post("/runtime-eval/flask/workflows/deploy/resources/<resource_id>/transition")
def workflow(resource_id):
    body=request.get_json(silent=True) or {}
    return jsonify(**body,resource_id=resource_id,state=body.get("to_state"),role=request.headers.get("X-Role",""))
@app.get("/runtime-eval/flask/routing/equivalent")
def routing(): return jsonify(area="route",role=request.headers.get("X-Role",""),route_form="canonical")
@app.patch("/runtime-eval/flask/profiles/<account>/settings")
def settings(account):
    body=request.get_json(silent=True) or {}
    return jsonify(id=account,applied_properties=sorted(body.keys()))
@app.post("/runtime-eval/flask/documents/bulk-fetch")
def bulk():
    body=request.get_json(silent=True) or {};items=[]
    for doc_id in body.get("resource_ids",[]):
        owner="robin" if doc_id=="doc-alpha" else "casey";tenant="alpha" if doc_id=="doc-alpha" else "beta"
        items.append({"resource_id":doc_id,"decision":"ALLOW","owner_id":owner,"tenant_id":tenant})
    return jsonify(items=items)
@app.get("/runtime-eval/flask/links/<alias>")
def link(alias):
    doc_id="doc-alpha" if alias=="link-alpha" else "doc-beta";owner="robin" if doc_id=="doc-alpha" else "casey";tenant="alpha" if doc_id=="doc-alpha" else "beta"
    return jsonify(alias=alias,resolved_resource_id=doc_id,resource={"id":doc_id,"owner_id":owner,"tenant_id":tenant})
if __name__=="__main__": app.run(host="127.0.0.1",port=int(os.environ.get("PORT","18302")),debug=False,use_reloader=False)
