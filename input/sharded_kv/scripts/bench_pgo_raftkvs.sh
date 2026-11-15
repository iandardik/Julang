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
CONFIG="${PGO_CONFIG:-$SCRIPT_DIR/pgo/local-3-bench.yaml}"
BENCH_OPS="${BENCH_OPS:-200}"
PIDFILE="$SKV_DIR/.pgo-raftkvs-pids"
WORKDIR="${PGO_WORKDIR:-/tmp/julang-pgo-raftkvs-bench}"

if [[ ! -x "$RAFTKVS/server" || ! -x "$RAFTKVS/client" ]]; then
  echo "error: need $RAFTKVS/server and client (build under tools/pgo)" >&2
  exit 1
fi

mkdir -p "$WORKDIR"
cleanup() {
  if [[ -f "$PIDFILE" ]]; then
    while IFS= read -r pid || [[ -n "$pid" ]]; do
      [[ -z "$pid" ]] && continue
      kill "$pid" 2>/dev/null || true
    done < "$PIDFILE"
    rm -f "$PIDFILE"
  fi
}
trap cleanup EXIT

: > "$PIDFILE"
echo "=== PGo raftkvs tier-0 put bench (ops=$BENCH_OPS) ==="
echo "config=$CONFIG"

i=1
while [[ "$i" -le 3 ]]; do
  log="$WORKDIR/srv$i.log"
  (cd "$RAFTKVS" && ./server -srvId "$i" -c "$CONFIG") >"$log" 2>&1 &
  echo $! >> "$PIDFILE"
  echo "  server $i pid=$! log=$log"
  i=$((i + 1))
done

echo "waiting 3s for election ..."
sleep 3

WL="$WORKDIR/puts.txt"
: > "$WL"
i=1
while [[ "$i" -le "$BENCH_OPS" ]]; do
  echo "put k$i v$i" >> "$WL"
  i=$((i + 1))
done

echo "running client workload ($BENCH_OPS puts) ..."
t0=$(python3 -c 'import time; print(time.perf_counter())')
(cd "$RAFTKVS" && ./client -clientId 1 -c "$CONFIG" -w "$WL") >"$WORKDIR/client.out" 2>"$WORKDIR/client.err" || {
  echo "client failed; tail logs:" >&2
  tail -30 "$WORKDIR/client.err" "$WORKDIR/srv1.log" >&2 || true
  exit 1
}
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
