#!/usr/bin/env bash
# Preliminary sanity compare: Julang SKV vs Julang Raft (tier-0 localhost).
# Optionally runs PGo raftkvs if PGO_RAFTKVS_DIR is set and built.
#
# Usage (from repo root):
#   input/sharded_kv/scripts/compare_sanity.sh
#
# Env:
#   BENCH_OPS / BENCH_CLIENTS — shared defaults (SKV default 200/4; Raft often needs fewer)
#   RAFT_BENCH_OPS / RAFT_BENCH_CLIENTS — override Raft side (defaults 20/1; Raft is slower)
#   SKIP_RAFT=1 — SKV only
#   PGO_RAFTKVS_DIR — path to DistCompiler/pgo/systems/raftkvs (optional)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKV_DIR/../.." && pwd)"
RAFT_SCRIPTS="$REPO_ROOT/input/raft/scripts"

BENCH_OPS="${BENCH_OPS:-200}"
BENCH_CLIENTS="${BENCH_CLIENTS:-4}"
RAFT_BENCH_OPS="${RAFT_BENCH_OPS:-20}"
RAFT_BENCH_CLIENTS="${RAFT_BENCH_CLIENTS:-1}"
SKIP_RAFT="${SKIP_RAFT:-0}"

OUT_DIR="${COMPARE_OUT_DIR:-$SKV_DIR/bench-out}"
mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
SUMMARY="$OUT_DIR/compare-$STAMP.txt"

{
  echo "=== SKV vs Raft sanity compare ($STAMP) ==="
  echo "host=$(hostname)  cwd=$REPO_ROOT"
  echo
} | tee "$SUMMARY"

echo ">>> Julang Sharded KV (put, ops=$BENCH_OPS clients=$BENCH_CLIENTS)"
SKV_LOG="$OUT_DIR/skv-$STAMP.log"
BENCH_OPS="$BENCH_OPS" BENCH_CLIENTS="$BENCH_CLIENTS" BENCH_MODE=put \
  "$SCRIPT_DIR/bench_tier0.sh" 2>&1 | tee "$SKV_LOG"
{
  echo "--- SKV ---"
  grep -E 'throughput_ok_rps=|latency_ms:|ops_total=' "$SKV_LOG" | head -20
  echo
} | tee -a "$SUMMARY"

if [[ "$SKIP_RAFT" != "1" ]]; then
  if [[ ! -x "$RAFT_SCRIPTS/bench_tier0.sh" ]]; then
    echo "warn: Raft bench missing at $RAFT_SCRIPTS" | tee -a "$SUMMARY"
  else
    echo ">>> Julang Raft JAR (append, ops=$RAFT_BENCH_OPS clients=$RAFT_BENCH_CLIENTS)"
    RAFT_LOG="$OUT_DIR/raft-$STAMP.log"
    BENCH_OPS="$RAFT_BENCH_OPS" BENCH_CLIENTS="$RAFT_BENCH_CLIENTS" BENCH_MODE=append \
      "$RAFT_SCRIPTS/bench_tier0.sh" 2>&1 | tee "$RAFT_LOG" || {
        echo "warn: Raft bench failed (see $RAFT_LOG)" | tee -a "$SUMMARY"
      }
    {
      echo "--- Julang Raft ---"
      grep -E 'throughput_ok_rps=|latency_ms:|ops_total=' "$RAFT_LOG" | head -20 || true
      echo
    } | tee -a "$SUMMARY"
  fi
fi

PGO_DIR="${PGO_RAFTKVS_DIR:-$REPO_ROOT/tools/pgo/systems/raftkvs}"
SKIP_PGO="${SKIP_PGO:-0}"
if [[ "$SKIP_PGO" == "1" ]]; then
  {
    echo "--- PGo raftkvs ---"
    echo "Skipped (SKIP_PGO=1)."
    echo
  } | tee -a "$SUMMARY"
elif [[ -x "$PGO_DIR/server" && -x "$PGO_DIR/client" ]]; then
  echo ">>> PGo raftkvs (put, ops=${PGO_BENCH_OPS:-$BENCH_OPS})"
  PGO_LOG="$OUT_DIR/pgo-$STAMP.log"
  PGO_RAFTKVS_DIR="$PGO_DIR" BENCH_OPS="${PGO_BENCH_OPS:-$BENCH_OPS}" \
    "$SCRIPT_DIR/bench_pgo_raftkvs.sh" 2>&1 | tee "$PGO_LOG" || {
      echo "warn: PGo bench failed (see $PGO_LOG)" | tee -a "$SUMMARY"
    }
  {
    echo "--- PGo raftkvs ---"
    grep -E 'throughput_ok_rps=|ops_total=|wall_seconds=' "$PGO_LOG" | head -20 || true
    echo
  } | tee -a "$SUMMARY"
else
  {
    echo "--- PGo raftkvs ---"
    echo "Skipped (no $PGO_DIR/server). Clone DistCompiler/pgo under tools/pgo and make build."
    echo "Or: BENCH_OPS=200 input/sharded_kv/scripts/bench_pgo_raftkvs.sh"
    echo
  } | tee -a "$SUMMARY"
fi

{
  echo "Caveats: SKV is ownership+redirect (no quorum commit). Raft append waits for"
  echo "leader election + replication. Different APIs/ops counts; use as a coarse sanity"
  echo "check that SKV is much faster, not as a paper figure."
  echo "See scripts/BASELINE.md for recorded numbers."
  echo
  echo "Summary written to $SUMMARY"
} | tee -a "$SUMMARY"
