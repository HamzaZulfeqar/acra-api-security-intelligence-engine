"""A deliberately vulnerable, isolated staging fixture for GUI execution CI."""

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class Handler(BaseHTTPRequestHandler):
    requests = []

    def do_GET(self):
        if self.path == "/__fixture_stats":
            self.reply(200, {"requests": self.requests})
            return
        if self.path not in ("/api/v1/demo", "/api/v1/demo/"):
            self.reply(404, {"error": "out of scope"})
            return
        authorization = self.headers.get("Authorization", "")
        self.requests.append({"path": self.path, "identity": {
            "Bearer limited-user": "tested", "Bearer approved-control": "positive"
        }.get(authorization, "anonymous")})
        allowed = authorization == "Bearer approved-control" or (
            authorization == "Bearer limited-user" and self.path.endswith("/"))
        self.reply(200 if allowed else 403, {"ok": allowed})

    def reply(self, status, data):
        body = json.dumps(data).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    host = os.environ["ACRA_FIXTURE_HOST"]
    if host.startswith("127.") or host == "0.0.0.0":
        raise ValueError("Fixture must bind one explicit non-loopback interface")
    ThreadingHTTPServer((host, int(os.environ.get("ACRA_FIXTURE_PORT", "18081"))), Handler).serve_forever()
