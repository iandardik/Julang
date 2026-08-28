#!/usr/bin/env bash
# Probe: leader should form and stay stable (not permanent NO_LEADER / hang).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RAFT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$RAFT_DIR"
./scripts/stop_cluster.sh >/dev/null 2>&1 || true
pkill -f 'RaftNode.jar' 2>/dev/null || true
sleep 1

./scripts/start_cluster.sh
for t in $(seq 1 20); do
  ok=0
  for p in 5001 5002 5003; do
    lsof -nP -iTCP:"$p" -sTCP:LISTEN >/dev/null 2>&1 && ok=$((ok + 1))
  done
  [[ "$ok" -eq 3 ]] && break
  sleep 1
done

echo "=== probe append on all nodes for 12s ==="
fail=0
saw_ok=0
for t in $(seq 0 12); do
  echo "--- t=${t}s ---"
  for p in 5001 5002 5003; do
    echo -n "  $p: "
    body="$(curl -sS -m 2 -X POST "http://127.0.0.1:$p/client/append" -d "probe-$t-$p" -w "|%{http_code}" 2>&1 || echo "FAIL|000")"
    echo "$body"
    if echo "$body" | grep -qE '\|200$|\|303$'; then
      saw_ok=1
    fi
  done
  if [[ -f .raft-pids ]]; then
    ps -o pid=,%cpu= -p "$(tr '\n' ',' < .raft-pids | sed 's/,$//')" 2>/dev/null | while read -r pid cpu; do
      echo "  cpu pid=$pid %cpu=$cpu"
    done
  fi
  # After t>=2, at least one node should accept or redirect (not all NO_LEADER).
  if [[ "$t" -ge 2 ]]; then
    all_nl=1
    for p in 5001 5002 5003; do
      b="$(curl -sS -m 2 -X POST "http://127.0.0.1:$p/client/append" -d "check-$t-$p" -w "|%{http_code}" 2>&1 || echo "FAIL|000")"
      if ! echo "$b" | grep -q 'NO_LEADER'; then
        all_nl=0
      fi
    done
    if [[ "$all_nl" -eq 1 ]]; then
      fail=1
      echo "REGRESSION: all nodes NO_LEADER at t=${t}s"
    fi
  fi
  sleep 1
done

./scripts/stop_cluster.sh >/dev/null 2>&1 || true
if [[ "$fail" -eq 1 ]] || [[ "$saw_ok" -eq 0 ]]; then
  echo "=== probe FAILED ==="
  exit 1
fi
echo "=== probe OK ==="
