#!/usr/bin/env bash
# Failover test: elect → append → kill leader → re-elect → append → revive → catch-up.
#
# Usage: ./failover_test.sh [configPath]
#
# Env (optional):
#   SMOKE_TIMEOUT_S    overall wall-clock timeout (default 180)
#   LISTEN_TIMEOUT_S   max seconds to wait for all ports (default 40)
#   ELECTION_WAIT_S    max seconds to wait for a leader (default 30)
#   CURL_TIMEOUT_S     curl max-time per request (default 8)
#   CATCHUP_WAIT_S     max seconds for revived node to catch up (default 30)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RAFT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$RAFT_DIR/../.." && pwd)"
CONFIG="${1:-$RAFT_DIR/cluster.conf}"
PIDFILE="$(dirname "$CONFIG")/.raft-pids"
LOGDIR="$(dirname "$CONFIG")"

SMOKE_TIMEOUT_S="${SMOKE_TIMEOUT_S:-180}"
LISTEN_TIMEOUT_S="${LISTEN_TIMEOUT_S:-40}"
ELECTION_WAIT_S="${ELECTION_WAIT_S:-30}"
CURL_TIMEOUT_S="${CURL_TIMEOUT_S:-8}"
CATCHUP_WAIT_S="${CATCHUP_WAIT_S:-30}"

if [[ -z "${SMOKE_UNDER_TIMEOUT:-}" ]]; then
  export SMOKE_UNDER_TIMEOUT=1
  if command -v perl >/dev/null 2>&1; then
    exec perl -e 'my $t=shift; $SIG{ALRM}=sub{print STDERR "error: failover test exceeded ${t}s\n"; kill "TERM", -$$; exit 124}; alarm $t; exec @ARGV' \
      "$SMOKE_TIMEOUT_S" "$0" "$@"
  fi
fi

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
  else
    return 1
  fi
}

NODE_JAR="$(find_jar RaftNode.jar "${RAFT_NODE_JAR:-}" || true)"
CLIENT_JAR="$(find_jar RaftClient.jar "${RAFT_CLIENT_JAR:-}" || true)"
if [[ -z "$NODE_JAR" ]]; then
  echo "error: RaftNode.jar not found" >&2
  exit 1
fi
if [[ -z "$CLIENT_JAR" ]]; then
  echo "error: RaftClient.jar not found" >&2
  exit 1
fi
export RAFT_NODE_JAR="$NODE_JAR"

if [[ ! -f "$CONFIG" ]]; then
  echo "error: config not found: $CONFIG" >&2
  exit 1
fi

URLS=()
while IFS= read -r line || [[ -n "$line" ]]; do
  trimmed="$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ -z "$trimmed" ]] && continue
  [[ "$trimmed" == \#* ]] && continue
  URLS+=("$trimmed")
done < "$CONFIG"

N="${#URLS[@]}"
if [[ "$N" -lt 3 ]]; then
  echo "error: failover test needs ≥3 nodes (got $N)" >&2
  exit 1
fi

port_from_url() {
  echo "$1" | sed -E 's|^[a-zA-Z]+://[^:/]+:([0-9]+).*|\1|'
}

PORTS=()
for url in "${URLS[@]}"; do
  PORTS+=("$(port_from_url "$url")")
done

CONFIG_ABS="$(cd "$(dirname "$CONFIG")" && pwd)/$(basename "$CONFIG")"

cleanup() {
  "$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" >/dev/null 2>&1 || true
}
trap cleanup EXIT

fail() {
  echo "error: $*" >&2
  exit 1
}

# Probe one URL: echo leader URL if this contact proves a live leader (200),
# or if a 303 names a reachable leader that itself accepts an append.
# Prints nothing and returns 1 otherwise.
probe_for_leader() {
  local url="$1"
  local body code leader leader_body leader_code
  body="$(mktemp)"
  code="$(curl -sS -m "$CURL_TIMEOUT_S" -o "$body" -w '%{http_code}' \
    -X POST "${url}/client/append" -d "__failover_probe__" 2>/dev/null || echo "000")"
  if [[ "$code" == "200" ]]; then
    rm -f "$body"
    echo "$url"
    return 0
  fi
  if [[ "$code" == "303" ]]; then
    leader="$(awk '/^LEADER /{print $2}' "$body")"
    rm -f "$body"
    if [[ -n "$leader" ]]; then
      leader_body="$(mktemp)"
      leader_code="$(curl -sS -m "$CURL_TIMEOUT_S" -o "$leader_body" -w '%{http_code}' \
        -X POST "${leader}/client/append" -d "__failover_probe__" 2>/dev/null || echo "000")"
      rm -f "$leader_body"
      if [[ "$leader_code" == "200" ]]; then
        echo "$leader"
        return 0
      fi
    fi
    return 1
  fi
  rm -f "$body"
  return 1
}

# Returns a live leader URL on stdout, or empty if none yet.
discover_leader() {
  local url
  for url in "${URLS[@]}"; do
    if probe_for_leader "$url"; then
      return 0
    fi
  done
  return 1
}

wait_for_leader() {
  local label="$1"
  local max_s="$2"
  local leader=""
  echo "=== wait for leader ($label, ≤${max_s}s) ===" >&2
  for ((t = 0; t < max_s; t++)); do
    if leader="$(discover_leader)"; then
      echo "leader=$leader after ${t}s" >&2
      echo "$leader"
      return 0
    fi
    sleep 1
  done
  fail "no leader elected within ${max_s}s ($label)"
}

node_index_for_url() {
  local want="$1"
  local i
  for i in "${!URLS[@]}"; do
    if [[ "${URLS[$i]}" == "$want" ]]; then
      echo "$((i + 1))"
      return 0
    fi
  done
  return 1
}

pid_for_node() {
  local node_id="$1"
  sed -n "${node_id}p" "$PIDFILE"
}

set_pid_for_node() {
  local node_id="$1"
  local new_pid="$2"
  local tmp
  tmp="$(mktemp)"
  local i=1
  while IFS= read -r pid || [[ -n "$pid" ]]; do
    if [[ "$i" -eq "$node_id" ]]; then
      echo "$new_pid" >> "$tmp"
    else
      echo "$pid" >> "$tmp"
    fi
    i=$((i + 1))
  done < "$PIDFILE"
  mv "$tmp" "$PIDFILE"
}

append_via_client() {
  local seed_url="$1"
  local value="$2"
  local out
  echo -n "  append '$value' via $seed_url ... "
  out="$("$JAVA" -jar "$CLIENT_JAR" "$seed_url" append "$value" 2>&1)" || true
  echo "$out"
  echo "$out" | grep -q '200 OK' || fail "append of '$value' did not report 200 OK"
}

# Poll /client/get on url until every value appears (or timeout).
wait_for_values_on() {
  local url="$1"
  local max_s="$2"
  shift 2
  local values=("$@")
  local t body missing v
  for ((t = 0; t < max_s; t++)); do
    body="$(curl -sS -m "$CURL_TIMEOUT_S" -X POST "${url}/client/get" -d '' 2>/dev/null || true)"
    missing=0
    for v in "${values[@]}"; do
      if ! grep -Fq "$v" <<<"$body"; then
        missing=1
        break
      fi
    done
    if [[ "$missing" -eq 0 ]]; then
      echo "  $url has all values after ${t}s: $body"
      return 0
    fi
    sleep 1
  done
  echo "  last get from $url: $body" >&2
  fail "$url missing required values within ${max_s}s: ${values[*]}"
}

echo "=== failover test (timeout ${SMOKE_TIMEOUT_S}s) ==="
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
[[ "$ready" -eq 1 ]] || fail "only $ok/$N ports listening after ${LISTEN_TIMEOUT_S}s"

# --- 1. Wait for election ---
LEADER="$(wait_for_leader "initial" "$ELECTION_WAIT_S")"
LEADER_ID="$(node_index_for_url "$LEADER")" || fail "leader URL $LEADER not in config"
LEADER_PID="$(pid_for_node "$LEADER_ID")"
echo "initial leader: node $LEADER_ID $LEADER pid=$LEADER_PID"

# --- 2. Append a few values ---
TAG="fo-$(date +%s)"
V1="${TAG}-a"
V2="${TAG}-b"
V3="${TAG}-c"
echo "=== append initial values ==="
append_via_client "$LEADER" "$V1"
append_via_client "$LEADER" "$V2"
append_via_client "$LEADER" "$V3"

echo "=== verify initial values on all live nodes ==="
for url in "${URLS[@]}"; do
  wait_for_values_on "$url" 20 "$V1" "$V2" "$V3"
done

# --- 3. Kill the leader; wait for a new one ---
echo "=== kill leader node $LEADER_ID (pid $LEADER_PID) ==="
kill "$LEADER_PID" 2>/dev/null || fail "failed to kill leader pid $LEADER_PID"
# Wait until process is gone and port is free.
for ((t = 0; t < 10; t++)); do
  if ! kill -0 "$LEADER_PID" 2>/dev/null; then
    break
  fi
  sleep 0.5
done
if kill -0 "$LEADER_PID" 2>/dev/null; then
  kill -9 "$LEADER_PID" 2>/dev/null || true
fi
set_pid_for_node "$LEADER_ID" "0"
OLD_LEADER="$LEADER"
OLD_LEADER_ID="$LEADER_ID"

# Seed discovery from a surviving node.
SEED=""
for i in "${!URLS[@]}"; do
  if [[ "$((i + 1))" -ne "$OLD_LEADER_ID" ]]; then
    SEED="${URLS[$i]}"
    break
  fi
done
[[ -n "$SEED" ]] || fail "no surviving seed URL"

NEW_LEADER="$(wait_for_leader "after kill" "$ELECTION_WAIT_S")"
[[ "$NEW_LEADER" != "$OLD_LEADER" ]] || fail "new leader is still the killed node $OLD_LEADER"
NEW_LEADER_ID="$(node_index_for_url "$NEW_LEADER")" || fail "new leader URL $NEW_LEADER not in config"
echo "new leader: node $NEW_LEADER_ID $NEW_LEADER (old was node $OLD_LEADER_ID)"

# --- 4. Append new entries to the new leader ---
V4="${TAG}-d"
V5="${TAG}-e"
echo "=== append post-failover values ==="
append_via_client "$NEW_LEADER" "$V4"
append_via_client "$NEW_LEADER" "$V5"

echo "=== verify post-failover values on surviving nodes ==="
for i in "${!URLS[@]}"; do
  if [[ "$((i + 1))" -eq "$OLD_LEADER_ID" ]]; then
    continue
  fi
  wait_for_values_on "${URLS[$i]}" 20 "$V1" "$V2" "$V3" "$V4" "$V5"
done

# --- 5. Bring killed node back; verify catch-up ---
echo "=== restart killed node $OLD_LEADER_ID ==="
log="$LOGDIR/raft-node-${OLD_LEADER_ID}.log"
{
  echo ""
  echo "===== RESTART $(date -u +%Y-%m-%dT%H:%M:%SZ) ====="
} >> "$log"
"$JAVA" -jar "$NODE_JAR" "$CONFIG_ABS" "$OLD_LEADER_ID" >>"$log" 2>&1 &
REVIVED_PID=$!
set_pid_for_node "$OLD_LEADER_ID" "$REVIVED_PID"
echo "  restarted node $OLD_LEADER_ID pid=$REVIVED_PID log=$log"

echo "=== wait for revived node listen ==="
revived_port="$(port_from_url "$OLD_LEADER")"
ready=0
for ((i = 1; i <= LISTEN_TIMEOUT_S; i++)); do
  if lsof -nP -iTCP:"$revived_port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "revived node listening at ${i}s"
    ready=1
    break
  fi
  sleep 1
done
[[ "$ready" -eq 1 ]] || fail "revived node did not listen within ${LISTEN_TIMEOUT_S}s"

echo "=== wait for catch-up on revived node (≤${CATCHUP_WAIT_S}s) ==="
wait_for_values_on "$OLD_LEADER" "$CATCHUP_WAIT_S" "$V1" "$V2" "$V3" "$V4" "$V5"

echo "=== verify all nodes still agree ==="
for url in "${URLS[@]}"; do
  wait_for_values_on "$url" 10 "$V1" "$V2" "$V3" "$V4" "$V5"
done

echo "=== failover test passed ==="
