#!/usr/bin/env bash
# Start one ShardNode process per URL in the cluster config (background).
#
# Usage: ./start_cluster.sh [configPath]
#
# Requires ShardNode binary (compile input/sharded_kv/sys.jul), or set SHARD_NODE_BIN.
# PIDs are written to .skv-pids next to the config file.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKV_DIR/../.." && pwd)"
CONFIG="${1:-$SKV_DIR/cluster.conf}"
PIDFILE="$(dirname "$CONFIG")/.skv-pids"
LOGDIR="$(dirname "$CONFIG")"

if [[ -n "${SHARD_NODE_BIN:-}" ]]; then
  BIN="$SHARD_NODE_BIN"
elif [[ -x "$SKV_DIR/ShardNode" ]]; then
  BIN="$SKV_DIR/ShardNode"
elif [[ -x "$REPO_ROOT/ShardNode" ]]; then
  BIN="$REPO_ROOT/ShardNode"
else
  echo "error: ShardNode not found; run input/sharded_kv/scripts/compile.sh or set SHARD_NODE_BIN" >&2
  exit 1
fi

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
if [[ "$N" -eq 0 ]]; then
  echo "error: no node URLs in $CONFIG" >&2
  exit 1
fi

CONFIG_ABS="$(cd "$(dirname "$CONFIG")" && pwd)/$(basename "$CONFIG")"

: > "$PIDFILE"
echo "Starting $N ShardNode(s) from $CONFIG_ABS using $BIN"

i=1
while [[ "$i" -le "$N" ]]; do
  log="$LOGDIR/skv-node-$i.log"
  "$BIN" "$CONFIG_ABS" "$i" >"$log" 2>&1 &
  pid=$!
  echo "$pid" >> "$PIDFILE"
  echo "  node $i (${URLS[$((i - 1))]}) pid=$pid log=$log"
  i=$((i + 1))
done

echo "PIDs written to $PIDFILE"
