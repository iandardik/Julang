#!/usr/bin/env bash
# Smoke-test a local Raft cluster: start → elect → append/get with redirect → stop.
#
# Usage: ./smoke_test.sh [configPath]
#
# Requires RaftNode.jar and RaftClient.jar (compile input/raft/main.jul), or set
# RAFT_NODE_JAR / RAFT_CLIENT_JAR. Optional env:
#   SMOKE_TIMEOUT_S    overall wall-clock timeout for the whole smoke test (default 120)
#   LISTEN_TIMEOUT_S   max seconds to wait for all ports (default 40)
#   ELECTION_WAIT_S    seconds after listen before client ops (default 3)
#   APPEND_WAIT_S      seconds to poll append until leader (default CLIENT_STEP_TIMEOUT_S)
#   CURL_TIMEOUT_S     curl max-time per request (default 8)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RAFT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$RAFT_DIR/../.." && pwd)"
CONFIG="${1:-$RAFT_DIR/cluster.conf}"

SMOKE_TIMEOUT_S="${SMOKE_TIMEOUT_S:-120}"
LISTEN_TIMEOUT_S="${LISTEN_TIMEOUT_S:-40}"
ELECTION_WAIT_S="${ELECTION_WAIT_S:-3}"
CURL_TIMEOUT_S="${CURL_TIMEOUT_S:-8}"

# Re-exec under an overall wall-clock timeout so a hang cannot run forever.
if [[ -z "${SMOKE_UNDER_TIMEOUT:-}" ]]; then
  export SMOKE_UNDER_TIMEOUT=1
  # Kill the entire process group on expiry (covers hung java children).
  if command -v perl >/dev/null 2>&1; then
    exec perl -e 'my $t=shift; $SIG{ALRM}=sub{print STDERR "error: smoke test exceeded ${t}s\n"; kill "TERM", -$$; exit 124}; alarm $t; exec @ARGV' \
      "$SMOKE_TIMEOUT_S" "$0" "$@"
  elif command -v timeout >/dev/null 2>&1; then
    exec timeout --foreground -k 5 "$SMOKE_TIMEOUT_S" "$0" "$@"
  elif command -v gtimeout >/dev/null 2>&1; then
    exec gtimeout --foreground -k 5 "$SMOKE_TIMEOUT_S" "$0" "$@"
  else
    (
      sleep "$SMOKE_TIMEOUT_S"
      echo "error: smoke test exceeded SMOKE_TIMEOUT_S=${SMOKE_TIMEOUT_S}s" >&2
      kill -TERM -$$ 2>/dev/null || kill -TERM 0 2>/dev/null || true
    ) &
    WATCHDOG_PID=$!
  fi
fi

# Prefer a real JDK over macOS /usr/bin/java stub.
if [[ -z "${JAVA:-}" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA="$JAVA_HOME/bin/java"
  elif [[ -x /Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home/bin/java ]]; then
    JAVA=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home/bin/java
  else
    JAVA=java
  fi
fi

find_jar() {
  local name="$1"
  local override="$2"
  if [[ -n "$override" && -f "$override" ]]; then
    echo "$override"
  elif [[ -f "$RAFT_DIR/$name" ]]; then
    echo "$RAFT_DIR/$name"
  elif [[ -f "$REPO_ROOT/$name" ]]; then
    echo "$REPO_ROOT/$name"
  elif [[ -f "$PWD/$name" ]]; then
    echo "$PWD/$name"
  else
    return 1
  fi
}

NODE_JAR="$(find_jar RaftNode.jar "${RAFT_NODE_JAR:-}" || true)"
CLIENT_JAR="$(find_jar RaftClient.jar "${RAFT_CLIENT_JAR:-}" || true)"
if [[ -z "$NODE_JAR" ]]; then
  echo "error: RaftNode.jar not found; compile input/raft/main.jul or set RAFT_NODE_JAR" >&2
  exit 1
fi
if [[ -z "$CLIENT_JAR" ]]; then
  echo "error: RaftClient.jar not found; compile input/raft/main.jul or set RAFT_CLIENT_JAR" >&2
  exit 1
fi
export RAFT_NODE_JAR="$NODE_JAR"

if [[ ! -f "$CONFIG" ]]; then
  echo "error: config not found: $CONFIG" >&2
  exit 1
fi

# Parse config the same way as Julay: trim, skip blanks and # comments.
URLS=()
while IFS= read -r line || [[ -n "$line" ]]; do
  trimmed="$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ -z "$trimmed" ]] && continue
  [[ "$trimmed" == \#* ]] && continue
  URLS+=("$trimmed")
done < "$CONFIG"

N="${#URLS[@]}"
if [[ "$N" -eq 0 ]]; then
  echo "error: no node URLs in $CONFIG" >&2
  exit 1
fi

port_from_url() {
  # http://host:port → port
  echo "$1" | sed -E 's|^[a-zA-Z]+://[^:/]+:([0-9]+).*|\1|'
}

PORTS=()
for url in "${URLS[@]}"; do
  PORTS+=("$(port_from_url "$url")")
done

cleanup() {
  "$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" >/dev/null 2>&1 || true
  if [[ -n "${WATCHDOG_PID:-}" ]]; then
    kill "$WATCHDOG_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "=== smoke timeout ${SMOKE_TIMEOUT_S}s ==="
echo "=== using NODE_JAR=$NODE_JAR CLIENT_JAR=$CLIENT_JAR ==="
echo "=== stop any previous cluster ==="
"$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" || true

echo "=== start cluster ==="
"$SCRIPT_DIR/start_cluster.sh" "$CONFIG"

echo "=== wait for listen (≤${LISTEN_TIMEOUT_S}s) ==="
ready=0
for ((i = 1; i <= LISTEN_TIMEOUT_S; i++)); do
  ok=0
  for port in "${PORTS[@]}"; do
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      ok=$((ok + 1))
    fi
  done
  if [[ "$ok" -eq "$N" ]]; then
    echo "all $N ports listening at ${i}s"
    ready=1
    break
  fi
  sleep 1
done
if [[ "$ready" -ne 1 ]]; then
  echo "error: only $ok/$N ports listening after ${LISTEN_TIMEOUT_S}s" >&2
  exit 1
fi

echo "=== wait ${ELECTION_WAIT_S}s for election/heartbeats ==="
sleep "$ELECTION_WAIT_S"

echo "=== curl POST /client/get on each node (local state machines) ==="
for idx in "${!URLS[@]}"; do
  url="${URLS[$idx]}"
  echo -n "  $url: "
  body_file="$(mktemp)"
  code="$(curl -sS -m "$CURL_TIMEOUT_S" -o "$body_file" -w '%{http_code}' \
    -X POST "${url}/client/get" -d '' || echo "fail")"
  body="$(cat "$body_file" 2>/dev/null || true)"
  rm -f "$body_file"
  echo "$body [$code]"
  if [[ "$code" != "200" ]]; then
    echo "error: expected local get 200 from $url, got code=$code body='$body'" >&2
    exit 1
  fi
done

# Enter via first URL; RaftClient follows 303 LEADER redirects for append.
# Poll: early 503 NO_LEADER is common before the first election finishes (client does not retry 503).
follower_url="${URLS[0]}"
VALUE="smoke-$(date +%s)"
CLIENT_STEP_TIMEOUT_S="${CLIENT_STEP_TIMEOUT_S:-30}"
APPEND_WAIT_S="${APPEND_WAIT_S:-$CLIENT_STEP_TIMEOUT_S}"
echo "=== RaftClient append via $follower_url (poll ≤${APPEND_WAIT_S}s) ==="
append_out=""
append_ok=0
for ((t=0; t<APPEND_WAIT_S; t++)); do
  set +e
  if command -v perl >/dev/null 2>&1; then
    append_out="$(perl -e 'alarm shift; exec @ARGV' "$CLIENT_STEP_TIMEOUT_S" "$JAVA" -jar "$CLIENT_JAR" "$follower_url" append "$VALUE" 2>&1)"
    append_rc=$?
  else
    append_out="$("$JAVA" -jar "$CLIENT_JAR" "$follower_url" append "$VALUE" 2>&1)"
    append_rc=$?
  fi
  set -e
  if [[ "$append_rc" -eq 0 ]] && grep -q '200 OK' <<<"$append_out"; then
    append_ok=1
    echo "$append_out"
    if [[ "$t" -gt 0 ]]; then
      echo "(leader ready after ${t}s)"
    fi
    break
  fi
  sleep 1
done
if [[ "$append_ok" -ne 1 ]]; then
  echo "$append_out"
  if [[ "${append_rc:-1}" -ne 0 ]]; then
    echo "error: append timed out or failed (rc=${append_rc:-1})" >&2
  else
    echo "error: append did not report 200 OK within ${APPEND_WAIT_S}s" >&2
  fi
  exit 1
fi

# client/get reads the contacted node's local committed SM (no leader redirect).
# After append-via-follower → leader commit, the entry node may briefly lag; poll.
echo "=== RaftClient get via $follower_url (poll ≤${CLIENT_STEP_TIMEOUT_S}s) ==="
GET_WAIT_S="${GET_WAIT_S:-$CLIENT_STEP_TIMEOUT_S}"
get_out=""
get_ok=0
for ((t=0; t<GET_WAIT_S; t++)); do
  set +e
  if command -v perl >/dev/null 2>&1; then
    get_out="$(perl -e 'alarm shift; exec @ARGV' "$CLIENT_STEP_TIMEOUT_S" "$JAVA" -jar "$CLIENT_JAR" "$follower_url" get 2>&1)"
    get_rc=$?
  else
    get_out="$("$JAVA" -jar "$CLIENT_JAR" "$follower_url" get 2>&1)"
    get_rc=$?
  fi
  set -e
  if [[ "$get_rc" -eq 0 ]] && grep -Fq "$VALUE" <<<"$get_out"; then
    get_ok=1
    echo "$get_out"
    if [[ "$t" -gt 0 ]]; then
      echo "(value visible after ${t}s replication lag)"
    fi
    break
  fi
  sleep 1
done
if [[ "$get_ok" -ne 1 ]]; then
  echo "$get_out"
  echo "error: get did not contain appended value '$VALUE' within ${GET_WAIT_S}s" >&2
  exit 1
fi
echo "$get_out" | grep -q '^200 ' || {
  echo "$get_out" | grep -q '200 \[' || {
    echo "error: get did not report 200" >&2
    exit 1
  }
}

echo "=== verify every node local state machine contains '$VALUE' ==="
SM_WAIT_S="${SM_WAIT_S:-20}"
sm_ok=0
for ((t=0; t<SM_WAIT_S; t++)); do
  missing=0
  for url in "${URLS[@]}"; do
    body_file="$(mktemp)"
    code="$(curl -sS -m "$CURL_TIMEOUT_S" -o "$body_file" -w '%{http_code}' \
      -X POST "${url}/client/get" -d '' || echo "fail")"
    body="$(cat "$body_file" 2>/dev/null || true)"
    rm -f "$body_file"
    if [[ "$code" != "200" ]] || ! grep -Fq "$VALUE" <<<"$body"; then
      missing=1
      break
    fi
  done
  if [[ "$missing" -eq 0 ]]; then
    sm_ok=1
    echo "all ${#URLS[@]} nodes have value after ${t}s"
    break
  fi
  sleep 1
done
if [[ "$sm_ok" -ne 1 ]]; then
  echo "error: not all followers/leader replicated '$VALUE' within ${SM_WAIT_S}s" >&2
  for url in "${URLS[@]}"; do
    echo -n "  $url: "
    curl -sS -m "$CURL_TIMEOUT_S" -X POST "${url}/client/get" -d '' || true
    echo
  done
  exit 1
fi

echo "=== smoke test passed ==="
