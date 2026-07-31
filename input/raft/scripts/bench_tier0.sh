#!/usr/bin/env bash
# Tier-0 Raft benchmark: start localhost cluster → elect → load → report → stop.
#
# Usage: ./bench_tier0.sh [configPath]
#
# Env (optional):
#   BENCH_OPS           ops per client (default 20)
#   BENCH_CLIENTS       concurrent clients (default 1)
#   BENCH_MODE          append | get | mix (default append)
#   BENCH_TIMEOUT_S     per-request HTTP timeout (default 60)
#   LISTEN_TIMEOUT_S    max seconds to wait for ports (default 40)
#   ELECTION_WAIT_S     seconds after listen before load (default 12)
#   KEEP_CLUSTER=1      leave the cluster running after the bench
#
# Requires RaftNode.jar (see start_cluster.sh). Numbers are local smoke only —
# not comparable to PGo / etcd / YCSB (printed in the Python report).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RAFT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG="${1:-$RAFT_DIR/cluster.conf}"

BENCH_OPS="${BENCH_OPS:-20}"
BENCH_CLIENTS="${BENCH_CLIENTS:-1}"
BENCH_MODE="${BENCH_MODE:-append}"
BENCH_TIMEOUT_S="${BENCH_TIMEOUT_S:-60}"
LISTEN_TIMEOUT_S="${LISTEN_TIMEOUT_S:-40}"
ELECTION_WAIT_S="${ELECTION_WAIT_S:-12}"
KEEP_CLUSTER="${KEEP_CLUSTER:-0}"

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
export JAVA

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
  echo "$1" | sed -E 's|^[a-zA-Z]+://[^:/]+:([0-9]+).*|\1|'
}

PORTS=()
for url in "${URLS[@]}"; do
  PORTS+=("$(port_from_url "$url")")
done

cleanup() {
  if [[ "$KEEP_CLUSTER" != "1" ]]; then
    "$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "=== Tier 0 Raft bench ==="
echo "config=$CONFIG ops=$BENCH_OPS clients=$BENCH_CLIENTS mode=$BENCH_MODE"
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

SEED_URL="${URLS[0]}"
echo "=== load via $SEED_URL ==="
python3 "$SCRIPT_DIR/bench_load.py" \
  --url "$SEED_URL" \
  --ops "$BENCH_OPS" \
  --clients "$BENCH_CLIENTS" \
  --mode "$BENCH_MODE" \
  --timeout "$BENCH_TIMEOUT_S" \
  --value-prefix "tier0-$(date +%s)"

echo "=== Tier 0 bench finished ==="
