#!/usr/bin/env bash
# Preliminary PGo raftkvs put microbench (localhost), for sanity compare vs Julang SKV.
#
# Requires a built DistCompiler/pgo checkout:
#   git clone https://github.com/DistCompiler/pgo.git tools/pgo
#   cd tools/pgo && go build -o systems/raftkvs/server ./systems/raftkvs/cmd/server/
#                  go build -o systems/raftkvs/client ./systems/raftkvs/cmd/client/
#
# Usage:
#   PGO_ROOT=tools/pgo ./bench_pgo_raftkvs.sh
# Env: BENCH_OPS (default 200), PGO_ROOT (default $REPO/tools/pgo)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKV_DIR/../.." && pwd)"
PGO_ROOT="${PGO_ROOT:-$REPO_ROOT/tools/pgo}"
RAFTKVS="$PGO_ROOT/systems/raftkvs"
CONFIG_IN="${PGO_CONFIG:-$SCRIPT_DIR/pgo/local-3-bench.yaml}"
# Resolve absolute path: server/client run with cwd=$RAFTKVS.
if [[ "$CONFIG_IN" = /* ]]; then
  CONFIG="$CONFIG_IN"
else
  CONFIG="$(cd "$(dirname "$CONFIG_IN")" && pwd)/$(basename "$CONFIG_IN")"
fi
BENCH_OPS="${BENCH_OPS:-200}"
PIDFILE="$SKV_DIR/.pgo-raftkvs-pids"
WORKDIR="${PGO_WORKDIR:-/tmp/julang-pgo-raftkvs-bench}"
# Startup wait scales with cluster size (election); override with PGO_READY_S.
PGO_READY_S="${PGO_READY_S:-}"
# Client wall-clock cap (seconds); PGo client retries forever on timeout.
PGO_CLIENT_TIMEOUT_S="${PGO_CLIENT_TIMEOUT_S:-120}"

if [[ ! -x "$RAFTKVS/server" || ! -x "$RAFTKVS/client" ]]; then
  echo "error: need $RAFTKVS/server and client (build under tools/pgo)" >&2
  exit 1
fi

if [[ ! -f "$CONFIG" ]]; then
  echo "error: PGo config not found: $CONFIG" >&2
  exit 1
fi

NUM_SERVERS="$(python3 - "$CONFIG" <<'PY'
import re, sys
text = open(sys.argv[1]).read()
m = re.search(r"^numServers:\s*(\d+)\s*$", text, re.M)
if not m:
    sys.exit("error: numServers missing in " + sys.argv[1])
print(m.group(1))
PY
)"

if [[ -z "$PGO_READY_S" ]]; then
  if [[ "$NUM_SERVERS" -le 1 ]]; then
    PGO_READY_S=2
  elif [[ "$NUM_SERVERS" -le 3 ]]; then
    PGO_READY_S=3
  else
    PGO_READY_S=5
  fi
fi

rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
cleanup() {
  if [[ -f "$PIDFILE" ]]; then
    while IFS= read -r pid || [[ -n "$pid" ]]; do
      [[ -z "$pid" ]] && continue
      kill "$pid" 2>/dev/null || true
      # Also kill children of the subshell wrappers.
      pkill -P "$pid" 2>/dev/null || true
    done < "$PIDFILE"
    rm -f "$PIDFILE"
  fi
  pkill -f "$RAFTKVS/server" 2>/dev/null || true
  pkill -f "$RAFTKVS/client" 2>/dev/null || true
}
trap cleanup EXIT

: > "$PIDFILE"
echo "=== PGo raftkvs tier-0 put bench (ops=$BENCH_OPS nodes=$NUM_SERVERS) ==="
echo "config=$CONFIG"

i=1
while [[ "$i" -le "$NUM_SERVERS" ]]; do
  log="$WORKDIR/srv$i.log"
  (cd "$RAFTKVS" && ./server -srvId "$i" -c "$CONFIG") >"$log" 2>&1 &
  echo $! >> "$PIDFILE"
  echo "  server $i pid=$! log=$log"
  i=$((i + 1))
done

echo "waiting ${PGO_READY_S}s for election ..."
sleep "$PGO_READY_S"

WL="$WORKDIR/puts.txt"
: > "$WL"
i=1
while [[ "$i" -le "$BENCH_OPS" ]]; do
  echo "put k$i v$i" >> "$WL"
  i=$((i + 1))
done

echo "running client workload ($BENCH_OPS puts, timeout ${PGO_CLIENT_TIMEOUT_S}s) ..."
t0=$(python3 -c 'import time; print(time.perf_counter())')
set +e
(cd "$RAFTKVS" && ./client -clientId 1 -c "$CONFIG" -w "$WL") >"$WORKDIR/client.out" 2>"$WORKDIR/client.err" &
CLIENT_PID=$!
echo "$CLIENT_PID" >> "$PIDFILE"
elapsed=0
client_rc=0
while kill -0 "$CLIENT_PID" 2>/dev/null; do
  if [[ "$elapsed" -ge "$PGO_CLIENT_TIMEOUT_S" ]]; then
    echo "error: PGo client timed out after ${PGO_CLIENT_TIMEOUT_S}s" >&2
    kill "$CLIENT_PID" 2>/dev/null || true
    wait "$CLIENT_PID" 2>/dev/null || true
    tail -30 "$WORKDIR/client.err" "$WORKDIR/srv1.log" >&2 || true
    exit 1
  fi
  sleep 1
  elapsed=$((elapsed + 1))
done
wait "$CLIENT_PID"
client_rc=$?
set -e
if [[ "$client_rc" -ne 0 ]]; then
  echo "client failed (rc=$client_rc); tail logs:" >&2
  tail -30 "$WORKDIR/client.err" "$WORKDIR/srv1.log" >&2 || true
  exit 1
fi
t1=$(python3 -c 'import time; print(time.perf_counter())')

python3 - "$t0" "$t1" "$BENCH_OPS" <<'PY'
import sys
t0, t1, ops = float(sys.argv[1]), float(sys.argv[2]), int(sys.argv[3])
wall = t1 - t0
rps = ops / wall if wall > 0 else 0
print()
print("=== Tier 0 PGo raftkvs results ===")
print(f"  ops_total={ops}  ok={ops}  (client exit 0)")
print(f"  wall_seconds={wall:.3f}  throughput_ok_rps={rps:.3f}")
print("  note: wall includes client startup; single client, sequential puts.")
print()
print("LIMITATIONS: TCP mailboxes + Raft quorum; not same wire as Julang SKV HTTP.")
PY
