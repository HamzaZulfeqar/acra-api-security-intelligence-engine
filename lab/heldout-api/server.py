#!/usr/bin/env python3
import base64, json, os, re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

MODE = os.environ.get("ACRA_HOLDOUT_MODE", "secure").lower()
PORT = int(os.environ.get("PORT", "18182" if MODE == "secure" else "18181"))

FILES = {
    "file-north": {"id": "file-north", "tenant_id": "north", "owner_id": "alice", "name": "North plan"},
    "file-south": {"id": "file-south", "tenant_id": "south", "owner_id": "bob", "name": "South plan"},
}
REPORTS = {
    "north": {"r-north": {"id": "r-north", "tenant_id": "north", "owner_id": "manager-n"}},
    "south": {"r-south": {"id": "r-south", "tenant_id": "south", "owner_id": "manager-s"}},
}
SHARES = {"share-north": "file-north", "share-south": "file-south"}
PROFILES = {
    "alice": {"id": "alice", "nickname": "Alice", "is_staff": False},
    "bob": {"id": "bob", "nickname": "Bob", "is_staff": False},
}

FILE_RE = re.compile(r"^/holdout/v1/files/(?P<file>file-(?:north|south))$")
TENANT_REPORT_RE = re.compile(r"^/holdout/v1/tenants/(?P<tenant>north|south)/reports/(?P<report>r-(?:north|south))$")
SHARE_RE = re.compile(r"^/holdout/v1/share/(?P<alias>share-(?:north|south))$")
PROFILE_RE = re.compile(r"^/holdout/v1/accounts/(?P<account>alice|bob)/profile$")
WORKFLOW_RE = re.compile(r"^/holdout/v1/workflows/change-request/resources/(?P<resource>[^/]+)/transition$")

def claim(token, name):
    try:
        part = token.split(".")[1]
        part += "=" * ((4 - len(part) % 4) % 4)
        return json.loads(base64.urlsafe_b64decode(part.encode()).decode()).get(name)
    except Exception:
        return None

class Handler(BaseHTTPRequestHandler):
    server_version = "ACRA-Holdout/1.0"

    def log_message(self, fmt, *args):
        pass

    def _json(self, status, obj):
        data = json.dumps(obj, sort_keys=True, separators=(",", ":")).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _identity(self):
        auth = self.headers.get("Authorization", "")
        if not auth.lower().startswith("bearer "):
            return None
        token = auth[7:].strip()
        return {
            "sub": claim(token, "sub"),
            "tenant_id": claim(token, "tenant_id"),
            "role": claim(token, "role"),
        }

    def _require_identity(self):
        ident = self._identity()
        if not ident or not ident.get("sub"):
            self._json(401, {"error": "unauthorized"})
            return None
        return ident

    def _read_json(self):
        length = min(int(self.headers.get("Content-Length", "0") or "0"), 65536)
        raw = self.rfile.read(length) if length else b"{}"
        try:
            value = json.loads(raw.decode())
        except Exception:
            return None
        return value if isinstance(value, dict) else None

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/health":
            return self._json(200, {"status": "ok", "mode": MODE})

        ident = self._require_identity()
        if not ident:
            return

        m = FILE_RE.match(path)
        if m:
            item = FILES[m.group("file")]
            authorized = ident["sub"] == item["owner_id"]
            if MODE == "secure" and not authorized:
                return self._json(403, {"error": "access_denied"})
            if MODE == "vulnerable" and not authorized and item["id"] != "file-south":
                return self._json(403, {"error": "access_denied"})
            return self._json(200, item)

        m = TENANT_REPORT_RE.match(path)
        if m:
            report = REPORTS.get(m.group("tenant"), {}).get(m.group("report"))
            if report is None:
                return self._json(404, {"error": "report_not_found"})
            tenant_allowed = ident["tenant_id"] == report["tenant_id"] or ident["role"] == "platform-admin"
            if MODE == "secure" and not tenant_allowed:
                return self._json(403, {"error": "access_denied"})
            if MODE == "vulnerable" and not tenant_allowed and report["id"] != "r-south":
                return self._json(403, {"error": "access_denied"})
            return self._json(200, report)

        if path in ("/holdout/v1/admin/audit", "/holdout//v1/admin/audit"):
            duplicate = path.startswith("/holdout//")
            privileged = ident["role"] in {"security-admin", "platform-admin"}
            if not privileged:
                if MODE == "vulnerable":
                    return self._json(200, {"area": "audit", "role": ident["role"], "route_form": "duplicate-separator"})
                return self._json(403, {"error": "access_denied"})
            return self._json(200, {"area": "audit", "role": ident["role"], "route_form": "duplicate-separator" if duplicate else "canonical"})

        m = SHARE_RE.match(path)
        if m:
            file_id = SHARES[m.group("alias")]
            item = FILES[file_id]
            authorized = ident["sub"] == item["owner_id"]
            if MODE == "secure" and not authorized:
                return self._json(403, {"error": "access_denied", "resolved_resource_id": file_id})
            return self._json(200, {"alias": m.group("alias"), "resolved_resource_id": file_id, "resource": item})

        return self._json(404, {"error": "not_found"})

    def do_PATCH(self):
        path = urlparse(self.path).path
        ident = self._require_identity()
        if not ident:
            return
        m = PROFILE_RE.match(path)
        if not m:
            return self._json(404, {"error": "not_found"})
        if ident["sub"] != m.group("account"):
            return self._json(403, {"error": "access_denied"})
        body = self._read_json()
        if body is None:
            return self._json(400, {"error": "malformed_json"})
        permitted = {"nickname"}
        forbidden = sorted(set(body) - permitted)
        if MODE == "secure" and forbidden:
            return self._json(403, {"error": "property_access_denied", "properties": forbidden})
        profile = PROFILES[m.group("account")]
        applied = []
        if isinstance(body.get("nickname"), str):
            profile["nickname"] = body["nickname"]
            applied.append("nickname")
        if MODE == "vulnerable" and isinstance(body.get("is_staff"), bool):
            profile["is_staff"] = body["is_staff"]
            applied.append("is_staff")
        return self._json(200, {"id": profile["id"], "nickname": profile["nickname"], "is_staff": profile["is_staff"], "applied_properties": sorted(applied)})

    def do_POST(self):
        path = urlparse(self.path).path
        ident = self._require_identity()
        if not ident:
            return

        if path == "/holdout/v1/files/batch-read":
            body = self._read_json()
            ids = body.get("resource_ids") if body else None
            if not isinstance(ids, list) or not ids or any(x not in FILES for x in ids):
                return self._json(400, {"error": "invalid_batch"})
            first = FILES[ids[0]]
            first_allowed = ident["sub"] == first["owner_id"]
            items = []
            for file_id in ids:
                item = FILES[file_id]
                per_item_allowed = ident["sub"] == item["owner_id"]
                allowed = first_allowed if MODE == "vulnerable" else per_item_allowed
                items.append({
                    "resource_id": file_id,
                    "decision": "ALLOW" if allowed else "DENY",
                    "owner_id": item["owner_id"] if allowed else None,
                    "tenant_id": item["tenant_id"] if allowed else None,
                })
            return self._json(200, {"items": items, "persisted": False})

        m = WORKFLOW_RE.match(path)
        if m:
            body = self._read_json()
            if body is None:
                return self._json(400, {"error": "malformed_json"})
            action = str(body.get("action", ""))
            from_state = str(body.get("from_state", ""))
            to_state = str(body.get("to_state", ""))
            allowed = False
            if action == "SUBMIT" and from_state == "DRAFT" and to_state == "PENDING" and ident["role"] == "requester":
                allowed = True
            elif action == "APPROVE" and from_state == "PENDING" and to_state == "APPROVED":
                if MODE == "vulnerable" and ident["role"] == "requester":
                    allowed = True
                elif ident["role"] == "approver" and bool(body.get("approval")):
                    allowed = True
            if not allowed:
                return self._json(403, {"error": "access_denied"})
            return self._json(200, {
                "resource_id": m.group("resource"),
                "action": action,
                "from_state": from_state,
                "to_state": to_state,
                "state": to_state,
                "actor": ident["sub"],
                "role": ident["role"],
                "persisted": False,
            })

        return self._json(404, {"error": "not_found"})

if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
