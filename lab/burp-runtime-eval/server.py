#!/usr/bin/env python3
from __future__ import annotations
import base64, json, re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT=18502
DOCS={
 "3001":{"id":"3001","tenant_id":"tenant-c","owner_id":"user-c","title":"Phase 7 eval C"},
 "4001":{"id":"4001","tenant_id":"tenant-d","owner_id":"user-d","title":"Phase 7 eval D"},
}
DOC_RE=re.compile(r"^/api/v1/tenants/(?P<tenant>[^/]+)/documents/(?P<doc>[^/?]+)$")

def claim(token,name):
    try:
        part=token.split(".")[1]
        part += "="*((4-len(part)%4)%4)
        return json.loads(base64.urlsafe_b64decode(part.encode()).decode()).get(name)
    except Exception:
        return None

class Handler(BaseHTTPRequestHandler):
    server_version="ACRA-Burp-Eval/1.0"
    def log_message(self, fmt, *args): pass
    def send_json(self,status,obj):
        raw=json.dumps(obj,separators=(",",":")).encode()
        self.send_response(status)
        self.send_header("Content-Type","application/json")
        self.send_header("Content-Length",str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)
    def identity(self):
        auth=self.headers.get("Authorization","")
        if not auth.lower().startswith("bearer "): return None
        token=auth[7:].strip()
        return {"sub":claim(token,"sub"),"tenant_id":claim(token,"tenant_id"),"role":claim(token,"role")}
    def do_GET(self):
        if self.path=="/health": return self.send_json(200,{"status":"ok","fixture":"burp-phase7-eval"})
        m=DOC_RE.match(self.path)
        if not m: return self.send_json(404,{"error":"not_found"})
        ident=self.identity()
        if not ident or not ident.get("sub"): return self.send_json(401,{"error":"unauthorized"})
        doc=DOCS.get(m.group("doc"))
        if doc is None or doc["tenant_id"]!=m.group("tenant"): return self.send_json(404,{"error":"document_not_found"})
        if ident.get("tenant_id")!=doc["tenant_id"]: return self.send_json(403,{"error":"access_denied"})
        return self.send_json(200,doc)

if __name__=="__main__":
    ThreadingHTTPServer(("127.0.0.1",PORT),Handler).serve_forever()
