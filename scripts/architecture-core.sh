#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Core may describe Burp-facing export contracts in strings/type names, but it
# must never compile against the Montoya/Burp API itself.
if grep -RniE 'burp\.api\.montoya|com\.portswigger' core/src/main/java --include='*.java'; then
  echo 'ARCHITECTURE FAIL: Burp/Montoya API dependency found in acra-core'
  exit 1
fi

# Sprint 4 adds a deliberately isolated active transport package. Preserve the
# original passive-core invariant by scanning every production package except
# io/acra/core/active, which has its own Sprint 4 architecture gate.
if find core/src/main/java -type f -name '*.java' ! -path '*/io/acra/core/active/*' -print0 \
  | xargs -0 grep -niE 'java\.net\.http\.HttpClient|java\.net\.Socket|java\.nio\.channels\.SocketChannel'; then
  echo 'ARCHITECTURE FAIL: active network client found outside isolated Sprint 4 active package'
  exit 1
fi

if find core -type f \( -name '*.jar' -o -name '*.class' \) | grep -q .; then
  echo 'ARCHITECTURE FAIL: compiled/binary dependency checked into core source tree'
  exit 1
fi

printf 'ARCHITECTURE BOUNDARY PASS\n'
