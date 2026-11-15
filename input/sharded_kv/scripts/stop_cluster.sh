#!/usr/bin/env bash
# Kill ShardNode processes started by start_cluster.sh for a given config.
#
# Usage: ./stop_cluster.sh [configPath]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG="${1:-$SKV_DIR/cluster.conf}"
PIDFILE="$(dirname "$CONFIG")/.skv-pids"

if [[ -f "$PIDFILE" ]]; then
  echo "Stopping nodes listed in $PIDFILE"
  while IFS= read -r pid || [[ -n "$pid" ]]; do
    [[ -z "$pid" ]] && continue
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      echo "  killed pid $pid"
    else
      echo "  pid $pid already dead"
    fi
  done < "$PIDFILE"
  rm -f "$PIDFILE"
else
  echo "No pidfile at $PIDFILE; trying pkill fallback for ShardNode"
  pkill -f 'ShardNode' 2>/dev/null || true
fi

echo "Done."
